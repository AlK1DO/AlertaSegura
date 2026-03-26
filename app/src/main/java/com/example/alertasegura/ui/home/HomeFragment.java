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
import java.util.List;
import java.util.Locale;

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
    private int  alertsToday    = 0;
    private int  alertsLimit    = 2;
    private long lastAlertReset = 0;

    private ObjectAnimator ringPulse;

    // ─── FIX 2: Handler centralizado para cancelar postDelayed al destruir ────
    private final Handler        whatsappHandler   = new Handler(Looper.getMainLooper());
    private final List<Runnable> whatsappRunnables = new ArrayList<>();

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

        binding.btnGoToPremium.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_premium));
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

                    Long storedAlertsToday = snap.getLong("alertsToday");
                    Long storedAlertsLimit = snap.getLong("alertsLimit");
                    Long storedLastReset   = snap.getLong("lastAlertReset");

                    alertsToday    = storedAlertsToday != null ? storedAlertsToday.intValue() : 0;
                    alertsLimit    = storedAlertsLimit != null ? storedAlertsLimit.intValue()  : 2;
                    lastAlertReset = storedLastReset   != null ? storedLastReset               : System.currentTimeMillis();

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

                // FIX 1: El repositorio YA incrementó en Firestore.
                // Aquí solo actualizamos la variable local para reflejar en UI.
                // NO llamar updateAlertCountInFirestore() — evita el doble conteo.
                alertsToday++;
                updateAlertCounterUI();
            }
        });

        alertViewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) return;
            if ("LIMIT_REACHED".equals(error)) {
                showLimitReachedDialog();
            } else {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
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
        checkAndResetDailyCounter();

        if (alertsToday >= alertsLimit) {
            showLimitReachedDialog();
            return;
        }

        if (trustedContacts.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin contactos de confianza")
                    .setMessage("Primero agrega al menos un contacto de confianza en la pestaña Contactos.")
                    .setPositiveButton("Ir a Contactos", (d, w) ->
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.contactsFragment))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

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
                        "No se pudo obtener la ubicación. Activa el GPS.",
                        Snackbar.LENGTH_LONG).show();
                return;
            }
            sendAlert(location);
        });
    }

    private void sendAlert(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // 1. Enviar al repositorio — ÉL es el único que incrementa en Firestore
        List<String> uids = new ArrayList<>();
        for (Contact c : trustedContacts) uids.add(c.getContactUid());
        alertViewModel.sendSosAlert(senderName, senderDni, location, uids);

        // 2. WhatsApp con Runnables cancelables
        cancelPendingWhatsappMessages();
        for (int i = 0; i < trustedContacts.size(); i++) {
            Contact contact = trustedContacts.get(i);
            long delay = i * 2000L;
            Runnable r = () -> {
                if (!isAdded() || binding == null) return; // fragment ya destruido
                WhatsAppHelper.openWhatsAppSos(
                        requireContext(), contact.getPhone(), senderName, lat, lng);
            };
            whatsappRunnables.add(r);
            whatsappHandler.postDelayed(r, delay);
        }
    }

    // ─── Cancelar Runnables pendientes ───────────────────────────────────────

    private void cancelPendingWhatsappMessages() {
        for (Runnable r : whatsappRunnables) whatsappHandler.removeCallbacks(r);
        whatsappRunnables.clear();
    }

    // ─── Límite diario ────────────────────────────────────────────────────────

    private void checkAndResetDailyCounter() {
        long now         = System.currentTimeMillis();
        long millisInDay = 24 * 60 * 60 * 1000L;
        if (now - lastAlertReset >= millisInDay) {
            alertsToday    = 0;
            lastAlertReset = now;
        }
    }

    private void showLimitReachedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Límite diario alcanzado")
                .setMessage(
                        "Has usado tus " + alertsLimit + " alertas comunitarias de hoy.\n\n" +
                                "⚠️ Tu contador se reinicia a medianoche.\n\n" +
                                "💡 Puedes seguir enviando tu ubicación por WhatsApp sin límite.")
                .setPositiveButton("Entendido", null)
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

            cancelPendingWhatsappMessages();
            for (int i = 0; i < trustedContacts.size(); i++) {
                Contact contact = trustedContacts.get(i);
                long delay = i * 2000L;
                Runnable r = () -> {
                    if (!isAdded() || binding == null) return;
                    WhatsAppHelper.openWhatsAppSos(
                            requireContext(), contact.getPhone(), senderName, lat, lng);
                };
                whatsappRunnables.add(r);
                whatsappHandler.postDelayed(r, delay);
            }
            Snackbar.make(binding.getRoot(),
                    "Abriendo WhatsApp con tus contactos...", Snackbar.LENGTH_LONG).show();
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

    private void updateAlertCounterUI() {
        if (binding == null) return;
        if (activeAlertId != null) return;
        int remaining = Math.max(0, alertsLimit - alertsToday);
        if (remaining == 0) {
            binding.tvAlertCounter.setText("🔕 Sin alertas comunitarias restantes hoy");
        } else {
            binding.tvAlertCounter.setText("🔔 Alertas comunitarias hoy: " + alertsToday + " / " + alertsLimit);
        }
        binding.tvAlertCounter.setVisibility(View.VISIBLE);
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
        cancelPendingWhatsappMessages(); // FIX 2: limpiar callbacks al destruir
        if (ringPulse != null) ringPulse.cancel();
        binding = null;
    }
}