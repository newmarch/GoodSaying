package com.example.sunchan.goodsaying;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MENU_SEND_TEST_REMINDER = Menu.FIRST;
    private static final int REQUEST_TEST_NOTIFICATION_PERMISSION = 1;
    private DBHandler db;
    private ListView listView;
    private CustomList adapter;
    private ArrayList<Item> items = new ArrayList<>();
    private int scrollDirection;
    private final Runnable dragScroll = new Runnable() {
        @Override
        public void run() {
            if (scrollDirection != 0) {
                int distance = (int) (24 * getResources().getDisplayMetrics().density);
                listView.smoothScrollBy(scrollDirection * distance, 80);
                listView.postDelayed(this, 90);
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        ScreenInsets.apply(this);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        db = new DBHandler(this);
        listView = findViewById(R.id.list_view);
        adapter = new CustomList(this, this::moveItem);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, row, position, id) ->
                openSaying(NotiActivity.class, items.get(position).getId()));
        listView.setOnItemLongClickListener((parent, row, position, id) -> {
            openSaying(ModifyActivity.class, items.get(position).getId());
            return true;
        });
        listView.setOnDragListener((view, event) -> handleDrag(event));
        findViewById(R.id.fab).setOnClickListener(view ->
                startActivity(new Intent(this, InputActivity.class)));
        openNotification(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        items = db.getAllItems();
        adapter.setItems(items);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openNotification(intent);
    }

    private void openNotification(Intent intent) {
        int id = intent.getIntExtra(AlarmReceive.EXTRA_SAYING_ID, -1);
        if (id > 0) {
            intent.removeExtra(AlarmReceive.EXTRA_SAYING_ID);
            openSaying(NotiActivity.class, id);
        }
    }

    private void openSaying(Class<?> activity, int id) {
        startActivity(new Intent(this, activity).putExtra(NotiActivity.EXTRA_ID, id));
    }

    private boolean handleDrag(DragEvent event) {
        if (!(event.getLocalState() instanceof Item)) {
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_LOCATION:
                float edge = 48 * getResources().getDisplayMetrics().density;
                setDragScroll(event.getY() < edge ? -1
                        : event.getY() > listView.getHeight() - edge ? 1 : 0);
                break;
            case DragEvent.ACTION_DROP:
                int target = listView.pointToPosition((int) event.getX(), (int) event.getY());
                if (target == ListView.INVALID_POSITION) {
                    target = items.size() - 1;
                }
                moveItem(((Item) event.getLocalState()).getId(), target);
                setDragScroll(0);
                break;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
                setDragScroll(0);
                break;
        }
        return true;
    }

    private void setDragScroll(int direction) {
        if (scrollDirection != direction) {
            scrollDirection = direction;
            listView.removeCallbacks(dragScroll);
            if (direction != 0) {
                listView.post(dragScroll);
            }
        }
    }

    private void moveItem(int id, int target) {
        if (target < 0 || target >= items.size()) {
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).getId() == id && index != target) {
                items.add(target, items.remove(index));
                db.saveOrder(items);
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (BuildConfig.DEBUG) {
            menu.add(Menu.NONE, MENU_SEND_TEST_REMINDER, Menu.NONE, "Send Test Reminder");
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (BuildConfig.DEBUG && requestCode == REQUEST_TEST_NOTIFICATION_PERMISSION
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AlarmReceive.postNextReminder(this, db);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.DEBUG && item.getItemId() == MENU_SEND_TEST_REMINDER) {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_TEST_NOTIFICATION_PERMISSION);
            } else {
                AlarmReceive.postNextReminder(this, db);
            }
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_appearance) {
            startActivity(new Intent(this, AppearanceActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        listView.removeCallbacks(dragScroll);
        db.close();
        super.onDestroy();
    }
}
