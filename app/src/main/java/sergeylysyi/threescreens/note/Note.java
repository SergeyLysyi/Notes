package sergeylysyi.threescreens.note;

import android.os.Parcel;
import android.os.Parcelable;

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
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");

    //for NoteSaver purposes
    long _id;

    private String title;
    private String description;
    private int color;
    private GregorianCalendar creationDate;
    private GregorianCalendar lastEditDate;
    private GregorianCalendar lastOpenDate;

    public Note() {
        this("", "", 0);
    }

    public Note(String title, String description, int color) {
        this.title = title;
        this.description = description;
        this.color = color;
        creationDate = new GregorianCalendar(TimeZone.getDefault());
        lastEditDate = creationDate;
        lastOpenDate = creationDate;
    }

    Note(String title, String description, int color,
         String creationDate, String lastEditDate, String lastOpenDate) throws ParseException {
        this(title, description, color);
        this.creationDate = parseDate(creationDate);
        this.lastEditDate = parseDate(lastEditDate);
        this.lastOpenDate = parseDate(lastOpenDate);
    }

    private Note(Parcel in) {
        title = in.readString();
        description = in.readString();
        color = in.readInt();
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
    private static GregorianCalendar parseDate(String date) throws ParseException {
        Date d = sdf.parse(date);
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
    private static String formatDate(GregorianCalendar calendar) {
        sdf.setTimeZone(calendar.getTimeZone());
        return sdf.format(calendar.getTime());
    }

    private void updateEditDate() {
        Date currentTime = new Date();
        lastEditDate.setTime(currentTime);
    }

    public void updateOpenDate() {
        Date currentTime = new Date();
        lastOpenDate.setTime(currentTime);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String newHeader) {
        this.title = newHeader;
        updateEditDate();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newBody) {
        this.description = newBody;
        updateEditDate();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int newColor) {
        this.color = newColor;
        updateEditDate();
    }

    String getCreated() {
        return Note.formatDate(creationDate);
    }

    String getEdited() {
        return Note.formatDate(lastEditDate);
    }

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
        dest.writeString(creationDate.getTimeZone().getID());
        dest.writeString(lastEditDate.getTimeZone().getID());
        dest.writeString(lastOpenDate.getTimeZone().getID());
        dest.writeString(creationDate.getTime().toString());
        dest.writeString(lastEditDate.getTime().toString());
        dest.writeString(lastOpenDate.getTime().toString());
    }
}
