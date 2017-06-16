package sergeylysyi.notes.note.RemoteNotes;

public class Errors extends Throwable {
    private Errors(String e) {
        super(e);
    }

    private Errors(Throwable e) {
        super(e);
    }

    public static class FailedRequest extends Errors {
        FailedRequest(String error) {
            super(error);
        }

        FailedRequest(Throwable e) {
            super(e);
        }
    }

    public static class IncorrectRequest extends Errors {
        IncorrectRequest(String errorBodyString) {
            super(errorBodyString);
        }
    }

    public static class UnparsableRecord extends Errors {
        UnparsableRecord(Throwable e) {
            super(e);
        }
    }
}
