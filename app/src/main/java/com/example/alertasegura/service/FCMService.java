package com.example.alertasegura.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.alertasegura.data.remote.FirestoreDataSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    // Se llama cuando Firebase genera un nuevo token para este dispositivo
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo FCM token: " + token);

        // Si el usuario ya está logueado, actualizar el token en Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            new FirestoreDataSource().updateFcmToken(currentUser.getUid(), token);
        }
    }

    // Se llama cuando llega una notificación con la app en primer plano
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensaje recibido de: " + remoteMessage.getFrom());

        // TODO Fase 2: procesar alertas SOS y mostrar notificación local
    }
}