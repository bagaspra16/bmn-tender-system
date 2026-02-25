package com.bnm.tender.model;

import java.util.UUID;

public class TenderRequest {
    private String id;
    private Buyer buyer;
    private String query;        // What the buyer typed (e.g. "Nasi Padang")
    private String preferences;  // Additional notes (e.g. "Spicy", "Cheap")
    private String buyerAddress; // Delivery address
    
    public TenderRequest(Buyer buyer, String query, String preferences) {
        this.id = UUID.randomUUID().toString();
        this.buyer = buyer;
        this.query = query;
        this.preferences = preferences;
        this.buyerAddress = buyer.getAddress();
    }

    public TenderRequest(Buyer buyer, String query, String preferences, String buyerAddress) {
        this.id = UUID.randomUUID().toString();
        this.buyer = buyer;
        this.query = query;
        this.preferences = preferences;
        this.buyerAddress = buyerAddress;
    }

    public String getRequestId() { return id; }
    public Buyer getBuyer() { return buyer; }
    public String getQuery() { return query; }
    public String getPreferences() { return preferences; }
    public String getBuyerAddress() { return buyerAddress; }
}
