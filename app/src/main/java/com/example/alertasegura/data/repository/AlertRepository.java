package com.example.alertasegura.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.Alert;
import com.example.alertasegura.data.model.HeatmapCluster;
import com.example.alertasegura.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

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
        this.db = FirebaseFirestore.getInstance();
        this.rtDb = FirebaseDatabase.getInstance().getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    // ─── ENVIAR ALERTA SOS ────────────────────────────────────────────────────

    public void sendAlert(String senderName, String senderDni,
                          double lat, double lng,
                          List<String> notifiedContactUids,
                          MutableLiveData<String> alertIdLiveData,
                          MutableLiveData<String> errorLiveData) {

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (uid.isEmpty()) {
            errorLiveData.setValue("No hay sesión activa.");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (!userSnap.exists()) {
                        errorLiveData.setValue("No se encontró el perfil de usuario.");
                        return;
                    }

                    long alertsToday = userSnap.getLong("alertsToday") != null ? userSnap.getLong("alertsToday") : 0;
                    long alertsLimit = userSnap.getLong("alertsLimit") != null ? userSnap.getLong("alertsLimit") : 2;
                    long lastAlertReset = userSnap.getLong("lastAlertReset") != null ? userSnap.getLong("lastAlertReset") : 0;

                    long now = System.currentTimeMillis();
                    long millisInDay = 24 * 60 * 60 * 1000L;
                    if (now - lastAlertReset >= millisInDay) {
                        alertsToday = 0;
                        lastAlertReset = now;
                    }

                    if (alertsToday >= alertsLimit) {
                        errorLiveData.setValue("LIMIT_REACHED");
                        return;
                    }

                    saveAlert(uid, senderName, senderDni, lat, lng,
                            notifiedContactUids, alertsToday, alertsLimit,
                            lastAlertReset, alertIdLiveData, errorLiveData);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    private void saveAlert(String uid, String senderName, String senderDni,
                           double lat, double lng,
                           List<String> notifiedContactUids,
                           long alertsToday, long alertsLimit, long lastAlertReset,
                           MutableLiveData<String> alertIdLiveData,
                           MutableLiveData<String> errorLiveData) {

        GeoPoint exact = new GeoPoint(lat, lng);
        GeoPoint approx = offsetLocation(lat, lng, Constants.APPROX_RADIUS_METERS);
        String shortName = getShortName(senderName);
        String maskedDni = maskDni(senderDni);

        Alert alert = new Alert(uid, senderName, senderDni, exact, approx);
        alert.setNotifiedContacts(notifiedContactUids);

        db.collection(Constants.COLLECTION_ALERTS)
                .add(alert)
                .addOnSuccessListener(docRef -> {
                    String alertId = docRef.getId();
                    alert.setAlertId(alertId);
                    alertIdLiveData.setValue(alertId);

                    Map<String, Object> counterUpdate = new HashMap<>();
                    counterUpdate.put("alertsToday", alertsToday + 1);
                    counterUpdate.put("alertsLimit", alertsLimit);
                    counterUpdate.put("lastAlertReset", lastAlertReset);

                    db.collection(Constants.COLLECTION_USERS).document(uid).update(counterUpdate);

                    Map<String, Object> rtEntry = new HashMap<>();
                    rtEntry.put("alertId", alertId);
                    rtEntry.put("senderName", shortName);
                    rtEntry.put("senderDni", maskedDni);
                    rtEntry.put("approxLat", approx.getLatitude());
                    rtEntry.put("approxLng", approx.getLongitude());
                    rtEntry.put("timestamp", alert.getTimestamp());
                    rtEntry.put("status", "active");

                    rtDb.child(Constants.RT_ACTIVE_ALERTS).child(alertId).setValue(rtEntry);

                    Map<String, Object> heatEntry = new HashMap<>();
                    heatEntry.put("lat", approx.getLatitude());
                    heatEntry.put("lng", approx.getLongitude());
                    heatEntry.put("timestamp", alert.getTimestamp());
                    rtDb.child(Constants.RT_HEATMAP_DATA).push().setValue(heatEntry);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // ─── RESOLVER ALERTA ──────────────────────────────────────────────────────

    public void resolveAlert(String alertId) {
        db.collection(Constants.COLLECTION_ALERTS)
                .document(alertId)
                .update("status", Constants.STATUS_RESOLVED);

        rtDb.child(Constants.RT_ACTIVE_ALERTS).child(alertId)
                .child("status").setValue(Constants.STATUS_RESOLVED);
    }

    // ─── FEED COMUNITARIO ─────────────────────────────────────────────────────

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
                                list.add(0, entry);
                            }
                        }
                        alertsLiveData.postValue(list);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    public void removeActiveAlertsListener() {
        if (activeAlertsListener != null) {
            rtDb.child(Constants.RT_ACTIVE_ALERTS).removeEventListener(activeAlertsListener);
        }
    }

    // ─── MAPA DE CALOR Y CLUSTERS ─────────────────────────────────────────────

    public void getHeatmapData(long sinceTimestamp, MutableLiveData<List<GeoPoint>> heatmapLiveData) {
        rtDb.child(Constants.RT_HEATMAP_DATA)
                .orderByChild("timestamp")
                .startAt(sinceTimestamp)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<GeoPoint> points = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double latVal = child.child("lat").getValue(Double.class);
                            Double lngVal = child.child("lng").getValue(Double.class);
                            if (latVal != null && lngVal != null) {
                                points.add(new GeoPoint(latVal, lngVal));
                            }
                        }
                        heatmapLiveData.postValue(points);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    public void getHeatmapClusters(long sinceTimestamp, MutableLiveData<List<HeatmapCluster>> clustersLiveData) {
        rtDb.child(Constants.RT_HEATMAP_DATA)
                .orderByChild("timestamp")
                .startAt(sinceTimestamp)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Map<String, List<double[]>> grid = new HashMap<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double latVal = child.child("lat").getValue(Double.class);
                            Double lngVal = child.child("lng").getValue(Double.class);
                            if (latVal == null || lngVal == null) continue;

                            double cellLat = Math.floor(latVal / 0.005) * 0.005;
                            double cellLng = Math.floor(lngVal / 0.005) * 0.005;
                            String key = cellLat + ":" + cellLng;

                            if (!grid.containsKey(key)) grid.put(key, new ArrayList<>());
                            grid.get(key).add(new double[]{latVal, lngVal});
                        }

                        List<HeatmapCluster> clusters = new ArrayList<>();
                        for (List<double[]> points : grid.values()) {
                            double sumLat = 0, sumLng = 0;
                            for (double[] p : points) { sumLat += p[0]; sumLng += p[1]; }
                            clusters.add(new HeatmapCluster(sumLat / points.size(), sumLng / points.size(), points.size()));
                        }
                        clustersLiveData.postValue(clusters);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private String getShortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "Usuario";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return capitalize(parts[0]);
        // Si hay 2 partes (Apellido Nombre), o más de 3 (AP1 AP2 N1 N2...)
        int firstNameIndex = (parts.length >= 3) ? 2 : 1;
        return capitalize(parts[firstNameIndex]) + " " + capitalize(parts[0]);
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return "";
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    private String maskDni(String dni) {
        if (dni == null || dni.length() != 8) return "—";
        return dni.charAt(0) + "XXXX" + dni.substring(5);
    }

    private GeoPoint offsetLocation(double lat, double lng, int radiusMeters) {
        Random rand = new Random();
        double dLat = (rand.nextDouble() * 2 - 1) * (radiusMeters / 111320.0);
        double dLng = (rand.nextDouble() * 2 - 1) * (radiusMeters / (111320.0 * Math.cos(Math.toRadians(lat))));
        return new GeoPoint(lat + dLat, lng + dLng);
    }
}