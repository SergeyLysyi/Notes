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
        return new Starter();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
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

    public class Starter extends Binder {
        public LocalBinder start(Context context) {
            startService(new Intent(context, this.getClass()));
            return new LocalBinder();
        }
    }

    public class LocalBinder extends Binder {
        public LocalSaver getSaver() {
            return saver;
        }
    }

    public class LocalSaver extends NoteSaver {
        public LocalSaver(Context context) {
            super(context);
        }

        @Override
        public boolean insertOrUpdate(final Note note) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.insertOrUpdate(note);
                }
            });
            // runnable almost never will be executed immediately;
            return false;
        }

        @Override
        public int deleteNote(final Note note) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.deleteNote(note);
                }
            });
            // runnable almost never will be executed immediately;
            return 0;
        }

        @Override
        public void repopulateWith(final List<Note> notes) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.repopulateWith(notes);
                }
            });
        }

        @Override
        public synchronized void close() {
            // ignore outer close call
        }

        public synchronized void innerClose() {
            super.close();
        }

        public class Query extends NoteSaver.Query {
            @Override
            public List<Note> get() {
                final List<Note> notes = new ArrayList<>();
                handler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        notes.addAll(Query.super.get());
                    }
                });
                return notes;
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
}
}