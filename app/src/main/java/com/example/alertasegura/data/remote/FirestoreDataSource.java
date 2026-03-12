// ============================================================
// data/remote/FirestoreDataSource.java
// ============================================================
package com.example.alertasegura.data.remote;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreDataSource {

    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore db;

    public FirestoreDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Versión que usa Listeners para mayor control desde el Repository.
     */
    public void createUserProfile(User user,
                                  OnSuccessListener<Void> successListener,
                                  OnFailureListener failureListener) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Versión antigua con LiveData (mantener por compatibilidad si se usa en otros lados).
     */
    public void createUserProfile(User user,
                                  MutableLiveData<Boolean> successLiveData,
                                  MutableLiveData<String> errorLiveData) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused -> {
                    if (successLiveData != null) successLiveData.setValue(true);
                })
                .addOnFailureListener(e -> {
                    if (errorLiveData != null) errorLiveData.setValue(e.getMessage());
                });
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