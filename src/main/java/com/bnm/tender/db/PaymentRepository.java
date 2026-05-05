package com.bnm.tender.db;

import com.bnm.tender.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PaymentRepository {

    /**
     * Run the atomic checkout stored procedure, then re-hydrate a Payment object
     * from the freshly inserted rows so timestamps/totals match the DB.
     */
    public Payment checkout(Buyer buyer, List<Offer> selectedOffers, String buyerAddress, String requestId) {
        if (selectedOffers == null || selectedOffers.isEmpty()) return null;

        String paymentId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID[] offerIds = selectedOffers.stream()
            .map(o -> UUID.fromString(o.getId()))
            .toArray(UUID[]::new);

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (CallableStatement cs = c.prepareCall("CALL process_checkout(?, ?, ?, ?, ?)")) {
                cs.setString(1, paymentId);
                cs.setObject(2, UUID.fromString(buyer.getId()));
                if (requestId != null) {
                    cs.setObject(3, UUID.fromString(requestId));
                } else {
                    cs.setNull(3, Types.OTHER);
                }
                Array offerIdArray = c.createArrayOf("uuid", offerIds);
                cs.setArray(4, offerIdArray);
                cs.setString(5, buyerAddress);
                cs.execute();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentRepository.checkout failed", e);
        }

        return findById(paymentId);
    }

    public Payment findById(String paymentId) {
        String sql =
            "SELECT p.id, p.total_amount, p.buyer_address, p.created_at, " +
            "       u.id AS buyer_id, u.name AS buyer_name, u.address AS buyer_db_address " +
            "FROM payments p JOIN users u ON p.buyer_id = u.id " +
            "WHERE p.id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Buyer buyer = new Buyer(
                    rs.getString("buyer_id"),
                    rs.getString("buyer_name"),
                    rs.getString("buyer_db_address")
                );
                List<Offer> items = loadItems(c, paymentId);
                return new PersistedPayment(
                    paymentId, buyer, items,
                    rs.getString("buyer_address"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentRepository.findById failed", e);
        }
    }

    public List<Payment> findAllForBuyer(String buyerId) {
        String sql = "SELECT id FROM payments WHERE buyer_id = ? ORDER BY created_at DESC";
        List<Payment> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(buyerId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = findById(rs.getString("id"));
                    if (p != null) out.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentRepository.findAllForBuyer failed", e);
        }
        return out;
    }

    private List<Offer> loadItems(Connection c, String paymentId) throws SQLException {
        String sql =
            "SELECT pi.offer_id, pi.price, pi.quantity, " +
            "       o.rating, o.comment, o.request_id, " +
            "       u.id AS seller_id, u.name AS seller_name, u.phone, u.address, " +
            "       p.name AS product_name, p.description AS product_desc, " +
            "       tr.query, tr.preferences, tr.buyer_address AS req_addr, " +
            "       bu.id AS req_buyer_id, bu.name AS req_buyer_name, bu.address AS req_buyer_addr " +
            "FROM payment_items pi " +
            "JOIN offers o   ON pi.offer_id = o.id " +
            "JOIN users  u   ON o.seller_id = u.id " +
            "LEFT JOIN products p ON o.product_id = p.id " +
            "LEFT JOIN tender_requests tr ON o.request_id = tr.id " +
            "LEFT JOIN users bu ON tr.buyer_id = bu.id " +
            "WHERE pi.payment_id = ?";
        List<Offer> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seller seller = new Seller(
                        rs.getString("seller_id"),
                        rs.getString("seller_name"),
                        rs.getString("phone"),
                        rs.getString("address")
                    );
                    String pname = rs.getString("product_name");
                    Product product = new Product(
                        pname == null ? "Unknown" : pname,
                        rs.getString("product_desc")
                    );
                    TenderRequest req = null;
                    String reqBuyerId = rs.getString("req_buyer_id");
                    if (reqBuyerId != null) {
                        Buyer rb = new Buyer(reqBuyerId, rs.getString("req_buyer_name"), rs.getString("req_buyer_addr"));
                        req = new TenderRequest(
                            rs.getString("request_id"), rb,
                            rs.getString("query"), rs.getString("preferences"),
                            rs.getString("req_addr")
                        );
                    }
                    Offer offer = new Offer(
                        seller, product,
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getDouble("rating"),
                        rs.getString("comment"),
                        req
                    );
                    offer.setId(rs.getString("offer_id"));
                    out.add(offer);
                }
            }
        }
        return out;
    }

    /** Payment subclass that carries DB-assigned id + timestamp instead of fresh ones. */
    public static class PersistedPayment extends Payment {
        private final String dbId;
        private final LocalDateTime dbTimestamp;

        public PersistedPayment(String id, Buyer buyer, List<Offer> items, String buyerAddress, LocalDateTime ts) {
            super(buyer, items, buyerAddress);
            this.dbId = id;
            this.dbTimestamp = ts;
        }
        @Override public String getId() { return dbId; }
        @Override public LocalDateTime getTimestamp() { return dbTimestamp; }
    }
}
