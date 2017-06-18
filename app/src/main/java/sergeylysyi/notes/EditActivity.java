package sergeylysyi.notes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import sergeylysyi.notes.note.Note;

import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_COLOR;
import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_COLOR_TO_EDIT;
import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_IS_CHANGED;


public class EditActivity extends AppCompatActivity {
    public static final String TAG = EditActivity.class.getName();

    public static final String INTENT_KEY_NOTE = "note";
    public static final String INTENT_KEY_NOTE_IS_CHANGED = "isChanged";
    public static final int EDIT_NOTE = 1;
    public static final String SAVED_KEY_CURRENT_COLOR = "current_color";
    public static final String SAVED_KEY_NOTE = "note";

    private EditText headerField;
    private EditText bodyField;
    private EditText imageURL;
    private ImageView imageFromURL;

    private CurrentColor currentColor;
    private Note noteToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        noteToEdit = intent.getParcelableExtra(INTENT_KEY_NOTE);
        currentColor = new CurrentColor(noteToEdit.getColor());

        currentColor.addViewForBackgroundChange(findViewById(R.id.colorView));

        headerField = (EditText) findViewById(R.id.title);
        bodyField = (EditText) findViewById(R.id.description);
        imageURL = (EditText) findViewById(R.id.imageURL);
        imageFromURL = (ImageView) findViewById(R.id.imageFromURL);
        headerField.setText(noteToEdit.getTitle());
        bodyField.setText(noteToEdit.getDescription());
        final String imageURLText = noteToEdit.getImageUrl();
        imageURL.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    loadImage();
            }
        });
        imageURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loadImage();
                }
                return false;
            }
        });
        if (imageURLText.length() > 0) {
            imageURL.setText(imageURLText);
        }

        findViewById(R.id.colorView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paletteForResult();
            }
        });
    }

    private void loadImage() {
        final String imageURLText = imageURL.getText().toString();
        if (imageURLText.length() > 0) {
            imageFromURL.setVisibility(View.VISIBLE);
            Picasso.with(EditActivity.this).load(imageURLText).into(imageFromURL, new Callback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError() {
                    imageFromURL.setVisibility(View.GONE);
                    Toast.makeText(EditActivity.this, R.string.load_from_url_error, Toast.LENGTH_LONG).show();
                    Log.w(TAG, "onError: error loading image on url:\"" + imageURLText + "\"");
                }
            });
        }
    }

    void paletteForResult() {
        Intent intent = new Intent(this, ScrollPalette.class);
        intent.putExtra(INTENT_KEY_COLOR_TO_EDIT, currentColor.getColor());
        startActivityForResult(intent, ScrollPalette.REQUEST_PALETTE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finishWithResult(false);
                return true;
            case R.id.action_done:
                finishWithResult(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finishWithResult(false);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void finishWithResult(boolean result) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_KEY_NOTE_IS_CHANGED, result);
        noteToEdit.setTitle(headerField.getText().toString());
        noteToEdit.setDescription(bodyField.getText().toString());
        noteToEdit.setColor(currentColor.getColor());
        noteToEdit.setImageURL(imageURL.getText().toString());
        intent.putExtra(INTENT_KEY_NOTE, noteToEdit);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ScrollPalette.REQUEST_PALETTE:
                    if (data.getBooleanExtra(INTENT_KEY_IS_CHANGED, false)) {
                        currentColor.change(data.getIntExtra(INTENT_KEY_COLOR, 0));
                    }
                    return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        loadImage();
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_KEY_CURRENT_COLOR, currentColor.getColor());
        outState.putParcelable(SAVED_KEY_NOTE, noteToEdit);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int color = savedInstanceState.getInt(SAVED_KEY_CURRENT_COLOR);
        currentColor.change(color);
        noteToEdit = savedInstanceState.getParcelable(SAVED_KEY_NOTE);
    }
}
