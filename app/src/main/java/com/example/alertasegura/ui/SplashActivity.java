package com.example.alertasegura.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertasegura.ui.auth.AuthActivity;
import com.example.alertasegura.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sin layout — solo lógica de redirección

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Usuario con sesión activa → ir directo a la app
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Sin sesión → ir a login
            startActivity(new Intent(this, AuthActivity.class));
        }

        finish(); // cerrar SplashActivity para que no quede en el back stack
    }
}