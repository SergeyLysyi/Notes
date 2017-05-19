package sergeylysyi.notes.note;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class NoteSaverService extends Service {
    static private String TAG = "NoteSaverService";

    private Handler handler;
    private LocalSaver saver;
    private NoteJsonImportExport fileOperator;

    public NoteSaverService() {
        Log.i(TAG, "constructor called");
        HandlerThread thread = new HandlerThread("NoteSaverServiceThread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        saver = new LocalSaver(this);
        fileOperator = new NoteJsonImportExport(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
//        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        handler.getLooper().quit();
        super.onDestroy();
    }

    public interface OnChangeNotesCallback {
        void onChangeNotes();
    }

    public interface OnGetNotesCallback {
        void onGetNotes(List<Note> notes);
    }

    public interface OnGetNoteCursorCallback {
        void onGetNoteCursor(NoteCursor cursor);
    }

    public class LocalBinder extends Binder {
        public LocalSaver getSaver() {
            return saver;
        }

        public NoteJsonImportExport getFileOperator() {
            return fileOperator;
        }
    }

    public class LocalSaver extends NoteSaver {
        LocalSaver(Context context) {
            super(context);
        }

        public void insertOrUpdateWithCallback(final Note note, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.insertOrUpdate(note);
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChangeNotes();
                            }
                        });
                    }
                }
            });
        }

        public void insertOrUpdateManyWithCallback(final List<Note> notes, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (Note note : notes) {
                        LocalSaver.super.insertOrUpdate(note);
                    }
                    Log.i(TAG, "many added");
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NoteSaverService.this, "many added", Toast.LENGTH_SHORT).show();
                                callback.onChangeNotes();
                            }
                        });
                    }
                }
            });
        }

        @Override
        public boolean insertOrUpdate(Note note) {
            insertOrUpdateWithCallback(note, null, null);
            // runnable almost never will be executed immediately;
            return false;
        }

        public void deleteNoteWithCallback(final Note note, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    System.out.println("DELETED :" + LocalSaver.super.deleteNote(note));
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChangeNotes();
                            }
                        });
                    }
                }
            });
        }

        @Override
        public int deleteNote(Note note) {
            deleteNoteWithCallback(note, null, null);
            // runnable almost never will be executed immediately;
            return 0;
        }

        public void repopulateWithWithCallback(final List<Note> notes, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.repopulateWith(notes);
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChangeNotes();
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void repopulateWith(final List<Note> notes) {
            repopulateWithWithCallback(notes, null, null);
        }

        @Override
        public synchronized void close() {
            // ignore outer close call
        }

        synchronized void innerClose() {
            super.close();
        }

        public class Query extends NoteSaver.Query {
            public void getWithCallback(final Handler handlerForCallback, final OnGetNotesCallback callback) {
                handler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        final List<Note> notes = Query.super.get();
                        if (handlerForCallback != null && callback != null) {
                            handlerForCallback.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onGetNotes(notes);
                                }
                            });
                        }
                    }
                });
            }

            public void getCursorWithCallback(final Handler handlerForCallback, final OnGetNoteCursorCallback callback) {
                handler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        final NoteCursor notes = Query.super.getCursor();
                        if (handlerForCallback != null && callback != null) {
                            handlerForCallback.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onGetNoteCursor(notes);
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public List<Note> get() {
                final List<Note> result = new ArrayList<>();
                getWithCallback(handler, new OnGetNotesCallback() {
                    @Override
                    public void onGetNotes(List<Note> notes) {
                        result.addAll(notes);
                    }
                });
                return result;
            }

            /**
             * method is synchronous.
             */
            @Override
            public NoteCursor getCursor() {
                return super.getCursor();
            }
        }
    }
}