package com.example.alertasegura.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.Alert;
import com.example.alertasegura.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AlertRepository {

    private final FirebaseFirestore db;
    private final DatabaseReference rtDb;
    private final FirebaseAuth auth;
    private ValueEventListener activeAlertsListener;

    public AlertRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.rtDb = FirebaseDatabase.getInstance().getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Envía una alerta SOS:
     * 1. Guarda en Firestore (colección "alerts") con ubicación exacta y aproximada.
     * 2. Publica en Realtime Database para el feed en tiempo real.
     */
    public void sendAlert(String senderName, String senderDni,
                          double lat, double lng,
                          List<String> notifiedContactUids,
                          MutableLiveData<String> alertIdLiveData,
                          MutableLiveData<String> errorLiveData) {

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        GeoPoint exact  = new GeoPoint(lat, lng);
        GeoPoint approx = offsetLocation(lat, lng, Constants.APPROX_RADIUS_METERS);

        Alert alert = new Alert(uid, senderName, senderDni, exact, approx);
        alert.setNotifiedContacts(notifiedContactUids);

        // 1. Guardar en Firestore
        db.collection(Constants.COLLECTION_ALERTS)
                .add(alert)
                .addOnSuccessListener(docRef -> {
                    String alertId = docRef.getId();
                    alert.setAlertId(alertId);
                    alertIdLiveData.setValue(alertId);

                    // 2. Publicar en Realtime Database (para feed comunitario en tiempo real)
                    Map<String, Object> rtEntry = new HashMap<>();
                    rtEntry.put("alertId",    alertId);
                    rtEntry.put("senderName", senderName);
                    rtEntry.put("senderDni",  senderDni);
                    rtEntry.put("approxLat",  approx.getLatitude());
                    rtEntry.put("approxLng",  approx.getLongitude());
                    rtEntry.put("timestamp",  alert.getTimestamp());
                    rtEntry.put("status",     "active");

                    rtDb.child(Constants.RT_ACTIVE_ALERTS).child(alertId).setValue(rtEntry);

                    // 3. También guardar en heatmapData con ubicación aprox
                    Map<String, Object> heatEntry = new HashMap<>();
                    heatEntry.put("lat",       approx.getLatitude());
                    heatEntry.put("lng",       approx.getLongitude());
                    heatEntry.put("timestamp", alert.getTimestamp());
                    rtDb.child(Constants.RT_HEATMAP_DATA).push().setValue(heatEntry);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /**
     * Resuelve/cancela una alerta activa.
     */
    public void resolveAlert(String alertId) {
        db.collection(Constants.COLLECTION_ALERTS)
                .document(alertId)
                .update("status", Constants.STATUS_RESOLVED);

        rtDb.child(Constants.RT_ACTIVE_ALERTS).child(alertId)
                .child("status").setValue(Constants.STATUS_RESOLVED);
    }

    /**
     * Escucha las alertas activas de la comunidad en tiempo real.
     */
    public void listenToActiveAlerts(MutableLiveData<List<Map<String, Object>>> alertsLiveData) {
        activeAlertsListener = rtDb.child(Constants.RT_ACTIVE_ALERTS)
                .orderByChild("timestamp")
                .limitToLast(50)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entry = (Map<String, Object>) child.getValue();
                            if (entry != null && "active".equals(entry.get("status"))) {
                                list.add(0, entry); // más recientes primero
                            }
                        }
                        alertsLiveData.postValue(list);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { /* ignorar */ }
                });
    }

    /**
     * Obtiene datos del mapa de calor para un período dado.
     */
    public void getHeatmapData(long sinceTimestamp,
                               MutableLiveData<List<GeoPoint>> heatmapLiveData) {
        rtDb.child(Constants.RT_HEATMAP_DATA)
                .orderByChild("timestamp")
                .startAt(sinceTimestamp)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<GeoPoint> points = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double lat = child.child("lat").getValue(Double.class);
                            Double lng = child.child("lng").getValue(Double.class);
                            if (lat != null && lng != null) {
                                points.add(new GeoPoint(lat, lng));
                            }
                        }
                        heatmapLiveData.postValue(points);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { /* ignorar */ }
                });
    }

    public void removeActiveAlertsListener() {
        if (activeAlertsListener != null) {
            rtDb.child(Constants.RT_ACTIVE_ALERTS).removeEventListener(activeAlertsListener);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Desplaza una coordenada por un radio aleatorio de hasta `radiusMeters`
     * para ofuscar la ubicación exacta en el feed comunitario.
     */
    private GeoPoint offsetLocation(double lat, double lng, int radiusMeters) {
        Random rand   = new Random();
        double dLat   = (rand.nextDouble() * 2 - 1) * (radiusMeters / 111320.0);
        double dLng   = (rand.nextDouble() * 2 - 1) * (radiusMeters / (111320.0 * Math.cos(Math.toRadians(lat))));
        return new GeoPoint(lat + dLat, lng + dLng);
    }
}