package sergeylysyi.notes;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class UserActivity extends AppCompatActivity implements UserAdapter.OnUserClick {
    public static final String TAG = UserActivity.class.getName();

    public static final int REQUEST_USER = 0;
    public static final String USERS_ARRAY = "users_array";
    public static final String KEY_RECORD_ID = "RecordID";
    public static final String KEY_USER_ID = "User_id";
    public static final String KEY_USER_NAME = "User_name";
    private List<User> users;
    private UserAdapter adapter;
    private JsonAdapter<List<User>> jsonUserAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user);

        Moshi moshi = new Moshi.Builder().build();
        Type type = Types.newParameterizedType(List.class, User.class);
        jsonUserAdapter = moshi.adapter(type);
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String json = sharedPreferences.getString(USERS_ARRAY, "");
        try {
            if (!"".equals(json)) {
                users = jsonUserAdapter.fromJson(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (users == null) {
            users = new ArrayList<>();
        }
        adapter = new UserAdapter(this, users, this);
        RecyclerView rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void launchAdd(View view) {
        newUser(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void newUser(final Runnable successCallback) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.add_user_layout, null);
        final Dialog d = new AlertDialog.Builder(this, 0)
                .setTitle(R.string.user_add)
                .setView(v)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String username = ((EditText) v.findViewById(R.id.username)).getText().toString();
                        String id = ((EditText) v.findViewById(R.id.userID)).getText().toString();
                        if (username.length() <= 0) {
                            Toast.makeText(UserActivity.this, R.string.new_user_username_not_empty, Toast.LENGTH_LONG).show();
                        } else if (id.length() <= 0) {
                            Toast.makeText(UserActivity.this, R.string.new_user_id_not_empty, Toast.LENGTH_LONG).show();
                        } else {
                            users.add(new User(username, Integer.parseInt(id)));
                            successCallback.run();
                        }
                    }
                })
                .create();
        d.show();
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent();
        intent.putExtra(KEY_RECORD_ID, users.indexOf(user));
        intent.putExtra(KEY_USER_ID, user.getUserID());
        intent.putExtra(KEY_USER_NAME, user.getName());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = jsonUserAdapter.toJson(users);
        editor.putString(USERS_ARRAY, json);
        editor.apply();
    }
}
