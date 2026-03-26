package com.example.alertasegura.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alertasegura.data.model.HeatmapCluster;
import com.example.alertasegura.databinding.FragmentMapBinding;
import com.example.alertasegura.viewmodel.AlertViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
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

    private static final String SOURCE_ID         = "alerta-heatmap-source";
    private static final String LAYER_ID          = "alerta-heatmap-layer";
    private static final String CLUSTER_SOURCE_ID = "alerta-cluster-source";
    private static final String CLUSTER_LAYER_ID  = "alerta-cluster-layer";
    private static final String STYLE_URL         = "https://tiles.openfreemap.org/styles/liberty";

    // Propiedad guardada en cada Feature para colorear y dimensionar los círculos
    private static final String PROP_COLOR  = "clusterColor";
    private static final String PROP_RADIUS = "clusterRadius";

    private FragmentMapBinding        binding;
    private AlertViewModel            alertViewModel;
    private MapLibreMap               mapLibreMap;
    private GeoJsonSource             heatmapSource;
    private GeoJsonSource             clusterSource;
    private FusedLocationProviderClient fusedLocation;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && mapLibreMap != null) {
                    enableLocationComponent(mapLibreMap.getStyle());
                    centerOnUser();
                }
            });

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(requireContext());
        fusedLocation = LocationServices.getFusedLocationProviderClient(requireActivity());
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

        setupFilters();

        // Observar puntos del heatmap
        alertViewModel.heatmapPoints.observe(getViewLifecycleOwner(), this::updateHeatmap);
        // Observar clusters de densidad
        alertViewModel.heatmapClusters.observe(getViewLifecycleOwner(), this::updateClusters);

        alertViewModel.loadHeatmap(24);
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void setupFilters() {
        binding.chip24h.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(24);
        });
        binding.chip7d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(168);
        });
        binding.chip30d.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) alertViewModel.loadHeatmap(720);
        });
    }

    // ─── Mapa listo ───────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        this.mapLibreMap = map;

        map.setStyle(STYLE_URL, style -> {
            // Fuente y capa del heatmap base
            heatmapSource = new GeoJsonSource(SOURCE_ID,
                    FeatureCollection.fromFeatures(new ArrayList<>()));
            style.addSource(heatmapSource);
            style.addLayer(buildHeatmapLayer());

            // Fuente y capa de círculos de densidad (encima del heatmap)
            clusterSource = new GeoJsonSource(CLUSTER_SOURCE_ID,
                    FeatureCollection.fromFeatures(new ArrayList<>()));
            style.addSource(clusterSource);
            style.addLayer(buildClusterLayer());

            if (hasLocationPermission()) {
                enableLocationComponent(style);
                centerOnUser();
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            // Renderizar si ya hay datos
            List<GeoPoint> currentPoints = alertViewModel.heatmapPoints.getValue();
            if (currentPoints != null) updateHeatmap(currentPoints);

            List<HeatmapCluster> currentClusters = alertViewModel.heatmapClusters.getValue();
            if (currentClusters != null) updateClusters(currentClusters);
        });
    }

    // ─── Heatmap base ─────────────────────────────────────────────────────────

    private HeatmapLayer buildHeatmapLayer() {
        return new HeatmapLayer(LAYER_ID, SOURCE_ID).withProperties(
                heatmapIntensity(interpolate(linear(), zoom(),
                        stop(0, 1), stop(9, 3), stop(15, 5))),
                heatmapColor(interpolate(linear(), heatmapDensity(),
                        stop(0,   rgba(33,  102, 172, 0)),
                        stop(0.2, rgb(103, 169, 207)),
                        stop(0.4, rgb(209, 229, 240)),
                        stop(0.6, rgb(253, 219, 199)),
                        stop(0.8, rgb(239, 138, 98)),
                        stop(1,   rgb(178, 24,  43))
                )),
                heatmapRadius(interpolate(linear(), zoom(),
                        stop(0, 2), stop(9, 20), stop(15, 40))),
                heatmapOpacity(0.75f)
        );
    }

    private void updateHeatmap(List<GeoPoint> points) {
        if (points == null || heatmapSource == null) return;
        binding.tvNoData.setVisibility(points.isEmpty() ? View.VISIBLE : View.GONE);

        List<Feature> features = new ArrayList<>();
        for (GeoPoint gp : points) {
            features.add(Feature.fromGeometry(
                    Point.fromLngLat(gp.getLongitude(), gp.getLatitude())));
        }
        heatmapSource.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    // ─── Capa de círculos de densidad ─────────────────────────────────────────

    /**
     * CircleLayer que lee el color y radio desde propiedades del Feature.
     * Así un solo layer dibuja los tres niveles (rojo/naranja/azul)
     * sin necesidad de tres layers separados.
     */
    private CircleLayer buildClusterLayer() {
        CircleLayer layer = new CircleLayer(CLUSTER_LAYER_ID, CLUSTER_SOURCE_ID);
        layer.withProperties(
                circleColor(get(PROP_COLOR)),
                circleRadius(division(get(PROP_RADIUS),
                        interpolate(linear(), zoom(),
                                stop(10, literal(12)),
                                stop(14, literal(4)),
                                stop(16, literal(2))))),
                circleOpacity(0.45f),
                circleBlur(0.6f)          // borde difuminado = fusión visual con el heatmap
        );
        return layer;
    }

    private void updateClusters(List<HeatmapCluster> clusters) {
        if (clusters == null || clusterSource == null) return;

        List<Feature> features = new ArrayList<>();
        for (HeatmapCluster c : clusters) {
            Feature f = Feature.fromGeometry(
                    Point.fromLngLat(c.lng, c.lat));
            f.addStringProperty(PROP_COLOR,  c.getColor());
            f.addNumberProperty(PROP_RADIUS, c.getRadiusMeters());
            features.add(f);
        }
        clusterSource.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    // ─── Ubicación ────────────────────────────────────────────────────────────

    @SuppressWarnings("MissingPermission")
    private void enableLocationComponent(@NonNull Style style) {
        LocationComponent locationComponent = mapLibreMap.getLocationComponent();
        locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(requireContext(), style).build());
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.COMPASS);
    }

    @SuppressWarnings("MissingPermission")
    private void centerOnUser() {
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mapLibreMap != null) {
                mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), 13));
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // ─── MapView lifecycle ────────────────────────────────────────────────────

    @Override public void onStart()    { super.onStart();    binding.mapView.onStart(); }
    @Override public void onResume()   { super.onResume();   binding.mapView.onResume(); }
    @Override public void onPause()    { super.onPause();    binding.mapView.onPause(); }
    @Override public void onStop()     { super.onStop();     binding.mapView.onStop(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); binding.mapView.onSaveInstanceState(out);
    }
    @Override public void onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory(); }
    @Override public void onDestroyView() {
        super.onDestroyView(); binding.mapView.onDestroy(); binding = null;
    }
}