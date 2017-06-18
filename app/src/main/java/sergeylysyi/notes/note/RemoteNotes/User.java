package sergeylysyi.notes.note.RemoteNotes;

public class User {
    private int userID;
    private String name;

    public User(String name, int id) {
        this.name = name;
        this.userID = id;
    }

    public String getName() {
        return this.name;
    }

    public int getUserID() {
        return userID;
    }
}
