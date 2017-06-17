package sergeylysyi.notes.note.RemoteNotes;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import sergeylysyi.notes.note.Note;
import sergeylysyi.notes.note.NoteJsonAdapter;

public class RESTClient {

    public static final String TAG = RESTClient.class.getName();

    private final IServer s;
    private final User user;
    private final Handler handler;

    public RESTClient(String serverURL, User forUser) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serverURL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build();
        s = retrofit.create(IServer.class);
        this.user = forUser;
        this.handler = new Handler();
    }

    protected void getInfo(OnSuccess<Map<String, String>> cb, OnError err) {
        Call<Response.Info> ci = s.getInfo();
        ci.enqueue(handler.new Info(cb, err));
    }

    public void get(Note note, OnSuccess<Note> cb, OnError err) {
        int noteID;
        try {
            noteID = note.getServerID();
        } catch (NullPointerException e) {
            Log.e(TAG, "get: called on note without ServerID", e);
            return;
        }
        Call<Response.Note> cn = s.getNote(user.getUserID(), noteID);
        cn.enqueue(handler.new Note(cb, err));
    }

    public void getAll(OnSuccess<List<Note>> cb, OnError err) {
        Call<Response.Notes> cns = s.getNotes(user.getUserID());
        cns.enqueue(handler.new Notes(cb, err));
    }

    public void add(Note note, OnSuccess<Integer> cb, OnError err) {
        Call<Response.PostNote> pn = s.postNote(user.getUserID(), new NoteJsonAdapter.NoteJson(note));
        pn.enqueue(handler.new PostNote(cb, err));
    }

    public void edit(Note note, OnSuccess<Void> cb, OnError err) {
        int noteID;
        try {
            noteID = note.getServerID();
        } catch (NullPointerException e) {
            Log.e(TAG, "edit: called on note without ServerID", e);
            return;
        }
        Call<Response.EditNote> en = s.editNote(user.getUserID(), noteID, new NoteJsonAdapter.NoteJson(note));
        en.enqueue(handler.new EditNote(cb, err));
    }

    public void delete(Note note, OnSuccess<Void> cb, OnError err) {
        int noteID;
        try {
            noteID = note.getServerID();
        } catch (NullPointerException e) {
            Log.e(TAG, "delete: called on note without ServerID", e);
            return;
        }
        Call<Response.DeleteNote> dn = s.deleteNote(user.getUserID(), noteID);
        dn.enqueue(handler.new DeleteNote(cb, err));
    }

    /**
     * @param cb       Called for every successfully deleted note
     * @param err      Called every time error occurred while performing deletion.
     * @param fatalErr Called if whole operation can not be performed (and no notes will be deleted).
     */
    public void deleteAll(final OnSuccess<Void> cb, final OnError err, final OnError fatalErr) {
        getAll(new OnSuccess<List<Note>>() {
            @Override
            public void success(List<Note> data) {
                for (Note note : data) {
                    delete(note, cb, err);
                }
            }
        }, new OnError() {
            @Override
            public void error(Errors e) {
                if (!(e instanceof Errors.UnparsableRecord)) {
                    fatalErr.error(e);
                }
            }
        });
    }

    /**
     * @param notes Notes to add.
     * @param cb    Called for every successfully added note.Keys of result is notes from initial argument by reference.
     * @param err   Called every time error occurred while performing addition.
     */
    public void addAll(List<Note> notes, final OnSuccess<Map<Note, Integer>> cb, OnError err) {
        for (final AtomicInteger i = new AtomicInteger(0); i.get() < notes.size(); i.incrementAndGet()) {
            final Note note = notes.get(i.get());
            add(note, new OnSuccess<Integer>() {
                @Override
                public void success(Integer data) {
                    Map<Note, Integer> m = new HashMap<>();
                    m.put(note, data);
                    cb.success(m);
                }
            }, err);
        }
    }
}
