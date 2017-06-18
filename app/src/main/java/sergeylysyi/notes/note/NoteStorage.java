package sergeylysyi.notes.note;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ListView;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import sergeylysyi.notes.R;
import sergeylysyi.notes.note.RemoteNotes.ChooseSourceDialog;
import sergeylysyi.notes.note.RemoteNotes.Errors;
import sergeylysyi.notes.note.RemoteNotes.OnError;
import sergeylysyi.notes.note.RemoteNotes.OnSuccess;
import sergeylysyi.notes.note.RemoteNotes.RESTClient;
import sergeylysyi.notes.User;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NoteStorage implements Closeable, Handler.Callback {
    public static final int NOTIFICATION_IMPORT_EXPORT_ID = 101;
    public static final int RETRY_AMOUNT = 5;
    private static final String TAG = NoteStorage.class.getName();
    private static final String ServerURL = "https://notesbackend-yufimtsev.rhcloud.com/";
    private final Context context;
    private RESTClient rc;
    private User user;
    private final NoteListAdapter adapter;
    private final AtomicReference<NoteSaverService.OnChange> onChangeDelegate;
    private NoteSaverService.LocalSaver saver;
    private NoteSaver.QueryFilter filter = new NoteSaver.QueryFilter();

    private MessageHandlerImport messageHandlerImport = new MessageHandlerImport();
    private MessageHandlerExport messageHandlerExport = new MessageHandlerExport();

    private NoteSaverService.LocalJson noteFileOperator;

    {
        onChangeDelegate = new AtomicReference<>();
        onChangeDelegate.set(new NoteSaverService.OnChange() {
            @Override
            public void onChange(long leftToAdd) {
                update(filter);
            }
        });
    }

    private NoteStorage(Context context, NoteSaverService.LocalBinder service, NoteListAdapter adapter, User user) {
        this.context = context;
        saver = service.getSaver();
        this.adapter = adapter;
        noteFileOperator = service.getFileOperator();
        saver.setAllFinishedCallback(new Handler(), onChangeDelegate.get());
        changeUser(user);
    }

    public void changeUser(User user){
        this.user = user;
        rc = new RESTClient(ServerURL, user);
        update(filter);
        synchronize();
    }

    public void synchronize() {
        final List<Note> localNotes = saver.new Query().get(user);
        rc.getAll(new OnSuccess<List<Note>>() {
            @Override
            public void success(List<Note> remoteNotes) {
                final Set<Note> onlyLocal = new HashSet<>();
                Set<Note> onlyRemote;
                final Set<Note> differentRemote = new HashSet<>();
                final Set<Note> differentLocal = new HashSet<>();
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
                    ChooseSourceDialog.show(context, new ChooseSourceDialog.OnResult() {
                        @Override
                        public void onResult(ChooseSourceDialog.Result result) {
                            switch (result) {
                                case USE_REMOTE:
                                    deleteLocalNotes(differentLocal, new Runnable() {
                                        @Override
                                        public void run() {
                                            saver.insertOrUpdateManyWithCallback(
                                                    user,
                                                    new ArrayList<>(differentRemote),
                                                    new Handler(),
                                                    onChangeDelegate.get()
                                            );
                                        }
                                    });
                                    break;
                                case USE_LOCAL:
                                    for (final Note local : differentLocal) {
                                        retryOnCall(new Retry<OnError>() {
                                            @Override
                                            public void retry(OnError retryCallback) {
                                                rc.edit(local, null, retryCallback);
                                            }
                                        }, RETRY_AMOUNT);
                                    }
                                    break;
                            }
                        }

                        @Override
                        public void onCancel() {
                            //do nothing
                        }
                    });
                }

                rc.addAll(new ArrayList<>(onlyLocal), new OnSuccess<Map<Note, Integer>>() {
                    @Override
                    public void success(Map<Note, Integer> data) {
                        for (Note note : data.keySet()) {
                            note.setServerID(data.get(note));
                            saver.insertOrUpdateWithCallback(user, note, null, null);
                        }
                    }
                }, null);

                for (final Note remote : onlyRemote) {
                    assert remote.getServerID() != null;
                    saver.new Query().getByServerIDLazy(remote.getServerID(), new Handler(),
                            new NoteSaverService.OnGetSingle() {
                                @Override
                                public void onGetSingle(Note note) {
                                    if (note != null) {
                                        saver.insertOrUpdateWithCallback(user, note, new Handler(), onChangeDelegate.get());
                                    } else {
                                        saver.insertOrUpdateWithCallback(user, remote, new Handler(), onChangeDelegate.get());
                                    }
                                }
                            });
                }
            }
        }, null);
    }

    public void setUpdateOnChange(boolean flag) {
        if (flag) {
            onChangeDelegate.set(onChangeDelegate.get());
        } else {
            onChangeDelegate.set(null);
        }
    }

    public void addNotes(final List<Note> notes) {
        saver.insertOrUpdateManyWithCallback(user, notes, new Handler(), onChangeDelegate.get());

        rc.addAll(notes, new OnSuccess<Map<Note, Integer>>() {
            @Override
            public void success(Map<Note, Integer> data) {
                for (Note note : data.keySet()) {
                    note.setServerID(data.get(note));
                    saver.insertOrUpdateWithCallback(user, note, null, null);
                }
            }
        }, null);
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

        final Retry<OnError> retry = new Retry<OnError>() {
            @Override
            public void retry(OnError retryCallback) {
                if (oldNote.getServerID() == null)
                    rc.add(oldNote, new OnSuccess<Integer>() {
                        @Override
                        public void success(Integer data) {
                            oldNote.setServerID(data);
                            saver.insertOrUpdateWithCallback(user, oldNote, null, onChangeDelegate.get());
                        }
                    }, retryCallback);
                else
                    rc.edit(oldNote, null, retryCallback);
            }
        };

        final Runnable toServerAndUpdate = new Runnable() {
            @Override
            public void run() {
                retryOnCall(retry, RETRY_AMOUNT);
            }
        };

        if (oldNote.getID() == null) {
            saver.insertOrUpdateWithCallback(user, oldNote, new Handler(), new NoteSaverService.OnChange() {
                @Override
                public void onChange(long leftToAdd) {
                    toServerAndUpdate.run();
                }
            });
        } else {
            int id = oldNote.getID().intValue();
            saver.new Query().getByIDLazy(id, new Handler(), new NoteSaverService.OnGetSingle() {
                @Override
                public void onGetSingle(Note note) {
                    if (note == null) {
                        // note was deleted
                        return;
                    }
                    if (note.getServerID() != null) {
                        oldNote.setServerID(note.getServerID());
                    }
                    toServerAndUpdate.run();
                }
            });
        }
    }

    public void deleteNote(final Note note) {
        saver.deleteNoteWithCallback(note, new Handler(), onChangeDelegate.get());

        retryOnCall(new Retry<OnError>() {
            @Override
            public void retry(OnError retryCallback) {
                rc.delete(note, null, retryCallback);
            }
        }, RETRY_AMOUNT);
    }

    private void deleteLocalNotes(final Collection<Note> notes, final Runnable onFinish) {
        if (notes == null) {
            if (onFinish != null) {
                onFinish.run();
            }
            return;
        }
        final Iterator<Note> iterator = notes.iterator();
        if (!iterator.hasNext()) {
            if (onFinish != null)
                onFinish.run();
            return;
        }
        final AtomicReference<Note> noteToDelete = new AtomicReference<>(iterator.next());
        final AtomicReference<NoteSaverService.OnChange> step = new AtomicReference<>();
        step.set(new NoteSaverService.OnChange() {
            @Override
            public void onChange(long leftToAdd) {
                if (iterator.hasNext()) {
                    noteToDelete.set(iterator.next());
                    saver.deleteNoteWithCallback(noteToDelete.get(),
                            new Handler(),
                            step.get());
                } else {
                    if (onFinish != null)
                        onFinish.run();
                }
            }
        });
        saver.deleteNoteWithCallback(noteToDelete.get(), new Handler(), step.get());
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
        adapter.updateData(query.getCursor(user));
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

    public void importFromFile(String filename) {
        noteFileOperator.importFromFile(user, filename, new Messenger(new Handler(this)));
    }

    public void exportToFile(String filename) {
        noteFileOperator.exportToFile(user, filename, new Messenger(new Handler(this)));
    }

    @Override
    public void close() {
        saver.close();
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

    interface Retry<T> {
        void retry(T retryCallback);
    }

    public static class UninitializedStorage {
        private Context context;
        private NoteStorage storage;
        private NoteListAdapter adapter;
        private User user;

        public UninitializedStorage(Context context, User user) {
            this.context = context;
            this.user = user;
            this.adapter = new NoteListAdapter(context, R.layout.layout_note);
        }

        public UninitializedStorage initStorage(NoteSaverService.LocalBinder binder) {
            this.storage = new NoteStorage(context, binder, this.adapter, user);
            return this;
        }

        public NoteStorage getStorage() {
            return this.storage;
        }

        public void setAdapterForView(ListView view) {
            view.setAdapter(adapter);
        }

    }

    private class MessageHandlerImport implements
            NoteJsonImportExport.ProgressCallback,
            NoteJsonImportExport.FinishCallback {

        @Override
        public void onProgress(double percentDone) {
            update(filter);
            Notification not = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_import_progress, percentDone)).build();
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }

        @Override
        public void onFinish(boolean isSucceed) {
            String result;
            if (isSucceed) {
                update(filter);
                synchronize();
                result = context.getString(R.string.notification_success);
            } else {
                result = context.getString(R.string.notification_failed);
            }
            Notification not = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_import_finish, result)).build();
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }
    }

    private class MessageHandlerExport implements
            NoteJsonImportExport.ProgressCallback,
            NoteJsonImportExport.FinishCallback {

        @Override
        public void onProgress(double percentDone) {
            Notification not = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_export_progress, percentDone)).build();
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }

        @Override
        public void onFinish(boolean isSucceed) {
            String result;
            if (isSucceed)
                result = context.getString(R.string.notification_success);
            else
                result = context.getString(R.string.notification_failed);
            Notification not = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_export_finish, result)).build();
            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_IMPORT_EXPORT_ID, not);
        }
    }

}
