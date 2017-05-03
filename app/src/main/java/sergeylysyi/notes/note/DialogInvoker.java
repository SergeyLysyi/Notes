package sergeylysyi.notes.note;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import sergeylysyi.notes.MainActivity;
import sergeylysyi.notes.R;

public class DialogInvoker {

    private final Context context;

    public DialogInvoker(Activity context) {
        this.context = context;
    }

    public void sortDialog(MainActivity.NoteSortOrder orderPreference, MainActivity.NoteSortField fieldPreference,
                           final ResultListener listener) {
        final MainActivity.NoteSortOrder[] noteSortOrder = new MainActivity.NoteSortOrder[1];
        final MainActivity.NoteSortField[] noteSortField = new MainActivity.NoteSortField[1];
        noteSortOrder[0] = orderPreference;
        noteSortField[0] = fieldPreference;
        Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle("Sort")
                .setView(R.layout.sort_layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sort", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SortDialogResult result = new SortDialogResult();
                                result.order = noteSortOrder[0];
                                result.field = noteSortField[0];
                                listener.onSortDialogResult(result);
                            }
                        }
                )
                .create();
        d.show();
        final RadioGroup order = ((RadioGroup) d.findViewById(R.id.sort_order_rg));
        ((RadioButton) order.getChildAt(noteSortOrder[0].ordinal())).setChecked(true);
        order.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.ascending:
                        noteSortOrder[0] = MainActivity.NoteSortOrder.ascending;
                        break;
                    case R.id.descending:
                        noteSortOrder[0] = MainActivity.NoteSortOrder.descending;
                        break;
                }
            }
        });
        final RadioGroup column = ((RadioGroup) d.findViewById(R.id.sort_column_rg));
        ((RadioButton) column.getChildAt(noteSortField[0].ordinal())).setChecked(true);
        column.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (column.getCheckedRadioButtonId()) {
                    case R.id.sort_by_title:
                        noteSortField[0] = MainActivity.NoteSortField.title;
                        break;
                    case R.id.sort_by_created:
                        noteSortField[0] = MainActivity.NoteSortField.created;
                        break;
                    case R.id.sort_by_edited:
                        noteSortField[0] = MainActivity.NoteSortField.edited;
                        break;
                    case R.id.sort_by_Viewed:
                        noteSortField[0] = MainActivity.NoteSortField.viewed;
                        break;
                }
            }
        });
    }

    public void searchDialog(final ResultListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.search_layout, null);
        final Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle("Search")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String patternTitle = ((EditText) v.findViewById(R.id.search_field_title))
                                .getText()
                                .toString();
                        String patternDescription = ((EditText) v.findViewById(R.id.search_field_description))
                                .getText()
                                .toString();
                        if (patternTitle.length() + patternDescription.length() > 0) {
                            SearchDialogResult result = new SearchDialogResult();
                            result.title = patternTitle;
                            result.description = patternDescription;
                            listener.onSearchDialogResult(result);
                        } else {
                            System.out.println("empty search pattern");
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        listener.onSearchCancel();
                    }
                })
                .create();
        d.show();
    }

    public void filterDialog(final ResultListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.filter_layout, null);
        final MainActivity.NoteDateField[] dateField = new MainActivity.NoteDateField[1];
        final GregorianCalendar[] after = new GregorianCalendar[1];
        final GregorianCalendar[] before = new GregorianCalendar[1];
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        dateField[0] = MainActivity.NoteDateField.created;
        after[0] = null;
        before[0] = null;
        final Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle("Filter")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Filter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FilterDialogResult result = new FilterDialogResult();
                        result.dateField = dateField[0];
                        result.after = after[0];
                        result.before = before[0];
                        listener.onFilterDialogResult(result);
                    }
                })
                .create();
        d.show();
        final TextView tv = (TextView) v.findViewById(R.id.column_filter);
        final String[] fields = new String[]{"Created", "Edited", "Viewed"};
        tv.setText(fields[dateField[0].ordinal()]);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context, 0)
                        .setTitle("Pick Time")
                        .setItems(fields, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dateField[0] = MainActivity.NoteDateField.values()[which];
                                tv.setText(fields[which]);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            }
        });

        v.findViewById(R.id.time_after_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimePicker tp = new TimePicker(context);
                //TODO:set hour, minute
                final TextView tv = (TextView) v;
                new AlertDialog.Builder(context, 0)
                        .setTitle("Pick Time")
                        .setView(tp)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (after[0] == null) {
                                    after[0] = new GregorianCalendar();
                                }
                                after[0].set(GregorianCalendar.HOUR, tp.getCurrentHour());
                                after[0].set(GregorianCalendar.MINUTE, tp.getCurrentMinute());
                                tv.setText(timeFormat.format(after[0].getTime()));
                            }
                        })
                        .create()
                        .show();
            }
        });
        v.findViewById(R.id.date_after_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatePicker dp = new DatePicker(context);
                //TODO: set date
                final TextView tv = (TextView) v;
                new AlertDialog.Builder(context, 0)
                        .setTitle("Pick Date")
                        .setView(dp)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (after[0] == null) {
                                    after[0] = new GregorianCalendar();
                                }
                                after[0].set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                                tv.setText(dateFormat.format(after[0].getTime()));
                            }
                        })
                        .create()
                        .show();
            }
        });

        v.findViewById(R.id.time_before_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimePicker tp = new TimePicker(context);
                //TODO:set hour, minute
                final TextView tv = (TextView) v;
                new AlertDialog.Builder(context, 0)
                        .setTitle("Pick Time")
                        .setView(tp)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (before[0] == null) {
                                    before[0] = new GregorianCalendar();
                                }
                                before[0].set(GregorianCalendar.HOUR, tp.getCurrentHour());
                                before[0].set(GregorianCalendar.MINUTE, tp.getCurrentMinute());
                                tv.setText(timeFormat.format(before[0].getTime()));
                            }
                        })
                        .create()
                        .show();
            }
        });
        v.findViewById(R.id.date_before_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatePicker dp = new DatePicker(context);
                //TODO: set date
                final TextView tv = (TextView) v;
                new AlertDialog.Builder(context, 0)
                        .setTitle("Pick Date")
                        .setView(dp)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (before[0] == null) {
                                    before[0] = new GregorianCalendar();
                                }
                                before[0].set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                                tv.setText(dateFormat.format(before[0].getTime()));
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    public interface SortResultListener {
        void onSortDialogResult(SortDialogResult result);
        void onSearchCancel();
    }

    public interface FilterResultListener {
        void onFilterDialogResult(FilterDialogResult result);
    }

    public interface SearchResultListener {
        void onSearchDialogResult(SearchDialogResult result);
    }

    public interface ResultListener extends SortResultListener, FilterResultListener, SearchResultListener {
    }

    public static class SortDialogResult {
        public MainActivity.NoteSortOrder order;
        public MainActivity.NoteSortField field;
    }

    public static class FilterDialogResult {
        public MainActivity.NoteDateField dateField;
        public GregorianCalendar after;
        public GregorianCalendar before;
    }

    public static class SearchDialogResult {
        public String title;
        public String description;
    }
}
