package sergeylysyi.notes.note;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Note implements Parcelable {

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override
        public Note createFromParcel(Parcel in) {
            return new Note(in);
        }

        @Override
        public Note[] newArray(int size) {
            return new Note[size];
        }
    };
    public static final String TAG = "Note";
    static final long ID_IF_NOT_IN_DB = -1;
    static final int ID_IF_NOT_ON_SERVER = -1;
    private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
    private static final String DEFAULT_TITLE = "";
    private static final String DEFAULT_DESCRIPTION = "";
    //for NoteSaver purposes
    private long _id = ID_IF_NOT_IN_DB;
    private int _sid = ID_IF_NOT_ON_SERVER;

    private String title;
    private String description;
    private int color;
    private String imageUrl;
    private GregorianCalendar creationDate;
    private GregorianCalendar lastEditDate;
    private GregorianCalendar lastOpenDate;

    public Note() {
        this(DEFAULT_TITLE, DEFAULT_DESCRIPTION, 0, null);
    }

    public Note(String title, String description, int color, String imageUrl) {
        this.title = title;
        this.description = description;
        this.color = color;
        this.imageUrl = imageUrl;
        creationDate = new GregorianCalendar(TimeZone.getDefault());
        lastEditDate = creationDate;
        lastOpenDate = creationDate;
    }

    Note(String title, String description, int color, String imageUrl,
         String creationDate, String lastEditDate, String lastOpenDate) throws ParseException {
        this(title, description, color, imageUrl);
        this.creationDate = parseDate(creationDate);
        this.lastEditDate = parseDate(lastEditDate);
        this.lastOpenDate = parseDate(lastOpenDate);
    }

    private Note(Parcel in) {
        title = in.readString();
        description = in.readString();
        color = in.readInt();
        imageUrl = in.readString();
        String creationTimeZoneID = in.readString();
        String editTimeZoneID = in.readString();
        String openTimeZoneID = in.readString();
        Date creation = new Date(in.readString());
        Date lastEdit = new Date(in.readString());
        Date lastOpen = new Date(in.readString());

        creationDate = new GregorianCalendar(TimeZone.getTimeZone(creationTimeZoneID));
        lastEditDate = new GregorianCalendar(TimeZone.getTimeZone(editTimeZoneID));
        lastOpenDate = new GregorianCalendar(TimeZone.getTimeZone(openTimeZoneID));
        creationDate.setTime(creation);
        lastEditDate.setTime(lastEdit);
        lastOpenDate.setTime(lastOpen);
    }

    /**
     * Parse date string to local time.
     *
     * @param date YYYY-MM-DDThh:mm:ss±hh:mm ISO 8601 date.
     * @return calendar with local time zone.
     * @throws ParseException - if the beginning of the specified string cannot be parsed.
     */
    static GregorianCalendar parseDate(String date) throws ParseException {
        Date d;
        try {
            d = date_format.parse(date);
        } catch (ParseException e) {
            // trying to catch mystery error where string is ok, but exception occurs
            Log.e(TAG, "parseDate: PARSE EXCEPTION ON DATE >\"" + date + "\"<", e);
            d = date_format.parse(date);
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(d);
        return calendar;
    }

    /**
     * Format calendar to ISO 8601 YYYY-MM-DDThh:mm:ss±hh:mm date string with calendar time zone.
     *
     * @param calendar calendar.
     * @return ISO 8601 YYYY-MM-DDThh:mm:ss±hh:mm date string.
     */
    static String formatDate(GregorianCalendar calendar) {
        date_format.setTimeZone(calendar.getTimeZone());
        try {
            return date_format.format(calendar.getTime());
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, "formatDate: calendar = " + calendar);
            throw e;
        }
    }

    private void updateEditDate() {
        Date currentTime = new Date();
        lastEditDate.setTime(currentTime);
    }

    public void updateOpenDate() {
        Date currentTime = new Date();
        lastOpenDate.setTime(currentTime);
    }

    /**
     * @return long if id was set or null if not.
     */
    @Nullable
    public Long getID() {
        if (_id == ID_IF_NOT_IN_DB)
            return null;
        return _id;
    }

    /**
     * @param id id to set.
     * @return id of note (might differ from argument if note already has id).
     */
    public long setID(long id) {
        Long noteID;
        if ((noteID = getID()) != null)
            return noteID;
        else
            return _id = id;
    }

    /**
     * @return long if id was set or null if not.
     */
    @Nullable
    public Integer getServerID() {
        if (_sid == ID_IF_NOT_ON_SERVER)
            return null;
        else
            return _sid;
    }

    public void setServerID(int id) {
        _sid = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(String newTitle) {
        if (newTitle == null)
            newTitle = "";
        this.title = newTitle;
        updateEditDate();
    }

    public void setImageURL(String imageUrl) {
        this.imageUrl = imageUrl;
        updateEditDate();
    }

    @NonNull
    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        if (newDescription == null)
            newDescription = "";
        this.description = newDescription;
        updateEditDate();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int newColor) {
        this.color = newColor;
        updateEditDate();
    }

    @NonNull
    String getCreated() {
        return Note.formatDate(creationDate);
    }

    @NonNull
    String getEdited() {
        return Note.formatDate(lastEditDate);
    }

    @NonNull
    String getViewed() {
        return Note.formatDate(lastOpenDate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(description);
        dest.writeInt(color);
        dest.writeString(imageUrl);
        dest.writeString(creationDate.getTimeZone().getID());
        dest.writeString(lastEditDate.getTimeZone().getID());
        dest.writeString(lastOpenDate.getTimeZone().getID());
        dest.writeString(creationDate.getTime().toString());
        dest.writeString(lastEditDate.getTime().toString());
        dest.writeString(lastOpenDate.getTime().toString());
    }

    public boolean localToRemoteEquals(Note note) {
        return this.getServerID() != null &&
                note.getServerID() != null &&
                this.getServerID().equals(note.getServerID()) &&
                this.getTitle().equals(note.getTitle()) &&
                this.getDescription().equals(note.getDescription()) &&
                this.getCreated().equals(note.getCreated()) &&
                this.getEdited().equals(note.getEdited()) &&
                this.getViewed().equals(note.getViewed()) &&
                this.getImageUrl().equals(note.getImageUrl());
    }
}
