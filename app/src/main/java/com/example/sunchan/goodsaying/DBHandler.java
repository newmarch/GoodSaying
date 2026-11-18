package com.example.sunchan.goodsaying;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {
    private static final String ORDER = "SORT_ORDER, ID";
    private static final String CREATE_ALARM = "CREATE TABLE IF NOT EXISTS SET_ALARM "
            + "(FLAG INTEGER, HOUR INTEGER, MINUTE INTEGER)";
    private static final String CREATE_PROGRESS = "CREATE TABLE NOTIFICATION_PROGRESS "
            + "(SINGLETON INTEGER PRIMARY KEY CHECK (SINGLETON = 1), LAST_ID INTEGER NOT NULL)";
    private final Context context;

    public DBHandler(Context context) {
        super(context.getApplicationContext(), "GOODSAYING", null, 2);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE GOODSAYING (ID INTEGER PRIMARY KEY AUTOINCREMENT, TEXT TEXT, "
                + "COUNT INTEGER, SORT_ORDER INTEGER NOT NULL DEFAULT 0, "
                + "NOTIFICATION_ENABLED INTEGER NOT NULL DEFAULT 1, NOTIFIED INTEGER NOT NULL DEFAULT 0)");
        db.execSQL(CREATE_ALARM);
        db.execSQL(CREATE_PROGRESS);
        int[] defaults = {R.string.default_1, R.string.default_2, R.string.default_3,
                R.string.default_4, R.string.default_5, R.string.default_6, R.string.default_7,
                R.string.default_8, R.string.default_9, R.string.default_10};
        for (int index = 0; index < defaults.length; index++) {
            db.execSQL("INSERT INTO GOODSAYING (TEXT, COUNT, SORT_ORDER) VALUES (?, 1, ?)",
                    new Object[]{context.getString(defaults[index]), index + 1});
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE GOODSAYING ADD COLUMN SORT_ORDER INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE GOODSAYING SET SORT_ORDER = ID");
            db.execSQL("ALTER TABLE GOODSAYING ADD COLUMN NOTIFICATION_ENABLED INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE GOODSAYING ADD COLUMN NOTIFIED INTEGER NOT NULL DEFAULT 0");
            db.execSQL(CREATE_ALARM);
            db.execSQL(CREATE_PROGRESS);
        }
    }

    public void insertItem(String text) {
        getWritableDatabase().execSQL("INSERT INTO GOODSAYING (TEXT, COUNT, SORT_ORDER) "
                + "SELECT ?, 1, COALESCE(MAX(SORT_ORDER), 0) + 1 FROM GOODSAYING", new Object[]{text});
    }

    public ArrayList<Item> getAllItems() {
        ArrayList<Item> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT ID, substr(TEXT, 1, 300), NOTIFICATION_ENABLED FROM GOODSAYING ORDER BY " + ORDER, null)) {
            while (cursor.moveToNext()) {
                Item item = new Item();
                item.setId(cursor.getInt(0));
                item.setText(cursor.getString(1));
                item.setNotificationEnabled(cursor.getInt(2) != 0);
                items.add(item);
            }
        }
        return items;
    }

    public String readText(int id) {
        // Bound individual CursorWindow rows even when a saying contains very large text.
        StringBuilder text = new StringBuilder();
        int start = 1;
        while (true) {
            try (Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT substr(TEXT, ?, 32768), length(TEXT) FROM GOODSAYING WHERE ID = ?",
                    new String[]{String.valueOf(start), String.valueOf(id)})) {
                if (!cursor.moveToFirst()) {
                    return null;
                }
                if (!cursor.isNull(0)) {
                    text.append(cursor.getString(0));
                }
                start += 32768;
                if (start > cursor.getInt(1)) {
                    return text.toString();
                }
            }
        }
    }

    public void updateItem(int id, String text) {
        ContentValues values = new ContentValues();
        values.put("TEXT", text);
        getWritableDatabase().update("GOODSAYING", values, "ID = ?", idArg(id));
    }

    public void deleteItem(int id) {
        getWritableDatabase().delete("GOODSAYING", "ID = ?", idArg(id));
    }

    public void setNotificationEnabled(int id, boolean enabled) {
        getWritableDatabase().execSQL("UPDATE GOODSAYING SET NOTIFICATION_ENABLED = ? WHERE ID = ?",
                new Object[]{enabled ? 1 : 0, id});
    }

    public void saveOrder(List<Item> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int index = 0; index < items.size(); index++) {
                db.execSQL("UPDATE GOODSAYING SET SORT_ORDER = ? WHERE ID = ?",
                        new Object[]{index + 1, items.get(index).getId()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static String[] idArg(int id) {
        return new String[]{String.valueOf(id)};
    }

    interface NotificationPoster {
        boolean post(int id, String preview);
    }

    void postNextNotification(NotificationPoster poster) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int lastId = -1;
            try (Cursor cursor = db.rawQuery("SELECT LAST_ID FROM NOTIFICATION_PROGRESS", null)) {
                if (cursor.moveToFirst()) {
                    lastId = cursor.getInt(0);
                }
            }

            ArrayList<Integer> ids = new ArrayList<>();
            ArrayList<Boolean> seen = new ArrayList<>();
            try (Cursor cursor = db.rawQuery("SELECT ID, NOTIFIED FROM GOODSAYING "
                    + "WHERE NOTIFICATION_ENABLED = 1 ORDER BY " + ORDER, null)) {
                while (cursor.moveToNext()) {
                    ids.add(cursor.getInt(0));
                    seen.add(cursor.getInt(1) != 0);
                }
            }
            NotificationSequence.Choice choice = NotificationSequence.next(ids, seen, lastId);
            if (choice == null) {
                return;
            }

            String preview;
            try (Cursor cursor = db.rawQuery(
                    "SELECT substr(TEXT, 1, 1000) FROM GOODSAYING WHERE ID = ?", idArg(choice.id))) {
                cursor.moveToFirst();
                preview = cursor.getString(0);
            }
            if (!poster.post(choice.id, preview)) {
                return;
            }
            if (choice.newCycle) {
                db.execSQL("UPDATE GOODSAYING SET NOTIFIED = 0");
            }
            db.execSQL("UPDATE GOODSAYING SET NOTIFIED = 1 WHERE ID = ?", new Object[]{choice.id});
            db.execSQL("INSERT OR REPLACE INTO NOTIFICATION_PROGRESS (SINGLETON, LAST_ID) VALUES (1, ?)",
                    new Object[]{choice.id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<AlarmItem> getAllAlarmItem() {
        ArrayList<AlarmItem> alarms = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT FLAG, HOUR, MINUTE FROM SET_ALARM ORDER BY rowid", null)) {
            while (cursor.moveToNext()) {
                alarms.add(new AlarmItem(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2)));
            }
        }
        return alarms;
    }

    public void saveAlarm(int hour, int minute) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("SET_ALARM", null, null);
            db.execSQL("INSERT INTO SET_ALARM (FLAG, HOUR, MINUTE) VALUES (1, ?, ?)",
                    new Object[]{hour, minute});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteAlarmItem() {
        getWritableDatabase().delete("SET_ALARM", null, null);
    }
}
