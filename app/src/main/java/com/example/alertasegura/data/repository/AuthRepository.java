// ============================================================
// app/src/main/java/com/alertasegura/data/repository/AuthRepository.java
// ============================================================
package com.example.alertasegura.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    // Devuelve el usuario actual (null si no hay sesión)
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Registro con email y contraseña
    public void register(String email, String password, MutableLiveData<FirebaseUser> userLiveData, MutableLiveData<String> errorLiveData) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    userLiveData.setValue(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue(e.getMessage());
                });
    }

    // Login con email y contraseña
    public void login(String email, String password, MutableLiveData<FirebaseUser> userLiveData, MutableLiveData<String> errorLiveData) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    userLiveData.setValue(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue(e.getMessage());
                });
    }

    // Cerrar sesión
    public void logout() {
        firebaseAuth.signOut();
    }
}