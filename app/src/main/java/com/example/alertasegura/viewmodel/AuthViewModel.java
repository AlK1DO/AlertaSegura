// com/example/alertasegura/viewmodel/AuthViewModel.java
package com.example.alertasegura.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alertasegura.data.model.User;
import com.example.alertasegura.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {

    private final UserRepository userRepository;

    public final MutableLiveData<FirebaseUser> firebaseUserLiveData  = new MutableLiveData<>();
    public final MutableLiveData<User>         registeredUserLiveData = new MutableLiveData<>();
    public final MutableLiveData<String>       errorLiveData          = new MutableLiveData<>();
    public final MutableLiveData<Boolean>      loadingLiveData        = new MutableLiveData<>(false);

    public AuthViewModel() {
        userRepository = new UserRepository();
    }

    // Registro completo: Firebase Auth + guardar perfil en Firestore
    public void register(String email, String password,
                         String fullName, String dni, String phone) {

        loadingLiveData.setValue(true);

        userRepository.registerWithProfile(
                email, password, fullName, dni, phone,
                registeredUserLiveData,   // se actualiza cuando Firestore confirma
                errorLiveData
        );

        // El loading se apaga desde el Fragment observando registeredUserLiveData/errorLiveData
        // NO usamos observeForever aquí para evitar fugas y acumulación de observers
    }

    // Login con email y contraseña
    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        userRepository.login(email, password, firebaseUserLiveData, errorLiveData);
    }

    public void logout() {
        userRepository.logout();
        firebaseUserLiveData.setValue(null);
        registeredUserLiveData.setValue(null);
    }

    public FirebaseUser getCurrentUser() {
        return userRepository.getCurrentFirebaseUser();
    }
}