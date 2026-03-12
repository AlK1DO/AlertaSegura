package com.example.alertasegura.data.model;

public class Contact {

    private String contactUid;
    private String displayName;
    private String phone;
    private String status; // "pending" | "accepted" | "rejected"
    private long addedAt;
    private boolean notifyOnAlert;

    public Contact() {}

    public Contact(String contactUid, String displayName, String phone) {
        this.contactUid = contactUid;
        this.displayName = displayName;
        this.phone = phone;
        this.status = "pending";
        this.addedAt = System.currentTimeMillis();
        this.notifyOnAlert = true;
    }

    public String getContactUid() { return contactUid; }
    public void setContactUid(String contactUid) { this.contactUid = contactUid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }

    public boolean isNotifyOnAlert() { return notifyOnAlert; }
    public void setNotifyOnAlert(boolean notifyOnAlert) { this.notifyOnAlert = notifyOnAlert; }
}