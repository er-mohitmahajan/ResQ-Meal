package com.resqmeal.app.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.NotificationRepository;
import com.resqmeal.app.utils.Constants;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class ClaimRepository {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final FirebaseFirestore db;
    private final CollectionReference claimsRef;
    private final CollectionReference postsRef;
    private final CollectionReference usersRef;
    private final NotificationRepository notificationRepository;

    public ClaimRepository() {
        db = FirebaseFirestore.getInstance();
        claimsRef = db.collection(Constants.COLLECTION_CLAIMS);
        postsRef = db.collection(Constants.COLLECTION_FOOD_POSTS);
        usersRef = db.collection(Constants.COLLECTION_USERS);
        notificationRepository = new NotificationRepository();
    }

    static String generatePickupCode() {
        return String.format("%04d", SECURE_RANDOM.nextInt(10_000));
    }

    static boolean hasActiveClaimCapacity(long activeClaimCount) {
        return activeClaimCount < Constants.MAX_ACTIVE_CLAIMS;
    }

    static boolean isValidPickupCodeFormat(String pickupCode) {
        return pickupCode != null && pickupCode.matches("[0-9]{4}");
    }

    public Task<String> claimFood(String receiverId, String postId) {
        DocumentReference receiverRef = usersRef.document(receiverId);
        return receiverRef.get().onSuccessTask(receiverSnapshot -> {
            if (receiverSnapshot.contains("activeClaimCount")) {
                return runClaimTransaction(receiverId, postId, 0);
            }
            return claimsRef.whereEqualTo("receiverId", receiverId)
                    .whereEqualTo("status", Constants.CLAIM_STATUS_ACTIVE)
                    .get()
                    .onSuccessTask(activeClaims -> runClaimTransaction(
                            receiverId, postId, activeClaims.size()));
        });
    }

    private Task<String> runClaimTransaction(
            String receiverId,
            String postId,
            long legacyActiveClaimCount
    ) {
        DocumentReference receiverRef = usersRef.document(receiverId);
        DocumentReference postRef = postsRef.document(postId);
        DocumentReference claimRef = claimsRef.document();
        String pickupCode = generatePickupCode();
        return db.runTransaction(transaction -> {
            DocumentSnapshot receiverSnapshot = transaction.get(receiverRef);
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            User receiver = receiverSnapshot.toObject(User.class);
            FoodPost post = postSnapshot.toObject(FoodPost.class);

            if (receiver == null || !Constants.ROLE_RECEIVER.equals(receiver.getRole())
                    || !Constants.APPROVAL_APPROVED.equals(receiver.getApprovalStatus())
                    || !receiver.isActive()) {
                throw new FirebaseFirestoreException(
                        "Only active, approved receivers can claim food",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (post == null) {
                throw new FirebaseFirestoreException(
                        "This food post is no longer available",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (receiverId.equals(post.getDonorId())) {
                throw new FirebaseFirestoreException(
                        "You cannot claim your own food post",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (!Constants.STATUS_AVAILABLE.equals(post.getStatus())) {
                throw new FirebaseFirestoreException(
                        "Someone just claimed this food. Please refresh the feed.",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            Timestamp now = Timestamp.now();
            if (post.getExpiryTime() == null || post.getExpiryTime().compareTo(now) <= 0) {
                throw new FirebaseFirestoreException(
                        "This food post has expired",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            long activeClaimCount = receiverSnapshot.contains("activeClaimCount")
                    ? receiver.getActiveClaimCount()
                    : Math.max(receiver.getActiveClaimCount(), legacyActiveClaimCount);
            if (!hasActiveClaimCapacity(activeClaimCount)) {
                throw new FirebaseFirestoreException(
                        "You can have no more than 3 active claims",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Claim claim = new Claim();
            claim.setClaimId(claimRef.getId());
            claim.setPostId(postId);
            claim.setPostTitle(post.getTitle());
            claim.setDonorId(post.getDonorId());
            claim.setDonorName(post.getDonorName());
            claim.setReceiverId(receiverId);
            claim.setReceiverName(receiver.getName());
            claim.setReceiverPhone(receiver.getPhone());
            claim.setPickupCode(pickupCode);
            claim.setStatus(Constants.CLAIM_STATUS_ACTIVE);
            claim.setClaimedAt(now);

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("status", Constants.STATUS_CLAIMED);
            postUpdates.put("claimedBy", receiverId);
            postUpdates.put("claimedByName", receiver.getName());
            postUpdates.put("claimId", claim.getClaimId());
            postUpdates.put("updatedAt", now);

            transaction.set(claimRef, claim);
            transaction.update(postRef, postUpdates);
            transaction.update(receiverRef, "activeClaimCount", activeClaimCount + 1);
            notificationRepository.addToTransaction(transaction, notificationRepository.createNotification(
                    post.getDonorId(),
                    "Your food post was claimed",
                    receiver.getName() + " claimed " + post.getTitle() + ".",
                    Constants.NOTIF_TYPE_POST_CLAIMED,
                    claim.getClaimId()));
            return claim.getClaimId();
        });
    }

    public Query getReceiverClaimsQuery(String receiverId) {
        return claimsRef.whereEqualTo("receiverId", receiverId)
                .orderBy("claimedAt", Query.Direction.DESCENDING);
    }

    public Task<DocumentSnapshot> getClaim(String claimId) {
        return claimsRef.document(claimId).get();
    }

    public Task<DocumentSnapshot> getPost(String postId) {
        return postsRef.document(postId).get();
    }

    public Task<Void> cancelClaim(String receiverId, String claimId) {
        DocumentReference receiverRef = usersRef.document(receiverId);
        DocumentReference claimRef = claimsRef.document(claimId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot receiverSnapshot = transaction.get(receiverRef);
            DocumentSnapshot claimSnapshot = transaction.get(claimRef);
            Claim claim = claimSnapshot.toObject(Claim.class);
            User receiver = receiverSnapshot.toObject(User.class);
            if (claim == null || receiver == null || !receiverId.equals(claim.getReceiverId())) {
                throw new FirebaseFirestoreException(
                        "This claim could not be found in your account",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (!Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus())) {
                throw new FirebaseFirestoreException(
                        "Only active claims can be cancelled",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            DocumentReference postRef = postsRef.document(claim.getPostId());
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            FoodPost post = postSnapshot.toObject(FoodPost.class);
            if (post == null) {
                throw new FirebaseFirestoreException(
                        "The food post for this claim could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            boolean postStillReferencesClaim = claimId.equals(post.getClaimId());
            boolean postClaimed = Constants.STATUS_CLAIMED.equals(post.getStatus());
            boolean postExpired = Constants.STATUS_EXPIRED.equals(post.getStatus());
            boolean postCancelled = Constants.STATUS_CANCELLED.equals(post.getStatus());
            if (!postCancelled && !(postStillReferencesClaim && (postClaimed || postExpired))) {
                throw new FirebaseFirestoreException(
                        "This claim is no longer active for its food post",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Timestamp now = Timestamp.now();
            Map<String, Object> claimUpdates = new HashMap<>();
            claimUpdates.put("status", Constants.CLAIM_STATUS_CANCELLED);
            claimUpdates.put("cancelReason", "Cancelled by receiver");
            claimUpdates.put("cancelledAt", now);
            transaction.update(claimRef, claimUpdates);

            if (!postCancelled && postClaimed) {
                Map<String, Object> postUpdates = new HashMap<>();
                postUpdates.put("updatedAt", now);
                if (post.getExpiryTime() != null && post.getExpiryTime().compareTo(now) > 0) {
                    postUpdates.put("status", Constants.STATUS_AVAILABLE);
                    postUpdates.put("claimedBy", "");
                    postUpdates.put("claimedByName", "");
                    postUpdates.put("claimId", "");
                } else {
                    postUpdates.put("status", Constants.STATUS_EXPIRED);
                }
                transaction.update(postRef, postUpdates);
            }

            long activeClaimCount = Math.max(0, receiver.getActiveClaimCount() - 1);
            transaction.update(receiverRef, "activeClaimCount", activeClaimCount);
            notificationRepository.addToTransaction(transaction, notificationRepository.createNotification(
                    claim.getDonorId(),
                    "A food claim was cancelled",
                    claim.getReceiverName() + " cancelled the claim for " + claim.getPostTitle() + ".",
                    Constants.NOTIF_TYPE_CLAIM_CANCELLED,
                    claim.getClaimId()));
            return null;
        });
    }

    public Task<Void> completePickup(String donorId, String claimId, String pickupCode) {
        if (donorId == null || donorId.trim().isEmpty()
                || claimId == null || claimId.trim().isEmpty()
                || !isValidPickupCodeFormat(pickupCode)) {
            return Tasks.forException(new FirebaseFirestoreException(
                    "Enter a valid 4-digit pickup code",
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT));
        }

        DocumentReference donorRef = usersRef.document(donorId);
        DocumentReference claimRef = claimsRef.document(claimId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot claimSnapshot = transaction.get(claimRef);
            Claim claim = claimSnapshot.toObject(Claim.class);
            if (claim == null) {
                throw new FirebaseFirestoreException(
                        "This claim could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!donorId.equals(claim.getDonorId())) {
                throw new FirebaseFirestoreException(
                        "You can only verify pickups for your own food posts",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (!Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus())) {
                throw new FirebaseFirestoreException(
                        "This claim has already been completed or cancelled",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (claim.getPostId() == null || claim.getReceiverId() == null) {
                throw new FirebaseFirestoreException(
                        "This claim is missing required pickup details",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (claim.getPickupCode() == null || !claim.getPickupCode().equals(pickupCode)) {
                throw new FirebaseFirestoreException(
                        "The pickup code does not match",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT);
            }

            DocumentReference postRef = postsRef.document(claim.getPostId());
            DocumentReference receiverRef = usersRef.document(claim.getReceiverId());
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            DocumentSnapshot donorSnapshot = transaction.get(donorRef);
            DocumentSnapshot receiverSnapshot = transaction.get(receiverRef);
            FoodPost post = postSnapshot.toObject(FoodPost.class);
            User donor = donorSnapshot.toObject(User.class);
            User receiver = receiverSnapshot.toObject(User.class);

            if (post == null || donor == null || receiver == null) {
                throw new FirebaseFirestoreException(
                        "The post or account for this pickup could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!Constants.ROLE_DONOR.equals(donor.getRole()) || !donor.isActive()) {
                throw new FirebaseFirestoreException(
                        "Only an active donor can verify a pickup",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            if (!Constants.ROLE_RECEIVER.equals(receiver.getRole())
                    || !donorId.equals(post.getDonorId())
                    || !claimId.equals(post.getClaimId())
                    || !claim.getReceiverId().equals(post.getClaimedBy())
                    || !Constants.STATUS_CLAIMED.equals(post.getStatus())) {
                throw new FirebaseFirestoreException(
                        "This claim is no longer active for its food post",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Timestamp now = Timestamp.now();
            if (post.getExpiryTime() == null || post.getExpiryTime().compareTo(now) <= 0) {
                throw new FirebaseFirestoreException(
                        "This pickup has expired and cannot be verified",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (post.getServings() <= 0) {
                throw new FirebaseFirestoreException(
                        "The post has an invalid serving count",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Map<String, Object> claimUpdates = new HashMap<>();
            claimUpdates.put("status", Constants.CLAIM_STATUS_COMPLETED);
            claimUpdates.put("completedAt", now);

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("status", Constants.STATUS_COMPLETED);
            postUpdates.put("updatedAt", now);

            Map<String, Object> donorUpdates = new HashMap<>();
            donorUpdates.put("totalMealsDonated", FieldValue.increment(post.getServings()));
            donorUpdates.put("updatedAt", now);

            Map<String, Object> receiverUpdates = new HashMap<>();
            receiverUpdates.put("totalClaimed", FieldValue.increment(1));
            receiverUpdates.put("totalMealsReceived", FieldValue.increment(post.getServings()));
            receiverUpdates.put("activeClaimCount", Math.max(0, receiver.getActiveClaimCount() - 1));
            receiverUpdates.put("updatedAt", now);

            transaction.update(claimRef, claimUpdates);
            transaction.update(postRef, postUpdates);
            transaction.update(donorRef, donorUpdates);
            transaction.update(receiverRef, receiverUpdates);
            notificationRepository.addToTransaction(transaction, notificationRepository.createNotification(
                    donorId,
                    "Pickup completed",
                    "The pickup for " + post.getTitle() + " has been verified.",
                    Constants.NOTIF_TYPE_PICKUP_COMPLETED,
                    claimId));
            notificationRepository.addToTransaction(transaction, notificationRepository.createNotification(
                    claim.getReceiverId(),
                    "Your pickup is complete",
                    "Your pickup for " + post.getTitle() + " has been confirmed.",
                    Constants.NOTIF_TYPE_PICKUP_COMPLETED,
                    claimId));
            return null;
        });
    }
}
