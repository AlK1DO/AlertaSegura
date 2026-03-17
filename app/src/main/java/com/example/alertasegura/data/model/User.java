package com.example.alertasegura.data.model;

import com.google.firebase.firestore.DocumentId;

public class User {

    @DocumentId
    private String uid;
    private String fullName;
    private String dni;
    private String phone;
    private String email;
    private String fcmToken;
    private long createdAt;
    private boolean isActive;

    // ─── Control de límite de alertas comunitarias ───────────────
    // Las alertas a contactos privados NO tienen límite (son de confianza)
    // Solo el feed público está limitado para evitar spam
    private int alertsToday;        // Cuántas alertas públicas envió hoy (se reinicia a medianoche)
    private int alertsLimit;        // Límite diario (default: 2 para usuarios free)
    private long lastAlertReset;    // Timestamp de cuándo se reinició el contador

    // Constructor vacío requerido por Firestore
    public User() {}

    public User(String uid, String fullName, String dni, String phone, String email) {
        this.uid = uid;
        this.fullName = fullName;
        this.dni = dni;
        this.phone = phone;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;

        // Valores por defecto para usuario nuevo
        this.alertsToday = 0;
        this.alertsLimit = 2;
        this.lastAlertReset = System.currentTimeMillis();
    }

    // ─── Lógica de límite ────────────────────────────────────────

    /**
     * Verifica si el usuario puede enviar una alerta pública hoy.
     * También reinicia el contador si ya pasó la medianoche.
     */
    public boolean canSendPublicAlert() {
        resetIfNewDay();
        return alertsToday < alertsLimit;
    }

    /**
     * Incrementa el contador de alertas del día.
     * Llamar después de confirmar que la alerta fue enviada.
     */
    public void incrementAlertCount() {
        resetIfNewDay();
        this.alertsToday++;
    }

    /**
     * Cuántas alertas le quedan al usuario hoy.
     */
    public int getRemainingAlerts() {
        resetIfNewDay();
        return Math.max(0, alertsLimit - alertsToday);
    }

    /**
     * Reinicia el contador si ya es un nuevo día (medianoche).
     */
    private void resetIfNewDay() {
        long now = System.currentTimeMillis();
        long millisInDay = 24 * 60 * 60 * 1000L;

        if (now - lastAlertReset >= millisInDay) {
            this.alertsToday = 0;
            this.lastAlertReset = now;
        }
    }

    // ─── Getters y Setters existentes ──────────────

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // ─── Getters y Setters nuevos ───────────────────

    public int getAlertsToday() { return alertsToday; }
    public void setAlertsToday(int alertsToday) { this.alertsToday = alertsToday; }

    public int getAlertsLimit() { return alertsLimit; }
    public void setAlertsLimit(int alertsLimit) { this.alertsLimit = alertsLimit; }

    public long getLastAlertReset() { return lastAlertReset; }
    public void setLastAlertReset(long lastAlertReset) { this.lastAlertReset = lastAlertReset; }
}