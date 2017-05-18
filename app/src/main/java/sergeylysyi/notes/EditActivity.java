package sergeylysyi.notes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_COLOR;
import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_COLOR_TO_EDIT;
import static sergeylysyi.notes.ScrollPalette.INTENT_KEY_IS_CHANGED;


public class EditActivity extends AppCompatActivity {
    public static final String INTENT_KEY_NOTE_TITLE = "header";
    public static final String INTENT_KEY_NOTE_DESCRIPTION = "body";
    public static final String INTENT_KEY_NOTE_COLOR = "color";
    public static final String INTENT_KEY_NOTE_INDEX = "index";
    public static final String INTENT_KEY_NOTE_IS_CHANGED = "isChanged";
    public static final int EDIT_NOTE = 1;
    public static final int DEFAULT_COLOR_FOR_INTENT = 0;
    public static final int DEFAULT_INDEX_FOR_INTENT = -1;
    public static final String SAVED_KEY_CURRENT_COLOR = "current_color";

    private int index;

    private EditText headerField;
    private EditText bodyField;

    private CurrentColor currentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        currentColor = new CurrentColor(intent.getIntExtra(INTENT_KEY_NOTE_COLOR, DEFAULT_COLOR_FOR_INTENT));
        index = intent.getIntExtra(INTENT_KEY_NOTE_INDEX, DEFAULT_INDEX_FOR_INTENT);

        currentColor.addViewForBackgroundChange(findViewById(R.id.colorView));

        headerField = (EditText) findViewById(R.id.title);
        bodyField = (EditText) findViewById(R.id.description);
        headerField.setText(intent.getStringExtra(INTENT_KEY_NOTE_TITLE));
        bodyField.setText(intent.getStringExtra(INTENT_KEY_NOTE_DESCRIPTION));

        findViewById(R.id.colorView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paletteForResult();
            }
        });
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
        intent.putExtra(INTENT_KEY_NOTE_TITLE, headerField.getText().toString());
        intent.putExtra(INTENT_KEY_NOTE_DESCRIPTION, bodyField.getText().toString());
        intent.putExtra(INTENT_KEY_NOTE_COLOR, currentColor.getColor());
        intent.putExtra(INTENT_KEY_NOTE_INDEX, index);
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_KEY_CURRENT_COLOR, currentColor.getColor());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int color = savedInstanceState.getInt(SAVED_KEY_CURRENT_COLOR);
        currentColor.change(color);
    }
}
