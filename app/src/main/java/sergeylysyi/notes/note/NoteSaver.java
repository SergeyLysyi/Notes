package sergeylysyi.notes.note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


public class NoteSaver extends SQLiteOpenHelper {
    private static final String COLUMN_TITLE = "Title";
    private static final String COLUMN_DESCRIPTION = "Description";
    private static final String COLUMN_COLOR = "Color";
    private static final String COLUMN_CREATED = "Created";
    private static final String COLUMN_EDITED = "Edited";
    private static final String COLUMN_VIEWED = "Opened";
    private static final String SORT_ORDER_ASCENDING = "ASC";
    private static final String SORT_ORDER_DESCENDING = "DESC";
    private static final String DB_NAME = "Notes.db";
    private static final int VERSION = 1;
    private static final String TABLE_NOTES = "Notes";
    private static final String COLUMN_ID = BaseColumns._ID;
    private static final String DEFAULT_SORT_COLUMN = COLUMN_ID;
    private static final String DEFAULT_SORT_ORDER = SORT_ORDER_ASCENDING;
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

    public void repopulateWith(List<Note> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(DROP_TABLE_QUERY);
        db.execSQL(CREATE_TABLE_QUERY);
        for (Note note : notes) {
            addNote(db, note);
        }
    }

    public void examine_debug() {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_CREATED, COLUMN_EDITED, COLUMN_VIEWED};
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, columns, null, null, null, null, null);
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

    private List<Note> getNotes(String sortByColumn, String order,
                                String titleSubstring, String descriptionSubstring,
                                String columnForDateFilter, GregorianCalendar afterDate, GregorianCalendar beforeDate) {
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
        List<Note> notesBeforeDateFilter = new ArrayList<>();
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_COLOR,
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
        Cursor cursor = null;
        if (selection.length() == 0) {
            selection = null;
        }
        try {
            cursor = db.query(TABLE_NOTES, columns, selection, null, null, null, String.format("%s %s", sortByColumn, order));
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Note note = new Note(cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                                cursor.getString(4), cursor.getString(5), cursor.getString(6));
                        note._id = cursor.getInt(0);
                        notesBeforeDateFilter.add(note);
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

        List<Note> notesDateFiltered = new ArrayList<>(notesBeforeDateFilter);
        if (columnForDateFilter != null) {
            for (Note note : notesBeforeDateFilter) {
                GregorianCalendar c;
                try {
                    switch (columnForDateFilter) {
                        case COLUMN_CREATED:
                            c = Note.parseDate(note.getCreated());
                            break;
                        case COLUMN_EDITED:
                            c = Note.parseDate(note.getEdited());
                            break;
                        case COLUMN_VIEWED:
                            c = Note.parseDate(note.getViewed());
                            break;
                        default:
                            throw new IllegalArgumentException("columnForDateFilter must be" +
                                    " from class FILTER_DATE_* constants");
                    }
                } catch (IllegalArgumentException | ParseException e) {
                    e.printStackTrace();
                    continue;
                }
                if (!((afterDate == null || c.after(afterDate)) && (beforeDate == null || c.before(beforeDate)))) {
                    notesDateFiltered.remove(note);
                }
            }
        }
        return notesDateFiltered;
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
    }
}
