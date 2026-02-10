package com.bnm.tender.model;

public class Offer {
    private Seller seller;
    private Product product;
    private double price;
    private double rating; // 0.0 to 5.0
    private String comment;
    private TenderRequest request;
    
    // Calculated score for AI ranking
    private double aiScore;

    public Offer(Seller seller, Product product, double price, double rating, String comment, TenderRequest request) {
        this.seller = seller;
        this.product = product;
        this.price = price;
        this.rating = rating;
        this.comment = comment;
        this.request = request;
        calculateScore();
    }

    private void calculateScore() {
        // Simple logic: Higher rating is better, lower price is better.
        // We normalize price assuming a baseline or just use inverse.
        // Score = (Rating * 20) - (Price / 1000) (Just a rough heuristic)
        // A better heuristic for "Cheapest & Best Rating":
        // We prioritize rating heavily but penalize high price.
        
        // Let's use a simpler weighted sum:
        // Rating (0-5) -> normalized to 0-100: rating * 20
        // Price -> lower is better. 
        // For this demo, let's say score = (Rating / Price) * 10000 
        // Example: Rating 5, Price 20000 -> (5/20000)*10000 = 2.5
        // Example: Rating 4, Price 15000 -> (4/15000)*10000 = 2.66 (Better because much cheaper)
        
        if (price > 0) {
            this.aiScore = (rating / price) * 10000;
        } else {
            this.aiScore = 0;
        }
    }

    public Seller getSeller() { return seller; }
    public Product getProduct() { return product; }
    public double getPrice() { return price; }
    public double getRating() { return rating; }
    public String getComment() { return comment; }
    public double getAiScore() { return aiScore; }
    
    @Override
    public String toString() {
        return String.format("%s - %s @ %.0f (%.1f*)", seller.getName(), product.getName(), price, rating);
    }
}
