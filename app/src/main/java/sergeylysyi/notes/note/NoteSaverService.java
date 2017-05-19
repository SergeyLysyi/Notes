package sergeylysyi.notes.note;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class NoteSaverService extends Service {
    private Handler handler;
    private LocalSaver saver;

    @Override
    public void onCreate() {
        saver = new LocalSaver(this);
        HandlerThread thread = new HandlerThread("NoteSaverServiceThread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Message msg = handler.obtainMessage();
        msg.arg1 = startId;
        handler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                saver.innerClose();
                NoteSaverService.this.stopSelf();
            }
        });
        return false;
    }

    @Override
    public void onDestroy() {
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

    public class LocalBinder extends Binder {
        public LocalSaver getSaver(Context context) {
            startService(new Intent(context, this.getClass()));
            return saver;
        }
    }

    public class LocalSaver extends NoteSaver {
        LocalSaver(Context context) {
            super(context);
        }

        public void insertOrUpdateWithCallback(final Note note, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.post(new Runnable() {
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
        public boolean insertOrUpdate(Note note) {
            insertOrUpdateWithCallback(note, null, null);
            // runnable almost never will be executed immediately;
            return false;
        }

        public void deleteNoteWithCallback(final Note note, final Handler handlerForCallback, final OnChangeNotesCallback callback) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.deleteNote(note);
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

        public synchronized void innerClose() {
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
        }
    }

//
//    private final class ServiceHandler extends Handler {
//        public ServiceHandler(Looper looper) {
//            super(looper);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            long endTime = System.currentTimeMillis() + 3 * 1000;
//            while (System.currentTimeMillis() < endTime) {
//                synchronized (this) {
//                    try {
//                        wait(endTime - System.currentTimeMillis());
//                    } catch (Exception ignored) {
//                    }
//                }
//            }
//            stopSelf(msg.arg1);
//        }
//}
}