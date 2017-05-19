package sergeylysyi.notes.note;

import android.content.Context;
import android.widget.AdapterView;

public class NoteStorage {
    private final Context context;
    NoteListAdapter adapter;
    NoteSaverService.LocalSaver saver;

    public NoteStorage(Context context, NoteSaverService.LocalBinder service) {
        this.context = context;
        saver = service.getSaver();
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
