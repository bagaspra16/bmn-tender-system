package com.bnm.tender.controller;

import com.bnm.tender.model.*;
import java.util.*;

/**
 * Singleton Controller acting as the Mediator between Buyer and Sellers.
 */
public class TenderController {
    private static TenderController instance;
    private List<Seller> sellers;
    private List<TenderRequest> activeRequests;
    private Map<String, List<Offer>> offersMap; // RequestId -> List<Offer>
    private List<Payment> paymentHistory;

    // Listeners for UI updates
    public interface TenderListener {
        void onNewRequest(TenderRequest request);
        void onNewOffer(String requestId, Offer offer);
        default void onPaymentCompleted(Payment payment) {}
    }
    
    private List<TenderListener> listeners;
    private Buyer currentUser; // The singleton buyer for this session

    private TenderController() {
        sellers = new ArrayList<>();
        activeRequests = new ArrayList<>();
        offersMap = new HashMap<>();
        listeners = new ArrayList<>();
        paymentHistory = new ArrayList<>();
        currentUser = new Buyer("King Buyer 👑"); // Default user
        
        // Setup initial dummy sellers with contact IDs
        setupDummySellers();
    }

    public static TenderController getInstance() {
        if (instance == null) {
            instance = new TenderController();
        }
        return instance;
    }

    private void setupDummySellers() {
        String[] contacts = {
            "081234567890", "082198765432", "085711112222",
            "087833334444", "081355556666", "081977778888",
            "085299990000", "083122223333", "084544445555",
            "086766667777"
        };
        String[] addresses = {
            "Jl. Sudirman No. 12, Jakarta Pusat",
            "Jl. Thamrin 45, Jakarta Selatan",
            "Jl. Gatot Subroto 88, Jakarta",
            "Jl. Rasuna Said Kav 1, Kuningan",
            "Jl. MH Thamrin 28, Jakarta",
            "Jl. HR Rasuna Said, Setiabudi",
            "Jl. Jenderal Sudirman Kav 52, Jakarta",
            "Jl. Prof. Dr. Satrio Kav 18, Jakarta",
            "Jl. Kuningan Barat 1, Jakarta",
            "Jl. Senopati 23, Kebayoran Baru"
        };
        for (int i = 1; i <= 10; i++) {
            sellers.add(new Seller("Merchant #" + i, contacts[i - 1], addresses[i - 1]));
        }
    }

    public List<Seller> getSellers() { return sellers; }
    public Buyer getCurrentUser() { return currentUser; }
    public List<Payment> getPaymentHistory() { return Collections.unmodifiableList(paymentHistory); }

    public void addListener(TenderListener listener) {
        listeners.add(listener);
    }

    /**
     * Buyer posts a request.
     */
    public void postRequest(String query, String preferences, String buyerAddress) {
        currentUser.setAddress(buyerAddress);
        TenderRequest request = new TenderRequest(currentUser, query, preferences, buyerAddress);
        activeRequests.add(request);
        offersMap.put(request.getRequestId(), new ArrayList<>());
        
        System.out.println("New Request Posted: " + query);
        notifyNewRequest(request);
    }

    // Backward-compatible overload
    public void postRequest(String query, String preferences) {
        postRequest(query, preferences, currentUser.getAddress());
    }

    /**
     * Seller submits an offer.
     */
    public void submitOffer(String requestId, Offer offer) {
        List<Offer> offers = offersMap.get(requestId);
        if (offers != null) {
            offers.add(offer);
            System.out.println("New Offer Received: " + offer);
            notifyNewOffer(requestId, offer);
        }
    }
    
    /**
     * Get sorted offers (AI Ranking).
     */
    public List<Offer> getBestOffers(String requestId) {
         List<Offer> offers = offersMap.getOrDefault(requestId, new ArrayList<>());
         List<Offer> sorted = new ArrayList<>(offers);
         sorted.sort(Comparator.comparingDouble(Offer::getAiScore).reversed());
         return sorted;
    }

    /**
     * Buyer completes checkout — creates a Payment record.
     */
    public Payment checkout(List<Offer> selectedOffers, String buyerAddress) {
        if (selectedOffers == null || selectedOffers.isEmpty()) return null;
        currentUser.setAddress(buyerAddress);
        Payment payment = new Payment(currentUser, new ArrayList<>(selectedOffers), buyerAddress);
        paymentHistory.add(0, payment); // Most recent first
        notifyPaymentCompleted(payment);
        return payment;
    }

    /**
     * Smart order parser: "fruit tea 2 hot tea 3" → {fruit tea: 2, hot tea: 3}
     * Scans tokens; when a number is found, everything before it (since last number) is the product name.
     */
    public Map<String, Integer> parseOrderText(String text) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) return result;

        String[] tokens = text.trim().split("\\s+");
        List<String> nameTokens = new ArrayList<>();

        for (String token : tokens) {
            try {
                int qty = Integer.parseInt(token);
                if (!nameTokens.isEmpty()) {
                    String productName = String.join(" ", nameTokens).trim();
                    result.put(productName, qty);
                    nameTokens.clear();
                }
            } catch (NumberFormatException e) {
                nameTokens.add(token);
            }
        }
        // Remaining tokens with no trailing number → qty 1
        if (!nameTokens.isEmpty()) {
            result.put(String.join(" ", nameTokens).trim(), 1);
        }
        return result;
    }

    private void notifyNewRequest(TenderRequest request) {
        for (TenderListener listener : listeners) {
            listener.onNewRequest(request);
        }
    }

    private void notifyNewOffer(String requestId, Offer offer) {
        for (TenderListener listener : listeners) {
            listener.onNewOffer(requestId, offer);
        }
    }

    private void notifyPaymentCompleted(Payment payment) {
        for (TenderListener listener : listeners) {
            listener.onPaymentCompleted(payment);
        }
    }
}
