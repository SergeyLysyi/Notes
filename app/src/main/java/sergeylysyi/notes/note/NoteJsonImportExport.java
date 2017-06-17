package sergeylysyi.notes.note;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonEncodingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import sergeylysyi.notes.R;

public class NoteJsonImportExport extends Handler {
    public static final int REQUEST_IMPORT = -1;
    public static final int REQUEST_EXPORT = 1;
    public static final int REPLY_PROGRESS = -27;
    public static final int REPLY_FINISH = -28;
    public static final String MSG_IMPORT_ILLEGAL_ARGUMENT = "message.obj is not instance of File";
    public static final String MSG_EXPORT_ILLEGAL_ARGUMENT = "message.obj is not instance of File";
    private static final int IMPORT_PACK_SIZE = 100;
    private static final int EXPORT_PACK_SIZE = 100;
    private final Context context;
    private final NoteSaverService.LocalSaver saver;

    public NoteJsonImportExport(Context context, NoteSaverService.LocalSaver saver, Looper taskLooper) {
        super(taskLooper);
        this.context = context;
        this.saver = saver;
    }

    private void notesToJson(final OutputStream outputStream, final RunnableFactory factory) {
        saver.new Query().getCursorWithCallback(this, new NoteSaverService.OnGetCursor() {
            @Override
            public void onGetCursor(NoteCursor cursor) {
                NoteJsonAdapter noteJsonAdapter = new NoteJsonAdapter();
                final int total = cursor.getCount();
                int done = 0;
                try {
                    noteJsonAdapter.startPack();
                    if (cursor.moveToFirst()) {
                        List<Note> pack = new ArrayList<>();
                        boolean isMoved;
                        do {
                            pack.clear();
                            do {
                                try {
                                    pack.add(cursor.getNote());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                isMoved = cursor.moveToNext();
                            } while (isMoved && pack.size() < EXPORT_PACK_SIZE);
                            outputStream.write(noteJsonAdapter.pack(pack));
                            done += pack.size();
                            double percentDone = 100 * (double) done / total;
                            factory.progressCallback.onProgress(percentDone);
                        } while (isMoved);
                        outputStream.write(noteJsonAdapter.endPack());
                        factory.finishCallback.onFinish(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    factory.finishCallback.onFinish(false);
                }
            }
        });
    }

    private void notesFromJson(InputStream inputStream, RunnableFactory factory) throws IOException, ParseException {
        NoteJsonAdapter arj = new NoteJsonAdapter();
        arj.startUnpack(inputStream);
        List<Note> pack;
        saver.clearWithCallback(null, null);
        do {
            pack = arj.readNextPack(IMPORT_PACK_SIZE);
            saver.insertOrUpdateManyWithCallback(pack, null, null);
            double percentDone = 100 * Math.min(1 - arj.unpackFractionLeft(), 1);
            factory.getProgressRunnable(percentDone).run();
        } while (pack.size() >= IMPORT_PACK_SIZE);
        factory.getFinishRunnable(true).run();
    }

    public void exportTo(final File file, final Messenger forReports) {
        final Toast toastOnFail = Toast.makeText(context,
                context.getString(R.string.export_error_toast_string_formatted, file.getAbsolutePath()),
                Toast.LENGTH_LONG);
        final Toast toastOnSucceed = Toast.makeText(context,
                context.getString(R.string.export_success_toast_string_formatted, file.getAbsolutePath()),
                Toast.LENGTH_LONG);
        final RunnableFactory factory = new RunnableFactory();
        factory.setProgressCallback(new ProgressCallback() {
            @Override
            public void onProgress(double percentDone) {
                Message m = new Message();
                m.what = REPLY_PROGRESS;
                m.arg1 = REQUEST_EXPORT;
                m.obj = percentDone;
                try {
                    forReports.send(m);
                } catch (RemoteException ignored) {
                }
            }
        });
        factory.setFinishCallback(new FinishCallback() {
            @Override
            public void onFinish(boolean isSucceed) {
                if (isSucceed) {
                    toastOnSucceed.show();
                } else {
                    toastOnFail.show();
                }
                Message m = new Message();
                m.what = REPLY_FINISH;
                m.arg1 = REQUEST_EXPORT;
                m.obj = isSucceed;
                try {
                    forReports.send(m);
                } catch (RemoteException ignored) {
                }
            }
        });
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            toastOnFail.show();
            return;
        }
        notesToJson(fos, factory);
    }

    private void importFrom(File file, final Messenger forReports) {
        final String filename = file.getAbsolutePath();
        RunnableFactory runnableFactory = new RunnableFactory();
        runnableFactory.setProgressCallback(new ProgressCallback() {
            @Override
            public void onProgress(double percentDone) {
                Message m = new Message();
                m.what = REPLY_PROGRESS;
                m.arg1 = REQUEST_IMPORT;
                m.obj = percentDone;
                try {
                    forReports.send(m);
                } catch (RemoteException ignored) {
                }
            }
        });
        runnableFactory.setFinishCallback(new FinishCallback() {
            @Override
            public void onFinish(boolean isSucceed) {
                Message m = new Message();
                m.what = REPLY_FINISH;
                m.arg1 = REQUEST_IMPORT;
                m.obj = isSucceed;
                try {
                    forReports.send(m);
                } catch (RemoteException ignored) {
                }
            }
        });

        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                notesFromJson(fis, runnableFactory);
            } finally {
                fis.close();
            }
        } catch (JsonEncodingException | JsonDataException | ParseException e) {
            Toast.makeText(context, context.getString(R.string.import_parse_error_toast_string_formatted, filename),
                    Toast.LENGTH_LONG).show();
            this.post(runnableFactory.getFinishRunnable(false));
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.import_access_error_toast_string_formatted, filename),
                    Toast.LENGTH_LONG).show();
            this.post(runnableFactory.getFinishRunnable(false));
            e.printStackTrace();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        Messenger m = msg.replyTo;
        switch (msg.what) {
            case REQUEST_IMPORT:
                if (msg.obj instanceof File) {
                    importFrom((File) msg.obj, m);
                } else {
                    IllegalArgumentException e = new IllegalArgumentException(MSG_IMPORT_ILLEGAL_ARGUMENT);
                    e.printStackTrace();
                }
                break;
            case REQUEST_EXPORT:
                if (msg.obj instanceof File) {
                    exportTo((File) msg.obj, m);
                } else {
                    IllegalArgumentException e = new IllegalArgumentException(MSG_EXPORT_ILLEGAL_ARGUMENT);
                    e.printStackTrace();
                }
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public interface ProgressCallback {
        void onProgress(double percentDone);
    }

    public interface FinishCallback {
        void onFinish(boolean isSucceed);
    }

    private static class RunnableFactory {
        private ProgressCallback progressCallback;
        private FinishCallback finishCallback;

        public RunnableFactory setProgressCallback(ProgressCallback progressCallback) {
            this.progressCallback = progressCallback;
            return this;
        }

        public RunnableFactory setFinishCallback(FinishCallback finishCallback) {
            this.finishCallback = finishCallback;
            return this;
        }

        public Runnable getProgressRunnable(final double percentDone) {
            if (progressCallback == null)
                return null;
            return new Runnable() {
                @Override
                public void run() {
                    progressCallback.onProgress(percentDone);
                }
            };
        }

        public Runnable getFinishRunnable(final boolean isSucceed) {
            if (finishCallback == null)
                return null;
            return new Runnable() {
                @Override
                public void run() {
                    finishCallback.onFinish(isSucceed);
                }
            };
        }
    }
}
