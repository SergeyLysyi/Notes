package sergeylysyi.notes.note;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sergeylysyi.notes.R;

import static sergeylysyi.notes.note.NoteJsonImportExport.REPLY_FINISH;
import static sergeylysyi.notes.note.NoteJsonImportExport.REPLY_PROGRESS;
import static sergeylysyi.notes.note.NoteJsonImportExport.REQUEST_EXPORT;
import static sergeylysyi.notes.note.NoteJsonImportExport.REQUEST_IMPORT;


public class NoteSaverService extends Service {
    static private String TAG = "NoteSaverService";
    static private String SAVER_HANDLER_NAME = String.format("%s %s", NoteSaver.class.getName(), TAG);
    static private String IMPORT_EXPORT_HANDLER_NAME = String.format("%s %s", NoteJsonImportExport.class.getName(), TAG);

    private Handler handlerSaver;
    private Handler handlerImportExport;
    private LocalSaver saver;
    private LocalJson fileOperator;

    public NoteSaverService() {
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(SAVER_HANDLER_NAME);
        thread.start();
        handlerSaver = new Handler(thread.getLooper());
        thread = new HandlerThread(IMPORT_EXPORT_HANDLER_NAME);
        thread.start();
        saver = new LocalSaver(this);
        handlerImportExport = new NoteJsonImportExport(this, saver, thread.getLooper());
        fileOperator = new LocalJson();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        handlerSaver.getLooper().quit();
        saver.innerClose();
        super.onDestroy();
    }

    public interface OnChange {
        void onChange(long leftToAdd);
    }

    public interface OnGet {
        void onGet(List<Note> notes);
    }

    public interface OnGetCursor {
        void onGetCursor(NoteCursor cursor);
    }

    public class LocalBinder extends Binder {
        public LocalSaver getSaver() {
            return saver;
        }

        public LocalJson getFileOperator() {
            return fileOperator;
        }
    }

    public class LocalJson {

        public void exportToFile(String filename, Messenger replyTo) {
            File file = new File(filename);
            Message m = new Message();
            m.what = NoteJsonImportExport.REQUEST_EXPORT;
            m.obj = file;
            m.replyTo = replyTo;
            handlerImportExport.sendMessage(m);
        }

        public void importFromFile(String filename, Messenger replyTo) {
            File file = new File(filename);
            Message m = new Message();
            m.what = NoteJsonImportExport.REQUEST_IMPORT;
            m.obj = file;
            m.replyTo = replyTo;
            handlerImportExport.sendMessage(m);
        }

        public boolean isMessageFromJsonSender(Message message) {
            switch (message.what) {
                case REPLY_PROGRESS:
                    return true;
                case REPLY_FINISH:
                    return true;
                default:
                    return false;
            }
        }

        public void translateMessage(Message message,
                                     NoteJsonImportExport.ProgressCallback onProgressImport,
                                     NoteJsonImportExport.FinishCallback onFinishImport,
                                     NoteJsonImportExport.ProgressCallback onProgressExport,
                                     NoteJsonImportExport.FinishCallback onFinishExport) {
            if (!isMessageFromJsonSender(message)) {
                Log.i(TAG, "not NoteJsonImportExport message");
                return;
            }
            Exception exUnknownType =
                    new IllegalArgumentException("unknown message type from message of " + NoteJsonImportExport.class.getName());
            Exception exUnknownArgument =
                    new IllegalArgumentException("unknown message argument from message of " + NoteJsonImportExport.class.getName());
            Object responseValue = message.obj;
            if (responseValue instanceof Double) {
                switch (message.arg1) {
                    case REQUEST_EXPORT:
                        onProgressExport.onProgress((Double) responseValue);
                        break;
                    case REQUEST_IMPORT:
                        onProgressImport.onProgress((Double) responseValue);
                        break;
                    default:
                        exUnknownArgument.printStackTrace();
                }
            } else if (responseValue instanceof Boolean) {
                switch (message.arg1) {
                    case REQUEST_EXPORT:
                        onFinishExport.onFinish((Boolean) responseValue);
                        break;
                    case REQUEST_IMPORT:
                        onFinishImport.onFinish((Boolean) responseValue);
                        break;
                    default:
                        exUnknownArgument.printStackTrace();
                }
            } else {
                exUnknownType.printStackTrace();
            }
        }
    }

    public class LocalSaver extends NoteSaver {
        public static final int NOTIFICATION_DATA_BASE_OPERATIONS_ID = 102;
        private long overAllToAdd = 0;
        private Context context;
        private OnChange allDoneCallback = null;
        private Handler allDoneHandler = null;
        private int clearCommandsInQueue = 0;

        LocalSaver(Context context) {
            super(context);
            this.context = context;
        }

        public void updateSaverNotification(long argumentValue) {
            if (argumentValue > 0) {
                Notification not = new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.btn_star)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_db_left_to_add, argumentValue)).build();
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_DATA_BASE_OPERATIONS_ID, not);
            } else {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_DATA_BASE_OPERATIONS_ID);
            }
        }

        private synchronized void clearCommandAppearsInQueue() {
            clearCommandsInQueue++;
        }

        private synchronized void clearCommandLeaveQueue() {
            clearCommandsInQueue = Math.max(clearCommandsInQueue - 1, 0);
        }

        private synchronized boolean isClearCommandFollowInQueue() {
            return clearCommandsInQueue > 0;
        }

        private synchronized void changeOverAllAddByValue(int value) {
            overAllToAdd += value;
            overAllToAdd = Math.max(overAllToAdd, 0);
            if (overAllToAdd == 0 && allDoneHandler != null && allDoneCallback != null) {
                allDoneHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        allDoneCallback.onChange(getOverAllToAdd());
                    }
                });
            }
        }

        protected synchronized long getOverAllToAdd() {
            return overAllToAdd;
        }

        protected void changeOverAllToAddByValueAndUpdate(int value) {
            changeOverAllAddByValue(value);
            updateSaverNotification(getOverAllToAdd());
        }

        public void insertOrUpdateWithCallback(final Note note, final Handler handlerForCallback, final OnChange callback) {
            final int sizeOfEntriesToAdd = 1;
            changeOverAllToAddByValueAndUpdate(sizeOfEntriesToAdd);
            handlerSaver.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    if (!isClearCommandFollowInQueue()) {
                        LocalSaver.super.insertOrUpdate(note);
                    }
                    changeOverAllToAddByValueAndUpdate(-sizeOfEntriesToAdd);
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChange(getOverAllToAdd());
                            }
                        });
                    }
                }
            });
        }

        public void insertOrUpdateManyWithCallback(final List<Note> notes, final Handler handlerForCallback, final OnChange callback) {
            final int sizeOfEntriesToAdd = notes.size();
            changeOverAllToAddByValueAndUpdate(sizeOfEntriesToAdd);
            handlerSaver.post(new Runnable() {
                @Override
                public void run() {
                    if (!isClearCommandFollowInQueue()) {
                        for (Note note : notes) {
                            LocalSaver.super.insertOrUpdate(note);
                        }
                    }
                    changeOverAllToAddByValueAndUpdate(-sizeOfEntriesToAdd);
                    Log.i(TAG, String.format("pack of %d notes added to database", sizeOfEntriesToAdd));
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChange(getOverAllToAdd());
                            }
                        });
                    }
                }
            });
        }

        @Override
        public boolean insertOrUpdate(Note note) {
            insertOrUpdateWithCallback(note, null, null);
            // runnable almost never will be executed immediately, so return value is false;
            return true;
        }

        public void deleteNoteWithCallback(final Note note, final Handler handlerForCallback, final OnChange callback) {
            handlerSaver.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.deleteNote(note);
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChange(getOverAllToAdd());
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

        public void clearWithCallback(final Handler handlerForCallback, final OnChange callback) {
            clearCommandAppearsInQueue();
            handlerSaver.post(new Runnable() {
                @Override
                public void run() {
                    LocalSaver.super.clear();
                    clearCommandLeaveQueue();
                    if (handlerForCallback != null && callback != null) {
                        handlerForCallback.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onChange(getOverAllToAdd());
                            }
                        });
                    }
                }
            });
        }

        public void setAllFinishedCallback(Handler handler, OnChange callback) {
            allDoneHandler = handler;
            allDoneCallback = callback;
        }

        @Override
        public void clear() {
            clearWithCallback(null, null);
        }

        @Override
        public synchronized void close() {
            // ignore outer close call
        }

        synchronized void innerClose() {
            super.close();
        }

        public class Query extends NoteSaver.Query {
            public void getWithCallback(final Handler handlerForCallback, final OnGet callback) {
                handlerSaver.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        final List<Note> notes = Query.super.get();
                        if (handlerForCallback != null && callback != null) {
                            handlerForCallback.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onGet(notes);
                                }
                            });
                        }
                    }
                });
            }

            public void getCursorWithCallback(final Handler handlerForCallback, final OnGetCursor callback) {
                handlerSaver.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        final NoteCursor notes = Query.super.getCursor();
                        if (handlerForCallback != null && callback != null) {
                            handlerForCallback.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onGetCursor(notes);
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public List<Note> get() {
                final List<Note> result = new ArrayList<>();
                getWithCallback(handlerSaver, new OnGet() {
                    @Override
                    public void onGet(List<Note> notes) {
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