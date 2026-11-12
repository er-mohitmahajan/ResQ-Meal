package com.resqmeal.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.resqmeal.app.model.AppNotification;
import com.resqmeal.app.repository.NotificationRepository;
import com.resqmeal.app.utils.Resource;

import java.util.List;

public class NotificationViewModel extends AndroidViewModel {
    private final NotificationRepository repository;
    private final FirebaseAuth auth;
    private final MutableLiveData<Resource<List<AppNotification>>> notifications =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> actionStatus = new MutableLiveData<>();
    private ListenerRegistration unreadNotificationsListener;
    private String listeningUserId;

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository();
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<Resource<List<AppNotification>>> getNotifications() {
        return notifications;
    }

    public LiveData<Resource<String>> getActionStatus() {
        return actionStatus;
    }

    public void startListeningForUnreadNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            notifications.setValue(Resource.error(
                    "Sign in with a verified email to view notifications", null));
            return;
        }
        String userId = currentUser.getUid();
        if (userId.equals(listeningUserId) && unreadNotificationsListener != null) {
            return;
        }
        stopListeningForUnreadNotifications();
        listeningUserId = userId;
        notifications.setValue(Resource.loading());
        unreadNotificationsListener = repository.addUserNotificationsListener(
                userId,
                (snapshot, error) -> {
                    if (!userId.equals(listeningUserId)) {
                        return;
                    }
                    if (error != null) {
                        stopListeningForUnreadNotifications();
                        notifications.setValue(Resource.error(
                                errorMessage(error, "Could not listen for notifications"), null));
                    } else if (snapshot != null) {
                        notifications.setValue(Resource.success(
                                repository.getUnreadNotifications(snapshot)));
                    }
                });
    }

    public void stopListeningForUnreadNotifications() {
        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }
        listeningUserId = null;
    }

    public void markAsRead(String notificationId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            actionStatus.setValue(Resource.error(
                    "Sign in with a verified email before updating notifications", null));
            return;
        }
        actionStatus.setValue(Resource.loading());
        repository.markAsRead(currentUser.getUid(), notificationId)
                .addOnSuccessListener(unused -> {
                    actionStatus.setValue(Resource.success(notificationId));
                })
                .addOnFailureListener(error -> actionStatus.setValue(Resource.error(
                        errorMessage(error, "Could not mark notification as read"), notificationId)));
    }

    private String errorMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                ? fallback
                : exception.getMessage();
    }

    @Override
    protected void onCleared() {
        stopListeningForUnreadNotifications();
        super.onCleared();
    }
}
