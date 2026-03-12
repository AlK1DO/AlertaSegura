package com.example.alertasegura.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class LocationService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Fase 2: iniciar FusedLocationProviderClient y enviar ubicación
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
