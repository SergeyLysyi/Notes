package sergeylysyi.notes.note;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayNoteJson {

    public static List<Note> unwrap(Collection<NoteJson> noteJsonCollection) {
        ArrayList<Note> arrayList = new ArrayList<>();
        for (NoteJson noteJson : noteJsonCollection) {
            try {
                arrayList.add(noteJson.getNote());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    public static List<NoteJson> wrap(List<Note> notes) {
        List<NoteJson> notesJson = new ArrayList<>();
        for (Note note : notes) {
            notesJson.add(new NoteJson(note));
        }
        return notesJson;
    }

    public static class NoteJson {
        String title;
        String description;
        String color;
        String created;
        String edited;
        String viewed;
        private int intColor;

        NoteJson(Note note) {
            title = note.getTitle();
            description = note.getDescription();
            intColor = note.getColor();
            color = String.format("#%06x", note.getColor());
            created = note.getCreated();
            edited = note.getEdited();
            viewed = note.getViewed();
        }

        Note getNote() throws ParseException {
            return new Note(title, description, intColor, created, edited, viewed);
        }
    }

}
