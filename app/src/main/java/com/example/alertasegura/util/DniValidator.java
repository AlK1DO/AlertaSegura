package com.example.alertasegura.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DniValidator {

    // API pública peruana para consulta de DNI
    // No requiere API key para consultas básicas
    private static final String TOKEN = "cc75f8e0d3f9e54163a020a2a2c4fa6d39afbfd61b5235b2666ec284f6023d0e";
    private static final String API_URL = "https://apiperu.dev/api/dni/";

    private static final OkHttpClient client   = new OkHttpClient();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler    = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onValid(String fullName);   // DNI existe → nombre completo de RENIEC
        void onInvalid(String reason);   // DNI no existe o error
    }

    /**
     * Valida el DNI consultando apiperu.dev en un hilo secundario.
     * El resultado se devuelve en el hilo principal (UI thread).
     */
    public static void validate(String dni, Callback callback) {
        // Validación básica de formato antes de hacer la petición
        if (dni == null || dni.length() != 8 || !dni.matches("\\d{8}")) {
            callback.onInvalid("El DNI debe tener exactamente 8 dígitos numéricos.");
            return;
        }

        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_URL + dni)
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer " + TOKEN)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    postInvalid(callback, "No se pudo verificar el DNI. Intenta de nuevo.");
                    return;
                }

                String body = response.body().string();
                JSONObject json = new JSONObject(body);

                // apiperu.dev devuelve { "success": true/false, "data": { "nombre_completo": "..." } }
                boolean success = json.optBoolean("success", false);

                if (!success) {
                    postInvalid(callback, "DNI no encontrado en RENIEC. Verifica el número.");
                    return;
                }

                JSONObject data     = json.optJSONObject("data");
                String fullName     = data != null ? data.optString("nombre_completo", "") : "";

                postValid(callback, fullName);

            } catch (IOException e) {
                postInvalid(callback, "Sin conexión. Verifica tu internet e intenta de nuevo.");
            } catch (Exception e) {
                postInvalid(callback, "Error al verificar el DNI. Intenta de nuevo.");
            }
        });
    }

    private static void postValid(Callback callback, String fullName) {
        mainHandler.post(() -> callback.onValid(fullName));
    }

    private static void postInvalid(Callback callback, String reason) {
        mainHandler.post(() -> callback.onInvalid(reason));
    }
}