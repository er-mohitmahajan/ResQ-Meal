package com.resqmeal.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseUser;
import com.resqmeal.app.model.User;
import com.resqmeal.app.repository.AuthRepository;
import com.resqmeal.app.utils.Resource;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository();
    }

    public boolean isUserLoggedIn() {
        return authRepository.isUserLoggedIn();
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return authRepository.getCurrentFirebaseUser();
    }

    public LiveData<Resource<User>> register(String name, String email, String phone, String password, String role, String type) {
        return authRepository.register(name, email, phone, password, role, type);
    }

    public LiveData<Resource<User>> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public LiveData<Resource<Void>> sendPasswordReset(String email) {
        return authRepository.sendPasswordReset(email);
    }

    public LiveData<Resource<Void>> sendEmailVerification() {
        return authRepository.sendEmailVerification();
    }

    public LiveData<Resource<Boolean>> checkEmailVerified() {
        return authRepository.checkEmailVerified();
    }

    public LiveData<Resource<User>> getCurrentUserData() {
        return authRepository.getCurrentUserData();
    }

    public void logout() {
        authRepository.logout();
    }
}

