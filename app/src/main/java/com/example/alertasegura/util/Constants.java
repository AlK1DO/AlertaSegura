package com.example.alertasegura.util;

public class Constants {

    // Firestore collections
    public static final String COLLECTION_USERS    = "users";
    public static final String COLLECTION_CONTACTS = "contacts";
    public static final String COLLECTION_ALERTS   = "alerts";

    // Realtime Database paths
    public static final String RT_ACTIVE_ALERTS  = "activeAlerts";
    public static final String RT_HEATMAP_DATA   = "heatmapData";

    // Alert status
    public static final String STATUS_ACTIVE   = "active";
    public static final String STATUS_RESOLVED = "resolved";
    public static final String STATUS_EXPIRED  = "expired";

    // Contact status
    public static final String CONTACT_PENDING  = "pending";
    public static final String CONTACT_ACCEPTED = "accepted";
    public static final String CONTACT_REJECTED = "rejected";

    // Location
    public static final int APPROX_RADIUS_METERS = 500;  // Radio de ofuscación para comunidad
    public static final int ALERT_EXPIRY_MINUTES = 30;   // Minutos hasta expirar alerta

    // Shared Preferences keys
    public static final String PREFS_NAME      = "alerta_segura_prefs";
    public static final String PREF_FCM_TOKEN  = "fcm_token";
}