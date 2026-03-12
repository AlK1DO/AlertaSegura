package com.example.alertasegura.viewmodel;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.repository.AlertRepository;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Map;

public class AlertViewModel extends AndroidViewModel {

    private final AlertRepository alertRepository;

    public final MutableLiveData<String>                   alertIdLiveData    = new MutableLiveData<>();
    public final MutableLiveData<String>                   errorLiveData      = new MutableLiveData<>();
    public final MutableLiveData<List<Map<String, Object>>> communityAlerts   = new MutableLiveData<>();
    public final MutableLiveData<List<GeoPoint>>           heatmapPoints      = new MutableLiveData<>();
    public final MutableLiveData<Boolean>                  loadingLiveData    = new MutableLiveData<>(false);

    public AlertViewModel(@NonNull Application application) {
        super(application);
        alertRepository = new AlertRepository();
    }

    /**
     * Envía la alerta SOS con la ubicación actual.
     * @param senderName nombre del usuario
     * @param senderDni  DNI del usuario
     * @param location   ubicación obtenida por LocationManager / FusedLocationProvider
     * @param contactUids lista de UIDs de contactos seleccionados para notificar
     */
    public void sendSosAlert(String senderName, String senderDni,
                             Location location, List<String> contactUids) {
        if (location == null) {
            errorLiveData.setValue("No se pudo obtener tu ubicación. Activa el GPS.");
            return;
        }
        loadingLiveData.setValue(true);

        alertRepository.sendAlert(
                senderName, senderDni,
                location.getLatitude(), location.getLongitude(),
                contactUids,
                alertIdLiveData, errorLiveData
        );
        loadingLiveData.setValue(false);
    }

    /** Resuelve/cancela la alerta activa. */
    public void resolveAlert(String alertId) {
        alertRepository.resolveAlert(alertId);
        alertIdLiveData.setValue(null);
    }

    /** Inicia la escucha del feed comunitario en tiempo real. */
    public void startListeningCommunityAlerts() {
        alertRepository.listenToActiveAlerts(communityAlerts);
    }

    /** Carga datos del mapa de calor para las últimas N horas. */
    public void loadHeatmap(int hours) {
        long sinceMs = System.currentTimeMillis() - (long) hours * 60 * 60 * 1000;
        alertRepository.getHeatmapData(sinceMs, heatmapPoints);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        alertRepository.removeActiveAlertsListener();
    }
}
