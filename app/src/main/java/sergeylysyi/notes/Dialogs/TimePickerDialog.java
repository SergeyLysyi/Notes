package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import sergeylysyi.notes.R;

public class TimePickerDialog implements View.OnClickListener {
    private Context context;
    private DateFormat dateFormat;
    private GregorianCalendar calendar;
    private AtomicBoolean presetTime;
    private Callback callback;

    /**
     * @param context    Application context.
     * @param dateFormat Format for time displayed.
     * @param calendar   Calendar instance to set and call with callback.
     * @param presetTime True - Set time from calendar or false - left TextView empty. Can vary after initialization.
     * @param callback   Called on successful input with GregorianCalendar as argument.
     */
    public TimePickerDialog(Context context, DateFormat dateFormat, final GregorianCalendar calendar, AtomicBoolean presetTime,
                            Callback callback) {
        this.context = context;
        this.dateFormat = dateFormat;
        if (calendar == null) {
            throw new IllegalArgumentException("calendar must be initialized");
        }
        this.calendar = calendar;
        this.presetTime = presetTime;
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        final TimePicker tp = new TimePicker(context);
        final TextView tv = (TextView) v;
        if (presetTime.get())
            tv.setText(dateFormat.format(calendar.getTime()));
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_pick_time_title)
                .setView(tp)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        calendar.set(GregorianCalendar.HOUR, tp.getCurrentHour());
                        calendar.set(GregorianCalendar.MINUTE, tp.getCurrentMinute());
                        tv.setText(dateFormat.format(calendar.getTime()));
                        if (callback != null)
                            callback.call(calendar);
                    }
                })
                .create()
                .show();
    }
}
