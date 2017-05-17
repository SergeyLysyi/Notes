package sergeylysyi.notes.note;

import android.content.Context;
import android.os.AsyncTask;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

    public void insertOrUpdateWithCallback(Note note, OnPostExecute callback) {
        Map<String, Object> argument = new HashMap<>();
        argument.put("note", note);
        createTask(TaskAction.InsertOrUpdate, argument, new AtomicReference(), callback).execute();
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

    public void deleteNoteWithCallback(Note note, OnPostExecute callback) {
        Map<String, Object> argument = new HashMap<>();
        argument.put("note", note);
        createTask(TaskAction.Delete, argument, new AtomicReference(), callback).execute();
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

    public void repopulateWithWithCallback(List<Note> notes, OnPostExecute callback) {
        Map<String, Object> argument = new HashMap<>();
        argument.put("notes", notes);
        createTask(TaskAction.RepopulateWith, argument, new AtomicReference(), callback).execute();
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

    public List<Note> getNotesWithCallback(String sortByColumn, String order,
                                           String titleSubstring, String descriptionSubstring,
                                           String columnForDateFilter,
                                           GregorianCalendar afterDate, GregorianCalendar beforeDate,
                                           OnPostExecute callback) {
        Map<String, Object> argument = new HashMap<>();
        argument.put("sortByColumn", sortByColumn);
        argument.put("order", order);
        argument.put("titleSubstring", titleSubstring);
        argument.put("descriptionSubstring", descriptionSubstring);
        argument.put("columnForDateFilter", columnForDateFilter);
        argument.put("afterDate", afterDate);
        argument.put("beforeDate", beforeDate);
        final AtomicReference<List<Note>> notes = new AtomicReference<>();
        createTask(TaskAction.GetNotes, argument, notes, callback).execute();
        return notes.get();
    }

    @Override
    protected List<Note> getNotes(final String sortByColumn, final String order,
                                  final String titleSubstring, final String descriptionSubstring,
                                  final String columnForDateFilter,
                                  final GregorianCalendar afterDate, final GregorianCalendar beforeDate) {
        return getNotesWithCallback(sortByColumn, order, titleSubstring,
                descriptionSubstring, columnForDateFilter, afterDate, beforeDate, new OnPostExecute() {
                    @Override
                    public void onPostExecute() {
                        handler.onPostGetNotes();
                    }
                });
    }

    private AsyncTask<Void, Void, Void> createTask(final TaskAction action,
                                                   final Map<String, Object> argument,
                                                   final AtomicReference result,
                                                   final OnPostExecute callback) {
        final AtomicReference<OnPostExecute> delegateCallback = new AtomicReference<>();
        delegateCallback.set(callback);
        return new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                switch (action) {
                    case InsertOrUpdate:
                        AsyncNoteSaver.super.insertOrUpdate((Note) argument.get("note"));
                        break;
                    case Delete:
                        AsyncNoteSaver.super.deleteNote((Note) argument.get("note"));
                        break;
                    case RepopulateWith:
                        AsyncNoteSaver.super.repopulateWith((List<Note>) argument.get("notes"));
                        break;
                    case GetNotes:
                        result.set(AsyncNoteSaver.super.getNotes(
                                (String) argument.get("sortByColumn"),
                                (String) argument.get("order"),
                                (String) argument.get("titleSubstring"),
                                (String) argument.get("descriptionSubstring"),
                                (String) argument.get("columnForDateFilter"),
                                (GregorianCalendar) argument.get("afterDate"),
                                (GregorianCalendar) argument.get("beforeDate")));
                        return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                delegateCallback.get().onPostExecute();
            }
        };
    }

    private enum TaskAction {InsertOrUpdate, Delete, RepopulateWith, GetNotes}

    interface OnPostExecute {
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
}
