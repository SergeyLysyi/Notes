package sergeylysyi.notes.Dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import sergeylysyi.notes.R;
import sergeylysyi.notes.note.NoteSaver;

public class DialogInvoker {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    private final Context context;

    public DialogInvoker(Activity context) {
        this.context = context;
    }

    public void sortDialog(NoteSaver.NoteSortField currentSortField, NoteSaver.NoteSortOrder currentSortOrder,
                           final ResultListener listener) {
        final NoteSaver.NoteSortOrder[] noteSortOrder = new NoteSaver.NoteSortOrder[1];
        final NoteSaver.NoteSortField[] noteSortField = new NoteSaver.NoteSortField[1];
        noteSortOrder[0] = currentSortOrder;
        noteSortField[0] = currentSortField;
        Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_sort_title)
                .setView(R.layout.sort_layout)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.dialog_sort_positive_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                NoteSaver.QueryFilter result = new NoteSaver.QueryFilter();
                                result.sortOrder = noteSortOrder[0];
                                result.sortField = noteSortField[0];
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
                        noteSortOrder[0] = NoteSaver.NoteSortOrder.ascending;
                        break;
                    case R.id.descending:
                        noteSortOrder[0] = NoteSaver.NoteSortOrder.descending;
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
                        noteSortField[0] = NoteSaver.NoteSortField.title;
                        break;
                    case R.id.sort_by_created:
                        noteSortField[0] = NoteSaver.NoteSortField.created;
                        break;
                    case R.id.sort_by_edited:
                        noteSortField[0] = NoteSaver.NoteSortField.edited;
                        break;
                    case R.id.sort_by_Viewed:
                        noteSortField[0] = NoteSaver.NoteSortField.viewed;
                        break;
                }
            }
        });
    }

    public void searchDialog(final ResultListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.search_layout, null);
        Callback listenerCallback = new Callback() {
            @Override
            public void call(Object result) {
                String[] stringResult = (String[]) result;
                DialogInvoker.SearchDialogResult searchResult = new DialogInvoker.SearchDialogResult();
                searchResult.title = stringResult[0];
                searchResult.description = stringResult[1];
                listener.onSearchDialogResult(searchResult);
            }
        };
        final Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_search_title)
                .setView(v)
                .setNegativeButton(R.string.dialog_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onSearchCancel();
                    }
                })
                .setPositiveButton(R.string.dialog_search_positive_button,
                        new SearchClickListener(
                                (EditText) v.findViewById(R.id.search_field_title),
                                (EditText) v.findViewById(R.id.search_field_description),
                                listenerCallback))
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        listener.onSearchCancel();
                    }
                })
                .create();
        d.show();
    }

    public void filterDialog(NoteSaver.QueryFilter currentFilter, final ResultListener listener) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final NoteSaver.NoteDateField[] dateField = new NoteSaver.NoteDateField[1];
        dateField[0] = currentFilter.dateField;
        final AtomicBoolean afterSet = new AtomicBoolean(false);
        final AtomicBoolean beforeSet = new AtomicBoolean(false);
        final GregorianCalendar after;
        final GregorianCalendar before;
        if (currentFilter.after != null) {
            afterSet.set(true);
            after = currentFilter.after;
        } else {
            after = new GregorianCalendar();
        }
        if (currentFilter.before != null) {
            beforeSet.set(true);
            before = currentFilter.before;
        } else {
            before = new GregorianCalendar();
        }

        final View v = inflater.inflate(R.layout.filter_layout, null);
        final Dialog d = new AlertDialog.Builder(context, 0)
                .setTitle(R.string.dialog_filter_title)
                .setView(v)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setPositiveButton(R.string.dialog_filter_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NoteSaver.QueryFilter result = new NoteSaver.QueryFilter();
                        result.dateField = dateField[0];
                        if (afterSet.get())
                            result.after = after;
                        if (beforeSet.get())
                            result.before = before;
                        listener.onFilterDialogResult(result);
                    }
                })
                .create();
        d.show();
        final TextView tv = (TextView) v.findViewById(R.id.column_filter);
        final String[] fields = new String[]{
                context.getString(R.string.dialog_filter_field_created),
                context.getString(R.string.dialog_filter_field_edited),
                context.getString(R.string.dialog_filter_field_viewed)};
        tv.setText(fields[dateField[0].ordinal()]);
        tv.setOnClickListener(
                new CriteriaDialog(context, fields, new Callback() {
                    @Override
                    public void call(Object result) {
                        int which = (int) result;
                        dateField[0] = NoteSaver.NoteDateField.values()[which];
                        tv.setText(fields[which]);
                    }
                }));

        final TextView timeAfterView = (TextView) v.findViewById(R.id.time_after_filter);
        final TextView dateAfterView = (TextView) v.findViewById(R.id.date_after_filter);
        final TextView timeBeforeView = (TextView) v.findViewById(R.id.time_before_filter);
        final TextView dateBeforeView = (TextView) v.findViewById(R.id.date_before_filter);

        Callback onAfterChange = new Callback() {
            @Override
            public void call(Object result) {
                if (result != null)
                    afterSet.set(true);
                if (afterSet.get()) {
                    timeAfterView.setText(timeFormat.format(after.getTime()));
                    dateAfterView.setText(dateFormat.format(after.getTime()));
                }
            }
        };
        Callback onBeforeChange = new Callback() {
            @Override
            public void call(Object result) {
                if (result != null)
                    beforeSet.set(true);
                if (beforeSet.get()) {
                    timeBeforeView.setText(timeFormat.format(before.getTime()));
                    dateBeforeView.setText(dateFormat.format(before.getTime()));
                }
            }
        };
        // preset time from argument
        onAfterChange.call(null);
        onBeforeChange.call(null);

        timeAfterView.setOnClickListener(
                new TimePickerDialog(context, after, onAfterChange));
        dateAfterView.setOnClickListener(
                new DatePickerDialog(context, after, onAfterChange));
        timeBeforeView.setOnClickListener(
                new TimePickerDialog(context, before, onBeforeChange));
        dateBeforeView.setOnClickListener(
                new DatePickerDialog(context, before, onBeforeChange));
    }

    public void manageFiltersDialog(final String[] entriesNames, final ManageFiltersResultListener listener) {
        final Set<Integer> deletedFilters = new HashSet<>();
        final String[] localNames = new String[entriesNames.length];
        System.arraycopy(entriesNames, 0, localNames, 0, localNames.length);
        final AtomicBoolean edited = new AtomicBoolean(false);

        final Callback editedOrDeletedCall = new Callback() {
            @Override
            public void call(Object result) {
                if (edited.get() || !deletedFilters.isEmpty()) {
                    int[] deleted = new int[deletedFilters.size()];
                    int i = 0;
                    for (Integer index : deletedFilters) {
                        deleted[i] = index;
                        i++;
                    }
                    listener.onEditFilterEntries(deleted);
                }
            }
        };
        final AlertDialog d = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_manage_filters_title)
                .setNegativeButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editedOrDeletedCall.call(null);
                    }
                })
                .setNeutralButton(R.string.dialog_manage_filters_add, null)
                .setItems(localNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editedOrDeletedCall.call(null);
                        listener.onApplyFilterEntry(entriesNames[which]);
                    }
                })
                .create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                d.getListView().setOnItemLongClickListener(
                        new ListItemLongClickListener(context, entriesNames, deletedFilters, localNames, edited));
                // button listener is set here to keep dialog open if addition failed
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new InputDialog(context, entriesNames, new Callback() {
                                    @Override
                                    public void call(Object result) {
                                        d.dismiss();
                                        editedOrDeletedCall.call(null);
                                        listener.onAddFilterEntry((String) result);
                                    }
                                }).show();
                            }
                        });
            }
        });
        d.show();
    }

    public interface SortResultListener {
        void onSortDialogResult(NoteSaver.QueryFilter result);
    }

    public interface FilterResultListener {
        void onFilterDialogResult(NoteSaver.QueryFilter result);
    }

    public interface SearchResultListener {
        void onSearchDialogResult(SearchDialogResult result);

        void onSearchCancel();
    }

    public interface ManageFiltersResultListener {
        void onEditFilterEntries(int[] deletedEntriesIndexes);

        void onAddFilterEntry(String newEntryName);

        void onApplyFilterEntry(String entryName);
    }

    public interface ResultListener extends
            SortResultListener,
            FilterResultListener,
            SearchResultListener,
            ManageFiltersResultListener {
    }

    public static class SearchDialogResult {
        public String title;
        public String description;
    }
}
