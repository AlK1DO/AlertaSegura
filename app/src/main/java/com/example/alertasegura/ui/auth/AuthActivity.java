package com.example.alertasegura.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertasegura.R;
import com.example.alertasegura.databinding.ActivityAuthBinding;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // El NavHostFragment definido en activity_auth.xml maneja la navegación
    }
}