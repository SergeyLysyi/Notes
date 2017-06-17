package sergeylysyi.notes.note;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import sergeylysyi.notes.R;
import sergeylysyi.notes.note.RemoteNotes.Errors;
import sergeylysyi.notes.note.RemoteNotes.OnError;
import sergeylysyi.notes.note.RemoteNotes.OnSuccess;
import sergeylysyi.notes.note.RemoteNotes.RESTClient;
import sergeylysyi.notes.note.RemoteNotes.User;

import static android.os.Looper.getMainLooper;

public class NoteStorage implements Closeable {
    public static final boolean SHOULD_UPDATE_ON_CHANGE_DEFAULT = true;
    public static final int RETRY_AMOUNT = 5;
    private static final String TAG = NoteStorage.class.getName();
    private static final String ServerURL = "https://notesbackend-yufimtsev.rhcloud.com/";
    private final RESTClient rc;
    private final NoteListAdapter adapter;
    private final AtomicBoolean updateOnChange = new AtomicBoolean(SHOULD_UPDATE_ON_CHANGE_DEFAULT);
    private NoteSaverService.LocalSaver saver;
    private NoteSaver.QueryFilter filter = new NoteSaver.QueryFilter();
    private NoteSaverService.OnChange onChangeUpdate = new NoteSaverService.OnChange() {
        @Override
        public void onChange(long leftToAdd) {
            update(filter);
        }
    };
    private final AtomicReference<NoteSaverService.OnChange>
            onChangeDelegate = new AtomicReference<>(onChangeUpdate);

    public NoteStorage(Context context) {
        User user = new User(6666);
        rc = new RESTClient(ServerURL, user);
        adapter = new NoteListAdapter(context, R.layout.layout_note);
    }

    public void synchronize() {
        List<Note> localNotes = saver.new Query().get();
        List<Note> remoteNotes;

        final AtomicReference<List<Note>> list_ref = new AtomicReference<>();
        final AtomicBoolean serverRequestError = new AtomicBoolean(false);
        rc.getAll(new OnSuccess<List<Note>>() {
            @Override
            public void success(List<Note> data) {
                list_ref.set(data);
            }
        }, new OnError() {
            @Override
            public void error(Errors e) {
                serverRequestError.set(true);
            }
        });
        if (serverRequestError.get()) {
            return;
        }
        remoteNotes = list_ref.get();

        final Set<Note> onlyLocal = new HashSet<>();
        Set<Note> onlyRemote;
        Set<Note> differentRemote = new HashSet<>();
        Set<Note> differentLocal = new HashSet<>();
        Set<Note> sameRemote = new HashSet<>();
        local_notes_loop:
        for (Note local : localNotes) {
            if (local.getServerID() == null) {
                onlyLocal.add(local);
            } else {
                for (Note remote : remoteNotes) {
                    if (local.getServerID().equals(remote.getServerID())) {
                        if (local.localToRemoteEquals(remote)) {
                            sameRemote.add(remote);
                        } else {
                            differentLocal.add(local);
                            differentRemote.add(remote);
                        }
                        continue local_notes_loop;
                    }
                }
                // has serverID but doesn't have match on server
                onlyLocal.add(local);
            }
        }
        remoteNotes.removeAll(sameRemote);
        remoteNotes.removeAll(differentRemote);
        onlyRemote = new HashSet<>(remoteNotes);

        if (!differentLocal.isEmpty() || !differentRemote.isEmpty()) {
            //show dialog
            //upload diff
        }

        retryOnCall(new Retry<OnError>() {
            @Override
            public void retry(OnError retryCallback) {
                rc.addAll(new ArrayList<>(onlyLocal), null, retryCallback);
            }
        }, RETRY_AMOUNT);

        saver.insertOrUpdateManyWithCallback(new ArrayList<>(onlyRemote), new Handler(getMainLooper()), onChangeUpdate);
    }

    public void setUpdateOnChange(boolean flag) {
        updateOnChange.set(flag);
        if (updateOnChange.get()) {
            onChangeDelegate.set(onChangeUpdate);
        } else {
            onChangeDelegate.set(null);
        }
    }

    public void finishInitFromBinder(NoteSaverService.LocalBinder service) {
        saver = service.getSaver();
        saver.setAllFinishedCallback(new Handler(), onChangeUpdate);
    }

    public void addNotes(final List<Note> notes) {
        saver.insertOrUpdateManyWithCallback(notes, new Handler(getMainLooper()), onChangeDelegate.get());

        retryOnCall(new Retry<OnError>() {
            @Override
            public void retry(OnError retryCallback) {
                rc.addAll(notes, new OnSuccess<Map<Note, Integer>>() {
                    @Override
                    public void success(Map<Note, Integer> data) {
                        for (Note note : data.keySet()) {
                            note.setServerID(data.get(note));
                            saver.insertOrUpdateWithCallback(note, null, null);
                        }
                    }
                }, retryCallback);
            }
        }, RETRY_AMOUNT);
    }

    public void notifyOpened(final Note note) {
        editNote(note, note);
    }

    public void editNote(final Note oldNote, Note newNote) {
        oldNote.updateOpenDate();
        if (!oldNote.getTitle().equals(newNote.getTitle()))
            oldNote.setTitle(newNote.getTitle());
        if (!oldNote.getDescription().equals(newNote.getDescription()))
            oldNote.setDescription(newNote.getDescription());
        if (!Integer.valueOf(oldNote.getColor()).equals(newNote.getColor()))
            oldNote.setColor(newNote.getColor());
        if (!oldNote.getImageUrl().equals(newNote.getImageUrl()))
            oldNote.setImageURL(newNote.getImageUrl());
        saver.insertOrUpdateWithCallback(oldNote, new Handler(getMainLooper()), onChangeDelegate.get());

        if (oldNote.getServerID() == null) {
            retryOnCall(new Retry<OnError>() {
                @Override
                public void retry(OnError retryCallback) {
                    rc.add(oldNote, new OnSuccess<Integer>() {
                        @Override
                        public void success(Integer data) {
                            oldNote.setServerID(data);
                            saver.insertOrUpdateWithCallback(oldNote, null, null);
                        }
                    }, retryCallback);
                }
            }, RETRY_AMOUNT);
        } else {
            retryOnCall(new Retry<OnError>() {
                @Override
                public void retry(OnError retryCallback) {
                    rc.edit(oldNote, null, retryCallback);
                }
            }, RETRY_AMOUNT);
        }
    }

    public void deleteNote(final Note note) {
        saver.deleteNoteWithCallback(note, new Handler(getMainLooper()), onChangeDelegate.get());

        retryOnCall(new Retry<OnError>() {
            @Override
            public void retry(OnError retryCallback) {
                rc.delete(note, null, retryCallback);
            }
        }, RETRY_AMOUNT);
    }

    public int getCount() {
        return adapter.getCount();
    }

    public void update(final NoteSaver.QueryFilter filter) {
        update(filter, null, null);
    }

    public void update(NoteSaver.QueryFilter filter, String searchInTitle, String searchInDescription) {
        this.filter = filter;
        NoteSaverService.LocalSaver.Query query = saver.new Query();
        query.fromFilter(filter).withSubstring(searchInTitle, searchInDescription);
        //TODO: get cursor async
        adapter.updateData(query.getCursor());
        Log.i(TAG, "updateNotesByQuery: adapter.getCount() = " + adapter.getCount());
    }

    public void searchSubstring(String inTitle, String inDescription) {
        update(filter, inTitle, inDescription);
    }

    public void setAdapterForView(ListView view) {
        view.setAdapter(adapter);
    }

    private void retryOnCall(final Retry<OnError> a, int retryAmount) {
        final AtomicInteger count = new AtomicInteger(retryAmount);
        final AtomicReference<OnError> m = new AtomicReference<>();
        m.set(new OnError() {
            @Override
            public void error(Errors e) {
                if (count.decrementAndGet() > 0) {
                    a.retry(m.get());
                }
            }
        });
        a.retry(m.get());
    }

    @Override
    public void close() {
        saver.close();
    }

    interface Retry<T> {
        void retry(T retryCallback);
    }
}
