package com.example.alertasegura.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PhoneContactsHelper {

    /**
     * Modelo ligero que representa un contacto del teléfono.
     */
    public static class DeviceContact {
        public final String displayName;
        public final String normalizedPhone; // ej: "51987654321"

        public DeviceContact(String displayName, String normalizedPhone) {
            this.displayName   = displayName;
            this.normalizedPhone = normalizedPhone;
        }
    }

    /**
     * Lee todos los contactos del dispositivo con al menos un número de teléfono.
     * Devuelve un mapa número→DeviceContact (sin duplicados de número).
     *
     * Debe llamarse en un hilo de fondo (no en el hilo principal).
     * El fragmento/ViewModel debe pedir el permiso READ_CONTACTS antes.
     */
    public static Map<String, DeviceContact> readDeviceContacts(Context context) {
        Map<String, DeviceContact> result = new LinkedHashMap<>();
        ContentResolver cr = context.getContentResolver();

        Cursor cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor == null) return result;

        try {
            int nameCol  = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String name  = cursor.getString(nameCol);
                String phone = cursor.getString(phoneCol);
                if (name == null || phone == null) continue;

                String normalized = normalizePhone(phone);
                if (normalized == null) continue;

                // Guardar sin duplicar por número
                if (!result.containsKey(normalized)) {
                    result.put(normalized, new DeviceContact(name.trim(), normalized));
                }
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /**
     * Normaliza un número de teléfono al formato internacional sin el "+".
     * Asume código de país +51 (Perú) si el número tiene 9 dígitos y empieza con 9.
     * Elimina espacios, guiones, paréntesis.
     *
     * Ejemplos:
     *   "987 654 321"   → "51987654321"
     *   "+51 987654321" → "51987654321"
     *   "987654321"     → "51987654321"
     *   "+1 650 555 1234"→ "16505551234"
     *
     * @return número normalizado, o null si no es un número válido.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;

        // Quitar todo lo que no sea dígito ni "+"
        String digits = raw.replaceAll("[^\\d+]", "");

        if (digits.startsWith("+")) {
            // Ya tiene código de país → quitar el "+"
            digits = digits.substring(1);
        } else if (digits.length() == 9 && digits.startsWith("9")) {
            // Número peruano local de 9 dígitos → agregar 51
            digits = "51" + digits;
        } else if (digits.startsWith("0")) {
            // Algunos países usan 0 como prefijo local → quitar el 0 y agregar 51
            digits = "51" + digits.substring(1);
        }

        // Validar longitud mínima (7) y máxima (15) según E.164
        if (digits.length() < 7 || digits.length() > 15) return null;

        // Rechazar números con todos los mismos dígitos (ej: 000000000)
        if (Pattern.matches("(\\d)\\1{6,}", digits)) return null;

        return digits;
    }

    /**
     * Extrae solo los números normalizados de la lista de contactos del dispositivo.
     * Útil para hacer la query en Firestore.
     */
    public static List<String> extractPhones(Map<String, DeviceContact> contacts) {
        return new ArrayList<>(contacts.keySet());
    }
}