package sergeylysyi.notes.Dialogs;

import android.content.DialogInterface;
import android.widget.EditText;

public class SearchClickListener implements DialogInterface.OnClickListener {

    private EditText titleEditor;
    private EditText descriptionEditor;
    private Callback callback;

    /**
     * @param titleEditor       View which input should be interpreted as title.
     * @param descriptionEditor View which input should be interpreted as description.
     * @param callback  Callback for result on successful input. Argument will be String[2]:
     *                  result[0] - title;
     *                  result[1] - description;
     */
    public SearchClickListener(EditText titleEditor, EditText descriptionEditor, Callback callback) {
        this.titleEditor = titleEditor;
        this.descriptionEditor = descriptionEditor;
        this.callback = callback;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String patternTitle = titleEditor.getText().toString();
        String patternDescription = descriptionEditor.getText().toString();
        if (patternTitle.length() + patternDescription.length() > 0) {
            callback.call(new String[]{patternTitle, patternDescription});
        }
    }
}
