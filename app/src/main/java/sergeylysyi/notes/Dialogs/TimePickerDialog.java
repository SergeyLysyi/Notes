package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TimePicker;

import java.util.GregorianCalendar;

import sergeylysyi.notes.R;

public class TimePickerDialog implements View.OnClickListener {
    private Context context;
    private GregorianCalendar calendar;
    private Callback callback;

    /**
     * @param context  Application context.
     * @param calendar Calendar instance to set and call with callback.
     * @param callback Called on successful input with GregorianCalendar as argument, or on unsuccessful with null.
     */
    public TimePickerDialog(Context context, final GregorianCalendar calendar, Callback callback) {
        this.context = context;
        if (calendar == null) {
            throw new IllegalArgumentException("calendar must be initialized");
        }
        this.calendar = calendar;
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        final TimePicker tp = new TimePicker(context);
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_pick_time_title)
                .setView(tp)
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
                        calendar.set(GregorianCalendar.HOUR, tp.getCurrentHour());
                        calendar.set(GregorianCalendar.MINUTE, tp.getCurrentMinute());
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
