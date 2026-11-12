package com.resqmeal.app.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.ClaimRepository;
import com.resqmeal.app.repository.FoodPostRepository;
import com.resqmeal.app.repository.UserRepository;
import com.resqmeal.app.utils.CloudinaryUploader;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoodPostViewModel extends AndroidViewModel {
    private final FoodPostRepository postRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final FirebaseAuth auth;

    private final MutableLiveData<Resource<Void>> publishStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FoodPost>>> donorPosts = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FoodPost>>> nearbyPosts = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> actionStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<FoodPost>> selectedPost = new MutableLiveData<>();
    private final MutableLiveData<Resource<Claim>> selectedClaim = new MutableLiveData<>();
    private Double nearbyCenterLat;
    private Double nearbyCenterLng;
    private double nearbyRadiusKm = Constants.DEFAULT_RADIUS_KM;
    private String nearbyFoodType;
    private String nearbyCategory;

    public FoodPostViewModel(@NonNull Application application) {
        super(application);
        postRepository = new FoodPostRepository();
        userRepository = new UserRepository();
        claimRepository = new ClaimRepository();
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<Resource<Void>> getPublishStatus() {
        return publishStatus;
    }

    public LiveData<Resource<List<FoodPost>>> getDonorPosts() {
        return donorPosts;
    }

    public LiveData<Resource<List<FoodPost>>> getNearbyPosts() {
        return nearbyPosts;
    }

    public LiveData<Resource<Void>> getActionStatus() {
        return actionStatus;
    }

    public LiveData<Resource<FoodPost>> getSelectedPost() {
        return selectedPost;
    }

    public LiveData<Resource<Claim>> getSelectedClaim() {
        return selectedClaim;
    }

    public boolean hasNearbySearch() {
        return nearbyCenterLat != null && nearbyCenterLng != null;
    }

    public double getNearbyCenterLat() {
        return nearbyCenterLat == null ? Double.NaN : nearbyCenterLat;
    }

    public double getNearbyCenterLng() {
        return nearbyCenterLng == null ? Double.NaN : nearbyCenterLng;
    }

    public double getNearbyRadiusKm() {
        return nearbyRadiusKm;
    }

    @Nullable
    public String getNearbyFoodType() {
        return nearbyFoodType;
    }

    @Nullable
    public String getNearbyCategory() {
        return nearbyCategory;
    }

    public void refreshNearbyPosts() {
        if (hasNearbySearch()) {
            fetchNearbyPosts(
                    nearbyCenterLat,
                    nearbyCenterLng,
                    nearbyRadiusKm,
                    nearbyFoodType,
                    nearbyCategory);
        }
    }

    public void publishPost(FoodPost post, Uri imageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            publishStatus.setValue(Resource.error("Please sign in before publishing a post", null));
            return;
        }
        if (post == null || imageUri == null) {
            publishStatus.setValue(Resource.error("A food post and photo are required", null));
            return;
        }

        publishStatus.setValue(Resource.loading());
        String donorId = currentUser.getUid();

        userRepository.getUser(donorId).addOnSuccessListener(documentSnapshot -> {
            User donor = documentSnapshot.toObject(User.class);
            if (donor == null) {
                publishStatus.setValue(Resource.error("Donor profile not found", null));
                return;
            }
            if (!Constants.ROLE_DONOR.equals(donor.getRole()) || !donor.isActive()) {
                publishStatus.setValue(Resource.error("Only active donor accounts can publish food posts", null));
                return;
            }
            if (donor.getName() == null || donor.getName().trim().isEmpty()
                    || donor.getDonorType() == null || donor.getDonorType().trim().isEmpty()) {
                publishStatus.setValue(Resource.error("Complete your donor profile before publishing a post", null));
                return;
            }

            post.setDonorId(donorId);
            post.setDonorName(donor.getName());
            post.setDonorType(donor.getDonorType());
            post.setStatus(Constants.STATUS_AVAILABLE);
            post.setClaimedBy("");
            post.setClaimedByName("");
            post.setClaimId("");
            post.setCreatedAt(Timestamp.now());
            post.setUpdatedAt(Timestamp.now());

            CloudinaryUploader.uploadImage(getApplication(), imageUri, Constants.CLOUDINARY_FOLDER_FOOD_POSTS, new CloudinaryUploader.UploadListener() {
                @Override
                public void onSuccess(String url, String publicId) {
                    post.setPhotoUrl(url);
                    post.setPhotoPublicId(publicId);

                    postRepository.createPost(donorId, post)
                            .addOnSuccessListener(aVoid -> publishStatus.setValue(Resource.success(null)))
                            .addOnFailureListener(e -> publishStatus.setValue(Resource.error(
                                    errorMessage(e, "Could not save the food post"), null)));
                }

                @Override
                public void onError(String error) {
                    publishStatus.setValue(Resource.error("Photo upload failed: " + error, null));
                }
            });

        }).addOnFailureListener(e -> publishStatus.setValue(Resource.error(
                errorMessage(e, "Could not load donor information"), null)));
    }

    private String errorMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                ? fallback
                : exception.getMessage();
    }

    public void updatePost(FoodPost post, Uri newImageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionStatus.setValue(Resource.error("Please sign in before editing a post", null));
            return;
        }
        if (post == null || !currentUser.getUid().equals(post.getDonorId())) {
            actionStatus.setValue(Resource.error("You can only edit your own post", null));
            return;
        }
        actionStatus.setValue(Resource.loading());

        if (newImageUri != null) {
            CloudinaryUploader.uploadImage(getApplication(), newImageUri, Constants.CLOUDINARY_FOLDER_FOOD_POSTS, new CloudinaryUploader.UploadListener() {
                @Override
                public void onSuccess(String url, String publicId) {
                    post.setPhotoUrl(url);
                    post.setPhotoPublicId(publicId);
                    saveUpdatedPost(post);
                }

                @Override
                public void onError(String error) {
                    actionStatus.setValue(Resource.error("Photo upload failed: " + error, null));
                }
            });
        } else {
            saveUpdatedPost(post);
        }
    }

    private void saveUpdatedPost(FoodPost post) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionStatus.setValue(Resource.error("Please sign in before editing a post", null));
            return;
        }
        postRepository.updateAvailablePost(currentUser.getUid(), post)
                .addOnSuccessListener(updated -> actionStatus.setValue(updated
                        ? Resource.success(null)
                        : Resource.error("This post has expired and cannot be edited", null)))
                .addOnFailureListener(e -> actionStatus.setValue(Resource.error(
                        errorMessage(e, "Could not update the post"), null)));
    }

    public void fetchDonorPosts(String status) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            donorPosts.setValue(Resource.error("Please sign in to view your posts", null));
            return;
        }

        donorPosts.setValue(Resource.loading());
        Timestamp now = Timestamp.now();
        postRepository.getDonorPostsQuery(currentUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    List<FoodPost> allPosts = new ArrayList<>();
                    List<Task<Void>> expiryUpdates = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        FoodPost post = document.toObject(FoodPost.class);
                        if (post == null) {
                            continue;
                        }
                        post.setPostId(document.getId());
                        if (isExpired(post, now)) {
                            post.setStatus(Constants.STATUS_EXPIRED);
                            expiryUpdates.add(postRepository.markExpiredIfDue(post.getPostId(), now));
                        }
                        allPosts.add(post);
                    }

                    Tasks.whenAll(expiryUpdates)
                            .addOnSuccessListener(unused -> {
                                List<FoodPost> filteredPosts = new ArrayList<>();
                                for (FoodPost post : allPosts) {
                                    if (status == null || status.equalsIgnoreCase("All")
                                            || status.equals(post.getStatus())) {
                                        filteredPosts.add(post);
                                    }
                                }
                                donorPosts.setValue(Resource.success(filteredPosts));
                            })
                            .addOnFailureListener(e -> donorPosts.setValue(Resource.error(
                                    errorMessage(e, "Could not update expired posts"), null)));
                })
                .addOnFailureListener(e -> donorPosts.setValue(Resource.error(
                        errorMessage(e, "Could not load your posts"), null)));
    }

    public void fetchPost(String postId) {
        if (postId == null || postId.trim().isEmpty()) {
            selectedPost.setValue(Resource.error("Post could not be found", null));
            return;
        }
        selectedPost.setValue(Resource.loading());
        postRepository.getPost(postId)
                .addOnSuccessListener(document -> {
                    FoodPost post = document.toObject(FoodPost.class);
                    if (post == null) {
                        selectedPost.setValue(Resource.error("Post could not be found", null));
                        return;
                    }
                    post.setPostId(document.getId());
                    Timestamp now = Timestamp.now();
                    if (isExpired(post, now)) {
                        post.setStatus(Constants.STATUS_EXPIRED);
                        postRepository.markExpiredIfDue(post.getPostId(), now)
                                .addOnSuccessListener(unused -> selectedPost.setValue(Resource.success(post)))
                                .addOnFailureListener(e -> selectedPost.setValue(Resource.error(
                                        errorMessage(e, "Could not update expired post status"), null)));
                    } else {
                        selectedPost.setValue(Resource.success(post));
                    }
                })
                .addOnFailureListener(e -> selectedPost.setValue(Resource.error(
                        errorMessage(e, "Could not load this post"), null)));
    }

    public void fetchClaim(String claimId) {
        if (claimId == null || claimId.trim().isEmpty()) {
            selectedClaim.setValue(Resource.error("Claim details are unavailable", null));
            return;
        }
        selectedClaim.setValue(Resource.loading());
        claimRepository.getClaim(claimId)
                .addOnSuccessListener(document -> {
                    Claim claim = document.toObject(Claim.class);
                    if (claim == null) {
                        selectedClaim.setValue(Resource.error("Claim details are unavailable", null));
                        return;
                    }
                    selectedClaim.setValue(Resource.success(claim));
                })
                .addOnFailureListener(e -> selectedClaim.setValue(Resource.error(
                        errorMessage(e, "Could not load receiver contact details"), null)));
    }

    private boolean isExpired(FoodPost post, Timestamp now) {
        return (Constants.STATUS_AVAILABLE.equals(post.getStatus())
                || Constants.STATUS_CLAIMED.equals(post.getStatus()))
                && post.getExpiryTime() != null
                && post.getExpiryTime().compareTo(now) <= 0;
    }

    public void fetchNearbyPosts(double lat, double lng, double radiusInKm) {
        fetchNearbyPosts(lat, lng, radiusInKm, null, null);
    }

    public void fetchNearbyPosts(
            double lat,
            double lng,
            double radiusInKm,
            @Nullable String foodType,
            @Nullable String category
    ) {
        nearbyPosts.setValue(Resource.loading());
        if (!GeoUtils.isValidCoordinates(lat, lng)
                || !Double.isFinite(radiusInKm)
                || radiusInKm <= 0) {
            nearbyPosts.setValue(Resource.error("A valid location and search distance are required", null));
            return;
        }

        nearbyCenterLat = lat;
        nearbyCenterLng = lng;
        nearbyRadiusKm = radiusInKm;
        nearbyFoodType = foodType;
        nearbyCategory = category;

        Task<List<QuerySnapshot>> nearbyQuery;
        try {
            nearbyQuery = postRepository.getNearbyAvailablePosts(lat, lng, radiusInKm);
        } catch (IllegalArgumentException e) {
            nearbyPosts.setValue(Resource.error(errorMessage(e, "Could not search nearby posts"), null));
            return;
        }

        nearbyQuery
                .addOnSuccessListener(snapshots -> {
                    java.util.Map<String, FoodPost> postsById = new java.util.LinkedHashMap<>();
                    Timestamp now = Timestamp.now();
                    for (QuerySnapshot snapshot : snapshots) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post == null
                                    || !Constants.STATUS_AVAILABLE.equals(post.getStatus())
                                    || post.getExpiryTime() == null
                                    || post.getExpiryTime().compareTo(now) <= 0
                                    || post.getLat() == null
                                    || post.getLng() == null
                                    || !GeoUtils.isValidCoordinates(post.getLat(), post.getLng())
                                    || !matchesCategory(post.getCategory(), category)
                                    || !matchesFoodType(post.getFoodType(), foodType)) {
                                continue;
                            }

                            float distance = GeoUtils.getDistanceBetween(
                                    lat, lng, post.getLat(), post.getLng());
                            if (distance <= radiusInKm * 1000) {
                                post.setPostId(doc.getId());
                                postsById.put(doc.getId(), post);
                            }
                        }
                    }

                    List<FoodPost> posts = new ArrayList<>(postsById.values());
                    Collections.sort(posts, (post1, post2) -> {
                        float distance1 = GeoUtils.getDistanceBetween(
                                lat, lng, post1.getLat(), post1.getLng());
                        float distance2 = GeoUtils.getDistanceBetween(
                                lat, lng, post2.getLat(), post2.getLng());
                        return Float.compare(distance1, distance2);
                    });

                    nearbyPosts.setValue(Resource.success(posts));
                })
                .addOnFailureListener(e -> nearbyPosts.setValue(Resource.error(
                        errorMessage(e, "Could not load nearby posts"), null)));
    }

    static boolean matchesFoodType(String postFoodType, @Nullable String selectedFoodType) {
        if (selectedFoodType == null || selectedFoodType.trim().isEmpty()) {
            return true;
        }
        if (Constants.FOOD_TYPE_BOTH.equals(postFoodType)) {
            return Constants.FOOD_TYPE_VEG.equals(selectedFoodType)
                    || Constants.FOOD_TYPE_NON_VEG.equals(selectedFoodType)
                    || Constants.FOOD_TYPE_BOTH.equals(selectedFoodType);
        }
        return selectedFoodType.equals(postFoodType);
    }

    static boolean matchesCategory(String postCategory, @Nullable String selectedCategory) {
        return selectedCategory == null || selectedCategory.trim().isEmpty()
                || selectedCategory.equals(postCategory);
    }

    public void cancelPost(String postId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionStatus.setValue(Resource.error("Please sign in before cancelling a post", null));
            return;
        }
        actionStatus.setValue(Resource.loading());
        postRepository.cancelPost(currentUser.getUid(), postId)
                .addOnSuccessListener(cancelled -> actionStatus.setValue(cancelled
                        ? Resource.success(null)
                        : Resource.error("This post has expired and cannot be cancelled", null)))
                .addOnFailureListener(e -> actionStatus.setValue(Resource.error(
                        errorMessage(e, "Could not cancel the post"), null)));
    }
}
