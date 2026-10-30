package com.resqmeal.app.utils;

import android.util.Patterns;

public final class Validators {

    private Validators() {
        // Utility class
    }

    public static boolean isValidName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 50;
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return !trimmed.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String trimmed = phone.trim();
        return trimmed.matches("^[0-9]{10}$");
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
                password.matches(".*[a-zA-Z].*") &&
                password.matches(".*[0-9].*");
    }

    public static boolean isValidFoodTitle(String title) {
        if (title == null) return false;
        String trimmed = title.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 60;
    }

    public static boolean isValidDescription(String description) {
        if (description == null) return true; // Description can be empty
        return description.trim().length() <= 300;
    }

    public static boolean isValidQuantity(double quantity) {
        return quantity > 0;
    }

    public static boolean isValidServings(long servings) {
        return servings > 0;
    }
}
