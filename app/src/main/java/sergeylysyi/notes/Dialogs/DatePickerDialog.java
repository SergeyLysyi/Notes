package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;

import java.util.GregorianCalendar;

import sergeylysyi.notes.R;

public class DatePickerDialog implements android.view.View.OnClickListener {
    private Context context;
    private GregorianCalendar calendar;
    private Callback callback;

    /**
     * @param context  Application context.
     * @param calendar Calendar instance to set and call with callback.
     * @param callback Called on successful input with GregorianCalendar as argument, or on unsuccessful with null.
     */
    public DatePickerDialog(Context context, final GregorianCalendar calendar, Callback callback) {
        this.context = context;
        if (calendar == null) {
            throw new IllegalArgumentException("calendar must be initialized");
        }
        this.calendar = calendar;
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        final DatePicker dp = new DatePicker(context);
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_pick_date)
                .setView(dp)
                .setNegativeButton(R.string.dialog_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null)
                            callback.call(null);
                    }
                })
                .setPositiveButton(R.string.dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                        if (callback != null)
                            callback.call(calendar);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (callback != null)
                            callback.call(null);
                    }
                })
                .create()
                .show();
    }
}
