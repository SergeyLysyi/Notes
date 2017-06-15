package sergeylysyi.notes.note.RemoteNotes;


import java.text.ParseException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import sergeylysyi.notes.note.ArrayNoteJson;

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
            this.success = success;
            this.error = error;
        }

        @Override
        public void onFailure(Call<T> call, Throwable throwable) {
            error.error(throwable);
        }
    }

    class Notes extends AbstractHandler<List<sergeylysyi.notes.note.Note>, Response.Notes> {
        final String TAG = Info.class.getName();

        Notes(OnSuccess<List<sergeylysyi.notes.note.Note>> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.Notes> call, retrofit2.Response<Response.Notes> response) {
            if (response.isSuccessful()) {
                Response.Notes r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                for (ArrayNoteJson.NoteJsonServerResponse note : r.data) {
                    try {
                        sb.append(String.format("%s\n", note.getNote().getTitle()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

    class Info extends AbstractHandler<Void, Response.Info> {
        final String TAG = Info.class.getName();

        Info(OnSuccess<Void> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.Info> call, retrofit2.Response<Response.Info> response) {
            if (response.isSuccessful()) {
                Response.Info r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                for (String key : r.data.keySet()) {
                    sb.append(String.format("%s : %s\n", key, r.data.get(key)));
                }
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

    class Note extends AbstractHandler<sergeylysyi.notes.note.Note, Response.Note> {
        final String TAG = Info.class.getName();

        Note(OnSuccess<sergeylysyi.notes.note.Note> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.Note> call, retrofit2.Response<Response.Note> response) {
            if (response.isSuccessful()) {
                Response.Note r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                try {
                    sb.append(String.format("%s\n", r.data.getNote().getTitle()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

    class PostNote extends AbstractHandler<Integer, Response.PostNote> {
        final String TAG = Info.class.getName();

        public PostNote(OnSuccess<Integer> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.PostNote> call, retrofit2.Response<Response.PostNote> response) {
            if (response.isSuccessful()) {
                Response.PostNote r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                sb.append(String.format("%s\n", r.data));
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

    class EditNote extends AbstractHandler<Void, Response.EditNote> {
        final String TAG = Info.class.getName();

        public EditNote(OnSuccess<Void> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.EditNote> call, retrofit2.Response<Response.EditNote> response) {
            if (response.isSuccessful()) {
                Response.EditNote r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                sb.append(String.format("%s\n", r.data));
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

    class DeleteNote extends AbstractHandler<Void, Response.DeleteNote> {
        final String TAG = Info.class.getName();

        public DeleteNote(OnSuccess<Void> success, OnError error) {
            super(success, error);
        }

        @Override
        public void onResponse(Call<Response.DeleteNote> call, retrofit2.Response<Response.DeleteNote> response) {
            if (response.isSuccessful()) {
                Response.DeleteNote r = response.body();
                assert r != null;
                System.out.println("onResponse: status: " + r.status);
                StringBuffer sb = new StringBuffer();
                sb.append(String.format("%s\n", r.data));
                System.out.println("onResponse: data: \n" + sb.toString());
            } else {
                System.out.println("onResponse: " + response.errorBody());
            }
        }
    }

}
