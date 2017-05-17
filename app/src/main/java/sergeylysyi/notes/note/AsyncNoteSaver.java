package sergeylysyi.notes.note;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class AsyncNoteSaver extends NoteSaver {
    private final PostCallbackHandler handler;

    public AsyncNoteSaver(Context context) {
        super(context);
        this.handler = new PostCallbackHandler();
    }

    public AsyncNoteSaver(Context context, PostCallbackHandler handler) {
        super(context);
        this.handler = handler;
    }

    public void insertOrUpdateWithCallback(final Note note, final OnPostExecute callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AsyncNoteSaver.super.insertOrUpdate(note);
                System.out.println("added");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                callback.onPostExecute();
            }
        }.execute();
    }

    @Override
    public boolean insertOrUpdate(Note note) {
        insertOrUpdateWithCallback(note, new OnPostExecute() {
            @Override
            public void onPostExecute() {
                handler.onPostInsertOrUpdate();
            }
        });
        // task almost never will be executed immediately;
        return false;
    }

    public void deleteNoteWithCallback(final Note note, final OnPostExecute callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AsyncNoteSaver.super.deleteNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                callback.onPostExecute();
            }
        }.execute();
    }

    @Override
    public int deleteNote(Note note) {
        deleteNoteWithCallback(note, new OnPostExecute() {
            @Override
            public void onPostExecute() {
                handler.onPostDelete();
            }
        });
        // task almost never will be executed immediately;
        return 0;
    }

    public void repopulateWithWithCallback(final List<Note> notes, final OnPostExecute callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AsyncNoteSaver.super.repopulateWith(notes);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                callback.onPostExecute();
            }
        }.execute();
    }

    @Override
    public void repopulateWith(List<Note> notes) {
        repopulateWithWithCallback(notes, new OnPostExecute() {
            @Override
            public void onPostExecute() {
                handler.onPostRepopulateWith();
            }
        });
    }

    public interface OnPostExecute {
        void onPostExecute();
    }

    public class PostCallbackHandler {
        void onPostInsertOrUpdate() {

        }

        void onPostDelete() {

        }

        void onPostGetNotes() {

        }

        void onPostRepopulateWith() {

        }
    }

    class AsyncQuery extends Query {
        @Override
        public List<Note> get() {
            return getWithCallback(new OnPostExecute() {
                @Override
                public void onPostExecute() {
                    handler.onPostInsertOrUpdate();
                }
            });
        }

        public List<Note> getWithCallback(final OnPostExecute callback) {
            final List<Note> notes = new ArrayList<>();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    notes.addAll(AsyncQuery.super.get());
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    callback.onPostExecute();
                }
            }.execute();
            return notes;
        }
    }
}
