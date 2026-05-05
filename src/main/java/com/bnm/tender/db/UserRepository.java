package com.bnm.tender.db;

import com.bnm.tender.model.Buyer;
import com.bnm.tender.model.Seller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public List<Seller> findAllSellers() {
        String sql = "SELECT id, name, phone, address FROM users WHERE role = 'SELLER' ORDER BY name";
        List<Seller> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Seller(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("address")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllSellers failed", e);
        }
        return out;
    }

    /** Returns the first BUYER row, creating "King Buyer 👑" if none exists. */
    public Buyer findOrCreateDefaultBuyer() {
        String sel = "SELECT id, name, address FROM users WHERE role = 'BUYER' ORDER BY created_at LIMIT 1";
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Buyer(rs.getString("id"), rs.getString("name"), rs.getString("address"));
                }
            }
            String ins = "INSERT INTO users (role, name, address) VALUES ('BUYER', ?, '') RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, "King Buyer 👑");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Buyer(rs.getString("id"), "King Buyer 👑", "");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findOrCreateDefaultBuyer failed", e);
        }
    }

    public void updateBuyerAddress(String buyerId, String address) {
        String sql = "UPDATE users SET address = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, address == null ? "" : address);
            ps.setObject(2, java.util.UUID.fromString(buyerId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateBuyerAddress failed", e);
        }
    }
}
