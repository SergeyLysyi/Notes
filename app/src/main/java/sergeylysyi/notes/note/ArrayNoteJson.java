package sergeylysyi.notes.note;

import android.graphics.Color;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

public class ArrayNoteJson {

    private static final JsonAdapter<NoteJson> jsonSingleNoteAdapter;

    static {
        Moshi moshi = new Moshi.Builder().build();
        jsonSingleNoteAdapter = moshi.adapter(ArrayNoteJson.NoteJson.class);
    }

    private JsonReader unpackJsonReader;
    private JsonWriter packJsonWriter;
    private Buffer unpackBuffer;
    private Buffer packBuffer;
    private Long startingUnpackSize;
    private InputStream unpackInputStream;

    public ArrayNoteJson() {
    }

    public static List<NoteJson> wrap(List<Note> notes) {
        List<NoteJson> notesJson = new ArrayList<>();
        for (Note note : notes) {
            notesJson.add(new NoteJson(note));
        }
        return notesJson;
    }

    public void startPack() throws IOException {
        packBuffer = new Buffer();
        packJsonWriter = JsonWriter.of(packBuffer);
        packJsonWriter.beginArray();
    }

    public void startUnpack(InputStream inputStream) throws IOException {
        unpackBuffer = new Buffer();
        unpackInputStream = inputStream;
        unpackBuffer.readFrom(unpackInputStream);
        startingUnpackSize = unpackBuffer.size();
        unpackJsonReader = JsonReader.of(unpackBuffer);
        unpackJsonReader.beginArray();
    }

    public List<Note> readNextPack(int packSize) throws IOException {
        List<Note> result = new ArrayList<>();
        unpackBuffer.readFrom(unpackInputStream);
        while (unpackJsonReader.hasNext() && result.size() < packSize) {
            Object v = unpackJsonReader.readJsonValue();
            ArrayNoteJson.NoteJson nj = jsonSingleNoteAdapter.fromJsonValue(v);
            try {
                result.add(nj.getNote());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public double unpackFractionLeft() {
        if (startingUnpackSize == null) {
            return 0;
        }
        return (double) unpackBuffer.size() / (double) startingUnpackSize;
    }

    public byte[] pack(List<Note> notes) throws IOException {
        for (Note note : notes) {
            jsonSingleNoteAdapter.toJson(packJsonWriter, new NoteJson(note));
        }
        return packBuffer.readByteArray();
    }

    public byte[] endPack() throws IOException {
        packJsonWriter.endArray();
        return packBuffer.readByteArray();
    }

    public static class NoteJson {
        public static final String COLOR_FORMAT = "#%06x";
        String title;
        String description;
        String imageUrl;
        String color;
        String created;
        String edited;
        String viewed;

        NoteJson(Note note) {
            title = note.getTitle();
            description = note.getDescription();
            color = String.format(COLOR_FORMAT, note.getColor());
            imageUrl = note.getImageUrl();
            created = note.getCreated();
            edited = note.getEdited();
            viewed = note.getViewed();
        }

        Note getNote() throws ParseException {
            return new Note(title, description, Color.parseColor(color), imageUrl, created, edited, viewed);
        }
    }

}