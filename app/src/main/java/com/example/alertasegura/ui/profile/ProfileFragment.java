package com.example.alertasegura.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.alertasegura.R;
import com.example.alertasegura.databinding.FragmentProfileBinding;
import com.example.alertasegura.ui.auth.AuthActivity;
import com.example.alertasegura.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseFirestore       db;
    private FirebaseAuth            auth;
    private String                  currentPlan = Constants.PLAN_FREE;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadUserProfile();
        setupButtons();
    }

    // ─── Cargar perfil ────────────────────────────────────────────────────────

    private void loadUserProfile() {
        var user = auth.getCurrentUser();
        if (user == null) return;

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;

                    String fullName = snap.getString("fullName");
                    String dni      = snap.getString("dni");
                    String phone    = snap.getString("phone");
                    String plan     = snap.getString("plan");

                    currentPlan = plan != null ? plan : Constants.PLAN_FREE;

                    // Nombre completo (privado, solo lo ve el usuario)
                    binding.tvFullNameReadOnly.setText(
                            fullName != null ? fullName : "—");

                    // Nombre corto → cómo aparece en el feed comunitario
                    binding.tvShortNameReadOnly.setText(getShortName(fullName));

                    // DNI enmascarado
                    binding.tvDniReadOnly.setText(maskDni(dni));

                    // Teléfono
                    binding.tvPhoneReadOnly.setText(phone != null ? phone : "—");

                    // Plan
                    updatePlanUI();
                });
    }

    // ─── Botones ──────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnUpgradeToPremium.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profile_to_premium));

        binding.btnSignOut.setOnClickListener(v -> confirmSignOut());
    }

    // ─── Cerrar sesión ────────────────────────────────────────────────────────

    private void confirmSignOut() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Confirmas que quieres cerrar sesión?")
                .setPositiveButton("Sí, salir", (d, w) -> {
                    auth.signOut();
                    Intent intent = new Intent(requireActivity(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private void updatePlanUI() {
        if (Constants.PLAN_PREMIUM.equals(currentPlan)) {
            binding.tvCurrentPlan.setText("⭐ Plan Premium activo");
            binding.tvCurrentPlan.setTextColor(
                    requireContext().getColor(android.R.color.holo_orange_light));
            binding.btnUpgradeToPremium.setVisibility(View.GONE);
        } else {
            binding.tvCurrentPlan.setText("Plan gratuito — 2 alertas / día");
            binding.tvCurrentPlan.setTextColor(
                    requireContext().getColor(android.R.color.darker_gray));
            binding.btnUpgradeToPremium.setVisibility(View.VISIBLE);
        }
    }

    /**
     * RENIEC devuelve el nombre en orden:
     * APELLIDO1 APELLIDO2 NOMBRE1 NOMBRE2
     *
     * Ejemplos:
     *   "PÉREZ QUISPE JUAN CARLOS" → "Juan Pérez"
     *   "GARCIA LOPEZ MARIA"       → "Maria Garcia"
     *   "FLORES ANA"               → "Ana Flores"
     *   "JUAN"                     → "Juan"
     */
    private String getShortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "Usuario";

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return capitalize(parts[0]);
        }
        if (parts.length == 2) {
            // APELLIDO NOMBRE → "Ana Flores"
            return capitalize(parts[1]) + " " + capitalize(parts[0]);
        }
        // Caso normal RENIEC: APELLIDO1 APELLIDO2 NOMBRE1 ...
        // → NOMBRE1 + APELLIDO1
        return capitalize(parts[2]) + " " + capitalize(parts[0]);
    }

    /**
     * Convierte "PÉREZ" → "Pérez"
     */
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return "";
        return word.charAt(0) + word.substring(1).toLowerCase();
    }

    /**
     * Enmascara el DNI: "74561234" → "7XXXX234"
     */
    private String maskDni(String dni) {
        if (dni == null || dni.length() != 8) return "—";
        return dni.charAt(0) + "XXXX" + dni.substring(5);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}