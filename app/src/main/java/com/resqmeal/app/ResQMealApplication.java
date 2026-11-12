package com.resqmeal.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.resqmeal.app.model.AppNotification;
import com.resqmeal.app.repository.NotificationRepository;
import com.resqmeal.app.utils.CloudinaryUploader;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.NotificationHelper;

public class ResQMealApplication extends Application {
    private static final String TAG = "ResQMealApplication";

    private FirebaseAuth.AuthStateListener authStateListener;
    private ListenerRegistration notificationsListener;
    private String listeningUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        CloudinaryUploader.init(this);
        NotificationHelper.createNotificationChannel(this);
        authStateListener = auth -> attachUserListeners(auth.getCurrentUser());
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    private void attachUserListeners(FirebaseUser user) {
        String userId = user == null || !user.isEmailVerified() ? null : user.getUid();
        if (userId == null) {
            removeUserListeners();
            return;
        }
        if (userId.equals(listeningUserId)) {
            return;
        }
        removeUserListeners();
        listeningUserId = userId;
        NotificationRepository notificationRepository = new NotificationRepository();
        boolean[] initialNotificationsSnapshot = {true};
        notificationsListener = notificationRepository.addUserNotificationsListener(
                userId,
                (snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Could not listen for notifications", error);
                        return;
                    }
                    if (snapshot == null) {
                        Log.e(TAG, "Notification listener returned an empty snapshot");
                        return;
                    }
                    if (initialNotificationsSnapshot[0]) {
                        initialNotificationsSnapshot[0] = false;
                        return;
                    }
                    if (!userId.equals(listeningUserId)) {
                        return;
                    }
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (!change.getType().equals(
                                DocumentChange.Type.ADDED)) {
                            continue;
                        }
                        DocumentSnapshot document = change.getDocument();
                        AppNotification notification = document.toObject(AppNotification.class);
                        if (notification != null && !notification.isRead()) {
                            notification.setNotificationId(document.getId());
                            NotificationHelper.show(this, notification);
                        }
                    }
                });
    }

    private void removeUserListeners() {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
        listeningUserId = null;
    }

    @Override
    public void onTerminate() {
        removeUserListeners();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
        super.onTerminate();
    }
}