package com.bnm.tender.model;

import java.util.UUID;

public class Seller {
    private String id;
    private String name;
    private String contactId; // Phone / WA number
    private String address;   // For map integration

    public Seller(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.contactId = "";
        this.address = "";
    }

    public Seller(String name, String contactId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.contactId = contactId != null ? contactId : "";
        this.address = "";
    }

    public Seller(String name, String contactId, String address) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.contactId = contactId != null ? contactId : "";
        this.address = address != null ? address : "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address != null ? address : ""; }

    @Override
    public String toString() {
        return name;
    }
}
