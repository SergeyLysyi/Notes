package sergeylysyi.notes;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import sergeylysyi.notes.note.RemoteNotes.User;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    public static final String TAG = UserAdapter.class.getName();

    private List<User> users;
    private OnUserClick onUserClick;
    private Context context;

    public UserAdapter(Context context, List<User> users, OnUserClick userClick) {
        this.onUserClick = userClick;
        this.users = users;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contactView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_user, parent, false);
        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.getName());
        holder.id.setText(context.getString(R.string.item_user_id, user.getUserID()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    interface OnUserClick {
        void onUserClick(User user);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView id;

        public ViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            id = (TextView) itemView.findViewById(R.id.id);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onUserClick.onUserClick(users.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PopupMenu menu;
                    menu = new PopupMenu(v.getContext(), v);
                    menu.getMenuInflater().inflate(R.menu.dialog_user_actions_menu, menu.getMenu());
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int position = getAdapterPosition();
                            users.remove(position);
                            UserAdapter.this.notifyItemRemoved(position);
                            return true;
                        }
                    });
                    menu.show();
                    return true;
                }
            });
        }
    }
}
