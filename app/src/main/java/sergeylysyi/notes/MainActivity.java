package sergeylysyi.notes;

import android.Manifest;
import android.content.ActivityNotFoundException;
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
import android.util.Log;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import sergeylysyi.notes.Dialogs.DialogInvoker;
import sergeylysyi.notes.note.ArrayNoteJson;
import sergeylysyi.notes.note.AsyncNoteSaver;
import sergeylysyi.notes.note.Note;
import sergeylysyi.notes.note.NoteListAdapter;
import sergeylysyi.notes.note.NoteSaver;

import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_COLOR;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_DESCRIPTION;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_INDEX;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_IS_CHANGED;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_TITLE;

public class MainActivity extends AppCompatActivity implements DialogInvoker.ResultListener {
    public static final String DEFAULT_NOTE_TITLE = "Note";
    public static final String DEFAULT_NOTE_DESCRIPTION = "Hello";
    public static final String CHARSET_DEFAULT = "UTF-8";
    private static final int IMPORT_REQUEST_CODE = 10;
    private static final int EXPORT_REQUEST_CODE = 11;
    private static final int REQUEST_WRITE_STORAGE = 13;
    private static final String SHARED_PREFERENCES_VERSION = "1";
    private static final String KEY_PREFIX = FiltersHolder.class.getName().concat("_");
    private static final String KEY_VERSION = KEY_PREFIX.concat("version");
    private static final String KEY_FILTER_SAVED = KEY_PREFIX.concat("filter_saved");
    private final NoteSaver.NoteSortOrder defaultSortOrderPreference = NoteSaver.NoteSortOrder.descending;
    private final NoteSaver.NoteSortField defaultSortFieldPreference = NoteSaver.NoteSortField.created;
    private final NoteSaver.NoteDateField defaultDateFieldPreference = NoteSaver.NoteDateField.edited;
    private List<Note> allNotes = new ArrayList<>();
    private NoteListAdapter adapter;
    private AsyncNoteSaver saver;
    private DialogInvoker dialogInvoker;
    private FiltersHolder filtersHolder;
    private boolean search_on = false;
    private MenuItem searchMenuItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase("Notes.db");

        //TODO: rewrite methods for async;
        saver = new AsyncNoteSaver(this);

        dialogInvoker = new DialogInvoker(this);

        ListView lv = (ListView) findViewById(R.id.listView);

        adapter = new NoteListAdapter(
                this,
                R.layout.layout_note,
                allNotes);
        lv.setAdapter(adapter);
        lv.setEmptyView(findViewById(R.id.empty));

        SharedPreferences settings = getPreferences(MODE_PRIVATE);

        String version = settings.getString(KEY_VERSION, null);
        boolean filterSaved = settings.getBoolean(KEY_FILTER_SAVED, false);
        if (filterSaved) {
            filtersHolder = FiltersHolder.fromSettings(settings);
        } else {
            filtersHolder = new FiltersHolder(
                    defaultSortFieldPreference,
                    defaultSortOrderPreference,
                    defaultDateFieldPreference
            );
        }

        resetFilterAndUpdate();
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
        builder.setMessage(R.string.dialog_delete_title)
                .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saver.deleteNote(note);
                        adapter.remove(note);
                    }
                })
                .setNegativeButton(R.string.dialog_negative_button, new DialogInterface.OnClickListener() {
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
        updateNotesFromSaver();
    }

    public void launchAdd(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        int noteIndex = allNotes.size();
        fillIntentWithNoteInfo(
                intent,
                new Note(getDefaultNoteTitleTextWithIndex(noteIndex),
                        DEFAULT_NOTE_DESCRIPTION,
                        getResources().getColor(R.color.colorPrimary)),
                noteIndex);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    private String getDefaultNoteTitleTextWithIndex(int index) {
        return DEFAULT_NOTE_TITLE + (index + 1);
    }

    private void fillIntentWithNoteInfo(Intent intent, Note note, int noteIndex) {
        intent.putExtra(INTENT_KEY_NOTE_TITLE, note.getTitle());
        intent.putExtra(INTENT_KEY_NOTE_DESCRIPTION, note.getDescription());
        intent.putExtra(INTENT_KEY_NOTE_COLOR, note.getColor());
        intent.putExtra(INTENT_KEY_NOTE_INDEX, noteIndex);
    }

    private void resetFilterAndUpdate() {
        filtersHolder.reset();
        clearSearch();
        updateNotesFromSaver();
    }

    private void updateNotesFromSaver() {
        NoteSaver.Query query = saver.new Query().fromFilter(filtersHolder.getCurrentFilterCopy());
        updateNotesByQuery(query);
    }

    private void updateNotesByQuery(NoteSaver.Query query) {
        allNotes.removeAll(allNotes);
        allNotes.addAll(query.get());
        adapter.notifyDataSetChanged();
    }

    private void updateNotesFromList(List<Note> noteList) {
        allNotes.removeAll(allNotes);
        allNotes.addAll(noteList);
        saver.repopulateWith(allNotes);
        adapter.notifyDataSetChanged();
    }

    private void addNotesFromList(final List<Note> noteList) {
        allNotes.addAll(noteList);
        for (Note note : noteList) {
            saver.insertOrUpdateWithCallback(note, new AsyncNoteSaver.OnPostExecute() {
                @Override
                public void onPostExecute() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void searchSubstring(String inTitle, String inDescription) {
        updateNotesByQuery(saver.new Query().withSubstring(inTitle, inDescription));
    }

    private void launchPickFile() {
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            startActivityForResult(theIntent, IMPORT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.import_no_file_manager, Toast.LENGTH_LONG).show();
            e.printStackTrace();
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
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.export_no_file_manager, Toast.LENGTH_LONG).show();
            e.printStackTrace();
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
            try {
                fos.write(notesToJson().getBytes());
                Toast.makeText(this, getString(R.string.export_success_toast_string_formatted, filename),
                        Toast.LENGTH_LONG).show();
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.export_error_toast_string_formatted, filename),
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
        updateNotesFromList(ArrayNoteJson.unwrap(notesJson));
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
                    fileString += new String(bytes, Charset.forName(CHARSET_DEFAULT));
                }
            } finally {
                fis.close();
            }
            notesFromJson(fileString);
            Toast.makeText(this, getString(R.string.import_success_toast_string_formatted, filename),
                    Toast.LENGTH_LONG).show();
        } catch (JsonEncodingException | JsonDataException | ParseException e) {
            Toast.makeText(this, getString(R.string.import_parse_error_toast_string_formatted, filename),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.import_access_error_toast_string_formatted, filename),
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
                    int index = data.getIntExtra(INTENT_KEY_NOTE_INDEX, -1);
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
                    if (data.getBooleanExtra(INTENT_KEY_NOTE_IS_CHANGED, false)) {
                        editNote(note,
                                data.getStringExtra(INTENT_KEY_NOTE_TITLE),
                                data.getStringExtra(INTENT_KEY_NOTE_DESCRIPTION),
                                data.getIntExtra(INTENT_KEY_NOTE_COLOR, note.getColor()));
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
                    Toast.makeText(this, R.string.permission_toast_success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.permission_toast_denied, Toast.LENGTH_LONG).show();
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
                resetFilterAndUpdate();
                break;
            case R.id.action_export:
                launchSaveFile();
                break;
            case R.id.action_import:
                launchPickFile();
                break;
            case R.id.action_sort:
                NoteSaver.QueryFilter queryFilter = filtersHolder.getCurrentFilterCopy();
                dialogInvoker.sortDialog(queryFilter.sortField, queryFilter.sortOrder, this);
                break;
            case R.id.action_filter:
                dialogInvoker.filterDialog(filtersHolder.getCurrentFilterCopy(), this);
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
                    clearSearch();
                    updateNotesFromSaver();
                }
                break;
            case R.id.action_fill:
                fillAndUpload();
                break;
            case R.id.action_manage_filters:
                dialogInvoker.manageFiltersDialog(filtersHolder.getFilterNames(), this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fillAndUpload() {
//        int total = 100000;
//        int count = 0;
//        for (int i = 0; i < 100; i++) {
//            final List<Note> list = new ArrayList<>(total / 100);
//            for (int j = 0; j < total / 100; j++) {
//                count++;
//                list.add(new Note("Generated Note " + (count), "Generated Description " + (count), Color.WHITE));
//            }
//            System.out.println(i + "%");
//            addNotesFromList(list);
//        }
//        System.out.println("ALL LAUNCHED");

        final List<Note> list = new ArrayList<>(100);
        for (int j = 0; j < 100; j++) {
            list.add(new Note("Generated Note " + (j), "Generated Description " + (j), Color.WHITE));
        }
        addNotesFromList(list);
    }

    private void clearSearch() {
        if (search_on) {
            search_on = false;
            searchMenuItem.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_search));
        }
    }

    @Override
    protected void onStop() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_VERSION, SHARED_PREFERENCES_VERSION);
        editor.putBoolean(KEY_FILTER_SAVED, true);
        filtersHolder.storeToPreferences(prefs);
        editor.apply();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saver.close();
        super.onDestroy();
    }

    @Override
    public void onSortDialogResult(NoteSaver.QueryFilter result) {
        filtersHolder.setCurrentFilterFrom(result);
        updateNotesFromSaver();
    }

    @Override
    public void onFilterDialogResult(NoteSaver.QueryFilter result) {
        filtersHolder.setCurrentFilterFrom(result);
        updateNotesFromSaver();
    }

    @Override
    public void onSearchDialogResult(DialogInvoker.SearchDialogResult result) {
        if (!(result.title == null && result.description == null)) {
            searchSubstring(result.title, result.description);
        } else {
            onSearchCancel();
        }
    }

    @Override
    public void onSearchCancel() {
        search_on = false;
        searchMenuItem.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_search));
    }

    @Override
    public void onEditFilterEntries(int[] deletedEntriesIndexes) {
        filtersHolder.remove(deletedEntriesIndexes);
    }

    @Override
    public void onAddFilterEntry(String entryName) {
        filtersHolder.add(entryName);
    }

    @Override
    public void onApplyFilterEntry(String entryName) {
        filtersHolder.apply(entryName);
        updateNotesFromSaver();
    }

}
