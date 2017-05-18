package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListItemLongClickListener implements ListView.OnItemLongClickListener {

    private final String[] localNames;
    private Context context;
    private String[] entriesNames;
    private Set<Integer> deletedFilters;
    private AtomicBoolean edited;

    /**
     * @param context        Activity context.
     * @param localNames     localNames[itemIndex] changes if item was deleted.
     * @param entriesNames   entriesNames[itemIndex] changes if item was edited (renamed).
     * @param deletedFilters itemIndex of deleted item will be added here.
     * @param edited         Will be set to true if tracked item state changes.
     */
    public ListItemLongClickListener(Context context,
                                     String[] entriesNames,
                                     Set<Integer> deletedFilters,
                                     String[] localNames,
                                     AtomicBoolean edited) {
        this.context = context;
        this.entriesNames = entriesNames;
        this.deletedFilters = deletedFilters;
        this.localNames = localNames;
        this.edited = edited;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, final long id) {
        PopupMenu menu;
        if (!deletedFilters.contains(position)) {
            menu = new EditPopupMenu(context, view, position, localNames, entriesNames, deletedFilters, edited);
        } else {
            menu = new DeletePopupMenu(context, view, new Callback() {
                @Override
                public void call(Object result) {
                    ((TextView) view).setText(entriesNames[position]);
                    deletedFilters.remove(position);
                }
            });
        }
        menu.show();
        return true;
    }
}
