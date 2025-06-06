package com.managemart;

public class SupplierModel {
    private String id;
    private String name;
    private String contact;
    private String extraInfo;

    // 🔹 Empty constructor required for Firestore deserialization
    public SupplierModel() {}

    // 🔹 Constructor to initialize all fields
    public SupplierModel(String id, String name, String contact, String extraInfo) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.extraInfo = extraInfo;
    }

    // 🔹 Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
}
