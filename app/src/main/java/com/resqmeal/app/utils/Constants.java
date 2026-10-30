package com.resqmeal.app.utils;

public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_FOOD_POSTS = "food_posts";
    public static final String COLLECTION_CLAIMS = "claims";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_REPORTS = "reports";

    // User Roles
    public static final String ROLE_DONOR = "donor";
    public static final String ROLE_RECEIVER = "receiver";
    public static final String ROLE_ADMIN = "admin";

    // Donor Types
    public static final String DONOR_TYPE_RESTAURANT = "restaurant";
    public static final String DONOR_TYPE_HOSTEL_MESS = "hostel_mess";
    public static final String DONOR_TYPE_EVENT = "event";
    public static final String DONOR_TYPE_INDIVIDUAL = "individual";

    // Receiver Types
    public static final String RECEIVER_TYPE_NGO = "ngo";
    public static final String RECEIVER_TYPE_SHELTER = "shelter";
    public static final String RECEIVER_TYPE_VOLUNTEER = "volunteer";

    // Approval Status (Users)
    public static final String APPROVAL_APPROVED = "approved";
    public static final String APPROVAL_PENDING = "pending";
    public static final String APPROVAL_REJECTED = "rejected";

    // Food Post Statuses
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_CLAIMED = "claimed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_CANCELLED = "cancelled";

    // Claim Statuses
    public static final String CLAIM_STATUS_ACTIVE = "active";
    public static final String CLAIM_STATUS_COMPLETED = "completed";
    public static final String CLAIM_STATUS_CANCELLED = "cancelled";

    // Food Types
    public static final String FOOD_TYPE_VEG = "veg";
    public static final String FOOD_TYPE_NON_VEG = "non_veg";
    public static final String FOOD_TYPE_BOTH = "both";

    // Food Categories
    public static final String CATEGORY_COOKED_MEAL = "cooked_meal";
    public static final String CATEGORY_BAKERY = "bakery";
    public static final String CATEGORY_FRUITS_VEG = "fruits_veg";
    public static final String CATEGORY_PACKAGED = "packaged";
    public static final String CATEGORY_OTHER = "other";

    // Units
    public static final String UNIT_MEALS = "meals";
    public static final String UNIT_KG = "kg";
    public static final String UNIT_PACKETS = "packets";

    // Notification Types
    public static final String NOTIF_TYPE_NEW_POST = "new_post";
    public static final String NOTIF_TYPE_POST_CLAIMED = "post_claimed";
    public static final String NOTIF_TYPE_CLAIM_CANCELLED = "claim_cancelled";
    public static final String NOTIF_TYPE_PICKUP_COMPLETED = "pickup_completed";
    public static final String NOTIF_TYPE_POST_EXPIRED = "post_expired";
    public static final String NOTIF_TYPE_ACCOUNT_APPROVED = "account_approved";
    public static final String NOTIF_TYPE_ACCOUNT_REJECTED = "account_rejected";

    // Report Targets & Reasons & Statuses
    public static final String REPORT_TARGET_POST = "post";
    public static final String REPORT_TARGET_USER = "user";

    public static final String REPORT_REASON_SPOILED_FOOD = "spoiled_food";
    public static final String REPORT_REASON_FAKE_LISTING = "fake_listing";
    public static final String REPORT_REASON_NO_SHOW = "no_show";
    public static final String REPORT_REASON_ABUSIVE_BEHAVIOR = "abusive_behavior";
    public static final String REPORT_REASON_OTHER = "other";

    public static final String REPORT_STATUS_OPEN = "open";
    public static final String REPORT_STATUS_RESOLVED = "resolved";
    public static final String REPORT_STATUS_DISMISSED = "dismissed";

    // Business Rules
    public static final int MAX_ACTIVE_CLAIMS = 3;
    public static final double DEFAULT_RADIUS_KM = 10.0;
    public static final int MIN_EXPIRY_MINUTES = 30;
    public static final int MAX_EXPIRY_HOURS = 24;
    public static final int GEOHASH_PRECISION = 9;

    // Cloudinary Configuration
    public static final String CLOUDINARY_CLOUD_NAME = "resqmeal"; // Replace with your Cloudinary cloud name
    public static final String CLOUDINARY_UPLOAD_PRESET = "resqmeal_unsigned"; // Supports "resqmeal_unsigned" or "foodbridge_unsigned"
    public static final String CLOUDINARY_FOLDER_FOOD_POSTS = "resqmeal/food_posts";
    public static final String CLOUDINARY_FOLDER_PROFILES = "resqmeal/profiles";

    // Shared Preferences Keys
    public static final String PREFS_NAME = "resqmeal_prefs";
    public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
}
