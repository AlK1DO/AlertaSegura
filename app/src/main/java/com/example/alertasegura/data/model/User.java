package com.example.alertasegura.data.model;

import com.google.firebase.firestore.DocumentId;

public class User {

    @DocumentId
    private String uid;
    private String fullName;
    private String dni;
    private String phone;
    private String email;
    private String fcmToken;
    private long createdAt;
    private boolean isActive;

    // Constructor vacío requerido por Firestore
    public User() {}

    public User(String uid, String fullName, String dni, String phone, String email) {
        this.uid = uid;
        this.fullName = fullName;
        this.dni = dni;
        this.phone = phone;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}