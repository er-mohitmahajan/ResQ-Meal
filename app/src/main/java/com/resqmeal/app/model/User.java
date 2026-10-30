package com.resqmeal.app.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class User implements Serializable {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String role; // donor, receiver, admin
    private String donorType; // restaurant, hostel_mess, event, individual
    private String receiverType; // ngo, shelter, volunteer
    private String address;
    private Double lat;
    private Double lng;
    private String geohash;
    private String profilePhotoUrl;
    private String approvalStatus; // approved, pending, rejected
    private String rejectionReason;
    private boolean isActive;
    private long totalPosted;
    private long totalMealsDonated;
    private long totalClaimed;
    private long totalMealsReceived;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
        // Required for Firestore serialization
        this.isActive = true;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDonorType() {
        return donorType;
    }

    public void setDonorType(String donorType) {
        this.donorType = donorType;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getTotalPosted() {
        return totalPosted;
    }

    public void setTotalPosted(long totalPosted) {
        this.totalPosted = totalPosted;
    }

    public long getTotalMealsDonated() {
        return totalMealsDonated;
    }

    public void setTotalMealsDonated(long totalMealsDonated) {
        this.totalMealsDonated = totalMealsDonated;
    }

    public long getTotalClaimed() {
        return totalClaimed;
    }

    public void setTotalClaimed(long totalClaimed) {
        this.totalClaimed = totalClaimed;
    }

    public long getTotalMealsReceived() {
        return totalMealsReceived;
    }

    public void setTotalMealsReceived(long totalMealsReceived) {
        this.totalMealsReceived = totalMealsReceived;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
