package com.example.alertasegura.data.model;

/**
 * Representa una zona agrupada del mapa de calor.
 * count determina el color: >=10 rojo, 4-9 naranja, 1-3 azul
 */
public class HeatmapCluster {
    public final double lat;
    public final double lng;
    public final int    count;

    public HeatmapCluster(double lat, double lng, int count) {
        this.lat   = lat;
        this.lng   = lng;
        this.count = count;
    }

    /** Color hex según densidad */
    public String getColor() {
        if (count >= 10) return "#B2182B"; // rojo — alto
        if (count >= 4)  return "#EF8A62"; // naranja — medio
        return "#2166AC";                  // azul — bajo
    }

    /** Radio del círculo en metros según densidad */
    public int getRadiusMeters() {
        if (count >= 10) return 400;
        if (count >= 4)  return 280;
        return 180;
    }
}