package com.resqmeal.app.repository;

import android.util.Log;

import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodPostRepository {
    private static final String TAG = "FoodPostRepository";

    private final FirebaseFirestore db;
    private final CollectionReference postsRef;
    private final NotificationRepository notificationRepository;

    public FoodPostRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.postsRef = db.collection(Constants.COLLECTION_FOOD_POSTS);
        this.notificationRepository = new NotificationRepository();
    }

    public Task<Void> createPost(String donorId, FoodPost post) {
        com.google.firebase.firestore.DocumentReference postRef = postsRef.document();
        com.google.firebase.firestore.DocumentReference donorRef =
                db.collection(Constants.COLLECTION_USERS).document(donorId);
        String id = postRef.getId();
        post.setPostId(id);
        Task<Void> createTask = db.runTransaction(transaction -> {
            DocumentSnapshot donorSnapshot = transaction.get(donorRef);
            if (!donorSnapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "Donor profile could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            transaction.set(postRef, post);
            transaction.update(donorRef, "totalPosted", FieldValue.increment(1));
            return null;
        });
        createTask.addOnSuccessListener(unused -> notificationRepository.notifyNearbyReceivers(post)
                .addOnFailureListener(error ->
                        Log.e(TAG, "Post was created, but nearby receivers could not be notified", error)));
        return createTask;
    }

    public Query getDonorPostsQuery(String donorId) {
        return postsRef.whereEqualTo("donorId", donorId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    public Query getDonorPostsByStatusQuery(String donorId, String status) {
        return postsRef.whereEqualTo("donorId", donorId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    public Task<Boolean> updateAvailablePost(String donorId, FoodPost post) {
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postsRef.document(post.getPostId()));
            FoodPost currentPost = snapshot.toObject(FoodPost.class);
            if (currentPost == null || !donorId.equals(currentPost.getDonorId())) {
                throw new FirebaseFirestoreException(
                        "This post could not be found in your listings",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!Constants.STATUS_AVAILABLE.equals(currentPost.getStatus())) {
                throw new FirebaseFirestoreException(
                        "Only available posts can be edited",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Timestamp now = Timestamp.now();
            if (currentPost.getExpiryTime() != null
                    && currentPost.getExpiryTime().compareTo(now) <= 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", Constants.STATUS_EXPIRED);
                updates.put("updatedAt", now);
                transaction.update(snapshot.getReference(), updates);
                addExpiredNotification(transaction, currentPost);
                return false;
            }
            post.setUpdatedAt(now);
            transaction.set(snapshot.getReference(), post);
            return true;
        });
    }

    public Task<Boolean> cancelPost(String donorId, String postId) {
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postsRef.document(postId));
            FoodPost post = snapshot.toObject(FoodPost.class);
            if (post == null || !donorId.equals(post.getDonorId())) {
                throw new FirebaseFirestoreException(
                        "This post could not be found in your listings",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!Constants.STATUS_AVAILABLE.equals(post.getStatus())
                    && !Constants.STATUS_CLAIMED.equals(post.getStatus())) {
                throw new FirebaseFirestoreException(
                        "This post can no longer be cancelled",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Timestamp now = Timestamp.now();
            if (post.getExpiryTime() != null && post.getExpiryTime().compareTo(now) <= 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", Constants.STATUS_EXPIRED);
                updates.put("updatedAt", now);
                transaction.update(snapshot.getReference(), updates);
                addExpiredNotification(transaction, post);
                return false;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Constants.STATUS_CANCELLED);
            updates.put("updatedAt", now);
            transaction.update(snapshot.getReference(), updates);
            return true;
        });
    }

    public Task<Void> markExpiredIfDue(String postId, Timestamp now) {
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postsRef.document(postId));
            FoodPost post = snapshot.toObject(FoodPost.class);
            if (post != null
                    && (Constants.STATUS_AVAILABLE.equals(post.getStatus())
                    || Constants.STATUS_CLAIMED.equals(post.getStatus()))
                    && post.getExpiryTime() != null
                    && post.getExpiryTime().compareTo(now) <= 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", Constants.STATUS_EXPIRED);
                updates.put("updatedAt", now);
                transaction.update(snapshot.getReference(), updates);
                addExpiredNotification(transaction, post);
            }
            return null;
        });
    }

    private void addExpiredNotification(
            com.google.firebase.firestore.Transaction transaction,
            FoodPost post
    ) {
        notificationRepository.addToTransaction(transaction, notificationRepository.createNotification(
                post.getDonorId(),
                "Food post expired",
                post.getTitle() + " expired without a completed pickup.",
                Constants.NOTIF_TYPE_POST_EXPIRED,
                post.getPostId()));
    }

    public Task<List<QuerySnapshot>> getNearbyAvailablePosts(double lat, double lng, double radiusInKm) {
        double radiusInMeters = radiusInKm * 1000;
        List<GeoQueryBounds> queries =
                GeoUtils.getGeoHashQueryBounds(lat, lng, radiusInMeters);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds bounds : queries) {
            Query query = postsRef
                    .whereEqualTo("status", Constants.STATUS_AVAILABLE)
                    .orderBy("geohash")
                    .startAt(bounds.startHash)
                    .endAt(bounds.endHash);
            tasks.add(query.get());
        }

        return Tasks.whenAllSuccess(tasks);
    }

    public Task<Void> updatePost(FoodPost post) {
        post.setUpdatedAt(Timestamp.now());
        return postsRef.document(post.getPostId()).set(post);
    }

    public Task<Void> updateStatus(String postId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", Timestamp.now());
        return postsRef.document(postId).update(updates);
    }

    public Task<DocumentSnapshot> getPost(String postId) {
        return postsRef.document(postId).get();
    }
}
