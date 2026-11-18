package com.example.sunchan.goodsaying;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class AlarmReceive extends BroadcastReceiver {
    static final String EXTRA_SAYING_ID = "reminder_saying_id";
    private static final String CHANNEL_ID = "daily_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        DBHandler db = new DBHandler(context);
        try {
            ArrayList<AlarmItem> alarms = db.getAllAlarmItem();
            if (alarms.isEmpty() || alarms.get(0).getFlag() != 1) {
                return;
            }
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                    || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
                AlarmItem alarm = alarms.get(0);
                schedule(context, alarm.getHour(), alarm.getMin());
            } else {
                postNextReminder(context, db);
            }
        } finally {
            db.close();
        }
    }

    static void postNextReminder(Context context, DBHandler db) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (canPost(context, manager)) {
            db.postNextNotification((id, preview) -> post(context, manager, id, preview));
        }
    }

    private static boolean canPost(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.set_alarm_title), NotificationManager.IMPORTANCE_DEFAULT));
            return manager.getNotificationChannel(CHANNEL_ID).getImportance()
                    != NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    private static boolean post(Context context, NotificationManager manager, int id, String preview) {
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_SAYING_ID, id);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT | immutableFlag());
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(preview)
                .setAutoCancel(true)
                .setContentIntent(tap);
        if (Build.VERSION.SDK_INT < 26) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
        try {
            manager.notify(0, Build.VERSION.SDK_INT >= 16 ? builder.build() : builder.getNotification());
            return true;
        } catch (SecurityException error) {
            Log.w("GoodSaying", "Notification permission changed before posting", error);
            return false;
        }
    }

    static void schedule(Context context, int hour, int minute) {
        Calendar next = Calendar.getInstance();
        long now = next.getTimeInMillis();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= now) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent(context, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    static void cancel(Context context) {
        PendingIntent intent = alarmIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (intent != null) {
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(intent);
            intent.cancel();
        }
    }

    private static PendingIntent alarmIntent(Context context, int flags) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceive.class),
                flags | immutableFlag());
    }

    private static int immutableFlag() {
        return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    }
}
