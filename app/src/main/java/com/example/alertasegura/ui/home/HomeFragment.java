// ============================================================
// ui/home/HomeFragment.java  — Botón SOS con selección de contactos
// ============================================================
package com.example.alertasegura.ui.home;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.data.model.User;
import com.example.alertasegura.databinding.FragmentHomeBinding;
import com.example.alertasegura.viewmodel.AlertViewModel;
import com.example.alertasegura.viewmodel.ContactViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AlertViewModel alertViewModel;
    private ContactViewModel contactViewModel;
    private FusedLocationProviderClient fusedLocation;

    private String activeAlertId = null;
    private String senderName    = "";
    private String senderDni     = "";
    private List<Contact> allContacts = new ArrayList<>();

    // Animación del anillo SOS
    private ObjectAnimator ringPulse;

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) triggerSos();
                else Snackbar.make(binding.getRoot(),
                        "Necesitamos tu ubicación para enviar la alerta", Snackbar.LENGTH_LONG).show();
            });

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocation    = LocationServices.getFusedLocationProviderClient(requireActivity());
        alertViewModel   = new ViewModelProvider(requireActivity()).get(AlertViewModel.class);
        contactViewModel = new ViewModelProvider(requireActivity()).get(ContactViewModel.class);

        setupUserInfo();
        setupObservers();
        setupSosButton();
        setupRingAnimation();
        contactViewModel.loadContacts();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void setupUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Cargar nombre y DNI desde Firestore para mostrar y usar en la alerta
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        senderName = snap.getString("fullName") != null ? snap.getString("fullName") : "";
                        senderDni  = snap.getString("dni") != null ? snap.getString("dni") : "";
                        String display = senderName.isEmpty() ? user.getEmail() : senderName;
                        binding.tvUserName.setText(display);
                    }
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

        contactViewModel.contactsLiveData.observe(getViewLifecycleOwner(), contacts -> {
            allContacts = contacts != null ? contacts : new ArrayList<>();
        });
    }

    private void setupSosButton() {
        binding.btnSOS.setOnLongClickListener(v -> {
            if (activeAlertId != null) {
                // Ya hay alerta activa → confirmar cancelación
                new AlertDialog.Builder(requireContext())
                        .setTitle("¿Cancelar alerta?")
                        .setMessage("¿Confirmas que estás a salvo y quieres cancelar la alerta activa?")
                        .setPositiveButton("Sí, estoy a salvo", (d, w) -> {
                            alertViewModel.resolveAlert(activeAlertId);
                            activeAlertId = null;
                            setAlertActive(false);
                            binding.tvLastAlertStatus.setText("✅ Alerta cancelada");
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                // Iniciar flujo de envío
                checkLocationAndSend();
            }
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

    // ─── SOS Logic ────────────────────────────────────────────────────────────

    private void checkLocationAndSend() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            triggerSos();
        }
    }

    @SuppressLint("MissingPermission")
    private void triggerSos() {
        if (allContacts.isEmpty()) {
            // Sin contactos → enviar solo a comunidad
            showContactSelectionDialog(new ArrayList<>());
        } else {
            // Mostrar selector de contactos
            showContactSelectionDialog(allContacts);
        }
    }

    private void showContactSelectionDialog(List<Contact> contacts) {
        if (contacts.isEmpty()) {
            // Sin contactos, enviar directo a comunidad
            confirmAndSend(new ArrayList<>());
            return;
        }

        String[] names    = new String[contacts.size()];
        boolean[] checked = new boolean[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            names[i]   = contacts.get(i).getDisplayName();
            checked[i] = true; // todos seleccionados por defecto
        }

        List<Contact> selected = new ArrayList<>(contacts);

        new AlertDialog.Builder(requireContext())
                .setTitle("¿A quién notificar?")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(contacts.get(which));
                    else selected.remove(contacts.get(which));
                })
                .setPositiveButton("Enviar alerta", (d, w) -> confirmAndSend(selected))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @SuppressLint("MissingPermission")
    private void confirmAndSend(List<Contact> selectedContacts) {
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                // Solicitar ubicación fresca si lastLocation es null
                Snackbar.make(binding.getRoot(), "Obteniendo tu ubicación...", Snackbar.LENGTH_SHORT).show();
                // Re-intentar con updateLocation en producción
                // Por ahora usamos una ubicación aproximada placeholder
                Toast.makeText(requireContext(), "Activa el GPS e inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                return;
            }
            alertViewModel.sendSosAlert(senderName, senderDni, location, selectedContacts);
        });
    }

    private void setAlertActive(boolean active) {
        if (active) {
            binding.btnSOS.setText("ACTIVA\n(manten para\ncancelar)");
            binding.btnSOS.setBackgroundResource(com.example.alertasegura.R.drawable.circle_sos_active);
            ringPulse.start();
        } else {
            binding.btnSOS.setText("SOS");
            binding.btnSOS.setBackgroundResource(com.example.alertasegura.R.drawable.circle_sos_button);
            ringPulse.cancel();
            binding.viewSosRing.setAlpha(1f);
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ringPulse != null) ringPulse.cancel();
        binding = null;
    }
}