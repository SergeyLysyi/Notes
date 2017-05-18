package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import sergeylysyi.notes.R;


class InputDialog {
    private final AlertDialog dialog;

    /**
     * @param context          Activity context.
     * @param forbiddenStrings Forbidden result values.
     * @param callback         Called with successful input. Result will be not empty String.
     */
    InputDialog(final Context context, final String[] forbiddenStrings, final Callback callback) {
        this(context, forbiddenStrings, null, callback);
    }

    /**
     * @param context          Activity context.
     * @param forbiddenStrings Forbidden result values.
     * @param callback         Called with successful input. Result will be not empty String.
     * @param textToFill       Fill TextView with that text. Use null to left empty.
     */
    InputDialog(final Context context, final String[] forbiddenStrings, String textToFill, final Callback callback) {
        final EditText input = new EditText(context);
        input.setHint(R.string.dialog_manage_filters_input_hint);
        if (textToFill != null) {
            input.setText(textToFill);
        }
        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_filter_manager_input_title)
                .setView(input)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.dialog_positive_button, null)
                .create();

        // button listener is set here to keep dialog open if editing failed
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface d) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String filterName = input.getText().toString();
                        if (filterName.length() == 0) {
                            Toast.makeText(context, R.string.dialog_manage_filters_input_ifempty,
                                    Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }
                        if (forbiddenStrings != null) {
                            for (String existingName : forbiddenStrings) {
                                if (filterName.equals(existingName)) {
                                    Toast.makeText(context, R.string.dialog_manage_filters_add_already_exists,
                                            Toast.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                            }
                        }
                        dialog.dismiss();
                        callback.call(filterName);
                    }
                });
            }
        });
    }

    void show() {
        dialog.show();
    }
}
