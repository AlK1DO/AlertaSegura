// com/example/alertasegura/ui/auth/RegisterFragment.java
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

import com.example.alertasegura.databinding.FragmentRegisterBinding;
import com.example.alertasegura.ui.main.MainActivity;
import com.example.alertasegura.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Registro exitoso → apagar loading e ir a MainActivity
        authViewModel.registeredUserLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                setLoading(false);
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Error → apagar loading y mostrar mensaje
        authViewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                authViewModel.errorLiveData.setValue(null); // limpiar
            }
        });

        // Botón registrar
        binding.btnRegister.setOnClickListener(v -> {
            String fullName        = binding.etFullName.getText().toString().trim();
            String dni             = binding.etDni.getText().toString().trim();
            String phone           = binding.etPhone.getText().toString().trim();
            String email           = binding.etEmail.getText().toString().trim();
            String password        = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (validateInputs(fullName, dni, phone, email, password, confirmPassword)) {
                setLoading(true);
                authViewModel.register(email, password, fullName, dni, phone);
            }
        });

        // Ir a login
        binding.tvGoToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );
    }

    private void setLoading(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private boolean validateInputs(String fullName, String dni, String phone,
                                   String email, String password, String confirmPassword) {
        if (fullName.isEmpty()) {
            binding.tilFullName.setError("Ingresa tu nombre completo");
            return false;
        }
        binding.tilFullName.setError(null);

        if (dni.isEmpty() || dni.length() != 8) {
            binding.tilDni.setError("El DNI debe tener 8 dígitos");
            return false;
        }
        binding.tilDni.setError(null);

        if (phone.isEmpty() || phone.length() != 9) {
            binding.tilPhone.setError("El teléfono debe tener 9 dígitos");
            return false;
        }
        binding.tilPhone.setError(null);

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Correo no válido");
            return false;
        }
        binding.tilEmail.setError(null);

        if (password.length() < 6) {
            binding.tilPassword.setError("Mínimo 6 caracteres");
            return false;
        }
        binding.tilPassword.setError(null);

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Las contraseñas no coinciden");
            return false;
        }
        binding.tilConfirmPassword.setError(null);

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}