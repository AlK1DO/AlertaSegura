package com.example.alertasegura.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.alertasegura.R;
import com.example.alertasegura.databinding.FragmentRegisterBinding;
import com.example.alertasegura.ui.main.MainActivity;
import com.example.alertasegura.util.DniValidator;
import com.example.alertasegura.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel           authViewModel;

    // Nombre verificado por RENIEC — se usa al crear la cuenta
    // para que el nombre no pueda ser inventado
    private String verifiedFullName = null;

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

        // Registro exitoso → ir a MainActivity
        authViewModel.registeredUserLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                setLoading(false);
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Error de Firebase → apagar loading y mostrar mensaje
        authViewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                authViewModel.errorLiveData.setValue(null);
            }
        });

        // Términos y condiciones
        binding.tvTermsLink.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.terms_and_conditions_title))
                        .setMessage(getString(R.string.terms_full_text))
                        .setPositiveButton("Cerrar", null)
                        .show()
        );

        // ── Cuando el usuario sale del campo DNI → verificar automáticamente ──
        binding.etDni.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String dni = binding.etDni.getText().toString().trim();
                if (dni.length() == 8) verifyDni(dni);
            }
        });

        // Botón registrar
        binding.btnRegister.setOnClickListener(v -> {
            String dni             = binding.etDni.getText().toString().trim();
            String phone           = binding.etPhone.getText().toString().trim();
            String email           = binding.etEmail.getText().toString().trim();
            String password        = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            // Si el DNI aún no fue verificado, verificar primero
            if (verifiedFullName == null) {
                if (dni.length() == 8) {
                    setLoading(true);
                    DniValidator.validate(dni, new DniValidator.Callback() {
                        @Override
                        public void onValid(String fullName) {
                            verifiedFullName = fullName;
                            // Autocompletar el nombre con el de RENIEC
                            binding.etFullName.setText(fullName);
                            binding.etFullName.setEnabled(false); // no editable
                            binding.tilDni.setError(null);
                            binding.tilDni.setHelperText("✓ DNI verificado en RENIEC");

                            setLoading(false);
                            // Ahora sí validar el resto y registrar
                            if (validateInputs(fullName, dni, phone, email, password, confirmPassword)) {
                                setLoading(true);
                                authViewModel.register(email, password, fullName, dni, phone);
                            }
                        }

                        @Override
                        public void onInvalid(String reason) {
                            setLoading(false);
                            verifiedFullName = null;
                            binding.tilDni.setError(reason);
                        }
                    });
                } else {
                    binding.tilDni.setError("El DNI debe tener 8 dígitos");
                }
                return;
            }

            // DNI ya verificado → validar resto y registrar
            if (validateInputs(verifiedFullName, dni, phone, email, password, confirmPassword)) {
                setLoading(true);
                authViewModel.register(email, password, verifiedFullName, dni, phone);
            }
        });

        // Ir a login
        binding.tvGoToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );
    }

    // ─── Verificación de DNI ──────────────────────────────────────────────────

    /**
     * Consulta apiperu.dev para verificar el DNI.
     * Si es válido: autocompleta el nombre y lo bloquea.
     * Si no: muestra error en el campo.
     */
    private void verifyDni(String dni) {
        // Resetear verificación anterior si cambió el DNI
        verifiedFullName = null;
        binding.etFullName.setEnabled(true);
        binding.tilDni.setHelperText("Verificando DNI...");
        binding.tilDni.setError(null);

        DniValidator.validate(dni, new DniValidator.Callback() {
            @Override
            public void onValid(String fullName) {
                verifiedFullName = fullName;

                // Autocompletar nombre con el de RENIEC y bloquearlo
                binding.etFullName.setText(fullName);
                binding.etFullName.setEnabled(false);

                binding.tilDni.setError(null);
                binding.tilDni.setHelperText("✓ DNI verificado en RENIEC");

                Snackbar.make(binding.getRoot(),
                        "DNI verificado: " + fullName,
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onInvalid(String reason) {
                verifiedFullName = null;
                binding.etFullName.setText("");
                binding.etFullName.setEnabled(true);
                binding.tilDni.setHelperText(null);
                binding.tilDni.setError(reason);
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

        if (dni.length() != 8) {
            binding.tilDni.setError("El DNI debe tener 8 dígitos");
            return false;
        }
        binding.tilDni.setError(null);

        // DNI debe haber sido verificado por RENIEC
        if (verifiedFullName == null) {
            binding.tilDni.setError("Verifica tu DNI antes de continuar");
            return false;
        }

        if (phone.length() != 9) {
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

        if (!binding.cbTerms.isChecked()) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.terms_error),
                    Snackbar.LENGTH_LONG).show();
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