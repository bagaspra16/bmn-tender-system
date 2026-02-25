package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Payment;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class OrderHistoryPanel extends JPanel implements TenderController.TenderListener {
    private JPanel listPanel;
    private JScrollPane scrollPane;

    public OrderHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(StyleUtil.BG_CREAM);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Registry
        TenderController.getInstance().addListener(this);

        // Header
        JLabel title = new JLabel("Your Order History 📜");
        title.setFont(StyleUtil.FONT_HEADER);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // List Area
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(StyleUtil.BG_CREAM);

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        
        refreshList();
    }

    private void refreshList() {
        listPanel.removeAll();
        List<Payment> history = TenderController.getInstance().getPaymentHistory();

        if (history.isEmpty()) {
            JLabel empty = new JLabel("No orders yet. Go buy some delicious food! 🍜");
            empty.setFont(StyleUtil.FONT_BODY);
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(50));
            listPanel.add(empty);
        } else {
            for (Payment p : history) {
                listPanel.add(createPaymentCard(p));
                listPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createPaymentCard(Payment p) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            StyleUtil.createRoundedBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(800, 200)); 
        // Note: BoxLayout respects maximum size height, but width needs to be managed if inside simple container

        // Header: Order ID + Date + Total
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel idLabel = new JLabel("Order #" + p.getId());
        idLabel.setFont(StyleUtil.FONT_BODY);
        
        JLabel dateLabel = new JLabel(p.getFormattedTimestamp());
        dateLabel.setFont(StyleUtil.FONT_SMALL);
        dateLabel.setForeground(Color.GRAY);
        
        JPanel leftHeader = new JPanel(new GridLayout(2, 1));
        leftHeader.setOpaque(false);
        leftHeader.add(idLabel);
        leftHeader.add(dateLabel);

        JLabel totalLabel = new JLabel("Rp " + StyleUtil.formatRupiah(p.getTotalAmount()));
        totalLabel.setFont(StyleUtil.FONT_HEADER);
        totalLabel.setForeground(new Color(0, 128, 0)); // Green
        
        header.add(leftHeader, BorderLayout.WEST);
        header.add(totalLabel, BorderLayout.EAST);
        
        // Content: Items
        JPanel itemsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        for (Offer item : p.getItems()) {
            JPanel itemRow = new JPanel(new BorderLayout());
            itemRow.setOpaque(false);
            JLabel itemLabel = new JLabel("• " + item.getProduct().getName() + " (x" + item.getQuantity() + ") - " + item.getSeller().getName());
            itemLabel.setFont(StyleUtil.FONT_BODY);
            itemRow.add(itemLabel, BorderLayout.CENTER);
            JButton contactBtn = new JButton("Contact seller");
            contactBtn.setFont(StyleUtil.FONT_SMALL);
            contactBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            contactBtn.setFocusPainted(false);
            contactBtn.setBorderPainted(false);
            contactBtn.setContentAreaFilled(false);
            contactBtn.setForeground(new Color(0, 102, 204));
            contactBtn.addActionListener(e -> SellerContactDialog.showFor(OrderHistoryPanel.this, item.getSeller()));
            itemRow.add(contactBtn, BorderLayout.EAST);
            itemsPanel.add(itemRow);
        }
        
        // Footer: Address
        JLabel addrLabel = new JLabel("📍 Delivered to: " + p.getBuyerAddress());
        addrLabel.setFont(StyleUtil.FONT_SMALL);
        addrLabel.setForeground(Color.GRAY);
        
        card.add(header, BorderLayout.NORTH);
        card.add(itemsPanel, BorderLayout.CENTER);
        card.add(addrLabel, BorderLayout.SOUTH);
        
        return card;
    }

    @Override
    public void onPaymentCompleted(Payment payment) {
        refreshList();
    }

    @Override
    public void onNewRequest(TenderRequest request) {}

    @Override
    public void onNewOffer(String requestId, Offer offer) {}
}
