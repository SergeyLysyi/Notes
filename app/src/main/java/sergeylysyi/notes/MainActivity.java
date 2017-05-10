package sergeylysyi.notes;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import sergeylysyi.notes.note.ArrayNoteJson;
import sergeylysyi.notes.note.DialogInvoker;
import sergeylysyi.notes.note.Note;
import sergeylysyi.notes.note.NoteListAdapter;
import sergeylysyi.notes.note.NoteSaver;

public class MainActivity extends AppCompatActivity implements DialogInvoker.ResultListener {
    private static final int IMPORT_REQUEST_CODE = 10;
    private static final int EXPORT_REQUEST_CODE = 11;
    private static final int REQUEST_WRITE_STORAGE = 13;

    private List<Note> allNotes = new ArrayList<>();
    private NoteListAdapter adapter;
    private NoteSaver saver;

    private DialogInvoker dialogInvoker;


    private NoteSortOrder defaultSortOrderPreference = NoteSortOrder.descending;
    private NoteSortField defaultSortFieldPreference = NoteSortField.created;
    private NoteSortOrder sortOrderPreference = defaultSortOrderPreference;
    private NoteSortField sortFieldPreference = defaultSortFieldPreference;

    private boolean search_on = false;
    private MenuItem searchMenuItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase("Notes.db");
        saver = new NoteSaver(this);

        dialogInvoker = new DialogInvoker(this);

        ListView lv = (ListView) findViewById(R.id.listView);

        adapter = new NoteListAdapter(
                this,
                R.layout.layout_note,
                allNotes);
        lv.setAdapter(adapter);
        lv.setEmptyView(findViewById(R.id.empty));

        SharedPreferences settings = getPreferences(MODE_PRIVATE);

        int prefOrderOrdinal = settings.getInt("sort_preferred_order", -1);
        int prefFieldOrdinal = settings.getInt("sort_preferred_field", -1);
        if (prefOrderOrdinal > -1) {
            sortOrderPreference = NoteSortOrder.values()[prefOrderOrdinal];
        }
        if (prefFieldOrdinal > -1) {
            sortFieldPreference = NoteSortField.values()[prefFieldOrdinal];
        }

        reloadNotes();
    }

    public void launchEdit(Note note) {
        Intent intent = new Intent(this, EditActivity.class);
        fillIntentWithNoteInfo(intent, note, allNotes.indexOf(note));
        note.updateOpenDate();
        // save new open date for note to db
        saver.insertOrUpdate(note);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    public void deleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Delete note ?")
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saver.deleteNote(note);
                        adapter.remove(note);
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        builder.create().show();
    }

    public void editNote(final Note note, String editedTitle, String editedDescription, int editedColor) {
        if (!note.getTitle().equals(editedTitle))
            note.setTitle(editedTitle);
        if (!note.getDescription().equals(editedDescription))
            note.setDescription(editedDescription);
        if (!Integer.valueOf(note.getColor()).equals(editedColor))
            note.setColor(editedColor);
        saver.insertOrUpdate(note);
        reloadNotes();
    }

    public void launchAdd(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        int noteIndex = allNotes.size();
        fillIntentWithNoteInfo(
                intent,
                new Note("Note " + (noteIndex + 1),
                        "Hello",
                        getResources().getColor(R.color.colorPrimary)),
                noteIndex);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    private void fillIntentWithNoteInfo(Intent intent, Note note, int noteIndex) {
        intent.putExtra("header", note.getTitle());
        intent.putExtra("body", note.getDescription());
        intent.putExtra("color", note.getColor());
        intent.putExtra("index", noteIndex);
    }

    private void reloadNotes() {
        DialogInvoker.SortDialogResult reloadQuery = new DialogInvoker.SortDialogResult();
        reloadQuery.order = sortOrderPreference;
        reloadQuery.field = sortFieldPreference;
        onSortDialogResult(reloadQuery);
    }

    private void updateNotesByQuery(NoteSaver.Query query) {
        allNotes.removeAll(allNotes);
        allNotes.addAll(query.get());
        adapter.notifyDataSetChanged();
    }

    private void updateNotesFromByList(List<Note> noteList) {
        allNotes.removeAll(allNotes);
        allNotes.addAll(noteList);
        saver.repopulateWith(allNotes);
        adapter.notifyDataSetChanged();
    }

    private void launchPickFile() {
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            startActivityForResult(theIntent, IMPORT_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void launchSaveFile() {
        //TODO: allow folder choose and file name input
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            startActivityForResult(theIntent, EXPORT_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportNotesToFile(String filename) {
        if (!hasIOExternalPermission()) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            File f = new File(filename);
            try {
                fos.write(notesToJson().getBytes());
                Toast.makeText(this, String.format("Notes exported to %s", filename),
                        Toast.LENGTH_LONG).show();
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, String.format("Export to file %s failed", filename),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String notesToJson() {
        Moshi moshi = new Moshi.Builder().build();
        Type listMyData = Types.newParameterizedType(List.class, ArrayNoteJson.NoteJson.class);
        JsonAdapter<List<ArrayNoteJson.NoteJson>> jsonAdapter = moshi.adapter(listMyData);
        return jsonAdapter.toJson(ArrayNoteJson.wrap(allNotes));
    }

    private void notesFromJson(String json) throws IOException, ParseException {
        Moshi moshi = new Moshi.Builder().build();
        Type listMyData = Types.newParameterizedType(List.class, ArrayNoteJson.NoteJson.class);
        JsonAdapter<List<ArrayNoteJson.NoteJson>> jsonAdapter = moshi.adapter(listMyData);
        List<ArrayNoteJson.NoteJson> notesJson = jsonAdapter.fromJson(json);
        updateNotesFromByList(ArrayNoteJson.unwrap(notesJson));
    }

    private void importNotesFromFile(String filename) {
        //TODO: for toasts cut off beginning of filename unexpected to user
        if (!hasIOExternalPermission()) {
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(filename);
            String fileString = "";

            byte[] bytes = new byte[fis.available()];
            try {
                int bytesRead = fis.read(bytes);
                while (bytesRead > 0) {
                    bytesRead = fis.read(bytes);
                    fileString += new String(bytes, Charset.forName("UTF-8"));
                }
            } finally {
                fis.close();
            }
            notesFromJson(fileString);
            Toast.makeText(this, String.format("Notes imported from %s", filename),
                    Toast.LENGTH_LONG).show();
        } catch (JsonEncodingException | JsonDataException | ParseException e) {
            Toast.makeText(this, String.format("Can't parse file %s \n, is that really json with notes ?", filename),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, String.format("Can't open file %s", filename),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private boolean hasIOExternalPermission() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        return hasPermission;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case EditActivity.EDIT_NOTE:
                    int index = data.getIntExtra("index", -1);
                    Note note;

                    try {
                        note = allNotes.get(index);
                    } catch (IndexOutOfBoundsException ex) {
                        // if new note was created
                        if (index == allNotes.size()) {
                            note = new Note();
                            allNotes.add(note);
                        } else {
                            throw ex;
                        }
                    }
                    if (data.getBooleanExtra("isChanged", false)) {
                        editNote(note,
                                data.getStringExtra("header"),
                                data.getStringExtra("body"),
                                data.getIntExtra("color", note.getColor()));
                    }
                    break;

                case IMPORT_REQUEST_CODE: {
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        importNotesFromFile(theFilePath);
                    }
                    break;
                }
                case EXPORT_REQUEST_CODE: {
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        exportNotesToFile(theFilePath);
                    }
                    break;
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO: catch that and to what app wanted to do
                    Toast.makeText(this, "Thank you! Tap that button again, it should work now", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "I need this permission to import/export files", Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                sortFieldPreference = defaultSortFieldPreference;
                sortOrderPreference = defaultSortOrderPreference;
                reloadNotes();
                break;
            case R.id.action_export:
                launchSaveFile();
                break;
            case R.id.action_import:
                launchPickFile();
                break;
            case R.id.action_sort:
                dialogInvoker.sortDialog(sortOrderPreference, sortFieldPreference, this);
                break;
            case R.id.action_filter:
                dialogInvoker.filterDialog(this);
                break;
            case R.id.action_search:
                if (searchMenuItem == null) {
                    searchMenuItem = item;
                }
                if (!search_on) {
                    search_on = true;
                    searchMenuItem.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
                    dialogInvoker.searchDialog(this);
                } else {
                    // clear search props here
                    search_on = false;
                    searchMenuItem.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_search));
                    reloadNotes();
                }
                break;
            case R.id.action_add_many:
                //TODO: add more randomness ?
                for (int i = 0; i < 100000; i++) {
                    Note note = new Note();
                    allNotes.add(note);
                    editNote(note,
                            "Generated Note " + i,
                            "Body of \"Generated note " + i + "\"",
                            Color.WHITE);
                    if (i%100 == 0){
                        System.out.println("Number " + i + " generated...");
                    }
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("prefs_ver", 1);
        editor.putInt("sort_preferred_order", sortOrderPreference.ordinal());
        editor.putInt("sort_preferred_field", sortFieldPreference.ordinal());
        editor.apply();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saver.close();
        super.onDestroy();
    }

    @Override
    public void onSortDialogResult(DialogInvoker.SortDialogResult result) {
        String column;
        String order;
        switch (result.field) {
            case title:
                column = NoteSaver.COLUMN_TITLE;
                break;
            case created:
                column = NoteSaver.COLUMN_CREATED;
                break;
            case edited:
                column = NoteSaver.COLUMN_EDITED;
                break;
            case viewed:
                column = NoteSaver.COLUMN_VIEWED;
                break;
            default:
                throw new IllegalArgumentException("no matching case for argument \"byField\"");
        }
        switch (result.order) {
            case ascending:
                order = NoteSaver.SORT_ORDER_ASCENDING;
                break;
            case descending:
                order = NoteSaver.SORT_ORDER_DESCENDING;
                break;
            default:
                throw new IllegalArgumentException("no matching case for argument \"withOrder\" ");
        }
        sortFieldPreference = result.field;
        sortOrderPreference = result.order;
        updateNotesByQuery(saver.new Query().sorted(column, order));
    }

    @Override
    public void onFilterDialogResult(DialogInvoker.FilterDialogResult result) {
        String fieldRequest;
        switch (result.dateField) {
            case created:
                fieldRequest = NoteSaver.COLUMN_CREATED;
                break;
            case edited:
                fieldRequest = NoteSaver.COLUMN_EDITED;
                break;
            case viewed:
                fieldRequest = NoteSaver.COLUMN_VIEWED;
                break;
            default:
                throw new IllegalArgumentException("no matching case for argument \"field\"");
        }
        updateNotesByQuery(saver.new Query().betweenDatesOf(fieldRequest, result.after, result.before));
    }

    @Override
    public void onSearchDialogResult(DialogInvoker.SearchDialogResult result) {
        if (!(result.title == null && result.description == null)) {
            updateNotesByQuery(saver.new Query().withSubstring(result.title, result.description));
        } else {
            onSearchCancel();
        }
    }

    @Override
    public void onSearchCancel() {
        search_on = false;
        searchMenuItem.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_search));
    }

    public enum NoteSortField {title, created, edited, viewed}

    public enum NoteSortOrder {ascending, descending}

    public enum NoteDateField {created, edited, viewed}
}
