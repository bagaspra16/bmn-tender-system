package com.bnm.tender.controller;

import com.bnm.tender.db.*;
import com.bnm.tender.model.*;
import java.util.*;

/**
 * Singleton Controller acting as the Mediator between Buyer and Sellers.
 * Backed by Supabase / PostgreSQL via the repository classes in com.bnm.tender.db.
 */
public class TenderController {
    private static TenderController instance;

    private final UserRepository userRepo = new UserRepository();
    private final TenderRequestRepository requestRepo = new TenderRequestRepository();
    private final OfferRepository offerRepo = new OfferRepository();
    private final PaymentRepository paymentRepo = new PaymentRepository();

    private List<Seller> sellers;
    private final Map<String, TenderRequest> activeRequests = new LinkedHashMap<>();
    private Buyer currentUser;
    private final List<Payment> paymentHistory = new ArrayList<>();

    public interface TenderListener {
        void onNewRequest(TenderRequest request);
        void onNewOffer(String requestId, Offer offer);
        default void onPaymentCompleted(Payment payment) {}
    }

    private final List<TenderListener> listeners = new ArrayList<>();

    private TenderController() {
        Database.init();
        this.sellers = userRepo.findAllSellers();
        this.currentUser = userRepo.findOrCreateDefaultBuyer();
        this.paymentHistory.addAll(paymentRepo.findAllForBuyer(currentUser.getId()));
    }

    public static TenderController getInstance() {
        if (instance == null) {
            instance = new TenderController();
        }
        return instance;
    }

    public List<Seller> getSellers() { return sellers; }
    public Buyer getCurrentUser() { return currentUser; }
    public List<Payment> getPaymentHistory() { return Collections.unmodifiableList(paymentHistory); }

    public void addListener(TenderListener listener) {
        listeners.add(listener);
    }

    /** Buyer posts a request — persisted to tender_requests. */
    public void postRequest(String query, String preferences, String buyerAddress) {
        currentUser.setAddress(buyerAddress);
        userRepo.updateBuyerAddress(currentUser.getId(), buyerAddress);

        TenderRequest request = requestRepo.insert(currentUser, query, preferences, buyerAddress);
        activeRequests.put(request.getRequestId(), request);

        System.out.println("New Request Posted: " + query + " (id=" + request.getRequestId() + ")");
        notifyNewRequest(request);
    }

    public void postRequest(String query, String preferences) {
        postRequest(query, preferences, currentUser.getAddress());
    }

    /** Seller submits an offer — persisted to offers (ai_score auto-computed by trigger). */
    public void submitOffer(String requestId, Offer offer) {
        TenderRequest req = activeRequests.get(requestId);
        if (req == null) {
            System.err.println("submitOffer: unknown requestId " + requestId);
            return;
        }
        // The incoming Offer was constructed with the request's UI-side reference.
        // Make sure the offer carries the same TenderRequest instance we tracked.
        Offer toPersist = (offer.getRequest() != null && offer.getRequest().getRequestId().equals(requestId))
            ? offer
            : new Offer(offer.getSeller(), offer.getProduct(), offer.getPrice(),
                        offer.getQuantity(), offer.getRating(), offer.getComment(), req);

        Offer saved = offerRepo.insert(toPersist);
        System.out.println("New Offer Received: " + saved + " (id=" + saved.getId() + ")");
        notifyNewOffer(requestId, saved);
    }

    /** Best offers for a request — sorted at the database (ORDER BY ai_score DESC). */
    public List<Offer> getBestOffers(String requestId) {
        TenderRequest req = activeRequests.get(requestId);
        if (req == null) return new ArrayList<>();
        return offerRepo.findByRequestSorted(req);
    }

    /** Offers on this request submitted by sellers OTHER than the given one. */
    public List<Offer> getCompetingOffers(String requestId, String excludeSellerId) {
        List<Offer> all = getBestOffers(requestId);
        List<Offer> out = new ArrayList<>();
        for (Offer o : all) {
            if (excludeSellerId == null || !excludeSellerId.equals(o.getSeller().getId())) {
                out.add(o);
            }
        }
        return out;
    }

    public TenderRequest getRequest(String requestId) {
        return activeRequests.get(requestId);
    }

    /** Buyer completes checkout — atomic via the process_checkout stored procedure. */
    public Payment checkout(List<Offer> selectedOffers, String buyerAddress) {
        if (selectedOffers == null || selectedOffers.isEmpty()) return null;

        currentUser.setAddress(buyerAddress);
        userRepo.updateBuyerAddress(currentUser.getId(), buyerAddress);

        String requestId = null;
        if (selectedOffers.get(0).getRequest() != null) {
            requestId = selectedOffers.get(0).getRequest().getRequestId();
        }

        Payment payment = paymentRepo.checkout(currentUser, selectedOffers, buyerAddress, requestId);
        if (payment != null) {
            paymentHistory.add(0, payment);
            if (requestId != null) activeRequests.remove(requestId);
            notifyPaymentCompleted(payment);
        }
        return payment;
    }

    /**
     * Smart order parser: "fruit tea 2 hot tea 3" → {fruit tea: 2, hot tea: 3}
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
        if (!nameTokens.isEmpty()) {
            result.put(String.join(" ", nameTokens).trim(), 1);
        }
        return result;
    }

    private void notifyNewRequest(TenderRequest request) {
        for (TenderListener l : listeners) l.onNewRequest(request);
    }
    private void notifyNewOffer(String requestId, Offer offer) {
        for (TenderListener l : listeners) l.onNewOffer(requestId, offer);
    }
    private void notifyPaymentCompleted(Payment payment) {
        for (TenderListener l : listeners) l.onPaymentCompleted(payment);
    }
}
