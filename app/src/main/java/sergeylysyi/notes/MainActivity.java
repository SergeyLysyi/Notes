package sergeylysyi.notes;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sergeylysyi.notes.Dialogs.DialogInvoker;
import sergeylysyi.notes.note.Note;
import sergeylysyi.notes.note.NoteJsonImportExport;
import sergeylysyi.notes.note.NoteListAdapter;
import sergeylysyi.notes.note.NoteSaver;
import sergeylysyi.notes.note.NoteSaverService;
import sergeylysyi.notes.note.RemoteNotes.Errors;
import sergeylysyi.notes.note.RemoteNotes.OnError;
import sergeylysyi.notes.note.RemoteNotes.OnSuccess;
import sergeylysyi.notes.note.RemoteNotes.RESTClient;
import sergeylysyi.notes.note.RemoteNotes.User;

import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_IS_CHANGED;

public class MainActivity extends AppCompatActivity implements
        DialogInvoker.ResultListener,
        ServiceConnection,
        Handler.Callback {
    public static final String DEFAULT_NOTE_TITLE = "Note";
    public static final String DEFAULT_NOTE_DESCRIPTION = "Hello";
    public static final String TAG = "MainActivity";
    public static final String KEY_NOTE_IN_CHANGING_STATE = "note in changing state";
    public static final String KEY_SEARCH_STRINGS = "search strings";
    public static final String DEFAULT_IMPORT_FILE_PATH = "itemlist.ili";
    public static final String DEFAULT_EXPORT_FILE_PATH = "itemlist.ili";
    public static final int FILL_TOTAL_AMOUNT = 100000;
    public static final int FILL_PACK_AMOUNT = 1000;
    public static final int[] COLORS_FOR_GENERATED = new int[]{Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK, Color.GREEN};
    public static final String TITLE_PREFIX_FOR_GENERATED = "Generated Note ";
    public static final String DESCRIPTION_PREFIX_FOR_GENERATED = "Generated Description ";
    public static final int NOTIFICATION_IMPORT_EXPORT_ID = 101;
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
    private NoteListAdapter adapter;
    private NoteSaverService.LocalSaver saver;
    private NoteSaverService.LocalJson noteFileOperator;
    private DialogInvoker dialogInvoker;
    private FiltersHolder filtersHolder;
    private boolean search_on = false;
    private MenuItem searchMenuItem = null;
    private Note noteInChangingState;
    private String searchInTitle;
    private String searchInDescription;
    private MessageHandlerImport messageHandlerImport = new MessageHandlerImport();
    private MessageHandlerExport messageHandlerExport = new MessageHandlerExport();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase("Notes.db");

        startService(new Intent(this, NoteSaverService.class));

        dialogInvoker = new DialogInvoker(this);

        ListView lv = (ListView) findViewById(R.id.listView);

        adapter = new NoteListAdapter(
                this,
                R.layout.layout_note);
        lv.setAdapter(adapter);
        lv.setEmptyView(findViewById(R.id.empty));

        if (savedInstanceState != null) {
            Note note = (Note) savedInstanceState.get(KEY_NOTE_IN_CHANGING_STATE);
            if (note != null) {
                noteInChangingState = note;
            }
            String[] searchStrings = savedInstanceState.getStringArray(KEY_SEARCH_STRINGS);
            if (searchStrings != null) {
                searchInTitle = searchStrings[0];
                searchInDescription = searchStrings[1];
                if (searchInTitle != null || searchInDescription != null) {
                    enableSearch();
                }
            }
        }

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
    }

    public void launchEdit(Note note) {
        noteInChangingState = note;
        final Intent intent = new Intent(this, EditActivity.class);
        fillIntentWithNoteInfo(intent, note);
        note.updateOpenDate();
        // save new open date for note to db
        saver.insertOrUpdateWithCallback(note, null, null);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    public void launchDelete(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_delete_title)
                .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saver.deleteNoteWithCallback(note, new Handler(getMainLooper()),
                                new NoteSaverService.OnChangeNotesCallback() {
                                    @Override
                                    public void onChangeNotes(long leftToAdd) {
                                        updateNotesFromSaver();
                                    }
                                });
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

    public void editNote(final Note originalNote, final Note changedNote) {
        if (!originalNote.getTitle().equals(changedNote.getTitle()))
            originalNote.setTitle(changedNote.getTitle());
        if (!originalNote.getDescription().equals(changedNote.getDescription()))
            originalNote.setDescription(changedNote.getDescription());
        if (!Integer.valueOf(originalNote.getColor()).equals(changedNote.getColor()))
            originalNote.setColor(changedNote.getColor());
        if (!originalNote.getImageUrl().equals(changedNote.getImageUrl()))
            originalNote.setImageURL(changedNote.getImageUrl());
        saver.insertOrUpdateWithCallback(originalNote, new Handler(getMainLooper()), new NoteSaverService.OnChangeNotesCallback() {
            @Override
            public void onChangeNotes(long leftToAdd) {
                updateNotesFromSaver();
            }
        });
    }

    public void launchAdd(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        int noteIndex = adapter.getCount();
        Note note = new Note(getDefaultNoteTitleTextWithIndex(noteIndex),
                DEFAULT_NOTE_DESCRIPTION,
                getResources().getColor(R.color.colorPrimary));
        noteInChangingState = note;
        fillIntentWithNoteInfo(intent, note);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    private String getDefaultNoteTitleTextWithIndex(int index) {
        return DEFAULT_NOTE_TITLE + (index + 1);
    }

    private void fillIntentWithNoteInfo(Intent intent, Note note) {
        intent.putExtra(INTENT_KEY_NOTE, note);
    }

    private void resetFilterAndUpdate() {
        filtersHolder.reset();
        clearSearch();
        updateNotesFromSaver();
    }

    private void updateNotesFromSaver() {
        NoteSaverService.LocalSaver.Query query = saver.new Query();
        updateNotesByQuery(query);
    }

    private void updateNotesByQuery(NoteSaverService.LocalSaver.Query query) {
        query.fromFilter(filtersHolder.getCurrentFilterCopy()).withSubstring(searchInTitle, searchInDescription);
        //TODO: get cursor async
        adapter.updateData(query.getCursor());
        Log.i(TAG, "updateNotesByQuery: adapter.getCount() = " + adapter.getCount());
    }

    private void addNotesFromList(final List<Note> noteList) {
        saver.insertOrUpdateManyWithCallback(noteList, new Handler(getMainLooper()), new NoteSaverService.OnChangeNotesCallback() {
            @Override
            public void onChangeNotes(long leftToAdd) {
                updateNotesFromSaver();
            }
        });
    }

    private void searchSubstring(String inTitle, String inDescription) {
        searchInTitle = inTitle;
        searchInDescription = inDescription;
        updateNotesFromSaver();
    }

    private void launchPickFile() {
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            try {
                startActivityForResult(theIntent, IMPORT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.import_no_file_manager, Toast.LENGTH_LONG).show();
                theIntent = new Intent();
                theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(DEFAULT_IMPORT_FILE_PATH)));
                onActivityResult(IMPORT_REQUEST_CODE, RESULT_OK, theIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void launchSaveFile() {
        //TODO: allow folder choose and file name input
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        try {
            try {
                startActivityForResult(theIntent, EXPORT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.export_no_file_manager, Toast.LENGTH_LONG).show();
                theIntent = new Intent();
                theIntent.setData(Uri.fromFile(Environment.getExternalStoragePublicDirectory(DEFAULT_EXPORT_FILE_PATH)));
                onActivityResult(EXPORT_REQUEST_CODE, RESULT_OK, theIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportNotesToFile(String filename) {
        if (!hasIOExternalPermission()) {
            return;
        }
        noteFileOperator.exportToFile(filename, new Messenger(new Handler(this)));
    }

    private void importNotesFromFile(String filename) {
        if (!hasIOExternalPermission()) {
            return;
        }
        noteFileOperator.importFromFile(filename, new Messenger(new Handler(this)));
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
                    if (data.getBooleanExtra(INTENT_KEY_NOTE_IS_CHANGED, false)) {
                        final Note oldNote = noteInChangingState;
                        final Note newNote = data.getParcelableExtra(INTENT_KEY_NOTE);
                        // service with saver will be bound at end of queue and might not exist right now
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                editNote(oldNote, newNote);
                            }
                        });
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
        searchMenuItem = menu.findItem(R.id.action_search);
        updateSearchIcon();
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
                if (!search_on) {
                    enableSearch();
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
        Random r = new Random();
        for (int i = 1; i <= FILL_TOTAL_AMOUNT; i++) {
            final List<Note> list = new ArrayList<>(FILL_PACK_AMOUNT);
            for (int j = 1; j <= FILL_PACK_AMOUNT; j++) {
                list.add(new Note(
                        getGeneratedTitle(i),
                        getGeneratedDescription(i),
                        getRandomColor(r)));
                i++;
            }
            Log.i(TAG, "fillAndUpload: launched to index " + i);
            addNotesFromList(list);
        }
    }

    private String getGeneratedTitle(int number) {
        return TITLE_PREFIX_FOR_GENERATED + number;
    }

    private String getGeneratedDescription(int number) {
        return DESCRIPTION_PREFIX_FOR_GENERATED + number;
    }

    private int getRandomColor(Random r) {
        return COLORS_FOR_GENERATED[r.nextInt(COLORS_FOR_GENERATED.length)];
    }

    private void clearSearch() {
        search_on = false;
        searchInTitle = null;
        searchInDescription = null;
        updateSearchIcon();
    }

    private void enableSearch() {
        search_on = true;
        updateSearchIcon();
    }

    private void updateSearchIcon() {
        final Drawable searchIcon;
        if (search_on) {
            searchIcon = getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            searchIcon = getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        if (searchMenuItem != null) {
            searchMenuItem.setIcon(searchIcon);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_NOTE_IN_CHANGING_STATE, noteInChangingState);
        outState.putStringArray(KEY_SEARCH_STRINGS, new String[]{searchInTitle, searchInDescription});
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, NoteSaverService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_VERSION, SHARED_PREFERENCES_VERSION);
        editor.putBoolean(KEY_FILTER_SAVED, true);
        filtersHolder.storeToPreferences(prefs);
        editor.apply();
        unbindService(this);
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
        clearSearch();
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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(getLocalClassName(), "Service bound");
        NoteSaverService.LocalBinder binder = ((NoteSaverService.LocalBinder) service);
        saver = binder.getSaver();
        saver.setAllFinishedCallback(new Handler(), new NoteSaverService.OnChangeNotesCallback() {
            @Override
            public void onChangeNotes(long leftToAdd) {
                updateNotesFromSaver();
            }
        });
        noteFileOperator = binder.getFileOperator();
        updateNotesFromSaver();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(getLocalClassName(), "Service unbound");
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (noteFileOperator == null || !noteFileOperator.isMessageFromJsonSender(msg))
            return false;
        noteFileOperator.translateMessage(
                msg,
                messageHandlerImport,
                messageHandlerImport,
                messageHandlerExport,
                messageHandlerExport);
        return true;
    }

    private class MessageHandlerImport implements
            NoteJsonImportExport.ProgressCallback,
            NoteJsonImportExport.FinishCallback {

        @Override
        public void onProgress(double percentDone) {
            updateNotesFromSaver();
            Notification not = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_import_progress, percentDone)).build();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }

        @Override
        public void onFinish(boolean isSucceed) {
            String result;
            if (isSucceed) {
                updateNotesFromSaver();
                result = getString(R.string.notification_success);
            } else {
                result = getString(R.string.notification_failed);
            }
            Notification not = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_import_finish, result)).build();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }
    }

    private class MessageHandlerExport implements
            NoteJsonImportExport.ProgressCallback,
            NoteJsonImportExport.FinishCallback {

        @Override
        public void onProgress(double percentDone) {
            Notification not = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_export_progress, percentDone)).build();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }

        @Override
        public void onFinish(boolean isSucceed) {
            String result;
            if (isSucceed)
                result = getString(R.string.notification_success);
            else
                result = getString(R.string.notification_failed);
            Notification not = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_export_finish, result)).build();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }
    }
}
