package com.example.alertasegura.util;

public class Constants {

    // ─── Firestore collections ────────────────────────────────────────────────
    public static final String COLLECTION_USERS    = "users";
    public static final String COLLECTION_CONTACTS = "contacts";
    public static final String COLLECTION_ALERTS   = "alerts";

    // ─── Realtime Database paths ──────────────────────────────────────────────
    public static final String RT_ACTIVE_ALERTS = "activeAlerts";
    public static final String RT_HEATMAP_DATA  = "heatmapData";

    // ─── Alert status ─────────────────────────────────────────────────────────
    public static final String STATUS_ACTIVE   = "active";
    public static final String STATUS_RESOLVED = "resolved";
    public static final String STATUS_EXPIRED  = "expired";

    // ─── Contact status ───────────────────────────────────────────────────────
    public static final String CONTACT_PENDING  = "pending";
    public static final String CONTACT_ACCEPTED = "accepted";
    public static final String CONTACT_REJECTED = "rejected";

    // ─── Location ─────────────────────────────────────────────────────────────
    public static final int APPROX_RADIUS_METERS = 500; // Radio de ofuscación para comunidad
    public static final int ALERT_EXPIRY_MINUTES = 30;  // Minutos hasta expirar alerta

    // ─── Límite diario de alertas comunitarias ────────────────────────────────
    // Las alertas a contactos privados (WhatsApp) no tienen límite.
    // Solo el feed público está limitado para evitar spam.
    public static final int ALERT_LIMIT_FREE    = 2;  // Usuario gratuito: 2 alertas/día
    public static final int ALERT_LIMIT_PREMIUM = 10; // Usuario Premium: 10 alertas/día

    // Error code que devuelve el repositorio cuando se alcanza el límite
    public static final String ERROR_LIMIT_REACHED = "LIMIT_REACHED";

    // ─── Plan Premium ─────────────────────────────────────────────────────────
    public static final String PLAN_FREE    = "free";
    public static final String PLAN_PREMIUM = "premium";

    // Beneficios Premium (para mostrar en la UI de upgrade)
    public static final int    PREMIUM_ALERT_LIMIT        = 10;   // Alertas comunitarias por día
    public static final int    PREMIUM_CONTACTS_LIMIT     = 10;   // Máx contactos de confianza
    public static final int    FREE_CONTACTS_LIMIT        = 3;    // Máx contactos plan free
    public static final int    PREMIUM_HISTORY_DAYS       = 90;   // Días de historial de alertas
    public static final int    FREE_HISTORY_DAYS          = 7;    // Días de historial plan free
    public static final int    PREMIUM_HEATMAP_RADIUS_KM  = 50;   // Radio del mapa de calor
    public static final int    FREE_HEATMAP_RADIUS_KM     = 5;    // Radio plan free

    // ─── Shared Preferences keys ──────────────────────────────────────────────
    public static final String PREFS_NAME     = "alerta_segura_prefs";
    public static final String PREF_FCM_TOKEN = "fcm_token";
    public static final String PREF_USER_PLAN = "user_plan"; // "free" | "premium"
}