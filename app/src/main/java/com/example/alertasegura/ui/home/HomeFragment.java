package com.example.alertasegura.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("🔴  Botón SOS — Fase 2");
        tv.setTextSize(22f);
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}