package sergeylysyi.notes.note;

import android.content.Context;
import android.util.Log;
import android.widget.AdapterView;

import sergeylysyi.notes.note.RemoteNotes.Errors;
import sergeylysyi.notes.note.RemoteNotes.OnError;
import sergeylysyi.notes.note.RemoteNotes.OnSuccess;
import sergeylysyi.notes.note.RemoteNotes.RESTClient;
import sergeylysyi.notes.note.RemoteNotes.User;

public class NoteStorage {
    private static final String TAG = NoteStorage.class.getName();

    private static final String ServerURL = "https://notesbackend-yufimtsev.rhcloud.com/";
    private final RESTClient rc;
    private final Context context;
    NoteListAdapter adapter;
    NoteSaverService.LocalSaver saver;

    public NoteStorage(Context context, NoteSaverService.LocalBinder service) {
        this.context = context;
        saver = service.getSaver();
        User user1 = new User(6666);
        rc = new RESTClient(ServerURL, user1);
    }


    public void addNote(Note note) {

    }

    public void editNote(Note newNote, Note oldNote) {

    }

    public void removeNote(Note note) {

    }

    public void importNotes(String filepath) {

    }

    public void exportNotes(String filepath) {

    }

    public void setAdapterForView(AdapterView view) {
        view.setAdapter(adapter);
    }
}
