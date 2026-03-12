package com.example.alertasegura.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertasegura.data.repository.AuthRepository;
import com.example.alertasegura.ui.auth.AuthActivity;
import com.example.alertasegura.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No necesita layout, solo redirige

        AuthRepository authRepository = new AuthRepository();

        if (authRepository.getCurrentUser() != null) {
            // Ya hay sesión activa → ir a la app principal
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // No hay sesión → ir al login
            startActivity(new Intent(this, AuthActivity.class));
        }

        finish(); // Cerrar el Splash para que no quede en el back stack
    }
}
