package sergeylysyi.notes.note.RemoteNotes;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import sergeylysyi.notes.note.NoteJsonAdapter;

interface IServer {

    @GET("info/")
    Call<Response.Info> getInfo();

    @GET("user/{userID}/notes")
    Call<Response.Notes> getNotes(@Path("userID") int userID);

    @GET("user/{userID}/note/{noteID}")
    Call<Response.Note> getNote(@Path("userID") int userID, @Path("noteID") int noteID);

    @POST("user/{userID}/notes")
    Call<Response.PostNote> postNote(@Path("userID") int userID, @Body NoteJsonAdapter.NoteJson note);

    @POST("user/{userID}/note/{noteID}")
    Call<Response.EditNote> editNote(@Path("userID") int userID, @Path("noteID") int noteID, @Body NoteJsonAdapter.NoteJson note);

    @DELETE("user/{userID}/note/{noteID}")
    Call<Response.DeleteNote> deleteNote(@Path("userID") int userID, @Path("noteID") int noteID);
}
