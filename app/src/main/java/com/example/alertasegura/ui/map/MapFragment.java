// ============================================================
// ui/map/MapFragment.java  — Mapa de calor de zonas peligrosas
// ============================================================
package com.example.alertasegura.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alertasegura.databinding.FragmentMapBinding;
import com.example.alertasegura.viewmodel.AlertViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private AlertViewModel alertViewModel;
    private GoogleMap googleMap;
    private HeatmapTileProvider heatmapProvider;
    private com.google.android.gms.maps.model.TileOverlay tileOverlay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertViewModel = new ViewModelProvider(requireActivity()).get(AlertViewModel.class);

        // Inicializar SupportMapFragment
        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(com.example.alertasegura.R.id.mapView);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        // Chips de período
        binding.chip24h.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(24);
        });
        binding.chip7d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(168);
        });
        binding.chip30d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(720);
        });

        // Observar puntos del mapa de calor
        alertViewModel.heatmapPoints.observe(getViewLifecycleOwner(), this::renderHeatmap);

        // Carga inicial (24h)
        alertViewModel.loadHeatmap(24);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Centrar en Lima, Perú por defecto
        LatLng lima = new LatLng(-12.0464, -77.0428);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f));

        // Si ya hay datos cargados, renderizar
        List<GeoPoint> current = alertViewModel.heatmapPoints.getValue();
        if (current != null && !current.isEmpty()) renderHeatmap(current);
    }

    private void renderHeatmap(List<GeoPoint> points) {
        if (googleMap == null) return;

        // Eliminar overlay anterior
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }

        if (points == null || points.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoData.setVisibility(View.GONE);

        List<LatLng> latLngs = new ArrayList<>();
        for (GeoPoint gp : points) {
            latLngs.add(new LatLng(gp.getLatitude(), gp.getLongitude()));
        }

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(latLngs)
                .radius(50)
                .build();

        tileOverlay = googleMap.addTileOverlay(
                new com.google.android.gms.maps.model.TileOverlayOptions()
                        .tileProvider(heatmapProvider));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}