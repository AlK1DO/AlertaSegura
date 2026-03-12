// ============================================================
// data/remote/FirestoreDataSource.java
// ============================================================
package com.example.alertasegura.data.remote;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreDataSource {

    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore db;

    public FirestoreDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Crear perfil de usuario en Firestore después del registro
    public void createUserProfile(User user,
                                  MutableLiveData<Boolean> successLiveData,
                                  MutableLiveData<String> errorLiveData) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused -> successLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Obtener perfil de usuario por UID
    public void getUserProfile(String uid,
                               MutableLiveData<User> userLiveData,
                               MutableLiveData<String> errorLiveData) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        userLiveData.setValue(snapshot.toObject(User.class));
                    } else {
                        errorLiveData.setValue("Perfil no encontrado");
                    }
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Actualizar FCM token cuando el usuario inicia sesión
    public void updateFcmToken(String uid, String token) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener(e -> {
                    // Log silencioso, no crítico
                });
    }
}