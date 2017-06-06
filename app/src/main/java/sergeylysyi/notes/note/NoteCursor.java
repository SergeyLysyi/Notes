package sergeylysyi.notes.note;

import android.database.Cursor;

import java.io.Closeable;
import java.text.ParseException;


public class NoteCursor implements Closeable {
    final Cursor cursor;

    public NoteCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public Note getNote() throws ParseException {
        Note note = new Note(cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                cursor.getString(5), cursor.getString(6), cursor.getString(7));
        note.setID(getID());
        note.setImageURL(cursor.getString(4));
        return note;
    }

    public boolean moveToPosition(int position) {
        return cursor.moveToPosition(position);
    }

    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    public int getCount() {
        return cursor.getCount();
    }

    public boolean moveToNext() {
        return cursor.moveToNext();
    }

    public long getID() {
        return cursor.getLong(0);
    }

    public boolean isClosed() {
        return cursor.isClosed();
    }

    @Override
    public void close() {
        cursor.close();
    }
}
