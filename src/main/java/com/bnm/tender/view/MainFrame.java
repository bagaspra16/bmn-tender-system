package com.bnm.tender.view;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("BMN Tender System - Buyers are KING (Modern Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);
        
        // Main Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5); // 50/50 split
        splitPane.setDividerSize(5); 
        
        BuyerPanel buyerPanel = new BuyerPanel();
        SellerPanel sellerPanel = new SellerPanel();
        
        // Wrap in panels with titles for clarity
        JPanel buyerContainer = new JPanel(new BorderLayout());
        JLabel buyerHeader = new JLabel("  Buyer Dashboard (King) 👑", JLabel.LEFT);
        buyerHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        buyerHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buyerContainer.add(buyerHeader, BorderLayout.NORTH);
        buyerContainer.add(buyerPanel, BorderLayout.CENTER);
        
        JPanel sellerContainer = new JPanel(new BorderLayout());
        JLabel sellerHeader = new JLabel("  Seller Dashboard (Marketplace) 🏪", JLabel.LEFT);
        sellerHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        sellerHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sellerContainer.add(sellerHeader, BorderLayout.NORTH);
        sellerContainer.add(sellerPanel, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(buyerContainer);
        splitPane.setRightComponent(sellerContainer);
        
        add(splitPane);
    }
}
