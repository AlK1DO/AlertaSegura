package com.example.alertasegura.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class Alert {

    @DocumentId
    private String alertId;
    private String senderUid;
    private String senderName;
    private String senderDni;
    private GeoPoint exactLocation;      // Solo visible para contactos privados
    private GeoPoint approxLocation;     // Visible en comunidad (~500m de offset)
    private int radiusMeters;
    private long timestamp;
    private String status;               // "active" | "resolved" | "expired"
    private boolean isPublic;
    private List<String> notifiedContacts;

    public Alert() {}

    public Alert(String senderUid, String senderName, String senderDni,
                 GeoPoint exactLocation, GeoPoint approxLocation) {
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.senderDni = senderDni;
        this.exactLocation = exactLocation;
        this.approxLocation = approxLocation;
        this.radiusMeters = 500;
        this.timestamp = System.currentTimeMillis();
        this.status = "active";
        this.isPublic = true;
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderDni() { return senderDni; }
    public void setSenderDni(String senderDni) { this.senderDni = senderDni; }

    public GeoPoint getExactLocation() { return exactLocation; }
    public void setExactLocation(GeoPoint exactLocation) { this.exactLocation = exactLocation; }

    public GeoPoint getApproxLocation() { return approxLocation; }
    public void setApproxLocation(GeoPoint approxLocation) { this.approxLocation = approxLocation; }

    public int getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(int radiusMeters) { this.radiusMeters = radiusMeters; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public List<String> getNotifiedContacts() { return notifiedContacts; }
    public void setNotifiedContacts(List<String> notifiedContacts) { this.notifiedContacts = notifiedContacts; }
}