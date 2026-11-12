package com.resqmeal.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.ClaimRepository;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.repository.UserRepository;
import com.resqmeal.app.utils.Resource;

import java.util.ArrayList;
import java.util.List;

public class ClaimViewModel extends AndroidViewModel {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;

    private final MutableLiveData<Resource<String>> claimStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Claim>>> receiverClaims = new MutableLiveData<>();
    private final MutableLiveData<Resource<Claim>> selectedClaim = new MutableLiveData<>();
    private final MutableLiveData<Resource<FoodPost>> claimPost = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> actionStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> completionStatus = new MutableLiveData<>();

    public ClaimViewModel(@NonNull Application application) {
        super(application);
        claimRepository = new ClaimRepository();
        userRepository = new UserRepository();
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<Resource<String>> getClaimStatus() {
        return claimStatus;
    }

    public LiveData<Resource<List<Claim>>> getReceiverClaims() {
        return receiverClaims;
    }

    public LiveData<Resource<Claim>> getSelectedClaim() {
        return selectedClaim;
    }

    public LiveData<Resource<FoodPost>> getClaimPost() {
        return claimPost;
    }

    public LiveData<Resource<Void>> getActionStatus() {
        return actionStatus;
    }

    public LiveData<Resource<Void>> getCompletionStatus() {
        return completionStatus;
    }

    public void claimFood(FoodPost post) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            claimStatus.setValue(Resource.error("Sign in with a verified email before claiming food", null));
            return;
        }
        if (post == null || post.getPostId() == null) {
            claimStatus.setValue(Resource.error("This food post is no longer available", null));
            return;
        }

        claimStatus.setValue(Resource.loading());

        userRepository.getUser(currentUser.getUid()).addOnSuccessListener(documentSnapshot -> {
            User receiver = documentSnapshot.toObject(User.class);
            if (receiver == null || !Constants.ROLE_RECEIVER.equals(receiver.getRole())
                    || !Constants.APPROVAL_APPROVED.equals(receiver.getApprovalStatus())
                    || !receiver.isActive()) {
                claimStatus.setValue(Resource.error(
                        "Only active, approved receiver accounts can claim food", null));
                return;
            }

            claimRepository.claimFood(receiver.getUid(), post.getPostId())
                    .addOnSuccessListener(claimId -> claimStatus.setValue(Resource.success(claimId)))
                    .addOnFailureListener(e -> claimStatus.setValue(Resource.error(
                            errorMessage(e, "Could not claim this food"), null)));

        }).addOnFailureListener(e -> claimStatus.setValue(Resource.error(
                errorMessage(e, "Could not load receiver profile"), null)));
    }

    public void fetchReceiverClaims() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            receiverClaims.setValue(Resource.error("Sign in to view your claims", null));
            return;
        }

        receiverClaims.setValue(Resource.loading());
        claimRepository.getReceiverClaimsQuery(currentUser.getUid()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Claim> claims = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Claim claim = doc.toObject(Claim.class);
                        if (claim != null) {
                            claim.setClaimId(doc.getId());
                            claims.add(claim);
                        }
                    }
                    receiverClaims.setValue(Resource.success(claims));
                })
                .addOnFailureListener(e -> receiverClaims.setValue(Resource.error(
                        errorMessage(e, "Could not load your claims"), null)));
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
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (claim == null || currentUser == null
                            || !currentUser.getUid().equals(claim.getReceiverId())) {
                        selectedClaim.setValue(Resource.error(
                                "This claim is not available in your account", null));
                        return;
                    }
                    claim.setClaimId(document.getId());
                    selectedClaim.setValue(Resource.success(claim));
                    fetchPostForClaim(claim.getPostId());
                })
                .addOnFailureListener(e -> selectedClaim.setValue(Resource.error(
                        errorMessage(e, "Could not load claim details"), null)));
    }

    private void fetchPostForClaim(String postId) {
        claimPost.setValue(Resource.loading());
        claimRepository.getPost(postId)
                .addOnSuccessListener(document -> {
                    FoodPost post = document.toObject(FoodPost.class);
                    if (post == null) {
                        claimPost.setValue(Resource.error("Pickup location is unavailable", null));
                        return;
                    }
                    post.setPostId(document.getId());
                    claimPost.setValue(Resource.success(post));
                })
                .addOnFailureListener(e -> claimPost.setValue(Resource.error(
                        errorMessage(e, "Could not load pickup details"), null)));
    }

    public void cancelClaim(String claimId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionStatus.setValue(Resource.error("Sign in before cancelling a claim", null));
            return;
        }
        actionStatus.setValue(Resource.loading());
        claimRepository.cancelClaim(currentUser.getUid(), claimId)
                .addOnSuccessListener(unused -> actionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(e -> actionStatus.setValue(Resource.error(
                        errorMessage(e, "Could not cancel this claim"), null)));
    }

    public void completePickup(String claimId, String pickupCode) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            completionStatus.setValue(Resource.error(
                    "Sign in with a verified email before verifying a pickup", null));
            return;
        }
        if (claimId == null || claimId.trim().isEmpty()) {
            completionStatus.setValue(Resource.error("Pickup details are unavailable", null));
            return;
        }

        completionStatus.setValue(Resource.loading());
        claimRepository.completePickup(currentUser.getUid(), claimId, pickupCode)
                .addOnSuccessListener(unused -> completionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(e -> completionStatus.setValue(Resource.error(
                        errorMessage(e, "Could not verify this pickup"), null)));
    }

    private String errorMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                ? fallback
                : exception.getMessage();
    }
}
