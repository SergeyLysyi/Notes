package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;

import sergeylysyi.notes.R;

public class CriteriaDialog implements View.OnClickListener {

    private Context context;
    private String[] items;
    private Callback callback;

    /**
     * @param context  Application context.
     * @param items    Text items to display.
     * @param callback Called on successful input with index (int) of clicked item.
     */
    public CriteriaDialog(Context context, String[] items, Callback callback) {
        this.context = context;
        if (items == null) {
            throw new IllegalArgumentException("Items must be initialized");
        }
        this.items = items;
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_filter_pick_criteria)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null)
                            callback.call(which);
                    }
                })
                .setNegativeButton(R.string.dialog_negative_button, null)
                .create()
                .show();
    }
}
