package com.example.alertasegura.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alertasegura.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Filtros de período
        binding.chip24h.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) loadHeatmap(24);
        });
        binding.chip7d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) loadHeatmap(168);
        });
        binding.chip30d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) loadHeatmap(720);
        });
    }

    private void loadHeatmap(int hours) {
        // TODO Fase 4: cargar datos de Firestore y pintar HeatmapTileProvider
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}