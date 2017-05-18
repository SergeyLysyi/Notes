package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import sergeylysyi.notes.R;

public class DatePickerDialog implements android.view.View.OnClickListener {
    private Context context;
    private DateFormat dateFormat;
    private GregorianCalendar calendar;
    private AtomicBoolean presetDate;
    private Callback callback;

    /**
     * @param context    Application context.
     * @param dateFormat Format for date displayed.
     * @param calendar   Calendar instance to set and call with callback.
     * @param presetDate True - Set date from calendar or false - left TextView empty. Can vary after initialization.
     * @param callback   Called on successful input with GregorianCalendar as argument.
     */
    public DatePickerDialog(Context context, DateFormat dateFormat, final GregorianCalendar calendar, AtomicBoolean presetDate,
                            Callback callback) {
        this.context = context;
        this.dateFormat = dateFormat;
        if (calendar == null) {
            throw new IllegalArgumentException("calendar must be initialized");
        }
        this.calendar = calendar;
        this.presetDate = presetDate;
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        final DatePicker dp = new DatePicker(context);
        final TextView tv = (TextView) v;
        if (presetDate.get())
            tv.setText(dateFormat.format(calendar.getTime()));
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_pick_date)
                .setView(dp)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                        tv.setText(dateFormat.format(calendar.getTime()));
                        if (callback != null)
                            callback.call(calendar);
                    }
                })
                .create()
                .show();
    }
}
