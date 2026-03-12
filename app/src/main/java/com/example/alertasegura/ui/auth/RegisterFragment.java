// ============================================================
// app/src/main/java/com/alertasegura/ui/auth/RegisterFragment.java
// ============================================================
package com.example.alertasegura.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.alertasegura.R;
import com.example.alertasegura.databinding.FragmentRegisterBinding;
import com.example.alertasegura.ui.main.MainActivity;
import com.example.alertasegura.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Observar registro exitoso
        authViewModel.userLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Observar errores
        authViewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observar estado de carga
        authViewModel.loadingLiveData.observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnRegister.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Botón de registro
        binding.btnRegister.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (validateInputs(email, password, confirmPassword)) {
                authViewModel.register(email, password);
            }
        });

        // Volver al login
        binding.tvGoToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (email.isEmpty()) {
            binding.etEmail.setError("Ingresa tu email");
            return false;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Ingresa una contraseña");
            return false;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Mínimo 6 caracteres");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Las contraseñas no coinciden");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}