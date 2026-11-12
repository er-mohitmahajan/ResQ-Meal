package com.resqmeal.app.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.resqmeal.app.R;
import com.resqmeal.app.model.AppNotification;
import com.resqmeal.app.ui.shell.BaseRoleActivity;
import com.resqmeal.app.ui.splash.SplashActivity;

public final class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "resqmeal_updates";
    private static final String CHANNEL_NAME = "ResQ Meal updates";

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            Log.e(TAG, "Notification manager is unavailable");
            return;
        }
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static void show(Context context, AppNotification notification) {
        if (notification == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent launchIntent = new Intent(context, SplashActivity.class);
        launchIntent.putExtra("notification_id", notification.getNotificationId());
        launchIntent.putExtra(BaseRoleActivity.EXTRA_OPEN_NOTIFICATIONS, true);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestCode = notification.getNotificationId() == null
                ? (int) System.currentTimeMillis()
                : notification.getNotificationId().hashCode();
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                requestCode,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_notifications)
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.getBody()))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build());
        } catch (SecurityException exception) {
            Log.w(TAG, "Notification permission is unavailable", exception);
        }
    }
}
