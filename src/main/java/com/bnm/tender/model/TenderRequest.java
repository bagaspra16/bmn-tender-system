package com.bnm.tender.model;

import java.util.UUID;

public class TenderRequest {
    private String id;
    private Buyer buyer;
    private String query; // What the buyer typed (e.g. "Nasi Padang")
    private String preferences; // Additional notes (e.g. "Spicy", "Cheap")
    
    public TenderRequest(Buyer buyer, String query, String preferences) {
        this.id = UUID.randomUUID().toString();
        this.buyer = buyer;
        this.query = query;
        this.preferences = preferences;
    }

    public String getRequestId() { return id; }
    public Buyer getBuyer() { return buyer; }
    public String getQuery() { return query; }
    public String getPreferences() { return preferences; }
}
