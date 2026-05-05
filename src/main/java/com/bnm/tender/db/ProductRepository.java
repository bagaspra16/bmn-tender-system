package com.bnm.tender.db;

import java.sql.*;

public class ProductRepository {

    /**
     * Find a product owned by a seller by name, or insert it.
     * Returns the product UUID (as string).
     */
    public String findOrCreate(String sellerId, String name, String description) {
        String sel = "SELECT id FROM products WHERE seller_id = ? AND name = ? LIMIT 1";
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setObject(1, java.util.UUID.fromString(sellerId));
                ps.setString(2, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("id");
                }
            }
            String ins = "INSERT INTO products (seller_id, name, description) VALUES (?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setObject(1, java.util.UUID.fromString(sellerId));
                ps.setString(2, name);
                ps.setString(3, description == null ? name : description);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getString("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ProductRepository.findOrCreate failed", e);
        }
    }
}
