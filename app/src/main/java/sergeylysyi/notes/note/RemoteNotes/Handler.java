package sergeylysyi.notes.note.RemoteNotes;


import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import sergeylysyi.notes.note.NoteJsonAdapter;

class Handler {
    public static final String TAG = Handler.class.getName();

    /**
     * @param <R> Return type of result.
     * @param <T> Return type of callback.
     */
    static abstract class AbstractHandler<R, T> implements Callback<T> {
        protected OnSuccess<R> success;
        protected OnError error;

        AbstractHandler(OnSuccess<R> success, OnError error) {
            if ((this.success = success) == null) {
                this.success = new OnSuccess<R>() {
                    @Override
                    public void success(R data) {
                    }
                };
            }
            if ((this.error = error) == null) {
                this.error = new OnError() {
                    @Override
                    public void error(Errors e) {
                        Log.w(TAG, "error: ", e);
                    }
                };
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable throwable) {
            error.error(new Errors.FailedRequest(throwable));
        }
    }

    static abstract class AbstractSuccessHandler<R, T extends Response.BasicResponse> extends AbstractHandler<R, T> {
        public static final String DEFAULT_FAILED_REQUEST_MSG = "Failed request";
        public final String TAG = Info.class.getName();

        AbstractSuccessHandler(OnSuccess<R> success, OnError error) {
            super(success, error);
        }

        abstract void onSuccess(T response);

        @Override
        public void onResponse(Call<T> call, retrofit2.Response<T> response) {
            if (response.isSuccessful()) {
                T r = response.body();
                assert r != null;
                if (r.isSuccessful()) {
                    onSuccess(r);
                } else {
                    error.error(new Errors.IncorrectRequest(r.error));
                }
            } else {
                String errorBodyString;
                try {
                    try {
                        errorBodyString = response.errorBody().string();
                    } catch (IOException e) {
                        errorBodyString = response.errorBody().toString();
                    }
                } catch (NullPointerException e) {
                    errorBodyString = DEFAULT_FAILED_REQUEST_MSG;
                }
                error.error(new Errors.FailedRequest(errorBodyString));
            }
        }
    }

    class Info extends AbstractSuccessHandler<Map<String, String>, Response.Info> {
        final String TAG = Info.class.getName();

        Info(OnSuccess<Map<String, String>> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.Info response) {
            success.success(response.data);
        }
    }

    class Notes extends AbstractSuccessHandler<List<sergeylysyi.notes.note.Note>, Response.Notes> {
        final String TAG = Notes.class.getName();

        Notes(OnSuccess<List<sergeylysyi.notes.note.Note>> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.Notes response) {
            List<sergeylysyi.notes.note.Note> notes = new ArrayList<>(response.data.size());
            for (NoteJsonAdapter.NoteJsonServerResponse noteJson : response.data) {
                try {
                    sergeylysyi.notes.note.Note note = noteJson.getNote();
                    notes.add(note);
                } catch (ParseException e) {
                    error.error(new Errors.UnparsableRecord(e));
                }
            }
            success.success(notes);
        }
    }

    class Note extends AbstractSuccessHandler<sergeylysyi.notes.note.Note, Response.Note> {
        final String TAG = Note.class.getName();

        Note(OnSuccess<sergeylysyi.notes.note.Note> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.Note response) {
            sergeylysyi.notes.note.Note note;
            try {
                note = response.data.getNote();
            } catch (ParseException e) {
                error.error(new Errors.UnparsableRecord(e));
                return;
            }
            success.success(note);
        }
    }

    class PostNote extends AbstractSuccessHandler<Integer, Response.PostNote> {
        final String TAG = PostNote.class.getName();

        public PostNote(OnSuccess<Integer> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.PostNote response) {
            success.success(response.data);
        }
    }

    class EditNote extends AbstractSuccessHandler<Void, Response.EditNote> {
        final String TAG = EditNote.class.getName();

        public EditNote(OnSuccess<Void> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.EditNote response) {
            success.success(null);
        }
    }

    class DeleteNote extends AbstractSuccessHandler<Void, Response.DeleteNote> {
        final String TAG = DeleteNote.class.getName();

        public DeleteNote(OnSuccess<Void> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onSuccess(Response.DeleteNote response) {
            success.success(null);
        }
    }
}
