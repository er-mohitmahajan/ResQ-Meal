package com.resqmeal.app.model;

public class AdminDashboardStats {
    private final long totalUsers;
    private final long donors;
    private final long receivers;
    private final long activePosts;
    private final long completedPickups;
    private final long mealsSaved;
    private final long pendingApprovals;
    private final long openReports;

    public AdminDashboardStats(
            long totalUsers,
            long donors,
            long receivers,
            long activePosts,
            long completedPickups,
            long mealsSaved,
            long pendingApprovals,
            long openReports
    ) {
        this.totalUsers = totalUsers;
        this.donors = donors;
        this.receivers = receivers;
        this.activePosts = activePosts;
        this.completedPickups = completedPickups;
        this.mealsSaved = mealsSaved;
        this.pendingApprovals = pendingApprovals;
        this.openReports = openReports;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getDonors() {
        return donors;
    }

    public long getReceivers() {
        return receivers;
    }

    public long getActivePosts() {
        return activePosts;
    }

    public long getCompletedPickups() {
        return completedPickups;
    }

    public long getMealsSaved() {
        return mealsSaved;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public long getOpenReports() {
        return openReports;
    }
}
