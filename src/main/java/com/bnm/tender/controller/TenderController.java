package com.bnm.tender.controller;

import com.bnm.tender.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

/**
 * Singleton Controller acting as the Mediator between Buyer and Sellers.
 */
public class TenderController {
    private static TenderController instance;
    private List<Seller> sellers;
    private List<TenderRequest> activeRequests;
    private Map<String, List<Offer>> offersMap; // RequestId -> List<Offer>

    // Listeners for UI updates
    public interface TenderListener {
        void onNewRequest(TenderRequest request);
        void onNewOffer(String requestId, Offer offer);
    }
    
    private List<TenderListener> listeners;
    private Buyer currentUser; // The singleton buyer for this session

    private TenderController() {
        sellers = new ArrayList<>();
        activeRequests = new ArrayList<>();
        offersMap = new HashMap<>();
        listeners = new ArrayList<>();
        currentUser = new Buyer("King Buyer 👑"); // Default user
        
        // Setup initial dummy sellers
        setupDummySellers();
    }

    public static TenderController getInstance() {
        if (instance == null) {
            instance = new TenderController();
        }
        return instance;
    }

    private void setupDummySellers() {
        for (int i = 1; i <= 10; i++) {
            sellers.add(new Seller("Merchant #" + i));
        }
    }

    public List<Seller> getSellers() {
        return sellers;
    }

    public void addListener(TenderListener listener) {
        listeners.add(listener);
    }

    /**
     * Buyer posts a request.
     */
    public void postRequest(String query, String preferences) {
        TenderRequest request = new TenderRequest(currentUser, query, preferences);
        activeRequests.add(request);
        offersMap.put(request.getRequestId(), new ArrayList<>());
        
        System.out.println("New Request Posted: " + query);
        notifyNewRequest(request);
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
         // sort by AI score descending
         Collections.sort(offers, Comparator.comparingDouble(Offer::getAiScore).reversed());
         return offers;
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
}
