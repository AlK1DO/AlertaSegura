package com.example.alertasegura.ui.home;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.alertasegura.R;
import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.data.model.User;
import com.example.alertasegura.databinding.FragmentHomeBinding;
import com.example.alertasegura.util.WhatsAppHelper;
import com.example.alertasegura.viewmodel.AlertViewModel;
import com.example.alertasegura.viewmodel.ContactViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding         binding;
    private AlertViewModel              alertViewModel;
    private ContactViewModel            contactViewModel;
    private FusedLocationProviderClient fusedLocation;

    private String        activeAlertId   = null;
    private String        senderName      = "";
    private String        senderDni       = "";
    private List<Contact> trustedContacts = new ArrayList<>();

    // ─── Estado del límite diario ─────────────────────────────────────────────
    private int  alertsToday     = 0;
    private int  alertsLimit     = 2;   // default para usuario free
    private long lastAlertReset  = 0;

    private ObjectAnimator ringPulse;

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) getLocationAndSend();
                else Snackbar.make(binding.getRoot(),
                        "Necesitamos tu ubicación para enviar la alerta",
                        Snackbar.LENGTH_LONG).show();
            });

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocation    = LocationServices.getFusedLocationProviderClient(requireActivity());
        alertViewModel   = new ViewModelProvider(requireActivity()).get(AlertViewModel.class);
        contactViewModel = new ViewModelProvider(requireActivity()).get(ContactViewModel.class);

        loadUserInfo();
        setupObservers();
        setupSosButton();
        setupRingAnimation();
        contactViewModel.loadContacts();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void loadUserInfo() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;
                    senderName = snap.getString("fullName") != null ? snap.getString("fullName") : "";
                    senderDni  = snap.getString("dni")      != null ? snap.getString("dni")      : "";
                    binding.tvUserName.setText(senderName.isEmpty() ? user.getEmail() : senderName);

                    // ── Cargar estado del límite diario desde Firestore ──────
                    Long storedAlertsToday    = snap.getLong("alertsToday");
                    Long storedAlertsLimit    = snap.getLong("alertsLimit");
                    Long storedLastReset      = snap.getLong("lastAlertReset");

                    alertsToday    = storedAlertsToday  != null ? storedAlertsToday.intValue()  : 0;
                    alertsLimit    = storedAlertsLimit  != null ? storedAlertsLimit.intValue()   : 2;
                    lastAlertReset = storedLastReset    != null ? storedLastReset                : System.currentTimeMillis();

                    // Si ya pasó la medianoche, reiniciar contador localmente
                    checkAndResetDailyCounter();

                    updateAlertCounterUI();
                });
    }

    private void setupObservers() {
        alertViewModel.alertIdLiveData.observe(getViewLifecycleOwner(), alertId -> {
            if (alertId != null) {
                activeAlertId = alertId;
                setAlertActive(true);
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                binding.tvLastAlertStatus.setText("⚠️ Alerta activa desde las " + time);
            }
        });


        alertViewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) return;

            if ("LIMIT_REACHED".equals(error)) {
                // Si el repositorio manda este código, abrimos tu diálogo
                showLimitReachedDialog();
            } else {
                // Para cualquier otro error (ej: "Error de red"), mostramos el Snackbar
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
            // Limpiamos para evitar que se repita al rotar la pantalla
            alertViewModel.errorLiveData.setValue(null);
        });

        contactViewModel.contactsLiveData.observe(getViewLifecycleOwner(), contacts -> {
            trustedContacts = contacts != null ? contacts : new ArrayList<>();
            updateContactsHint();
        });
    }

    private void setupSosButton() {
        binding.btnSOS.setOnLongClickListener(v -> {
            if (activeAlertId != null) confirmCancel();
            else checkPermissionAndSend();
            return true;
        });
    }

    private void setupRingAnimation() {
        ringPulse = ObjectAnimator.ofFloat(binding.viewSosRing, View.ALPHA, 0.3f, 1f);
        ringPulse.setDuration(800);
        ringPulse.setRepeatCount(ValueAnimator.INFINITE);
        ringPulse.setRepeatMode(ValueAnimator.REVERSE);
        ringPulse.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    // ─── Flujo SOS ────────────────────────────────────────────────────────────

    private void checkPermissionAndSend() {
        // ── 1. Verificar límite diario ANTES de cualquier otra cosa ──────────
        checkAndResetDailyCounter();

        if (alertsToday >= alertsLimit) {
            showLimitReachedDialog();
            return;
        }

        // ── 2. Verificar contactos ───────────────────────────────────────────
        if (trustedContacts.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin contactos de confianza")
                    .setMessage("Primero agrega al menos un contacto de confianza en la pestaña Contactos. Ellos recibirán tu ubicación cuando actives SOS.")
                    .setPositiveButton("Ir a Contactos", (d, w) ->
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.contactsFragment))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        // ── 3. Verificar permiso de ubicación ────────────────────────────────
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getLocationAndSend();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocationAndSend() {
        Snackbar.make(binding.getRoot(), "Obteniendo ubicación...", Snackbar.LENGTH_SHORT).show();

        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Snackbar.make(binding.getRoot(),
                        "No se pudo obtener la ubicación. Activa el GPS e inténtalo de nuevo.",
                        Snackbar.LENGTH_LONG).show();
                return;
            }
            sendAlert(location);
        });
    }

    private void sendAlert(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // 1. Guardar en Firestore → dispara Cloud Function → FCM a contactos con la app
        List<String> uids = new ArrayList<>();
        for (Contact c : trustedContacts) uids.add(c.getContactUid());
        alertViewModel.sendSosAlert(senderName, senderDni, location, uids);

        // 2. Incrementar contador local y actualizar en Firestore
        alertsToday++;
        updateAlertCountInFirestore();
        updateAlertCounterUI();

        // 3. Abrir WhatsApp para cada contacto de confianza, uno a uno
        //    (WhatsApp NO consume el límite, solo el feed comunitario)
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < trustedContacts.size(); i++) {
            Contact contact = trustedContacts.get(i);
            long delay = i * 2000L;
            handler.postDelayed(() ->
                            WhatsAppHelper.openWhatsAppSos(
                                    requireContext(),
                                    contact.getPhone(),
                                    senderName,
                                    lat,
                                    lng),
                    delay);
        }
    }

    // ─── Límite diario ────────────────────────────────────────────────────────

    /**
     * Reinicia el contador si ya pasó la medianoche desde el último reset.
     */
    private void checkAndResetDailyCounter() {
        long now          = System.currentTimeMillis();
        long millisInDay  = 24 * 60 * 60 * 1000L;

        if (now - lastAlertReset >= millisInDay) {
            alertsToday    = 0;
            lastAlertReset = now;
            updateAlertCountInFirestore(); // sincronizar el reset con Firestore
        }
    }

    /**
     * Persiste el contador actualizado en Firestore.
     */
    private void updateAlertCountInFirestore() {
        var firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("alertsToday",    alertsToday);
        updates.put("alertsLimit",    alertsLimit);
        updates.put("lastAlertReset", lastAlertReset);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .update(updates);
        // No se necesita callback: si falla, la Cloud Function lo validará igual
    }

    /**
     * Muestra el diálogo cuando el usuario ya agotó sus alertas del día.
     */
    private void showLimitReachedDialog() {
        int remaining = Math.max(0, alertsLimit - alertsToday);

        new AlertDialog.Builder(requireContext())
                .setTitle("Límite diario alcanzado")
                .setMessage(
                        "Has usado tus " + alertsLimit + " alertas comunitarias de hoy.\n\n" +
                                "⚠️ Tu contador se reinicia a medianoche.\n\n" +
                                "💡 Recuerda: puedes seguir enviando tu ubicación a tus contactos de confianza " +
                                "por WhatsApp sin límite.")
                .setPositiveButton("Entendido", null)
                // Botón secundario: abrir WhatsApp igual, porque la emergencia puede ser real
                .setNeutralButton("Alertar por WhatsApp", (d, w) -> {
                    if (!trustedContacts.isEmpty()) {
                        if (ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            sendWhatsAppOnlyAlert();
                        } else {
                            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }
                })
                .show();
    }

    /**
     * Envía la alerta SOLO por WhatsApp, sin publicar en el feed comunitario.
     * Se usa cuando el usuario ya agotó su límite diario pero hay una emergencia real.
     */
    @SuppressLint("MissingPermission")
    private void sendWhatsAppOnlyAlert() {
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Snackbar.make(binding.getRoot(),
                        "No se pudo obtener la ubicación.", Snackbar.LENGTH_LONG).show();
                return;
            }
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            Handler handler = new Handler(Looper.getMainLooper());
            for (int i = 0; i < trustedContacts.size(); i++) {
                Contact contact = trustedContacts.get(i);
                long delay = i * 2000L;
                handler.postDelayed(() ->
                                WhatsAppHelper.openWhatsAppSos(
                                        requireContext(),
                                        contact.getPhone(),
                                        senderName,
                                        lat,
                                        lng),
                        delay);
            }
            Snackbar.make(binding.getRoot(),
                    "Abriendo WhatsApp con tus contactos de confianza...",
                    Snackbar.LENGTH_LONG).show();
        });
    }

    // ─── Cancelar alerta ──────────────────────────────────────────────────────

    private void confirmCancel() {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Cancelar alerta?")
                .setMessage("¿Confirmas que estás a salvo?")
                .setPositiveButton("Sí, estoy a salvo", (d, w) -> {
                    alertViewModel.resolveAlert(activeAlertId);
                    activeAlertId = null;
                    setAlertActive(false);
                    binding.tvLastAlertStatus.setText("✅ Alerta cancelada");
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    /**
     * Actualiza el indicador de alertas restantes en el home.
     * Ejemplo: "🔔 Alertas hoy: 1 / 2"
     */
    private void updateAlertCounterUI() {
        if (binding == null) return;
        int remaining = Math.max(0, alertsLimit - alertsToday);
        if (activeAlertId != null) return; // no pisar el estado de alerta activa

        if (remaining == 0) {
            binding.tvAlertCounter.setText("🔕 Sin alertas comunitarias restantes hoy");
            binding.tvAlertCounter.setVisibility(View.VISIBLE);
        } else {
            binding.tvAlertCounter.setText("🔔 Alertas comunitarias hoy: " + alertsToday + " / " + alertsLimit);
            binding.tvAlertCounter.setVisibility(View.VISIBLE);
        }
    }

    private void updateContactsHint() {
        if (activeAlertId != null) return;
        if (trustedContacts.isEmpty()) {
            binding.tvLastAlertStatus.setText("⚠️ Sin contactos de confianza — ve a la pestaña Contactos");
        } else {
            int n = trustedContacts.size();
            binding.tvLastAlertStatus.setText("✅ " + n + " contacto" + (n > 1 ? "s" : "") + " de confianza");
        }
    }

    private void setAlertActive(boolean active) {
        if (active) {
            binding.btnSOS.setText("ACTIVA\n(mantén para\ncancelar)");
            binding.btnSOS.setBackgroundResource(R.drawable.circle_sos_active);
            ringPulse.start();
        } else {
            binding.btnSOS.setText("SOS");
            binding.btnSOS.setBackgroundResource(R.drawable.circle_sos_button);
            ringPulse.cancel();
            binding.viewSosRing.setAlpha(1f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ringPulse != null) ringPulse.cancel();
        binding = null;
    }
}