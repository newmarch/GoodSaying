package com.example.sunchan.goodsaying;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class NotiActivity extends AppCompatActivity {
    static final String EXTRA_ID = "saying_id";

    private DBHandler db;
    private ScrollView scroll;
    private TextView text;
    private CheckBox eligibility;
    private ArrayList<Item> items;
    private int currentId;
    private int index;
    private float downX;
    private float downY;
    private boolean canGoNext;
    private boolean canGoPrevious;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.noti_layout);
        ScreenInsets.apply(this);
        setSupportActionBar((Toolbar) findViewById(R.id.sub_toolbar));
        db = new DBHandler(this);
        currentId = state == null ? getIntent().getIntExtra(EXTRA_ID, -1)
                : state.getInt(EXTRA_ID, -1);
        index = state == null ? 0 : state.getInt("index");
        scroll = findViewById(R.id.saying_scroll);
        text = findViewById(R.id.noti_text_view);
        eligibility = findViewById(R.id.notification_enabled);
        DisplayStyle style = DisplayStyle.load(this);
        style.apply(findViewById(R.id.noti_layout),
                findViewById(R.id.saying_background), text, style.readSavedImage(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        items = db.getAllItems();
        if (items.isEmpty()) {
            finish();
            return;
        }
        for (int position = 0; position < items.size(); position++) {
            if (items.get(position).getId() == currentId) {
                show(position);
                return;
            }
        }
        show(index % items.size());
    }

    private void show(int position) {
        index = position;
        Item item = items.get(position);
        currentId = item.getId();
        text.setText(db.readText(currentId));
        scroll.scrollTo(0, 0);
        eligibility.setOnCheckedChangeListener(null);
        eligibility.setChecked(item.isNotificationEnabled());
        eligibility.setOnCheckedChangeListener((button, checked) -> {
            db.setNotificationEnabled(item.getId(), checked);
            item.setNotificationEnabled(checked);
        });
    }

    private void move(int direction) {
        if (items != null && items.size() > 1) {
            show((index + direction + items.size()) % items.size());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Rect area = new Rect();
            boolean inside = scroll.getGlobalVisibleRect(area)
                    && area.contains((int) event.getX(), (int) event.getY());
            downX = event.getX();
            downY = event.getY();
            canGoNext = inside && !scroll.canScrollVertically(1);
            canGoPrevious = inside && !scroll.canScrollVertically(-1);
        } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            canGoNext = canGoPrevious = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float distance = event.getY() - downY;
            float threshold = 72 * getResources().getDisplayMetrics().density;
            if (Math.abs(distance) > threshold && Math.abs(distance) > 1.5f * Math.abs(event.getX() - downX)
                    && (distance < 0 ? canGoNext : canGoPrevious)
                    && items != null && items.size() > 1) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                move(distance < 0 ? 1 : -1);
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saying, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_saying) {
            startActivity(new Intent(this, ModifyActivity.class).putExtra(EXTRA_ID, currentId));
            return true;
        }
        if (item.getItemId() == R.id.action_delete_saying) {
            int id = currentId;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.btn_delete)
                    .setMessage(R.string.dial_del_msg)
                    .setNegativeButton(R.string.txt_cancel, null)
                    .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                        db.deleteItem(id);
                        refresh();
                    })
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(EXTRA_ID, currentId);
        state.putInt("index", index);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
