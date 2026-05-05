package com.bnm.tender.db;

import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Product;
import com.bnm.tender.model.Seller;
import com.bnm.tender.model.TenderRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfferRepository {

    private final ProductRepository products = new ProductRepository();

    /**
     * Persist an offer. The DB trigger recomputes ai_score; we read it back so the
     * Java object's score matches what the database stored.
     * Returns the same Offer instance with id populated.
     */
    public Offer insert(Offer offer) {
        String productId = products.findOrCreate(
            offer.getSeller().getId(),
            offer.getProduct().getName(),
            offer.getProduct().getDescription()
        );

        String sql = "INSERT INTO offers (request_id, seller_id, product_id, price, quantity, rating, comment) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id, ai_score";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(offer.getRequest().getRequestId()));
            ps.setObject(2, UUID.fromString(offer.getSeller().getId()));
            ps.setObject(3, UUID.fromString(productId));
            ps.setDouble(4, offer.getPrice());
            ps.setInt(5, offer.getQuantity());
            ps.setDouble(6, offer.getRating());
            ps.setString(7, offer.getComment() == null ? "" : offer.getComment());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                offer.setId(rs.getString("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("OfferRepository.insert failed", e);
        }
        return offer;
    }

    /**
     * Load all offers for a request, sorted by ai_score DESC. Re-hydrates
     * the seller and product so UI can render seller name / product name.
     */
    public List<Offer> findByRequestSorted(TenderRequest request) {
        String sql =
            "SELECT o.id, o.price, o.quantity, o.rating, o.comment, o.ai_score, " +
            "       u.id AS seller_id, u.name AS seller_name, u.phone, u.address, " +
            "       p.name AS product_name, p.description AS product_desc " +
            "FROM offers o " +
            "JOIN users u    ON o.seller_id = u.id " +
            "LEFT JOIN products p ON o.product_id = p.id " +
            "WHERE o.request_id = ? " +
            "ORDER BY o.ai_score DESC";
        List<Offer> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(request.getRequestId()));
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
                    Offer o = new Offer(
                        seller, product,
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getDouble("rating"),
                        rs.getString("comment"),
                        request
                    );
                    o.setId(rs.getString("id"));
                    out.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("OfferRepository.findByRequestSorted failed", e);
        }
        return out;
    }
}
