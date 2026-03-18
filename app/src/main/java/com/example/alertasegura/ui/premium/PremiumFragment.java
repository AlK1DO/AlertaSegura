package com.example.alertasegura.ui.premium;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.alertasegura.databinding.FragmentPremiumBinding;
import com.example.alertasegura.util.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PremiumFragment extends Fragment {

    private FragmentPremiumBinding binding;
    private FirebaseFirestore      db;
    private String                 currentPlan = Constants.PLAN_FREE;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPremiumBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        loadCurrentPlan();
        setupButtons();
    }

    // ─── Cargar plan actual ───────────────────────────────────────────────────

    private void loadCurrentPlan() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;
                    String plan = snap.getString("plan");
                    currentPlan = plan != null ? plan : Constants.PLAN_FREE;
                    updateUIForPlan();
                });
    }

    // ─── UI según plan ────────────────────────────────────────────────────────

    private void updateUIForPlan() {
        if (Constants.PLAN_PREMIUM.equals(currentPlan)) {
            // Usuario Premium: badge activo, ocultar suscribirse, mostrar cancelar
            binding.badgePremiumActive.setVisibility(View.VISIBLE);
            binding.layoutSubscribe.setVisibility(View.GONE);
            binding.layoutCancelPremium.setVisibility(View.VISIBLE);
        } else {
            // Usuario Free: ocultar badge y cancelar, mostrar suscribirse
            binding.badgePremiumActive.setVisibility(View.GONE);
            binding.layoutSubscribe.setVisibility(View.VISIBLE);
            binding.layoutCancelPremium.setVisibility(View.GONE);
        }
    }

    // ─── Botones ──────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.btnSubscribe.setOnClickListener(v -> {
            if (Constants.PLAN_PREMIUM.equals(currentPlan)) return;
            showSubscribeConfirmDialog();
        });

        binding.btnCancelPremium.setOnClickListener(v ->
                showCancelConfirmDialog());
    }

    // ─── Flujo suscripción ────────────────────────────────────────────────────

    private void showSubscribeConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Activar Plan Premium")
                .setMessage(
                        "Plan Premium — S/. 4.50 / mes\n\n" +
                                "✅ 10 alertas comunitarias por día\n" +
                                "✅ Hasta 10 contactos de confianza\n" +
                                "✅ Historial de 90 días\n" +
                                "✅ Mapa de calor radio 50 km\n\n" +
                                "⚠️ Esto es una demostración. " +
                                "En producción se integraría con Google Play Billing.")
                .setPositiveButton("Activar ahora", (d, w) -> activatePremium())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void activatePremium() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        binding.btnSubscribe.setEnabled(false);
        binding.btnSubscribe.setText("Activando...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("plan",           Constants.PLAN_PREMIUM);
        updates.put("alertsLimit",    Constants.ALERT_LIMIT_PREMIUM);
        updates.put("alertsToday",    0);
        updates.put("lastAlertReset", System.currentTimeMillis());

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    currentPlan = Constants.PLAN_PREMIUM;
                    updateUIForPlan();
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    binding.btnSubscribe.setEnabled(true);
                    binding.btnSubscribe.setText("⭐ Activar Premium — S/. 4.50 / mes");
                    Snackbar.make(binding.getRoot(),
                            "Error al activar Premium: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⭐ ¡Bienvenido a Premium!")
                .setMessage(
                        "Tu plan Premium está activo.\n\n" +
                                "Ahora tienes:\n" +
                                "• 10 alertas comunitarias por día\n" +
                                "• Hasta 10 contactos de confianza\n" +
                                "• Historial de 90 días\n" +
                                "• Mapa de calor en radio de 50 km")
                .setPositiveButton("¡Listo!", (d, w) ->
                        Navigation.findNavController(requireView()).navigateUp())
                .setCancelable(false)
                .show();
    }

    // ─── Flujo cancelación ────────────────────────────────────────────────────

    private void showCancelConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar Premium")
                .setMessage(
                        "¿Seguro que quieres cancelar?\n\n" +
                                "Mantendrás los beneficios Premium hasta el fin del período actual.")
                .setPositiveButton("Sí, cancelar", (d, w) -> cancelPremium())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelPremium() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        binding.btnCancelPremium.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("plan",        Constants.PLAN_FREE);
        updates.put("alertsLimit", Constants.ALERT_LIMIT_FREE);

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    currentPlan = Constants.PLAN_FREE;
                    updateUIForPlan();
                    Snackbar.make(binding.getRoot(),
                            "Suscripción cancelada. Gracias por usar Premium.",
                            Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    binding.btnCancelPremium.setEnabled(true);
                    Snackbar.make(binding.getRoot(),
                            "Error al cancelar: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}