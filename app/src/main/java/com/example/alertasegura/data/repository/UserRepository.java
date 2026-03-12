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

    // Registro completo: crea Auth y luego el perfil en Firestore
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

                    // Creamos el objeto User con la información recibida
                    User newUser = new User(
                            firebaseUser.getUid(),
                            fullName, dni, phone, email
                    );

                    // Callback directo a Firestore tras el éxito en Auth <--- Pruena
                    firestoreDataSource.createUserProfile(
                            newUser,
                            unused -> {
                                android.util.Log.d("DEBUG", "Firestore OK → Perfil creado");
                                userLiveData.setValue(newUser);
                            },
                            e -> {
                                android.util.Log.e("DEBUG", "Firestore ERROR: " + e.getMessage());
                                errorLiveData.setValue(e.getMessage());
                            }
                    ); //hasta aca
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Inicio de sesión simple usando Firebase Auth
    public void login(String email, String password,
                      MutableLiveData<FirebaseUser> firebaseUserLiveData,
                      MutableLiveData<String> errorLiveData) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> firebaseUserLiveData.setValue(authResult.getUser()))
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Carga los datos adicionales del usuario desde la base de datos
    public void loadCurrentUserProfile(MutableLiveData<User> userLiveData,
                                       MutableLiveData<String> errorLiveData) {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current == null) {
            errorLiveData.setValue("No hay sesión activa");
            return;
        }
        firestoreDataSource.getUserProfile(current.getUid(), userLiveData, errorLiveData);
    }

    // Retorna el usuario de Auth actual
    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Cierra la sesión en Firebase
    public void logout() {
        firebaseAuth.signOut();
    }
}