package com.resqmeal.app.repository;

import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.resqmeal.app.model.AppNotification;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationRepository {
    private static final double NEW_POST_RADIUS_METERS = 10_000.0;

    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;
    private final CollectionReference usersRef;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
        notificationsRef = db.collection(Constants.COLLECTION_NOTIFICATIONS);
        usersRef = db.collection(Constants.COLLECTION_USERS);
    }

    public ListenerRegistration addUserNotificationsListener(
            String userId,
            EventListener<QuerySnapshot> listener
    ) {
        return notificationsRef.whereEqualTo("toUserId", userId)
                .addSnapshotListener(listener);
    }

    public List<AppNotification> getUnreadNotifications(QuerySnapshot snapshot) {
        List<AppNotification> notifications = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            AppNotification notification = document.toObject(AppNotification.class);
            if (notification != null && !notification.isRead()) {
                notification.setNotificationId(document.getId());
                notifications.add(notification);
            }
        }
        notifications.sort(Comparator.comparing(
                AppNotification::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return notifications;
    }

    public Task<Void> markAsRead(String userId, String notificationId) {
        if (userId == null || userId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            return Tasks.forException(new FirebaseFirestoreException(
                    "This notification could not be found",
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT));
        }
        DocumentReference notificationRef = notificationsRef.document(notificationId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(notificationRef);
            AppNotification notification = snapshot.toObject(AppNotification.class);
            if (notification == null || !userId.equals(notification.getToUserId())) {
                throw new FirebaseFirestoreException(
                        "This notification does not belong to your account",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (!notification.isRead()) {
                transaction.update(notificationRef, "isRead", true);
            }
            return null;
        });
    }

    public AppNotification createNotification(
            String userId,
            String title,
            String body,
            String type,
            String referenceId
    ) {
        AppNotification notification = new AppNotification();
        DocumentReference notificationRef = notificationsRef.document();
        notification.setNotificationId(notificationRef.getId());
        notification.setToUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setRefId(referenceId);
        notification.setRead(false);
        notification.setCreatedAt(Timestamp.now());
        return notification;
    }

    public void addToTransaction(Transaction transaction, AppNotification notification) {
        transaction.set(
                notificationsRef.document(notification.getNotificationId()),
                notification);
    }

    public Task<Void> notifyNearbyReceivers(FoodPost post) {
        if (post == null || post.getLat() == null || post.getLng() == null
                || !GeoUtils.isValidCoordinates(post.getLat(), post.getLng())) {
            return Tasks.forException(new FirebaseFirestoreException(
                    "A valid post location is required to notify nearby receivers",
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT));
        }

        List<Task<QuerySnapshot>> queries = new ArrayList<>();
        for (GeoQueryBounds bounds : GeoUtils.getGeoHashQueryBounds(
                post.getLat(), post.getLng(), NEW_POST_RADIUS_METERS)) {
            Query query = usersRef.orderBy("geohash")
                    .startAt(bounds.startHash)
                    .endAt(bounds.endHash);
            queries.add(query.get());
        }

        return Tasks.whenAllSuccess(queries).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() == null
                        ? new IllegalStateException("Could not find nearby receivers")
                        : task.getException();
            }

            Set<String> notifiedUserIds = new HashSet<>();
            List<AppNotification> notifications = new ArrayList<>();
            for (Object result : task.getResult()) {
                if (!(result instanceof QuerySnapshot)) {
                    continue;
                }
                for (DocumentSnapshot document : (QuerySnapshot) result) {
                    User receiver = document.toObject(User.class);
                    if (receiver == null
                            || receiver.getLat() == null
                            || receiver.getLng() == null
                            || !GeoUtils.isValidCoordinates(receiver.getLat(), receiver.getLng())
                            || notifiedUserIds.contains(document.getId())) {
                        continue;
                    }
                    float distance = GeoUtils.getDistanceBetween(
                            post.getLat(), post.getLng(), receiver.getLat(), receiver.getLng());
                    if (!isEligibleNearbyReceiver(receiver, distance)) {
                        continue;
                    }
                    notifiedUserIds.add(document.getId());
                    notifications.add(createNotification(
                            document.getId(),
                            "Food available nearby",
                            post.getTitle() == null || post.getTitle().trim().isEmpty()
                                    ? "A new food post is available near you."
                                    : post.getTitle() + " is available near you.",
                            Constants.NOTIF_TYPE_NEW_POST,
                            post.getPostId()));
                }
            }
            return commitInBatches(notifications);
        });
    }

    static boolean isEligibleNearbyReceiver(User user, double distanceMeters) {
        return user != null
                && Constants.ROLE_RECEIVER.equals(user.getRole())
                && Constants.APPROVAL_APPROVED.equals(user.getApprovalStatus())
                && user.isActive()
                && Double.isFinite(distanceMeters)
                && distanceMeters >= 0
                && distanceMeters <= NEW_POST_RADIUS_METERS;
    }

    public Task<Void> createAccountStatusNotification(String receiverId, String approvalStatus) {
        String title;
        String body;
        String type;
        if (Constants.APPROVAL_APPROVED.equals(approvalStatus)) {
            title = "Receiver account approved";
            body = "Your account is approved. You can now browse and claim nearby food.";
            type = Constants.NOTIF_TYPE_ACCOUNT_APPROVED;
        } else if (Constants.APPROVAL_REJECTED.equals(approvalStatus)) {
            title = "Receiver account update";
            body = "Your receiver account was not approved. Open the app for details.";
            type = Constants.NOTIF_TYPE_ACCOUNT_REJECTED;
        } else {
            return Tasks.forException(new FirebaseFirestoreException(
                    "Only an approval or rejection can create an account notification",
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT));
        }
        AppNotification notification = createNotification(
                receiverId, title, body, type, receiverId);
        return notificationsRef.document(notification.getNotificationId()).set(notification);
    }

    private Task<Void> commitInBatches(List<AppNotification> notifications) {
        List<Task<Void>> commits = new ArrayList<>();
        for (int start = 0; start < notifications.size(); start += 450) {
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            int end = Math.min(start + 450, notifications.size());
            for (int index = start; index < end; index++) {
                AppNotification notification = notifications.get(index);
                batch.set(notificationsRef.document(notification.getNotificationId()), notification);
            }
            commits.add(batch.commit());
        }
        return Tasks.whenAll(commits);
    }
}
