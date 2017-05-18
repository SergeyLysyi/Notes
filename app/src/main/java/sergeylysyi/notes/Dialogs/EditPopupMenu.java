package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import sergeylysyi.notes.R;


public class EditPopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {
    private int itemIndex;
    private View anchor;
    private Context context;
    private String[] localNames;
    private String[] entriesNames;
    private Set<Integer> deletedFilters;
    private AtomicBoolean edited;

    /**
     * @param itemIndex      Position of tracked item in localNames and entriesNames.
     * @param localNames     localNames[itemIndex] changes if item was deleted.
     * @param entriesNames   entriesNames[itemIndex] changes if item was edited (renamed).
     * @param deletedFilters itemIndex of deleted item will be added here.
     * @param edited         Will be set to true if tracked item state changes.
     */
    public EditPopupMenu(@NonNull Context context, @NonNull View anchor,
                         int itemIndex,
                         String[] localNames,
                         String[] entriesNames,
                         Set<Integer> deletedFilters,
                         AtomicBoolean edited) {
        super(context, anchor);

        this.localNames = localNames;
        this.entriesNames = entriesNames;
        this.itemIndex = itemIndex;
        this.anchor = anchor;
        this.context = context;
        this.deletedFilters = deletedFilters;
        this.edited = edited;

        getMenuInflater().inflate(R.menu.dialog_filter_item_actions_menu, getMenu());
        setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                localNames[itemIndex] = context.getString(R.string.dialog_deleted_item_string_format,
                        context.getString(R.string.dialog_manage_filter_deleted_prefix),
                        entriesNames[itemIndex]);
                ((TextView) anchor).setText(localNames[itemIndex]);
                deletedFilters.add(itemIndex);
                return true;

            case R.id.menu_edit:
                String[] forbidden = new String[entriesNames.length - 1];
                System.arraycopy(entriesNames, 0, forbidden, 0, itemIndex);
                System.arraycopy(entriesNames, itemIndex + 1, forbidden, itemIndex, forbidden.length - itemIndex);

                new InputDialog(context, forbidden, entriesNames[itemIndex], new Callback() {
                    @Override
                    public void call(Object result) {
                        String name = (String) result;
                        if (!entriesNames[itemIndex].equals(name)) {
                            edited.set(true);
                            entriesNames[itemIndex] = name;
                            localNames[itemIndex] = entriesNames[itemIndex];
                            ((TextView) anchor).setText(localNames[itemIndex]);
                        }
                    }
                }).show();
                return true;
        }
        return false;
    }
}
