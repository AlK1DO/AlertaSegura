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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AlertRepository {

    private final FirebaseFirestore db;
    private final DatabaseReference rtDb;
    private final FirebaseAuth      auth;
    private ValueEventListener      activeAlertsListener;

    public AlertRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.rtDb = FirebaseDatabase.getInstance().getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    // ─── Enviar alerta SOS ────────────────────────────────────────────────────

    /**
     * Envía una alerta SOS con validación del límite diario en el servidor.
     *
     * Flujo:
     *  1. Lee el documento del usuario en Firestore para verificar alertsToday
     *  2. Si ya alcanzó el límite → error, no guarda nada
     *  3. Si puede → guarda la alerta, incrementa el contador, publica en Realtime DB
     *
     * La validación en el servidor es la fuente de verdad.
     * El chequeo en HomeFragment es solo para evitar el viaje de red innecesario.
     */
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

        // ── Paso 1: Verificar límite diario en Firestore (fuente de verdad) ──
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(userSnap -> {

                    if (!userSnap.exists()) {
                        errorLiveData.setValue("No se encontró el perfil de usuario.");
                        return;
                    }

                    // Leer campos del límite
                    long alertsToday    = userSnap.getLong("alertsToday")    != null ? userSnap.getLong("alertsToday")    : 0;
                    long alertsLimit    = userSnap.getLong("alertsLimit")    != null ? userSnap.getLong("alertsLimit")    : 2;
                    long lastAlertReset = userSnap.getLong("lastAlertReset") != null ? userSnap.getLong("lastAlertReset") : 0;

                    // Reiniciar contador si ya pasó la medianoche
                    long now         = System.currentTimeMillis();
                    long millisInDay = 24 * 60 * 60 * 1000L;
                    if (now - lastAlertReset >= millisInDay) {
                        alertsToday    = 0;
                        lastAlertReset = now;
                    }

                    // ── Paso 2: Bloquear si ya alcanzó el límite ─────────────
                    if (alertsToday >= alertsLimit) {
                        errorLiveData.setValue("LIMIT_REACHED"); // HomeFragment muestra el diálogo
                        return;
                    }

                    // ── Paso 3: Todo OK → guardar la alerta ──────────────────
                    saveAlert(uid, senderName, senderDni, lat, lng,
                            notifiedContactUids, alertsToday, alertsLimit,
                            lastAlertReset, alertIdLiveData, errorLiveData);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /**
     * Guarda la alerta en Firestore y Realtime DB,
     * e incrementa el contador del usuario en la misma operación.
     */
    private void saveAlert(String uid, String senderName, String senderDni,
                           double lat, double lng,
                           List<String> notifiedContactUids,
                           long alertsToday, long alertsLimit, long lastAlertReset,
                           MutableLiveData<String> alertIdLiveData,
                           MutableLiveData<String> errorLiveData) {

        GeoPoint exact  = new GeoPoint(lat, lng);
        GeoPoint approx = offsetLocation(lat, lng, Constants.APPROX_RADIUS_METERS);

        // Nombre corto para el feed público → "Juan Pérez"
        // Nombre completo queda en Firestore → solo visible para admin
        String shortName = getShortName(senderName);

        // DNI enmascarado para el feed público → "7XXXX321"
        String maskedDni = maskDni(senderDni);

        Alert alert = new Alert(uid, senderName, senderDni, exact, approx);
        alert.setNotifiedContacts(notifiedContactUids);

        // 1. Guardar alerta en Firestore (con nombre y DNI completos para trazabilidad)
        db.collection(Constants.COLLECTION_ALERTS)
                .add(alert)
                .addOnSuccessListener(docRef -> {
                    String alertId = docRef.getId();
                    alert.setAlertId(alertId);
                    alertIdLiveData.setValue(alertId);

                    // 2. Incrementar contador del usuario en Firestore
                    Map<String, Object> counterUpdate = new HashMap<>();
                    counterUpdate.put("alertsToday",    alertsToday + 1);
                    counterUpdate.put("alertsLimit",    alertsLimit);
                    counterUpdate.put("lastAlertReset", lastAlertReset);

                    db.collection(Constants.COLLECTION_USERS)
                            .document(uid)
                            .update(counterUpdate);

                    // 3. Publicar en Realtime Database con nombre corto y DNI enmascarado
                    //    El feed público NUNCA recibe nombre completo ni DNI real
                    Map<String, Object> rtEntry = new HashMap<>();
                    rtEntry.put("alertId",    alertId);
                    rtEntry.put("senderName", shortName);  // "Juan Pérez" no "PÉREZ QUISPE JUAN CARLOS"
                    rtEntry.put("senderDni",  maskedDni);  // "7XXXX321" no "74561234"
                    rtEntry.put("approxLat",  approx.getLatitude());
                    rtEntry.put("approxLng",  approx.getLongitude());
                    rtEntry.put("timestamp",  alert.getTimestamp());
                    rtEntry.put("status",     "active");

                    rtDb.child(Constants.RT_ACTIVE_ALERTS).child(alertId).setValue(rtEntry);

                    // 4. Guardar en heatmapData con ubicación aproximada
                    Map<String, Object> heatEntry = new HashMap<>();
                    heatEntry.put("lat",       approx.getLatitude());
                    heatEntry.put("lng",       approx.getLongitude());
                    heatEntry.put("timestamp", alert.getTimestamp());
                    rtDb.child(Constants.RT_HEATMAP_DATA).push().setValue(heatEntry);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    // ─── Resolver alerta ──────────────────────────────────────────────────────

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

    // ─── Feed comunitario ─────────────────────────────────────────────────────

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

    // ─── Mapa de calor ────────────────────────────────────────────────────────

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
                            Double latVal = child.child("lat").getValue(Double.class);
                            Double lngVal = child.child("lng").getValue(Double.class);
                            if (latVal != null && lngVal != null) {
                                points.add(new GeoPoint(latVal, lngVal));
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
     * RENIEC devuelve: APELLIDO1 APELLIDO2 NOMBRE1 NOMBRE2
     * Este método extrae solo NOMBRE1 + APELLIDO1 para el feed público.
     *
     * Ejemplos:
     *   "PÉREZ QUISPE JUAN CARLOS" → "Juan Pérez"
     *   "GARCIA LOPEZ MARIA"       → "Maria Garcia"
     *   "FLORES ANA"               → "Ana Flores"
     */
    private String getShortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "Usuario";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return capitalize(parts[0]);
        if (parts.length == 2) return capitalize(parts[1]) + " " + capitalize(parts[0]);
        return capitalize(parts[2]) + " " + capitalize(parts[0]);
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return "";
        return word.charAt(0) + word.substring(1).toLowerCase();
    }

    /**
     * Enmascara el DNI para el feed público.
     * "74561234" → "7XXXX234"
     */
    private String maskDni(String dni) {
        if (dni == null || dni.length() != 8) return "—";
        return dni.charAt(0) + "XXXX" + dni.substring(5);
    }

    /**
     * Desplaza una coordenada por un radio aleatorio de hasta `radiusMeters`
     * para ofuscar la ubicación exacta en el feed comunitario.
     */
    private GeoPoint offsetLocation(double lat, double lng, int radiusMeters) {
        Random rand = new Random();
        double dLat = (rand.nextDouble() * 2 - 1) * (radiusMeters / 111320.0);
        double dLng = (rand.nextDouble() * 2 - 1) * (radiusMeters / (111320.0 * Math.cos(Math.toRadians(lat))));
        return new GeoPoint(lat + dLat, lng + dLng);
    }
}