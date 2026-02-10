package com.bnm.tender.model;

import java.util.UUID;

public class Seller {
    private String id;
    private String name;

    public Seller(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
