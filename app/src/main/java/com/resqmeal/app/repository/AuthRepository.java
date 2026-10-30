package com.resqmeal.app.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;

public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Constructor for testing or injection
    public AuthRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public LiveData<Resource<User>> register(String name, String email, String phone, String password, String role, String type) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        result.setValue(Resource.error(errorMsg, null));
                        return;
                    }

                    FirebaseUser fbUser = auth.getCurrentUser();
                    // Send verification email immediately
                    fbUser.sendEmailVerification();

                    // Create user profile in Firestore
                    User user = new User();
                    user.setUid(fbUser.getUid());
                    user.setName(name.trim());
                    user.setEmail(email.trim());
                    user.setPhone(phone.trim());
                    user.setRole(role);
                    if (Constants.ROLE_DONOR.equals(role)) {
                        user.setDonorType(type);
                        user.setApprovalStatus(Constants.APPROVAL_APPROVED);
                    } else if (Constants.ROLE_RECEIVER.equals(role)) {
                        user.setReceiverType(type);
                        user.setApprovalStatus(Constants.APPROVAL_PENDING);
                    } else {
                        user.setApprovalStatus(Constants.APPROVAL_APPROVED);
                    }

                    user.setActive(true);
                    user.setAddress("");
                    user.setLat(0.0);
                    user.setLng(0.0);
                    user.setGeohash("");
                    user.setProfilePhotoUrl("");
                    user.setRejectionReason("");
                    user.setTotalPosted(0);
                    user.setTotalMealsDonated(0);
                    user.setTotalClaimed(0);
                    user.setTotalMealsReceived(0);
                    user.setCreatedAt(Timestamp.now());
                    user.setUpdatedAt(Timestamp.now());

                    firestore.collection(Constants.COLLECTION_USERS)
                            .document(fbUser.getUid())
                            .set(user)
                            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(user)))
                            .addOnFailureListener(e -> {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Failed to save user profile";
                                result.setValue(Resource.error(errorMsg, null));
                            });
                });

        return result;
    }

    public LiveData<Resource<User>> login(String email, String password) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        result.setValue(Resource.error(errorMsg, null));
                        return;
                    }

                    FirebaseUser fbUser = auth.getCurrentUser();
                    fbUser.reload().addOnCompleteListener(reloadTask -> {
                        firestore.collection(Constants.COLLECTION_USERS)
                                .document(fbUser.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null) {
                                            if (!user.isActive()) {
                                                auth.signOut();
                                                result.setValue(Resource.error("Your account has been deactivated.", null));
                                            } else {
                                                result.setValue(Resource.success(user));
                                            }
                                        } else {
                                            result.setValue(Resource.error("Failed to parse user document.", null));
                                        }
                                    } else {
                                        result.setValue(Resource.error("User profile document not found.", null));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Error fetching user details";
                                    result.setValue(Resource.error(errorMsg, null));
                                });
                    });
                });

        return result;
    }

    public LiveData<Resource<Void>> sendPasswordReset(String email) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        auth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result.setValue(Resource.success(null));
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        result.setValue(Resource.error(errorMsg, null));
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> sendEmailVerification() {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            result.setValue(Resource.error("No signed-in user found.", null));
            return result;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result.setValue(Resource.success(null));
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to resend verification email";
                        result.setValue(Resource.error(errorMsg, null));
                    }
                });

        return result;
    }

    public LiveData<Resource<Boolean>> checkEmailVerified() {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            result.setValue(Resource.error("No signed-in user found.", null));
            return result;
        }

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                result.setValue(Resource.success(user.isEmailVerified()));
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to refresh email verification status";
                result.setValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    public LiveData<Resource<User>> getCurrentUserData() {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Not logged in", null));
            return result;
        }

        result.setValue(Resource.loading(null));
        firestore.collection(Constants.COLLECTION_USERS)
                .document(fbUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        result.setValue(Resource.success(user));
                    } else {
                        result.setValue(Resource.error("User record not found", null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    public void logout() {
        auth.signOut();
    }
}
