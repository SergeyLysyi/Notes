package sergeylysyi.threescreens.note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class NoteSaver extends SQLiteOpenHelper {
    public static final String DB_NAME = "Notes.db";
    public static final int VERSION = 1;
    public static final String TABLE_NOTES = "Notes";
    public static final String COLUMN_TITLE = "Title";
    public static final String COLUMN_DESCRIPTION = "Description";
    public static final String COLUMN_COLOR = "Color";
    public static final String COLUMN_CREATED = "Created";
    public static final String COLUMN_EDITED = "Edited";
    public static final String COLUMN_VIEWED = "Opened";
    public static final String SORT_ORDER_ASCENDING = "ASC";
    public static final String SORT_ORDER_DESCENDING = "DESC";
    private static final String COLUMN_ID = BaseColumns._ID;
    private static final String DEFAULT_SORT_COLUMN = COLUMN_EDITED;
    private static final String DEFAULT_SORT_ORDER = SORT_ORDER_DESCENDING;

    private static final String CREATE_TABLE_QUERY = String.format(
            "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s INTEGER, " +
                    "%s TEXT, %s TEXT, %s TEXT)",
            TABLE_NOTES, COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_COLOR,
            COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED);

    private static final String DROP_TABLE_QUERY = String.format("DROP TABLE IF EXISTS %s", TABLE_NOTES);

    public NoteSaver(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
    }

    private long addNote(SQLiteDatabase db, Note note) {
        ContentValues values = new ContentValues(3);
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_COLOR, note.getColor());
        values.put(COLUMN_CREATED, note.getCreated());
        values.put(COLUMN_EDITED, note.getEdited());
        values.put(COLUMN_VIEWED, note.getViewed());
        long result = db.insert(TABLE_NOTES, null, values);
        note._id = result;
        return result;
    }

    private int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_COLOR, note.getColor());
        values.put(COLUMN_CREATED, note.getCreated());
        values.put(COLUMN_EDITED, note.getEdited());
        values.put(COLUMN_VIEWED, note.getViewed());
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(note._id)};
        try {
            return db.update(TABLE_NOTES, values, selection, selectionArgs);
        } finally {
            db.close();
        }
    }

    public boolean insertOrUpdate(Note note) {
        long result = updateNote(note);
        if (result == 0) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                result = addNote(db, note);
            } finally {
                db.close();
            }
        }
        return result > 0;
    }

    public int deleteNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(note._id)};
        try {
            return db.delete(TABLE_NOTES, selection, selectionArgs);
        } finally {
            db.close();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_QUERY);
        onCreate(db);
    }

    public List<Note> getAllNotes() {
        return getNotes(DEFAULT_SORT_COLUMN, DEFAULT_SORT_ORDER);
    }

    public List<Note> getAllSorted(String byColumn, String withOrder) {
        return getNotes(byColumn, withOrder);
    }

    private List<Note> getNotes(String sortByColumn, String order) {
        switch (sortByColumn) {
            case COLUMN_TITLE:
                break;
            case COLUMN_CREATED:
                break;
            case COLUMN_EDITED:
                break;
            case COLUMN_VIEWED:
                break;
            default:
                sortByColumn = DEFAULT_SORT_COLUMN;
        }
        switch (order) {
            case SORT_ORDER_ASCENDING:
                break;
            case SORT_ORDER_DESCENDING:
                break;
            default:
                order = DEFAULT_SORT_ORDER;
        }
//        System.out.println(String.format("ORDER BY %s %s\n", sortByColumn, order));
        SQLiteDatabase db = getReadableDatabase();
        List<Note> notes = new ArrayList<>();
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_COLOR,
                COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED};
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, columns, null, null, null, null, String.format("%s %s", sortByColumn, order));
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Note note = new Note(cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                                cursor.getString(4), cursor.getString(5), cursor.getString(6));
                        note._id = cursor.getInt(0);
                        notes.add(note);
                    } catch (ParseException e) {
                        System.err.println(String.format("ParseException at %s: %d", COLUMN_ID, cursor.getInt(0)));
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.close();
        }
        System.out.println("Queried notes:");
        for (Note note : notes) {
            System.out.println(note.getTitle());
        }
        return notes;
    }

    public void repopulateWith(List<Note> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(DROP_TABLE_QUERY);
        db.execSQL(CREATE_TABLE_QUERY);
        for (Note note : notes) {
            addNote(db, note);
        }
    }

    public void examine() {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED};
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, columns, null, null, null, null, null);
            System.out.println("EXAMINE :::");
            System.out.format("Total notes in query: %d\n", cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    System.out.printf("index:%d title:\"%s\" description:\"%s\"\n created:%s edited:%s viewed:%s\n",
                            cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4), cursor.getString(5));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.close();
        }
    }
}
