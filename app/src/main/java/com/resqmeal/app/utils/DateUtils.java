package com.resqmeal.app.utils;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {
        // Utility class
    }

    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        return dateTimeFormat.format(timestamp.toDate());
    }

    public static String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        return timeFormat.format(timestamp.toDate());
    }

    public static String getExpiryCountdown(Timestamp expiryTime) {
        if (expiryTime == null) return "Expired";
        
        long diff = expiryTime.toDate().getTime() - System.currentTimeMillis();
        if (diff <= 0) {
            return "Expired";
        }

        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff / (1000 * 60)) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm remaining", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm remaining", minutes);
        }
    }
}
