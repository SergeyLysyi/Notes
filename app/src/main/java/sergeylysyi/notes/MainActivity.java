package sergeylysyi.notes;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sergeylysyi.notes.Dialogs.DialogInvoker;
import sergeylysyi.notes.note.Note;
import sergeylysyi.notes.note.NoteSaver;
import sergeylysyi.notes.note.NoteSaverService;
import sergeylysyi.notes.note.NoteStorage;
import sergeylysyi.notes.note.RemoteNotes.User;

import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE;
import static sergeylysyi.notes.EditActivity.INTENT_KEY_NOTE_IS_CHANGED;
import static sergeylysyi.notes.UserActivity.KEY_USER_ID;
import static sergeylysyi.notes.UserActivity.KEY_USER_NAME;
import static sergeylysyi.notes.UserActivity.REQUEST_USER;

public class MainActivity extends AppCompatActivity implements DialogInvoker.ResultListener, ServiceConnection {
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
    public static final String DEFAULT_USER_NAME = "DefaultUser";
    public static final int DEFAULT_USER_ID = 0;
    private static final int IMPORT_REQUEST_CODE = 10;
    private static final int EXPORT_REQUEST_CODE = 11;
    private static final int REQUEST_WRITE_STORAGE = 13;
    private static final String SHARED_PREFERENCES_VERSION = "1";
    private static final String KEY_PREFIX = FiltersHolder.class.getName().concat("_");
    private static final String KEY_VERSION = KEY_PREFIX.concat("version");
    private static final String KEY_FILTER_SAVED = KEY_PREFIX.concat("filter_saved");
    private static final String KEY_LAST_USER_NAME = KEY_PREFIX.concat("last_user_name");
    private static final String KEY_LAST_USER_ID = KEY_PREFIX.concat("last_user_id");
    public static final String ANDROID_NET_CONN_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private final NoteSaver.NoteSortOrder defaultSortOrderPreference = NoteSaver.NoteSortOrder.descending;
    private final NoteSaver.NoteSortField defaultSortFieldPreference = NoteSaver.NoteSortField.created;
    private final NoteSaver.NoteDateField defaultDateFieldPreference = NoteSaver.NoteDateField.edited;
    private NoteStorage.UninitializedStorage uninitializedStorage;
    private NoteStorage storage;
    private DialogInvoker dialogInvoker;
    private FiltersHolder filtersHolder;
    private boolean search_on = false;
    private MenuItem searchMenuItem = null;
    private Note noteInChangingState;
    private String searchInTitle;
    private String searchInDescription;
    private User currentUser;
    private MenuItem userMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        boolean noUserFromSettings = false;

        if (!settings.contains(KEY_LAST_USER_ID) || !settings.contains(KEY_LAST_USER_NAME))
            noUserFromSettings = true;

        int id = settings.getInt(KEY_LAST_USER_ID, DEFAULT_USER_ID);
        String username = settings.getString(KEY_LAST_USER_NAME, DEFAULT_USER_NAME);
        currentUser = new User(username, id);

        if (noUserFromSettings) {
            Log.i(TAG, "onCreate: no user from settings");
            startActivityForResult(new Intent(this, UserActivity.class), REQUEST_USER);
            return;
        }

//        deleteDatabase("Notes.db");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));
                    if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable()) {
                        if (storage != null)
                            storage.synchronize();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ANDROID_NET_CONN_CONNECTIVITY_CHANGE);
        registerReceiver(receiver, intentFilter);

        uninitializedStorage = new NoteStorage.UninitializedStorage(this, currentUser);

        startService(new Intent(this, NoteSaverService.class));

        dialogInvoker = new DialogInvoker(this);

        ListView lv = (ListView) findViewById(R.id.listView);

        uninitializedStorage.setAdapterForView(lv);
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
    }

    public void launchEdit(Note note) {
        noteInChangingState = note;
        final Intent intent = new Intent(this, EditActivity.class);
        fillIntentWithNoteInfo(intent, note);
        startActivityForResult(intent, EditActivity.EDIT_NOTE);
    }

    public void launchDelete(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_delete_title)
                .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        storage.deleteNote(note);
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

    public void launchAdd(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        int noteIndex = storage.getCount();
        Note note = new Note(getDefaultNoteTitleTextWithIndex(noteIndex),
                DEFAULT_NOTE_DESCRIPTION,
                getResources().getColor(R.color.colorPrimary), null);
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
        storage.update(filtersHolder.getCurrentFilterCopy());
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
        storage.exportToFile(filename);
    }

    private void importNotesFromFile(String filename) {
        if (!hasIOExternalPermission()) {
            return;
        }
        storage.importFromFile(filename);
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
                    final Note oldNote = noteInChangingState;
                    final Note newNote;
                    if (data.getBooleanExtra(INTENT_KEY_NOTE_IS_CHANGED, false)) {
                        newNote = data.getParcelableExtra(INTENT_KEY_NOTE);
                        // service with saver will be bound at end of queue and might not exist right now
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                storage.editNote(oldNote, newNote);
                            }
                        });
                    } else {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                storage.notifyOpened(oldNote);
                            }
                        });
                    }
                    break;
                case IMPORT_REQUEST_CODE:
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        importNotesFromFile(theFilePath);
                    }
                    break;
                case EXPORT_REQUEST_CODE:
                    if (data != null && data.getData() != null) {
                        String theFilePath = data.getData().getPath();
                        exportNotesToFile(theFilePath);
                    }
                    break;
                case REQUEST_USER:
                    Log.i(TAG, "onActivityResult: UserActivity ");
                    currentUser = new User(data.getStringExtra(KEY_USER_NAME),
                            data.getIntExtra(KEY_USER_ID, DEFAULT_USER_ID));
                    userMenuItem.setTitle(getString(R.string.main_menu_user, currentUser.getName()));
                    storage.changeUser(currentUser);
                    break;
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
        userMenuItem = menu.findItem(R.id.action_users);
        updateSearchIcon();
        userMenuItem.setTitle(getString(R.string.main_menu_user, currentUser.getName()));
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
                    storage.update(filtersHolder.getCurrentFilterCopy());
                }
                break;
            case R.id.action_fill:
                fillAndUpload();
                break;
            case R.id.action_manage_filters:
                dialogInvoker.manageFiltersDialog(filtersHolder.getFilterNames(), this);
                break;
            case R.id.action_users:
                Intent intent = new Intent(this, UserActivity.class);
                startActivityForResult(intent, REQUEST_USER);
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
                        getRandomColor(r),
                        null));
                i++;
            }
            Log.i(TAG, "fillAndUpload: launched to index " + i);
            storage.addNotes(list);
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
        editor.putString(KEY_LAST_USER_NAME, currentUser.getName());
        editor.putInt(KEY_LAST_USER_ID, currentUser.getUserID());
        filtersHolder.storeToPreferences(prefs);
        editor.apply();
        unbindService(this);
    }

    @Override
    protected void onDestroy() {
        storage.close();
        super.onDestroy();
    }

    @Override
    public void onSortDialogResult(NoteSaver.QueryFilter result) {
        filtersHolder.setCurrentFilterFrom(result);
        storage.update(filtersHolder.getCurrentFilterCopy());
    }

    @Override
    public void onFilterDialogResult(NoteSaver.QueryFilter result) {
        filtersHolder.setCurrentFilterFrom(result);
        storage.update(filtersHolder.getCurrentFilterCopy());
    }

    @Override
    public void onSearchDialogResult(DialogInvoker.SearchDialogResult result) {
        if (!(result.title == null && result.description == null)) {
            storage.searchSubstring(result.title, result.description);
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
        storage.update(filtersHolder.getCurrentFilterCopy());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(getLocalClassName(), "Service bound");
        NoteSaverService.LocalBinder binder = ((NoteSaverService.LocalBinder) service);
        if (uninitializedStorage != null) {
            storage = uninitializedStorage.initStorage(binder).getStorage();
            storage.update(filtersHolder.getCurrentFilterCopy());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(getLocalClassName(), "Service unbound");
    }
}
