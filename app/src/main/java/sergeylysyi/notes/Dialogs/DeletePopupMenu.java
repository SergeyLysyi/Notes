package sergeylysyi.notes.Dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import sergeylysyi.notes.R;


public class DeletePopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {
    private final Callback callback;

    /**
     * @param onRestore Called if restore button clicked with null argument.
     */
    public DeletePopupMenu(@NonNull Context context, @NonNull View anchor, Callback onRestore) {
        super(context, anchor);
        this.callback = onRestore;
        getMenuInflater().inflate(R.menu.dialog_filter_deleted_item_actions, getMenu());
        setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_restore:
                callback.call(null);
                return true;
        }
        return false;
    }
}
