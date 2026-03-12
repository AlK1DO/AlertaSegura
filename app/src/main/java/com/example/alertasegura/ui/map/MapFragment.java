package com.example.alertasegura.ui.map;

import android.graphics.Color;
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
import com.google.firebase.firestore.GeoPoint;


import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap; // Antes MapboxMap
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.HeatmapLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import static org.maplibre.android.style.expressions.Expression.*;
import static org.maplibre.android.style.layers.PropertyFactory.*;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String SOURCE_ID  = "alerta-heatmap-source";
    private static final String LAYER_ID   = "alerta-heatmap-layer";

    // Tile URL pública sin API key (OpenFreeMap, raster fallback)
    // Puedes cambiarla por cualquier estilo MapTiler/Stamen/etc.
    private static final String STYLE_URL =
            "https://tiles.openfreemap.org/styles/liberty";

    private FragmentMapBinding binding;
    private AlertViewModel alertViewModel;
    private MapLibreMap mapLibreMap;
    private GeoJsonSource heatmapSource;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MapLibre requiere inicialización con un token vacío (no necesita uno real)
        MapLibre.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertViewModel = new ViewModelProvider(requireActivity()).get(AlertViewModel.class);

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);

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
        alertViewModel.heatmapPoints.observe(getViewLifecycleOwner(), this::updateHeatmap);

        // Carga inicial
        alertViewModel.loadHeatmap(24);
    }

    // ─── MapLibre callbacks ────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        this.mapLibreMap = map;

        // Centrar en Lima, Perú
        map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(-12.0464, -77.0428))
                .zoom(11.5)
                .build());

        map.setStyle(STYLE_URL, style -> {
            // Crear fuente GeoJSON vacía
            heatmapSource = new GeoJsonSource(SOURCE_ID,
                    FeatureCollection.fromFeatures(new ArrayList<>()));
            style.addSource(heatmapSource);

            // Agregar capa de heatmap
            style.addLayer(buildHeatmapLayer());

            // Si ya hay datos cargados, renderizarlos
            List<GeoPoint> pending = alertViewModel.heatmapPoints.getValue();
            if (pending != null && !pending.isEmpty()) {
                updateHeatmap(pending);
            }
        });
    }

    // ─── Heatmap ──────────────────────────────────────────────────────────────

    private HeatmapLayer buildHeatmapLayer() {
        HeatmapLayer layer = new HeatmapLayer(LAYER_ID, SOURCE_ID);
        layer.setMaxZoom(18);
        layer.setProperties(
                // Intensidad según zoom
                heatmapIntensity(
                        interpolate(linear(), zoom(),
                                stop(0,  1),
                                stop(9,  3),
                                stop(15, 6)
                        )
                ),
                // Color: azul → amarillo → rojo
                heatmapColor(
                        interpolate(linear(), heatmapDensity(),
                                stop(0,    rgba(0,   0,   255, 0)),
                                stop(0.2f, rgba(0,   255, 255, 0.6f)),
                                stop(0.4f, rgba(0,   255, 0,   0.7f)),
                                stop(0.6f, rgba(255, 255, 0,   0.8f)),
                                stop(0.8f, rgba(255, 128, 0,   0.9f)),
                                stop(1,    rgba(255, 0,   0,   1))
                        )
                ),
                // Radio aumenta con el zoom
                heatmapRadius(
                        interpolate(linear(), zoom(),
                                stop(0,  5),
                                stop(9,  30),
                                stop(15, 60)
                        )
                ),
                // Opacidad disminuye a zoom alto (se ven puntos individuales)
                heatmapOpacity(
                        interpolate(linear(), zoom(),
                                stop(13, 1),
                                stop(16, 0)
                        )
                )
        );
        return layer;
    }

    private void updateHeatmap(List<GeoPoint> points) {
        if (points == null) return;

        binding.tvNoData.setVisibility(points.isEmpty() ? View.VISIBLE : View.GONE);

        if (heatmapSource == null) return; // el mapa aún no está listo

        List<Feature> features = new ArrayList<>();
        for (GeoPoint gp : points) {
            features.add(Feature.fromGeometry(
                    Point.fromLngLat(gp.getLongitude(), gp.getLatitude())));
        }
        heatmapSource.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    // ─── MapView lifecycle (obligatorio con MapLibre) ─────────────────────────

    @Override public void onStart()   { super.onStart();   binding.mapView.onStart(); }
    @Override public void onResume()  { super.onResume();  binding.mapView.onResume(); }
    @Override public void onPause()   { super.onPause();   binding.mapView.onPause(); }
    @Override public void onStop()    { super.onStop();    binding.mapView.onStop(); }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.mapView.onDestroy();
        binding = null;
    }
}