package com.bnm.tender.view;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("BMN Tender System - Buyers are KING (Modern Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Full desktop window
        setLocationRelativeTo(null);

        // Main Layout: Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Tab 1: Marketplace (3-column layout)
        BuyerPanel buyerPanel = new BuyerPanel();
        RecommendationPanel recommendationPanel = new RecommendationPanel(buyerPanel);
        SellerPanel sellerPanel = new SellerPanel();

        // Buyer column
        JPanel buyerContainer = new JPanel(new BorderLayout());
        JLabel buyerHeader = new JLabel("  Buyer", JLabel.LEFT);
        buyerHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        buyerHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buyerContainer.add(buyerHeader, BorderLayout.NORTH);
        buyerContainer.add(buyerPanel, BorderLayout.CENTER);

        // Recommended column
        JPanel recContainer = new JPanel(new BorderLayout());
        JLabel recHeader = new JLabel("  Recommended for Buyer", JLabel.LEFT);
        recHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        recHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        recContainer.add(recHeader, BorderLayout.NORTH);
        recContainer.add(recommendationPanel, BorderLayout.CENTER);

        // Seller column
        JPanel sellerContainer = new JPanel(new BorderLayout());
        JLabel sellerHeader = new JLabel("  Seller Display", JLabel.LEFT);
        sellerHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        sellerHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sellerContainer.add(sellerHeader, BorderLayout.NORTH);
        sellerContainer.add(sellerPanel, BorderLayout.CENTER);

        // 3 resizable columns using nested split panes
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, recContainer, sellerContainer);
        centerSplit.setResizeWeight(0.5);
        centerSplit.setDividerSize(5);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buyerContainer, centerSplit);
        mainSplit.setResizeWeight(0.33);
        mainSplit.setDividerSize(5);

        tabbedPane.addTab("🛒 Marketplace", mainSplit);

        // Tab 2: Order History
        OrderHistoryPanel orderHistoryPanel = new OrderHistoryPanel();
        tabbedPane.addTab("📋 Order History", orderHistoryPanel);

        add(tabbedPane);
    }
}
