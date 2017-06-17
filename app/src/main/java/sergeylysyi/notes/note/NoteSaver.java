package sergeylysyi.notes.note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


public class NoteSaver extends SQLiteOpenHelper {
    public static final String TAG = NoteSaver.class.getName();
    private static final String COLUMN_ID = BaseColumns._ID;
    private static final String COLUMN_SERVER_ID = "ServerID";
    private static final String COLUMN_TITLE = "Title";
    private static final String COLUMN_DESCRIPTION = "Description";
    private static final String COLUMN_COLOR = "Color";
    private static final String COLUMN_IMAGE_URL = "ImageURL";
    private static final String COLUMN_CREATED = "Created";
    private static final String COLUMN_EDITED = "Edited";
    private static final String COLUMN_VIEWED = "Opened";
    private static final String SORT_ORDER_ASCENDING = "ASC";
    private static final String SORT_ORDER_DESCENDING = "DESC";
    private static final String DB_NAME = "Notes.db";
    private static final int VERSION = 1;
    private static final String TABLE_NOTES = "Notes";
    private static final String DEFAULT_SORT_COLUMN = COLUMN_ID;
    private static final String DEFAULT_SORT_ORDER = SORT_ORDER_ASCENDING;
    private static final String CREATE_TABLE_QUERY = String.format(
            "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER, %s TEXT, %s TEXT, %s INTEGER, %s TEXT," +
                    " %s TEXT, %s TEXT, %s TEXT)",
            TABLE_NOTES, COLUMN_ID, COLUMN_SERVER_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_COLOR, COLUMN_IMAGE_URL,
            COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED);
    private static final String DROP_TABLE_QUERY = String.format("DROP TABLE IF EXISTS %s", TABLE_NOTES);

    public NoteSaver(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
    }

    private long addNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getNoteContentValues(note);
        long result = db.insert(TABLE_NOTES, null, values);
        note.setID(result);
        return result;
    }

    private int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getNoteContentValues(note);
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.getID())};
        return db.update(TABLE_NOTES, values, selection, selectionArgs);
    }

    private ContentValues getNoteContentValues(Note note) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, note.getServerID());
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_COLOR, note.getColor());
        values.put(COLUMN_IMAGE_URL, note.getImageUrl());
        values.put(COLUMN_CREATED, note.getCreated());
        values.put(COLUMN_EDITED, note.getEdited());
        values.put(COLUMN_VIEWED, note.getViewed());
        return values;
    }

    public boolean insertOrUpdate(Note note) {
        long result = updateNote(note);
        if (result == 0) {
            result = addNote(note);
        }
        return result <= 0;
    }

    public int deleteNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.getID())};
        return db.delete(TABLE_NOTES, selection, selectionArgs);
    }

    public void clear() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        db.execSQL(DROP_TABLE_QUERY);
        db.execSQL(CREATE_TABLE_QUERY);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void examine_debug() {
        SQLiteDatabase db = getWritableDatabase();
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED};
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, columns, null, null, null, null, null);
            Log.d(TAG, String.format("Total notes in query: %d\n", cursor.getCount()));
            if (cursor.moveToFirst()) {
                do {
                    Log.d(TAG, String.format("index:%d title:\"%s\" description:\"%s\"\n created:%s edited:%s viewed:%s\n",
                            cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4), cursor.getString(5)));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    protected List<Note> getNotes(String sortByColumn, String order,
                                  String titleSubstring, String descriptionSubstring,
                                  final String columnForDateFilter, final GregorianCalendar afterDate, final GregorianCalendar beforeDate) {

        List<Note> resultNotes = new ArrayList<>();
        NoteCursor cursor = getNotesCursor(
                sortByColumn, order, titleSubstring, descriptionSubstring,
                columnForDateFilter, afterDate, beforeDate);
        try {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Note note = cursor.getNote();
                        note.setID(cursor.getID());
                        resultNotes.add(note);
                    } catch (ParseException e) {
                        Log.e(TAG, String.format("ParseException at %d", cursor.getID()));
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return resultNotes;
    }

    protected NoteCursor getNotesCursor(String sortByColumn, String order,
                                        String titleSubstring, String descriptionSubstring,
                                        final String columnForDateFilter, final GregorianCalendar afterDate,
                                        final GregorianCalendar beforeDate) {
        if (sortByColumn != null)
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
        else
            sortByColumn = DEFAULT_SORT_COLUMN;
        if (order != null)
            switch (order) {
                case SORT_ORDER_ASCENDING:
                    break;
                case SORT_ORDER_DESCENDING:
                    break;
                default:
                    order = DEFAULT_SORT_ORDER;
            }
        else {
            order = DEFAULT_SORT_ORDER;
        }

        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {COLUMN_ID, COLUMN_SERVER_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_COLOR, COLUMN_IMAGE_URL,
                COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED};
        String selection = "";
        //TODO: protect from injecting
        if (titleSubstring != null && titleSubstring.length() > 0) {
            selection += String.format("%s LIKE \"%%%s%%\"", COLUMN_TITLE, titleSubstring);
        }
        if (descriptionSubstring != null && descriptionSubstring.length() > 0) {
            if (selection.length() > 0) {
                selection += " AND ";
            }
            selection += String.format("%s LIKE \"%%%s%%\"", COLUMN_DESCRIPTION, descriptionSubstring);
        }
        if (columnForDateFilter != null) {
            if (afterDate != null) {
                if (selection.length() > 0) {
                    selection += " AND ";
                }
                selection += String.format("datetime(\"%s\") <= datetime(%s)",
                        Note.formatDate(afterDate),
                        columnForDateFilter);
            }
            if (beforeDate != null) {
                if (selection.length() > 0) {
                    selection += " AND ";
                }
                selection += String.format("datetime(\"%s\") >= datetime(%s)",
                        Note.formatDate(beforeDate),
                        columnForDateFilter);
            }
        }
        if (selection.length() == 0) {
            selection = null;
        }
        return new NoteCursor(db.query(
                TABLE_NOTES, columns, selection, null, null, null, String.format("%s %s", sortByColumn, order)));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_QUERY);
        onCreate(db);
    }

    public enum NoteSortField {title, created, edited, viewed}

    public enum NoteSortOrder {ascending, descending}

    public enum NoteDateField {created, edited, viewed}


    static public class QueryFilter {
        public NoteSortOrder sortOrder;
        public NoteSortField sortField;
        public NoteDateField dateField;
        public GregorianCalendar after;
        public GregorianCalendar before;

        public QueryFilter() {
        }

        public QueryFilter(QueryFilter filter) {
            sortOrder = filter.sortOrder;
            sortField = filter.sortField;
            dateField = filter.dateField;
            after = filter.after;
            before = filter.before;
        }
    }

    public class Query {
        String sortByColumn = null;
        String sortWithOrder = null;
        String titleSubstring = null;
        String descriptionSubstring = null;
        String columnForDateFilter = null;
        GregorianCalendar afterDate = null;
        GregorianCalendar beforeDate = null;

        public Query() {
        }

        public Query fromFilter(QueryFilter filter) {
            return this
                    .sorted(filter.sortField, filter.sortOrder)
                    .betweenDatesOf(filter.dateField, filter.after, filter.before);
        }

        public Query sorted(NoteSortField byColumn, NoteSortOrder withOrder) {
            if (byColumn != null) {
                switch (byColumn) {
                    case title:
                        sortByColumn = NoteSaver.COLUMN_TITLE;
                        break;
                    case created:
                        sortByColumn = NoteSaver.COLUMN_CREATED;
                        break;
                    case edited:
                        sortByColumn = NoteSaver.COLUMN_EDITED;
                        break;
                    case viewed:
                        sortByColumn = NoteSaver.COLUMN_VIEWED;
                        break;
                    default:
                        throw new IllegalArgumentException("no matching case for argument \"byField\"");
                }
            }

            if (withOrder != null) {
                switch (withOrder) {
                    case ascending:
                        sortWithOrder = NoteSaver.SORT_ORDER_ASCENDING;
                        break;
                    case descending:
                        sortWithOrder = NoteSaver.SORT_ORDER_DESCENDING;
                        break;
                    default:
                        throw new IllegalArgumentException("no matching case for argument \"withOrder\" ");
                }
            }
            return this;
        }

        public Query withSubstring(String titleSubstring, String descriptionSubstring) {
            this.titleSubstring = titleSubstring;
            this.descriptionSubstring = descriptionSubstring;
            return this;
        }

        public Query betweenDatesOf(NoteDateField column, GregorianCalendar after, GregorianCalendar before) {
            if (column != null) {
                switch (column) {
                    case created:
                        columnForDateFilter = NoteSaver.COLUMN_CREATED;
                        break;
                    case edited:
                        columnForDateFilter = NoteSaver.COLUMN_EDITED;
                        break;
                    case viewed:
                        columnForDateFilter = NoteSaver.COLUMN_VIEWED;
                        break;
                    default:
                        throw new IllegalArgumentException("no matching case for argument \"sortField\"");
                }
            }
            afterDate = after;
            beforeDate = before;
            return this;
        }

        public List<Note> get() {
            return NoteSaver.this.getNotes(sortByColumn, sortWithOrder, titleSubstring, descriptionSubstring,
                    columnForDateFilter, afterDate, beforeDate);
        }

        public NoteCursor getCursor() {
            return NoteSaver.this.getNotesCursor(sortByColumn, sortWithOrder, titleSubstring, descriptionSubstring,
                    columnForDateFilter, afterDate, beforeDate);
        }
    }
}
