// ============================================================
// data/remote/FirestoreDataSource.java
// ============================================================
package com.example.alertasegura.data.remote;

import com.example.alertasegura.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.lifecycle.MutableLiveData;

public class FirestoreDataSource {

    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore db;

    public FirestoreDataSource() {
        // Inicializa la instancia de Firebase Firestore
        this.db = FirebaseFirestore.getInstance();
    }

    // Crea el perfil del usuario en la colección "users" usando su UID
    public void createUserProfile(User user,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Obtiene los datos del usuario y los envía a los LiveData correspondientes
    public void getUserProfile(String uid,
                               MutableLiveData<User> userLiveData,
                               MutableLiveData<String> errorLiveData) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Convierte el documento de Firestore a la clase User
                        userLiveData.setValue(snapshot.toObject(User.class));
                    } else {
                        errorLiveData.setValue("Perfil no encontrado");
                    }
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // Actualiza solo el token de notificaciones (FCM) para el usuario
    public void updateFcmToken(String uid, String token) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener(e -> {
                    // Error silencioso: no interrumpe el flujo principal
                });
    }
}