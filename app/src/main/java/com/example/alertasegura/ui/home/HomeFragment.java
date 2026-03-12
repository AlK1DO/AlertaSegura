// ============================================================
// ui/home/HomeFragment.java
//
// Flujo simplificado:
//   1. Mantener presionado SOS
//   2. Obtiene ubicación
//   3. Guarda alerta en Firestore (feed comunitario)
//   4. Abre WhatsApp directo a cada contacto de confianza
//      → sin selector, el usuario solo toca "Enviar" en cada chat
//
// Los contactos de confianza se configuran UNA SOLA VEZ
// en la pestaña "Contactos".
// ============================================================
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
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                alertViewModel.errorLiveData.setValue(null);
            }
        });

        // Mantener lista de contactos de confianza actualizada
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
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getLocationAndSend();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocationAndSend() {
        // Sin contactos → redirigir a configurarlos primero
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

        // 2. Abrir WhatsApp para cada contacto de confianza, uno a uno
        //    Delay de 2s entre chats para que el usuario pueda tocar "Enviar"
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

    private void updateContactsHint() {
        if (activeAlertId != null) return; // no pisar el estado de alerta activa
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