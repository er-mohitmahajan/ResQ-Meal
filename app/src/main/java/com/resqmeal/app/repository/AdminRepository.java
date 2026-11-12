package com.resqmeal.app.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.AggregateField;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.resqmeal.app.model.AdminDashboardStats;
import com.resqmeal.app.model.AppNotification;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.Report;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final CollectionReference usersRef = db.collection(Constants.COLLECTION_USERS);
    private final CollectionReference postsRef = db.collection(Constants.COLLECTION_FOOD_POSTS);
    private final CollectionReference claimsRef = db.collection(Constants.COLLECTION_CLAIMS);
    private final CollectionReference reportsRef = db.collection(Constants.COLLECTION_REPORTS);
    private final NotificationRepository notificationRepository = new NotificationRepository();

    public Task<List<User>> getPendingReceivers() {
        return authorizedQuery(
                usersRef.whereEqualTo("role", Constants.ROLE_RECEIVER)
                        .whereEqualTo("approvalStatus", Constants.APPROVAL_PENDING)
                        )
                .continueWith(this::toUsers);
    }

    public Task<List<User>> getUsers() {
        return authorizedQuery(usersRef.orderBy("createdAt", Query.Direction.DESCENDING))
                .continueWith(this::toUsers);
    }

    public Task<List<Report>> getOpenReports() {
        return authorizedQuery(reportsRef.whereEqualTo("status", Constants.REPORT_STATUS_OPEN)
                        .orderBy("createdAt", Query.Direction.DESCENDING))
                .continueWith(task -> {
                    QuerySnapshot snapshot = getResult(task);
                    List<Report> reports = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Report report = document.toObject(Report.class);
                        if (report != null) {
                            report.setReportId(document.getId());
                            reports.add(report);
                        }
                    }
                    return reports;
                });
    }

    public Task<List<FoodPost>> getPosts() {
        return authorizedQuery(postsRef.orderBy("createdAt", Query.Direction.DESCENDING))
                .continueWith(task -> {
                    QuerySnapshot snapshot = getResult(task);
                    List<FoodPost> posts = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        FoodPost post = document.toObject(FoodPost.class);
                        if (post != null) {
                            post.setPostId(document.getId());
                            posts.add(post);
                        }
                    }
                    return posts;
                });
    }

    public Task<AdminDashboardStats> getDashboardStats() {
        return requireAdmin().continueWithTask(task -> {
            getResult(task);
            return Tasks.whenAllSuccess(
                    count(usersRef),
                    count(usersRef.whereEqualTo("role", Constants.ROLE_DONOR)),
                    count(usersRef.whereEqualTo("role", Constants.ROLE_RECEIVER)),
                    count(postsRef.whereEqualTo("status", Constants.STATUS_AVAILABLE)),
                    count(postsRef.whereEqualTo("status", Constants.STATUS_CLAIMED)),
                    count(claimsRef.whereEqualTo("status", Constants.CLAIM_STATUS_COMPLETED)),
                    sum(postsRef.whereEqualTo("status", Constants.STATUS_COMPLETED), "servings"),
                    count(usersRef.whereEqualTo("role", Constants.ROLE_RECEIVER)
                            .whereEqualTo("approvalStatus", Constants.APPROVAL_PENDING)),
                    count(reportsRef.whereEqualTo("status", Constants.REPORT_STATUS_OPEN)));
        })
                .continueWith(task -> {
                    List<?> counts = getResult(task);
                    if (counts.size() != 9) {
                        throw new IllegalStateException("Could not calculate dashboard statistics");
                    }
                    return new AdminDashboardStats(
                            countResult(counts, 0),
                            countResult(counts, 1),
                            countResult(counts, 2),
                            countResult(counts, 3) + countResult(counts, 4),
                            countResult(counts, 5),
                            countResult(counts, 6),
                            countResult(counts, 7),
                            countResult(counts, 8));
                });
    }

    private Task<Long> count(Query query) {
        return query.count().get(AggregateSource.SERVER)
                .continueWith(task -> getResult(task).getCount());
    }

    private Task<Long> sum(Query query, String field) {
        AggregateField sumField = AggregateField.sum(field);
        return query.aggregate(sumField)
                .get(AggregateSource.SERVER)
                .continueWith(task -> {
                    AggregateQuerySnapshot snapshot = getResult(task);
                    Object value = snapshot.get(sumField);
                    return value instanceof Number ? ((Number) value).longValue() : 0L;
                });
    }

    private long countResult(List<?> counts, int index) {
        Object value = counts.get(index);
        if (!(value instanceof Number)) {
            throw new IllegalStateException("Could not calculate dashboard statistics");
        }
        return ((Number) value).longValue();
    }

    public Task<Void> setReceiverApproval(String receiverId, boolean approved, String rejectionReason) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return failed("Receiver account could not be found", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        String normalizedReason = rejectionReason == null ? "" : rejectionReason.trim();
        if (!approved && normalizedReason.isEmpty()) {
            return failed("Enter a reason when rejecting a receiver", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        String status = approved ? Constants.APPROVAL_APPROVED : Constants.APPROVAL_REJECTED;
        String adminId = currentUserId();
        if (adminId == null) {
            return failed("Sign in as an admin to manage approvals", FirebaseFirestoreException.Code.PERMISSION_DENIED);
        }

        DocumentReference adminRef = usersRef.document(adminId);
        DocumentReference receiverRef = usersRef.document(receiverId);
        return db.runTransaction(transaction -> {
            requireAdmin(transaction, adminRef);
            DocumentSnapshot receiverSnapshot = transaction.get(receiverRef);
            User receiver = receiverSnapshot.toObject(User.class);
            if (receiver == null || !Constants.ROLE_RECEIVER.equals(receiver.getRole())) {
                throw new FirebaseFirestoreException(
                        "This receiver account could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!Constants.APPROVAL_PENDING.equals(receiver.getApprovalStatus())) {
                throw new FirebaseFirestoreException(
                        "This receiver has already been reviewed",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Timestamp now = Timestamp.now();
            Map<String, Object> updates = new HashMap<>();
            updates.put("approvalStatus", status);
            updates.put("rejectionReason", approved ? "" : normalizedReason);
            updates.put("updatedAt", now);
            transaction.update(receiverRef, updates);

            AppNotification notification = notificationRepository.createNotification(
                    receiverId,
                    approved ? "Receiver account approved" : "Receiver account update",
                    approved
                            ? "Your account is approved. You can now browse and claim nearby food."
                            : "Your receiver account was not approved. Reason: " + normalizedReason,
                    approved ? Constants.NOTIF_TYPE_ACCOUNT_APPROVED : Constants.NOTIF_TYPE_ACCOUNT_REJECTED,
                    receiverId);
            notificationRepository.addToTransaction(transaction, notification);
            return null;
        });
    }

    public Task<Void> setUserActive(String targetUserId, boolean active) {
        String adminId = currentUserId();
        if (adminId == null || targetUserId == null || targetUserId.trim().isEmpty()) {
            return failed("Select a valid user account", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        DocumentReference adminRef = usersRef.document(adminId);
        DocumentReference targetRef = usersRef.document(targetUserId);
        return db.runTransaction(transaction -> {
            requireAdmin(transaction, adminRef);
            DocumentSnapshot targetSnapshot = transaction.get(targetRef);
            User target = targetSnapshot.toObject(User.class);
            if (target == null) {
                throw new FirebaseFirestoreException(
                        "This user account could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (adminId.equals(targetUserId) || Constants.ROLE_ADMIN.equals(target.getRole())) {
                throw new FirebaseFirestoreException(
                        "Admin accounts cannot be deactivated here",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", active);
            updates.put("updatedAt", Timestamp.now());
            transaction.update(targetRef, updates);
            return null;
        });
    }

    public Task<Void> resolveReport(
            String reportId,
            String status,
            String adminNote,
            boolean deactivateTarget,
            boolean removeTargetPost
    ) {
        if (!Constants.REPORT_STATUS_RESOLVED.equals(status)
                && !Constants.REPORT_STATUS_DISMISSED.equals(status)) {
            return failed("Choose resolve or dismiss for this report",
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        String adminId = currentUserId();
        if (adminId == null || reportId == null || reportId.trim().isEmpty()) {
            return failed("Select a valid report", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        DocumentReference adminRef = usersRef.document(adminId);
        DocumentReference reportRef = reportsRef.document(reportId);
        return db.runTransaction(transaction -> {
            requireAdmin(transaction, adminRef);
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);
            Report report = reportSnapshot.toObject(Report.class);
            if (report == null) {
                throw new FirebaseFirestoreException(
                        "This report could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!Constants.REPORT_STATUS_OPEN.equals(report.getStatus())) {
                throw new FirebaseFirestoreException(
                        "This report has already been handled",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            DocumentReference targetRef = null;
            DocumentSnapshot targetSnapshot = null;
            if (deactivateTarget || removeTargetPost) {
                if ((deactivateTarget && !Constants.REPORT_TARGET_USER.equals(report.getTargetType()))
                        || (removeTargetPost && !Constants.REPORT_TARGET_POST.equals(report.getTargetType()))) {
                    throw new FirebaseFirestoreException(
                            "The selected action does not match the reported content",
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT);
                }
                if (Constants.REPORT_TARGET_USER.equals(report.getTargetType())) {
                    targetRef = usersRef.document(report.getTargetId());
                } else if (Constants.REPORT_TARGET_POST.equals(report.getTargetType())) {
                    targetRef = postsRef.document(report.getTargetId());
                } else {
                    throw new FirebaseFirestoreException(
                            "This report has an unsupported target type",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                }
                targetSnapshot = transaction.get(targetRef);
                if (!targetSnapshot.exists()) {
                    throw new FirebaseFirestoreException(
                            "The reported content could not be found",
                            FirebaseFirestoreException.Code.NOT_FOUND);
                }
            }

            Timestamp now = Timestamp.now();
            Map<String, Object> reportUpdates = new HashMap<>();
            reportUpdates.put("status", status);
            reportUpdates.put("adminNote", adminNote == null ? "" : adminNote.trim());
            reportUpdates.put("resolvedBy", adminId);
            reportUpdates.put("resolvedAt", now);
            transaction.update(reportRef, reportUpdates);

            if (deactivateTarget) {
                User target = targetSnapshot.toObject(User.class);
                if (target == null || Constants.ROLE_ADMIN.equals(target.getRole())
                        || adminId.equals(report.getTargetId())) {
                    throw new FirebaseFirestoreException(
                            "Admin accounts cannot be deactivated through a report",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                }
                transaction.update(targetRef, "active", false);
                transaction.update(targetRef, "updatedAt", now);
            }
            if (removeTargetPost) {
                FoodPost post = targetSnapshot.toObject(FoodPost.class);
                if (post == null) {
                    throw new FirebaseFirestoreException(
                            "The reported food post could not be found",
                            FirebaseFirestoreException.Code.NOT_FOUND);
                }
                transaction.update(targetRef, "status", Constants.STATUS_CANCELLED);
                transaction.update(targetRef, "updatedAt", now);
            }
            return null;
        });
    }

    public Task<Void> removePost(String postId) {
        String adminId = currentUserId();
        if (adminId == null || postId == null || postId.trim().isEmpty()) {
            return failed("Select a valid food post", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        DocumentReference adminRef = usersRef.document(adminId);
        DocumentReference postRef = postsRef.document(postId);
        return db.runTransaction(transaction -> {
            requireAdmin(transaction, adminRef);
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            FoodPost post = postSnapshot.toObject(FoodPost.class);
            if (post == null) {
                throw new FirebaseFirestoreException(
                        "This food post could not be found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            transaction.update(postRef, "status", Constants.STATUS_CANCELLED);
            transaction.update(postRef, "updatedAt", Timestamp.now());
            return null;
        });
    }

    private Task<DocumentSnapshot> requireAdmin() {
        String adminId = currentUserId();
        if (adminId == null) {
            return Tasks.forException(new FirebaseFirestoreException(
                    "Sign in as an admin to access this panel",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED));
        }

        return usersRef.document(adminId).get().continueWith(task -> {
            DocumentSnapshot snapshot = getResult(task);
            User user = snapshot.toObject(User.class);
            if (user == null || !Constants.ROLE_ADMIN.equals(user.getRole()) || !user.isActive()) {
                throw new FirebaseFirestoreException(
                        "Admin access is required",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }
            return snapshot;
        });
    }

    private Task<QuerySnapshot> authorizedQuery(Query query) {
        return requireAdmin().continueWithTask(task -> {
            getResult(task);
            return query.get();
        });
    }

    private void requireAdmin(Transaction transaction, DocumentReference adminRef)
            throws FirebaseFirestoreException {
        DocumentSnapshot adminSnapshot = transaction.get(adminRef);
        User admin = adminSnapshot.toObject(User.class);
        if (admin == null || !Constants.ROLE_ADMIN.equals(admin.getRole()) || !admin.isActive()) {
            throw new FirebaseFirestoreException(
                    "Admin access is required",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED);
        }
    }

    private String currentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser == null || !currentUser.isEmailVerified()
                ? null : currentUser.getUid();
    }

    private List<User> toUsers(com.google.android.gms.tasks.Task<QuerySnapshot> task)
            throws Exception {
        QuerySnapshot snapshot = getResult(task);
        List<User> users = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setUid(document.getId());
                users.add(user);
            }
        }
        return users;
    }

    private static <T> T getResult(com.google.android.gms.tasks.Task<T> task) throws Exception {
        if (!task.isSuccessful()) {
            throw task.getException() == null
                    ? new IllegalStateException("Admin request failed")
                    : task.getException();
        }
        return task.getResult();
    }

    private static <T> Task<T> failed(String message, FirebaseFirestoreException.Code code) {
        return Tasks.forException(new FirebaseFirestoreException(message, code));
    }
}
