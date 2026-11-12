package com.resqmeal.app.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection(Constants.COLLECTION_USERS);
    }

    public Task<DocumentSnapshot> getUser(String uid) {
        return usersRef.document(uid).get();
    }

    public Task<Void> updateUser(User user) {
        return usersRef.document(user.getUid()).set(user);
    }

    public Task<Void> updateProfilePhoto(String uid, String photoUrl, String publicId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePhotoUrl", photoUrl);
        updates.put("profilePhotoPublicId", publicId);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        return usersRef.document(uid).update(updates);
    }

    public Task<Void> updateProfile(String uid, String name, String phone, String address, Double lat, Double lng, String geohash) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("lat", lat);
        updates.put("lng", lng);
        updates.put("geohash", geohash);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        return usersRef.document(uid).update(updates);
    }
}
