package com.resqmeal.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOnboardingCompleted(Context context) {
        return getPrefs(context).getBoolean(Constants.KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        getPrefs(context).edit().putBoolean(Constants.KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static void clear(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}

