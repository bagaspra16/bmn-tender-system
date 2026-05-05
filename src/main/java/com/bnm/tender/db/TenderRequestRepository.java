package com.bnm.tender.db;

import com.bnm.tender.model.Buyer;
import com.bnm.tender.model.TenderRequest;

import java.sql.*;

public class TenderRequestRepository {

    /** Inserts a new tender_request and returns it with the DB-generated UUID. */
    public TenderRequest insert(Buyer buyer, String query, String preferences, String buyerAddress) {
        String sql = "INSERT INTO tender_requests (buyer_id, query, preferences, buyer_address) " +
                     "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(buyer.getId()));
            ps.setString(2, query);
            ps.setString(3, preferences);
            ps.setString(4, buyerAddress);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                String id = rs.getString("id");
                return new TenderRequest(id, buyer, query, preferences, buyerAddress);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TenderRequestRepository.insert failed", e);
        }
    }

    public void close(String requestId) {
        String sql = "UPDATE tender_requests SET status = 'CLOSED' WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(requestId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("TenderRequestRepository.close failed", e);
        }
    }
}
