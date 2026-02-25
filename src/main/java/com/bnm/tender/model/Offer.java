package com.bnm.tender.model;

public class Offer {
    private Seller seller;
    private Product product;
    private double price;       // Unit price per item
    private int quantity;       // Number of items
    private double rating;      // 0.0 to 5.0
    private String comment;
    private TenderRequest request;
    
    // Calculated score for AI ranking
    private double aiScore;

    public Offer(Seller seller, Product product, double price, int quantity, double rating, String comment, TenderRequest request) {
        this.seller = seller;
        this.product = product;
        this.price = price;
        this.quantity = quantity;
        this.rating = rating;
        this.comment = comment;
        this.request = request;
        calculateScore();
    }

    // Backward-compatible constructor (quantity defaults to 1)
    public Offer(Seller seller, Product product, double price, double rating, String comment, TenderRequest request) {
        this(seller, product, price, 1, rating, comment, request);
    }

    private void calculateScore() {
        // Score = (Rating / UnitPrice) * 10000
        // Higher rating + lower price = better score
        if (price > 0) {
            this.aiScore = (rating / price) * 10000;
        } else {
            this.aiScore = 0;
        }
    }

    public Seller getSeller() { return seller; }
    public Product getProduct() { return product; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); calculateScore(); }
    public double getTotalPrice() { return price * quantity; }
    public double getRating() { return rating; }
    public String getComment() { return comment; }
    public double getAiScore() { return aiScore; }
    
    @Override
    public String toString() {
        return String.format("%s - %s x%d @ %.0f (%.1f*)", 
            seller.getName(), product.getName(), quantity, price, rating);
    }
}
