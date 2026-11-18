package com.example.sunchan.goodsaying;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

final class CustomList extends BaseAdapter {
    interface MoveItem {
        void move(int id, int target);
    }

    private final Context context;
    private final MoveItem moveItem;
    private List<Item> items = Collections.emptyList();

    CustomList(Context context, MoveItem moveItem) {
        this.context = context;
        this.moveItem = moveItem;
    }

    void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Item getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return getItem(position).getId(); }
    @Override public boolean hasStableIds() { return true; }

    @Override
    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = LayoutInflater.from(context).inflate(R.layout.list_layout, parent, false);
        }
        Item item = getItem(position);
        TextView text = row.findViewById(R.id.text_view);
        text.setText(item.getText());
        ImageView notificationStatus = row.findViewById(R.id.notification_status);
        boolean enabled = item.isNotificationEnabled();
        notificationStatus.setImageResource(R.drawable.ic_reminder_on);
        notificationStatus.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        notificationStatus.setContentDescription(enabled
                ? context.getString(R.string.reminder_status_enabled) : null);
        View handle = row.findViewById(R.id.drag_handle);
        final View dragRow = row;
        handle.setOnLongClickListener(view -> view.startDrag(
                ClipData.newPlainText("", ""), new View.DragShadowBuilder(dragRow), item, 0));
        handle.setOnClickListener(view -> new AlertDialog.Builder(context)
                .setTitle(R.string.reorder_saying)
                .setItems(new CharSequence[]{context.getText(R.string.move_up), context.getText(R.string.move_down)}, (dialog, choice) ->
                        moveItem.move(item.getId(), position + (choice == 0 ? -1 : 1)))
                .show());
        return row;
    }
}
