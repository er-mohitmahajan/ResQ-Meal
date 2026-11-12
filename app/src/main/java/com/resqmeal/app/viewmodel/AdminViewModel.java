package com.resqmeal.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resqmeal.app.model.AdminDashboardStats;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.Report;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.AdminRepository;
import com.resqmeal.app.utils.Resource;

import java.util.List;

public class AdminViewModel extends AndroidViewModel {
    private final AdminRepository repository;
    private final MutableLiveData<Resource<AdminDashboardStats>> dashboardStats = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<User>>> users = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Report>>> reports = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FoodPost>>> posts = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> actionStatus = new MutableLiveData<>();

    public AdminViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
    }

    public LiveData<Resource<AdminDashboardStats>> getDashboardStats() {
        return dashboardStats;
    }

    public LiveData<Resource<List<User>>> getUsers() {
        return users;
    }

    public LiveData<Resource<List<Report>>> getReports() {
        return reports;
    }

    public LiveData<Resource<List<FoodPost>>> getPosts() {
        return posts;
    }

    public LiveData<Resource<Void>> getActionStatus() {
        return actionStatus;
    }

    public void loadDashboard() {
        dashboardStats.setValue(Resource.loading());
        repository.getDashboardStats()
                .addOnSuccessListener(stats -> dashboardStats.setValue(Resource.success(stats)))
                .addOnFailureListener(error -> dashboardStats.setValue(Resource.error(
                        errorMessage(error, "Could not load admin dashboard"), null)));
    }

    public void loadApprovals() {
        users.setValue(Resource.loading());
        repository.getPendingReceivers()
                .addOnSuccessListener(result -> users.setValue(Resource.success(result)))
                .addOnFailureListener(error -> users.setValue(Resource.error(
                        errorMessage(error, "Could not load receiver approvals"), null)));
    }

    public void loadUsers() {
        users.setValue(Resource.loading());
        repository.getUsers()
                .addOnSuccessListener(result -> users.setValue(Resource.success(result)))
                .addOnFailureListener(error -> users.setValue(Resource.error(
                        errorMessage(error, "Could not load users"), null)));
    }

    public void loadReports() {
        reports.setValue(Resource.loading());
        repository.getOpenReports()
                .addOnSuccessListener(result -> reports.setValue(Resource.success(result)))
                .addOnFailureListener(error -> reports.setValue(Resource.error(
                        errorMessage(error, "Could not load reports"), null)));
    }

    public void loadPosts() {
        posts.setValue(Resource.loading());
        repository.getPosts()
                .addOnSuccessListener(result -> posts.setValue(Resource.success(result)))
                .addOnFailureListener(error -> posts.setValue(Resource.error(
                        errorMessage(error, "Could not load food posts"), null)));
    }

    public void setReceiverApproval(String receiverId, boolean approved, String rejectionReason) {
        actionStatus.setValue(Resource.loading());
        repository.setReceiverApproval(receiverId, approved, rejectionReason)
                .addOnSuccessListener(unused -> actionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(error -> actionStatus.setValue(Resource.error(
                        errorMessage(error, "Could not update receiver approval"), null)));
    }

    public void setUserActive(String userId, boolean active) {
        actionStatus.setValue(Resource.loading());
        repository.setUserActive(userId, active)
                .addOnSuccessListener(unused -> actionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(error -> actionStatus.setValue(Resource.error(
                        errorMessage(error, "Could not update user status"), null)));
    }

    public void resolveReport(
            String reportId,
            String status,
            String adminNote,
            boolean deactivateTarget,
            boolean removeTargetPost
    ) {
        actionStatus.setValue(Resource.loading());
        repository.resolveReport(reportId, status, adminNote, deactivateTarget, removeTargetPost)
                .addOnSuccessListener(unused -> actionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(error -> actionStatus.setValue(Resource.error(
                        errorMessage(error, "Could not update report"), null)));
    }

    public void removePost(String postId) {
        actionStatus.setValue(Resource.loading());
        repository.removePost(postId)
                .addOnSuccessListener(unused -> actionStatus.setValue(Resource.success(null)))
                .addOnFailureListener(error -> actionStatus.setValue(Resource.error(
                        errorMessage(error, "Could not remove post"), null)));
    }

    private String errorMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                ? fallback
                : exception.getMessage();
    }
}
