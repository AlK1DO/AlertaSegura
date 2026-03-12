package com.example.alertasegura.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WhatsAppHelper {

    /**
     * Abre WhatsApp directo al chat de un contacto con el mensaje SOS prellenado.
     * El usuario solo necesita tocar "Enviar".
     *
     * @param context       contexto de la actividad/fragmento
     * @param phone         número normalizado sin "+" (ej: "51987654321")
     * @param senderName    nombre del usuario que envía la alerta
     * @param lat           latitud exacta
     * @param lng           longitud exacta
     */
    public static void openWhatsAppSos(Context context,
                                       String phone,
                                       String senderName,
                                       double lat,
                                       double lng) {
        String message = buildSosMessage(senderName, lat, lng);
        String encoded;
        try {
            encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            encoded = message.replace(" ", "%20");
        }

        // Intentar abrir WhatsApp directo; si no está instalado, abrir wa.me en el browser
        String url = "https://wa.me/" + phone + "?text=" + encoded;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Construye el mensaje SOS con link de Google Maps.
     * Ejemplo:
     *   🚨 ALERTA SOS — Juan Pérez necesita ayuda.
     *   📍 Mi ubicación exacta:
     *   https://maps.google.com/?q=-12.0464,-77.0428
     *   Por favor contáctame o llama al 105 (Policía) / 116 (Bomberos).
     */
    public static String buildSosMessage(String senderName, double lat, double lng) {
        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lng;
        return "🚨 *ALERTA SOS* — *" + senderName + "* necesita ayuda urgente.\n\n"
                + "📍 Mi ubicación exacta:\n"
                + mapsLink + "\n\n"
                + "Por favor contáctame o llama al:\n"
                + "🚔 105 (Policía Nacional)\n"
                + "🚒 116 (Bomberos)\n"
                + "🚑 117 (SAMU)";
    }
}