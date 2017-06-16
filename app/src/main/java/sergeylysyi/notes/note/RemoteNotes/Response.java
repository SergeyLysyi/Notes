package sergeylysyi.notes.note.RemoteNotes;

import java.util.List;
import java.util.Map;

import sergeylysyi.notes.note.NoteJsonAdapter;

class Response {
    private static String STATUS_OK = "ok";
    private static String STATUS_ERROR = "error";

    static class BasicResponse<T> {
        public String status;
        public T data;
        public String error;

        public boolean isSuccessful() {
            return status.equals(STATUS_OK);
        }

        public boolean isError() {
            return status.equals(STATUS_ERROR);
        }
    }

    static class Info extends BasicResponse<Map<String, String>> {
    }

    static class Notes extends BasicResponse<List<NoteJsonAdapter.NoteJsonServerResponse>> {
    }

    static class Note extends BasicResponse<NoteJsonAdapter.NoteJsonServerResponse> {
    }

    static class PostNote extends BasicResponse<Integer> {
    }

    static class EditNote extends BasicResponse<Object> {
    }

    static class DeleteNote extends BasicResponse<Object> {
    }
}