package com.example.alertasegura.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alertasegura.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // Los Fragments observan estos LiveData
    public final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    public final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public void register(String email, String password) {
        loadingLiveData.setValue(true);
        authRepository.register(email, password, userLiveData, errorLiveData);
        // Cuando userLiveData o errorLiveData se actualicen, el Fragment reacciona
        userLiveData.observeForever(user -> loadingLiveData.setValue(false));
        errorLiveData.observeForever(err -> loadingLiveData.setValue(false));
    }

    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        authRepository.login(email, password, userLiveData, errorLiveData);
        userLiveData.observeForever(user -> loadingLiveData.setValue(false));
        errorLiveData.observeForever(err -> loadingLiveData.setValue(false));
    }

    public void logout() {
        authRepository.logout();
        userLiveData.setValue(null);
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }
}