package sergeylysyi.threescreens;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

import sergeylysyi.threescreens.note.ArrayNoteJson;
import sergeylysyi.threescreens.note.Note;
import sergeylysyi.threescreens.note.NoteListAdapter;
import sergeylysyi.threescreens.note.NoteSaver;

public class MainActivity extends AppCompatActivity {
    private static final int IMPORT_REQUEST_CODE = 10;
    private static final int EXPORT_REQUEST_CODE = 11;
    private static final int REQUEST_WRITE_STORAGE = 13;
    private List<Note> allNotes;
    private NoteListAdapter adapter;
    private NoteSaver saver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.deleteDatabase(NoteSaver.DB_NAME);
        saver = new NoteSaver(this);
        saver.examine();

        ListView lv = (ListView) findViewById(R.id.listView);

        allNotes = saver.getAllNotes();

        adapter = new NoteListAdapter(
                this,
                R.layout.layout_note,
                allNotes);
        lv.setAdapter(adapter);
        lv.setEmptyView(findViewById(R.id.empty));
    }

    public void editNote(Note note) {
        Intent intent = new Intent(this, EditActivity.class);
        fillIntentWithNoteInfo(intent, note, allNotes.indexOf(note));
        note.updateOpenDate();
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

    public void addNote(View view) {
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

    private void sortNotes(NoteSortFields byField, NoteSortOrder withOrder) {
        String column;
        String order;
        switch (byField) {
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
                throw new IllegalArgumentException("argument \"byField\" must be Enum");
        }
        switch (withOrder) {
            case ascending:
                order = NoteSaver.SORT_ORDER_ASCENDING;
                break;
            case descending:
                order = NoteSaver.SORT_ORDER_DESCENDING;
                break;
            default:
                throw new IllegalArgumentException("argument \"withOrder\" must be Enum");
        }
        allNotes.removeAll(allNotes);
        allNotes.addAll(saver.getAllSorted(column, order));
        adapter.notifyDataSetChanged();
    }

    private void reloadAllNotes() {
        allNotes.removeAll(allNotes);
        allNotes.addAll(saver.getAllNotes());
        saver.examine();
        adapter.notifyDataSetChanged();
    }

    private void pickFile() {
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            startActivityForResult(theIntent, IMPORT_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveToFile() {
        //TODO: allow folder choose and file name input
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            startActivityForResult(theIntent, EXPORT_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        note.setTitle(data.getStringExtra("header"));
                        note.setDescription(data.getStringExtra("body"));
                        note.setColor(data.getIntExtra("color", note.getColor()));
                        adapter.notifyDataSetChanged();
                    }
                    // open date changed so must save to database anyway
                    saver.insertOrUpdate(note);
                    saver.examine();
                    break;

                case IMPORT_REQUEST_CODE: {
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        System.out.println("LOAD FROM: " + theFilePath);
                        importNotesFromFile(theFilePath);
                    }
                    break;
                }
                case EXPORT_REQUEST_CODE: {
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        System.out.println("SAVE TO: " + theFilePath);
                        exportNotesToFile(theFilePath);
                    }
                    break;
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void exportNotesToFile(String filename) {
        if (!hasIOExternalPermission()) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            File f = new File(filename);
            System.out.println("Will write to: " + f.getAbsoluteFile());
            try {
                fos.write(toJson().getBytes());
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

    private String toJson() {
        Moshi moshi = new Moshi.Builder().build();
        Type listMyData = Types.newParameterizedType(List.class, ArrayNoteJson.NoteJson.class);
        JsonAdapter<List<ArrayNoteJson.NoteJson>> jsonAdapter = moshi.adapter(listMyData);
        return jsonAdapter.toJson(ArrayNoteJson.wrap(allNotes));
    }

    private void fromJson(String json) throws IOException, ParseException {
        Moshi moshi = new Moshi.Builder().build();
        Type listMyData = Types.newParameterizedType(List.class, ArrayNoteJson.NoteJson.class);
        JsonAdapter<List<ArrayNoteJson.NoteJson>> jsonAdapter = moshi.adapter(listMyData);
        List<ArrayNoteJson.NoteJson> notesJson = jsonAdapter.fromJson(json);
        allNotes.removeAll(allNotes);
        allNotes.addAll(ArrayNoteJson.unwrap(notesJson));
    }

    private void importNotesFromFile(String filename) {
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

            fromJson(fileString);
            saver.repopulateWith(allNotes);
            saver.examine();
            adapter.notifyDataSetChanged();
            //TODO: cut off beginning of filename unexpected to user
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

//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putParcelableArrayList("allNotes", allNotes);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                reloadAllNotes();
                break;
            case R.id.action_export:
                saveToFile();
                break;
            case R.id.action_import:
                pickFile();
                break;
            case R.id.sort_title:
                sortNotes(NoteSortFields.title, NoteSortOrder.ascending);
                break;
            case R.id.sort_created:
                sortNotes(NoteSortFields.created, NoteSortOrder.ascending);
                break;
            case R.id.sort_edited:
                sortNotes(NoteSortFields.edited, NoteSortOrder.ascending);
                break;
            case R.id.sort_viewed:
                sortNotes(NoteSortFields.viewed, NoteSortOrder.ascending);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        saver.close();
        super.onDestroy();
    }

    private enum NoteSortFields {title, created, edited, viewed}

    private enum NoteSortOrder {ascending, descending}

}
