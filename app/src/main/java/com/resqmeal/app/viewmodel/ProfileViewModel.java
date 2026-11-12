package com.resqmeal.app.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.UserRepository;
import com.resqmeal.app.utils.CloudinaryUploader;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;

public class ProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final FirebaseAuth auth;
    
    private final MutableLiveData<Resource<User>> userData = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> updateStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> profilePhotoStatus = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<Resource<User>> getUserData() {
        return userData;
    }

    public LiveData<Resource<Void>> getUpdateStatus() {
        return updateStatus;
    }

    public LiveData<Resource<Void>> getProfilePhotoStatus() {
        return profilePhotoStatus;
    }

    public void fetchCurrentUser() {
        if (auth.getCurrentUser() == null) {
            userData.setValue(Resource.error("You are signed out. Please sign in again.", null));
            return;
        }
        
        userData.setValue(Resource.loading());
        userRepository.getUser(auth.getCurrentUser().getUid())
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        userData.setValue(Resource.success(user));
                    } else {
                        userData.setValue(Resource.error("User not found", null));
                    }
                })
                .addOnFailureListener(e -> userData.setValue(Resource.error(e.getMessage(), null)));
    }

    public void updateProfile(String name, String phone, String address, Double lat, Double lng, String geohash) {
        if (auth.getCurrentUser() == null) {
            updateStatus.setValue(Resource.error("You are signed out. Please sign in again.", null));
            return;
        }

        updateStatus.setValue(Resource.loading());
        userRepository.updateProfile(auth.getCurrentUser().getUid(), name, phone, address, lat, lng, geohash)
                .addOnSuccessListener(aVoid -> {
                    updateStatus.setValue(Resource.success(null));
                    fetchCurrentUser(); // Refresh data
                })
                .addOnFailureListener(e -> updateStatus.setValue(Resource.error(e.getMessage(), null)));
    }

    public void uploadProfilePhoto(Uri imageUri) {
        if (auth.getCurrentUser() == null) {
            profilePhotoStatus.setValue(Resource.error("You are signed out. Please sign in again.", null));
            return;
        }
        if (imageUri == null) {
            profilePhotoStatus.setValue(Resource.error("Select a photo before uploading.", null));
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        profilePhotoStatus.setValue(Resource.loading());
        CloudinaryUploader.uploadImage(getApplication(), imageUri, Constants.CLOUDINARY_FOLDER_PROFILES, new CloudinaryUploader.UploadListener() {
            @Override
            public void onSuccess(String url, String publicId) {
                userRepository.updateProfilePhoto(uid, url, publicId)
                        .addOnSuccessListener(aVoid -> {
                            fetchCurrentUser();
                            profilePhotoStatus.setValue(Resource.success(null));
                        })
                        .addOnFailureListener(e -> profilePhotoStatus.setValue(
                                Resource.error(e.getMessage(), null)));
            }

            @Override
            public void onError(String error) {
                profilePhotoStatus.setValue(Resource.error(error, null));
            }
        });
    }

    public void logout() {
        auth.signOut();
    }
}
