// ============================================================
// data/repository/UserRepository.java
//
// Coordina Firebase Auth + Firestore para el perfil completo.
// ============================================================
package com.example.alertasegura.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.User;
import com.example.alertasegura.data.remote.FirestoreDataSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirestoreDataSource firestoreDataSource;

    public UserRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestoreDataSource = new FirestoreDataSource();
    }

    // Registro completo: crea Auth + perfil en Firestore
    public void registerWithProfile(String email, String password,
                                    String fullName, String dni, String phone,
                                    MutableLiveData<User> userLiveData,
                                    MutableLiveData<String> errorLiveData) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        errorLiveData.setValue("Error al crear usuario");
                        return;
                    }

                    // Crear el modelo User con los datos del formulario
                    User newUser = new User(
                            firebaseUser.getUid(),
                            fullName,
                            dni,
                            phone,
                            email
                    );

                    // Guardar en Firestore
                    MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();
                    firestoreDataSource.createUserProfile(newUser, successLiveData, errorLiveData);

                    successLiveData.observeForever(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            userLiveData.setValue(newUser);
                        }
                    });
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Login simple (solo Auth, el perfil se carga después si hace falta)
    public void login(String email, String password,
                      MutableLiveData<FirebaseUser> firebaseUserLiveData,
                      MutableLiveData<String> errorLiveData) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> firebaseUserLiveData.setValue(authResult.getUser()))
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Cargar perfil del usuario actual desde Firestore
    public void loadCurrentUserProfile(MutableLiveData<User> userLiveData,
                                       MutableLiveData<String> errorLiveData) {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current == null) {
            errorLiveData.setValue("No hay sesión activa");
            return;
        }
        firestoreDataSource.getUserProfile(current.getUid(), userLiveData, errorLiveData);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void logout() {
        firebaseAuth.signOut();
    }
}