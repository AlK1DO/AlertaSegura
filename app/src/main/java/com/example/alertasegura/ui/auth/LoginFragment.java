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
import com.example.alertasegura.databinding.FragmentLoginBinding;
import com.example.alertasegura.ui.main.MainActivity;
import com.example.alertasegura.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Observar si el login fue exitoso
        authViewModel.userLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Login exitoso → ir a MainActivity
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
            binding.btnLogin.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Botón de login
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (validateInputs(email, password)) {
                authViewModel.login(email, password);
            }
        });

        // Ir a pantalla de registro
        binding.tvGoToRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register)
        );
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            binding.etEmail.setError("Ingresa tu email");
            return false;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Ingresa tu contraseña");
            return false;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Mínimo 6 caracteres");
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