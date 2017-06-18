package sergeylysyi.notes.note;

import android.database.Cursor;

import java.io.Closeable;
import java.text.ParseException;


public class NoteCursor implements Closeable {
    private final Cursor cursor;

    public NoteCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public Note getNote() throws ParseException {
        Note note = new Note(cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getString(6),
                cursor.getString(7), cursor.getString(8), cursor.getString(9));
        note.setID(getID());
        if (!cursor.isNull(1)) {
            note.setServerID(cursor.getInt(1));
        }
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
