package sergeylysyi.notes;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import sergeylysyi.notes.note.NoteSaver;

public class FiltersHolder {

    static private final String VERSION = "1";
    static private final String KEY_VERSION = "version";
    static private final String KEY_PREFIX = FiltersHolder.class.getName().concat("_");
    static private final String KEY_DEFAULT_FILTER_PREFIX = "default.";
    static private final String KEY_CURRENT_FILTER_PREFIX = "current.";
    static private final String KEY_FILTERS_AMOUNT = "amount";
    static private final String KEY_FILTER_NAME = "name";
    static private final String KEY_SORT_ORDER = "sortOrder";
    static private final String KEY_SORT_FIELD = "sort_field";
    static private final String KEY_DATE_FIELD = "date_field";
    static private final String KEY_DATE_AFTER = "date_after";
    static private final String KEY_DATE_BEFORE = "date_before";
    static private final int VALUE_FOR_NULL = -8;
    static private final int VALUE_FOR_EXCEPTION_INT = -1;
    static private final String VALUE_FOR_EXCEPTION_STRING = null;

    private List<NoteSaver.QueryFilter> filters = new ArrayList<>();
    private NoteSaver.QueryFilter defaultFilter;
    private NoteSaver.QueryFilter currentFilter;
    private String[] names = new String[0];
    ;

    private FiltersHolder() {
    }

    public FiltersHolder(
            NoteSaver.NoteSortField defaultSortField,
            NoteSaver.NoteSortOrder defaultSortOrder,
            NoteSaver.NoteDateField defaultDateField) {
        NoteSaver.QueryFilter defaultFilter = new NoteSaver.QueryFilter();
        defaultFilter.sortField = defaultSortField;
        defaultFilter.sortOrder = defaultSortOrder;
        defaultFilter.dateField = defaultDateField;
        this.defaultFilter = defaultFilter;
        this.currentFilter = new NoteSaver.QueryFilter(this.defaultFilter);
    }

    public static FiltersHolder fromSettings(SharedPreferences settings) {
        FiltersHolder filtersHolder = new FiltersHolder();

        String version = settings.getString(KEY_VERSION, VALUE_FOR_EXCEPTION_STRING);
        int filtersAmount = settings.getInt(getKeyClass(KEY_FILTERS_AMOUNT), VALUE_FOR_EXCEPTION_INT);

        String filterName;
        filtersHolder.defaultFilter = getFilter(settings, KEY_DEFAULT_FILTER_PREFIX);
        for (int i = filtersAmount - 1; i >= 0; i--) {
            filtersHolder.currentFilter = getFilter(settings, getFilterItemKey(i));
            filterName = getFilterItemName(settings, getFilterItemKey(i));
            filtersHolder.add(filterName);
        }
        filtersHolder.currentFilter = getFilter(settings, KEY_CURRENT_FILTER_PREFIX);
        return filtersHolder;
    }

    static private String getKeyClass(String keySuffix) {
        return KEY_PREFIX.concat(keySuffix);
    }

    static private String getKeyFilterName(String filterName, String keySuffix) {
        return getKeyClass(KEY_FILTER_NAME).concat(filterName).concat(keySuffix);
    }

    private static String getFilterItemName(SharedPreferences preferences, String uniqueKey) {
        return preferences.getString(getKeyFilterName(uniqueKey, KEY_FILTER_NAME), VALUE_FOR_EXCEPTION_STRING);
    }

    private static NoteSaver.QueryFilter getFilter(SharedPreferences preferences, String uniqueKey) {
        GregorianCalendar calendar;

        NoteSaver.QueryFilter filter = new NoteSaver.QueryFilter();

        int sortOrderOrdinal = preferences.getInt(getKeyFilterName(uniqueKey, KEY_SORT_ORDER), VALUE_FOR_EXCEPTION_INT);
        if (sortOrderOrdinal != VALUE_FOR_NULL)
            filter.sortOrder = NoteSaver.NoteSortOrder.values()[sortOrderOrdinal];

        int sortFieldOrdinal = preferences.getInt(getKeyFilterName(uniqueKey, KEY_SORT_FIELD), VALUE_FOR_EXCEPTION_INT);
        if (sortFieldOrdinal != VALUE_FOR_NULL)
            filter.sortField = NoteSaver.NoteSortField.values()[sortFieldOrdinal];

        int dateFieldOrdinal = preferences.getInt(getKeyFilterName(uniqueKey, KEY_DATE_FIELD), VALUE_FOR_EXCEPTION_INT);
        if (dateFieldOrdinal != VALUE_FOR_NULL)
            filter.dateField = NoteSaver.NoteDateField.values()[dateFieldOrdinal];

        long afterInMillis = preferences.getLong(getKeyFilterName(uniqueKey, KEY_DATE_AFTER), VALUE_FOR_EXCEPTION_INT);
        if (afterInMillis != VALUE_FOR_NULL) {
            calendar = new GregorianCalendar();
            calendar.setTimeInMillis(afterInMillis);
            filter.after = calendar;
        }

        long beforeInMillis = preferences.getLong(getKeyFilterName(uniqueKey, KEY_DATE_BEFORE), VALUE_FOR_EXCEPTION_INT);
        if (beforeInMillis != VALUE_FOR_NULL) {
            calendar = new GregorianCalendar();
            calendar.setTimeInMillis(beforeInMillis);
            filter.before = calendar;
        }
        return filter;
    }

    private static String getFilterItemKey(int i) {
        return String.valueOf(i);
    }

    /**
     * @return Copy of current filter. Sort field, order and date always not null.
     */
    public NoteSaver.QueryFilter getCurrentFilterCopy() {
        NoteSaver.QueryFilter filter = new NoteSaver.QueryFilter();
        filter.sortField = currentFilter.sortField != null ? currentFilter.sortField : defaultFilter.sortField;
        filter.sortOrder = currentFilter.sortOrder != null ? currentFilter.sortOrder : defaultFilter.sortOrder;
        filter.dateField = currentFilter.dateField != null ? currentFilter.dateField : defaultFilter.dateField;
        filter.after = currentFilter.after != null ? currentFilter.after : defaultFilter.after;
        filter.before = currentFilter.before != null ? currentFilter.before : defaultFilter.before;
        return filter;
    }

    /**
     * Set fields from filter to current filter. Null fields will be ignored.
     *
     * @param filter Filter from which values will be taken.
     */
    public void setCurrentFilterFrom(NoteSaver.QueryFilter filter) {
        currentFilter.sortField = filter.sortField != null ? filter.sortField : currentFilter.sortField;
        currentFilter.sortOrder = filter.sortOrder != null ? filter.sortOrder : currentFilter.sortOrder;
        currentFilter.dateField = filter.dateField != null ? filter.dateField : currentFilter.dateField;
        currentFilter.after = filter.after != null ? filter.after : currentFilter.after;
        currentFilter.before = filter.before != null ? filter.before : currentFilter.before;
    }

    public final String[] getFilterNames() {
        return names;
    }

    public void add(String filterName) {
        String[] afterAddition = new String[names.length + 1];
        afterAddition[0] = filterName;
        System.arraycopy(names, 0, afterAddition, 1, names.length);
        names = afterAddition;
        filters.add(0, new NoteSaver.QueryFilter(currentFilter));
    }

    public void remove(int[] deleted) {
        String[] afterDeletion = new String[names.length - deleted.length];
        int prev = 0;
        int destPos = prev;
        Arrays.sort(deleted);
        List<NoteSaver.QueryFilter> forDeletion = new ArrayList<>(deleted.length);
        for (int d : deleted) {
            System.arraycopy(names, prev, afterDeletion, destPos, d - prev);
            destPos += d - prev;
            prev = d + 1;
            forDeletion.add(filters.get(d));
        }
        filters.removeAll(forDeletion);
        System.arraycopy(names, prev, afterDeletion, destPos, names.length - prev);
        names = afterDeletion;
    }

    public void apply(String name) {
        int i = 0;
        while (i < names.length && !names[i].equals(name)) {
            i++;
        }
        currentFilter = new NoteSaver.QueryFilter(filters.get(i));
        if (filters.size() != names.length) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * reset current filter to default
     */
    public void reset() {
        currentFilter = new NoteSaver.QueryFilter(defaultFilter);
    }

    /**
     * Stores all filters but current to SharedPreferences and applies changes
     *
     * @param prefs SharedPreferences instance to store filters
     */
    public void storeToPreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(getKeyClass(KEY_VERSION), VERSION);
        editor.putInt(getKeyClass(KEY_FILTERS_AMOUNT), filters.size());

        putCurrentFilter(editor);
        putDefaultFilter(editor);
        for (int i = 0; i < filters.size(); i++) {
            putFilterItem(editor, i);
        }
        editor.apply();
    }

    private void putCurrentFilter(SharedPreferences.Editor editor) {
        putFilter(editor, KEY_CURRENT_FILTER_PREFIX, KEY_CURRENT_FILTER_PREFIX, currentFilter);
    }

    private void putDefaultFilter(SharedPreferences.Editor editor) {
        putFilter(editor, KEY_DEFAULT_FILTER_PREFIX, KEY_DEFAULT_FILTER_PREFIX, defaultFilter);
    }

    private void putFilterItem(SharedPreferences.Editor editor, int i) {
        putFilter(editor, getFilterItemKey(i), names[i], filters.get(i));
    }

    private void putFilter(SharedPreferences.Editor editor,
                           String uniqueKey,
                           String name,
                           NoteSaver.QueryFilter filter) {
        int sortOrderOrdinal = VALUE_FOR_NULL;
        int sortFieldOrdinal = VALUE_FOR_NULL;
        int dateFieldOrdinal = VALUE_FOR_NULL;
        long afterInMillis = VALUE_FOR_NULL;
        long beforeInMillis = VALUE_FOR_NULL;

        editor.putString(getKeyFilterName(uniqueKey, KEY_FILTER_NAME), name);

        if (filter.sortOrder != null)
            sortOrderOrdinal = filter.sortOrder.ordinal();
        if (filter.sortField != null)
            sortFieldOrdinal = filter.sortField.ordinal();
        if (filter.dateField != null)
            dateFieldOrdinal = filter.dateField.ordinal();
        if (filter.after != null)
            afterInMillis = filter.after.getTimeInMillis();
        if (filter.before != null)
            beforeInMillis = filter.before.getTimeInMillis();

        editor.putInt(getKeyFilterName(uniqueKey, KEY_SORT_ORDER), sortOrderOrdinal);
        editor.putInt(getKeyFilterName(uniqueKey, KEY_SORT_FIELD), sortFieldOrdinal);
        editor.putInt(getKeyFilterName(uniqueKey, KEY_DATE_FIELD), dateFieldOrdinal);
        editor.putLong(getKeyFilterName(uniqueKey, KEY_DATE_AFTER), afterInMillis);
        editor.putLong(getKeyFilterName(uniqueKey, KEY_DATE_BEFORE), beforeInMillis);
    }
}
