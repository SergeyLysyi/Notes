package sergeylysyi.notes.note.RemoteNotes;

import sergeylysyi.notes.note.ArrayNoteJson;

import java.util.List;
import java.util.Map;

class Response {
    static class Info {
        public String status;
        public Map<String, String> data;
    }

    static class Notes {
        public String status;
        public List<ArrayNoteJson.NoteJsonServerResponse> data;
    }

    static class Note {
        public String status;
        public ArrayNoteJson.NoteJsonServerResponse data;
    }

    static class PostNote {
        public String status;
        public Integer data;
    }

    static class EditNote {
        public String status;
        public Object data;
    }

    static class DeleteNote {
        public String status;
        public Object data;
    }
}
