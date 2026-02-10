package com.bnm.tender.model;

import java.util.UUID;

public class Buyer {
    private String id;
    private String name;

    public Buyer(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
