package sergeylysyi.notes.note;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sergeylysyi.notes.HelperTextView;
import sergeylysyi.notes.MainActivity;
import sergeylysyi.notes.R;


public class NoteListAdapter extends ArrayAdapter {
    private final LayoutInflater inflater;
    private final int resource;
    private NoteCursor cursor;
    private List<Note> notes;

    public NoteListAdapter(@NonNull Context context, @LayoutRes int resource) {
        this(context, resource, new ArrayList<Note>());
    }

    public NoteListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Note> objects) {
        super(context, resource, objects);
        inflater = LayoutInflater.from(getContext());
        this.resource = resource;
        this.notes = objects;
    }

    public void updateData(NoteCursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    @Nullable
    @Override
    public Note getItem(int position) {
        try {
            cursor.moveToPosition(position);
            return cursor.getNote();
        } catch (NullPointerException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getCount() {
        int number;
        if (cursor != null) {
            number = cursor.getCount();
        } else {
            number = 0;
        }
        return number;
    }

    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(resource, parent, false);
            holder = new ViewHolder();
            holder.helper = ((HelperTextView) convertView.findViewById(R.id.helper));
            holder.header = ((TextView) convertView.findViewById(R.id.title));
            holder.body = ((TextView) convertView.findViewById(R.id.description));
            holder.rectangle = ((ImageView) convertView.findViewById(R.id.color));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) v.getContext()).launchEdit(getItem(holder.position));
                }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ((MainActivity) v.getContext()).launchDelete(getItem(holder.position));
                    return true;
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);
        holder.position = position;
        holder.header.setText(note.getTitle());
        holder.body.setText(note.getDescription());
        holder.rectangle.setBackgroundColor(note.getColor());

        return convertView;
    }

    private static class ViewHolder {
        int position;
        HelperTextView helper;
        TextView header;
        TextView body;
        ImageView rectangle;
    }
}
