package com.bnm.tender.model;

import java.util.UUID;

public class Buyer {
    private String id;
    private String name;
    private String address;

    public Buyer(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.address = "";
    }

    public Buyer(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address != null ? address : "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
