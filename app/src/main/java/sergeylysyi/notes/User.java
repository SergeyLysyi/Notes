package sergeylysyi.notes;

import sergeylysyi.notes.note.NoteSaver;
import sergeylysyi.notes.note.RemoteNotes.RESTClient;

public class User implements NoteSaver.DBUser, RESTClient.RemoteUser {
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
