package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Payment;
import com.bnm.tender.model.Seller;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuyerPanel extends JPanel implements TenderController.TenderListener {
    private JPanel chatContentPanel;
    private JScrollPane chatScrollPane;
    
    // Input Area
    private JTextField requestInput;
    private JButton sendButton;
    private JTextField addressInput; // New Address Field
    
    // Cart / Selected Items Area
    private JPanel cartPanel;
    private JLabel totalLabel;
    private JButton checkoutButton; // New Checkout Button
    private List<Offer> selectedOffers;
    
    // Map request ID to a container panel so we can update it with offers later
    private Map<String, JPanel> requestOfferPanels;

    public BuyerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Registry
        TenderController.getInstance().addListener(this);
        requestOfferPanels = new HashMap<>();
        selectedOffers = new ArrayList<>();

        // 1. Chat Area (Center)
        chatContentPanel = new JPanel();
        chatContentPanel.setLayout(new BoxLayout(chatContentPanel, BoxLayout.Y_AXIS));
        chatContentPanel.setBackground(Color.WHITE);
        
        chatScrollPane = new JScrollPane(chatContentPanel);
        chatScrollPane.setBorder(null);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.getViewport().setBackground(Color.WHITE);

        // 2. Bottom Container (Cart + Input)
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(Color.WHITE);
        bottomContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        
        // 2a. Cart Panel (Top of Bottom)
        cartPanel = new JPanel();
        cartPanel.setLayout(new BoxLayout(cartPanel, BoxLayout.Y_AXIS));
        cartPanel.setBackground(new Color(250, 250, 250));
        cartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(250, 250, 250));
        
        // Address Field in Cart Area
        JPanel addressPanel = new JPanel(new BorderLayout(5, 0));
        addressPanel.setOpaque(false);
        addressPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel addrLabel = new JLabel("📍 Address:");
        addrLabel.setFont(StyleUtil.FONT_SMALL);
        addressInput = new JTextField(TenderController.getInstance().getCurrentUser().getAddress());
        addressInput.setFont(StyleUtil.FONT_BODY);
        addressInput.putClientProperty("JTextField.placeholderText", "Enter delivery address... (e.g. Room 101)");
        addressPanel.add(addrLabel, BorderLayout.WEST);
        addressPanel.add(addressInput, BorderLayout.CENTER);
        
        // Total & Checkout
        JPanel checkoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        checkoutPanel.setOpaque(false);
        
        totalLabel = new JLabel("Total: Rp 0");
        totalLabel.setFont(StyleUtil.FONT_HEADER);
        
        checkoutButton = new JButton("Pay Now 💳");
        StyleUtil.styleActionButton(checkoutButton, StyleUtil.VIBRANT_TEAL);
        checkoutButton.addActionListener(e -> performCheckout());
        
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(Box.createHorizontalStrut(10));
        checkoutPanel.add(checkoutButton);
        
        totalPanel.add(addressPanel, BorderLayout.NORTH);
        totalPanel.add(checkoutPanel, BorderLayout.EAST);
        
        JPanel cartWrapper = new JPanel(new BorderLayout());
        JLabel cartHeader = new JLabel("  " + StyleUtil.ICON_CART + " Your Selection:");
        StyleUtil.styleHeader(cartHeader);
        cartHeader.setFont(StyleUtil.FONT_BODY); 
        cartWrapper.add(cartHeader, BorderLayout.NORTH);
        cartWrapper.add(cartPanel, BorderLayout.CENTER);
        cartWrapper.add(totalPanel, BorderLayout.SOUTH);
        
        // 2b. Input Area (Bottom of Bottom)
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        
        requestInput = new JTextField();
        requestInput.setFont(StyleUtil.FONT_BODY);
        requestInput.putClientProperty("JTextField.placeholderText", "Type request (e.g. 'fruit tea 2 hot tea 3')...");
        requestInput.addActionListener(e -> sendRequest());
        
        sendButton = new JButton("Post Request " + StyleUtil.ICON_SEND);
        StyleUtil.styleActionButton(sendButton, StyleUtil.VIBRANT_ORANGE);
        sendButton.addActionListener(e -> sendRequest());
        
        inputPanel.add(requestInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        bottomContainer.add(cartWrapper, BorderLayout.CENTER);
        bottomContainer.add(inputPanel, BorderLayout.SOUTH);

        add(chatScrollPane, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);
        
        updateCartDisplay();
    }

    private void sendRequest() {
        String text = requestInput.getText().trim();
        if (!text.isEmpty()) {
            addUserMessage(text);
            
            // Smart Parsing check
            Map<String, Integer> parsed = TenderController.getInstance().parseOrderText(text);
            if (!parsed.isEmpty()) {
                StringBuilder details = new StringBuilder("<b>Parsed Order:</b><br>");
                for (Map.Entry<String, Integer> entry : parsed.entrySet()) {
                    details.append("• ").append(entry.getKey()).append(" x").append(entry.getValue()).append("<br>");
                }
                addSystemMessage(details.toString());
            }

            TenderController.getInstance().postRequest(text, "", addressInput.getText());
            requestInput.setText("");
        }
    }
    
    private void performCheckout() {
        if (selectedOffers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }
        String address = addressInput.getText().trim();
        if (address.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a delivery address!");
            return;
        }
        
        Payment payment = TenderController.getInstance().checkout(selectedOffers, address);
        if (payment != null) {
            String msg = "<b>Payment Successful! 💸</b><br>" + 
                         "Order ID: " + payment.getId() + "<br>" +
                         "Total: Rp " + StyleUtil.formatRupiah(payment.getTotalAmount()) + "<br>" +
                         "Delivering to: " + payment.getBuyerAddress();
            addSystemMessage(msg);
            
            selectedOffers.clear();
            updateCartDisplay();
            JOptionPane.showMessageDialog(this, "Payment Complete! Order #" + payment.getId());
        }
    }

    private void addUserMessage(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 50)); 
        
        JLabel label = new JLabel("<html><body style='width: 250px; padding: 10px; background-color: #E3F2FD; border-radius: 10px; color: #333;'>" 
             + text + "</body></html>");
        label.setFont(StyleUtil.FONT_BODY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        
        JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bubbleWrapper.setBackground(Color.WHITE);
        bubbleWrapper.add(label);
        
        row.add(bubbleWrapper, BorderLayout.WEST);
        
        chatContentPanel.add(row);
        chatContentPanel.add(Box.createVerticalStrut(10));
        
        chatContentPanel.revalidate();
        chatContentPanel.repaint();
        scrollToBottom();
    }
    
    private void addSystemMessage(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(5, 50, 5, 5)); // Indent left
        
        JLabel label = new JLabel("<html><div style='width: 250px; padding: 10px; background-color: #F0F4C3; border: 1px solid #CDDC39; border-radius: 10px; color: #333;'>" 
             + text + "</div></html>");
        label.setFont(StyleUtil.FONT_SMALL);
        
        JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bubbleWrapper.setBackground(Color.WHITE);
        bubbleWrapper.add(label);
        
        row.add(bubbleWrapper, BorderLayout.EAST);
        
        chatContentPanel.add(row);
        chatContentPanel.add(Box.createVerticalStrut(10));
        chatContentPanel.revalidate();
        chatContentPanel.repaint();
        scrollToBottom();
    }

    @Override
    public void onNewRequest(TenderRequest request) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        
        JPanel offerContainer = new JPanel();
        offerContainer.setLayout(new BoxLayout(offerContainer, BoxLayout.Y_AXIS));
        offerContainer.setBackground(Color.WHITE);
        
        JPanel alignRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        alignRight.setBackground(Color.WHITE);
        alignRight.add(offerContainer);
        
        row.add(alignRight, BorderLayout.EAST);
        
        requestOfferPanels.put(request.getRequestId(), offerContainer);
        chatContentPanel.add(row);
        chatContentPanel.add(Box.createVerticalStrut(10));
        
        chatContentPanel.revalidate();
        chatContentPanel.repaint();
        scrollToBottom();
        
        JLabel waitingLabel = new JLabel("Searching for offers... 🔎");
        waitingLabel.setFont(StyleUtil.FONT_SMALL);
        waitingLabel.setForeground(Color.GRAY);
        offerContainer.add(waitingLabel);
    }

    @Override
    public void onNewOffer(String requestId, Offer offer) {
        JPanel container = requestOfferPanels.get(requestId);
        if (container != null) {
            updateOfferBubble(container, requestId);
        }
    }
    
    private void updateOfferBubble(JPanel container, String requestId) {
        // Now we only show a simple message in the chat that
        // recommendations are available in the middle panel.
        container.removeAll();
        JLabel info = new JLabel("<html><div style='width:240px; font-size:11px; color:#555;'>Offers found. Please see the middle window <b>\"Recommended for Buyer\"</b> to choose your package.</div></html>");
        info.setFont(StyleUtil.FONT_SMALL);
        container.add(info);
        container.revalidate();
        container.repaint();
        scrollToBottom();
    }
    
    public void addOfferToCart(Offer offer) {
        selectedOffers.add(offer);
        updateCartDisplay();
    }
    
    private void removeOfferFromCart(Offer offer) {
        selectedOffers.remove(offer);
        updateCartDisplay();
    }
    
    private void updateCartDisplay() {
        cartPanel.removeAll();
        double total = 0;
        
        if (selectedOffers.isEmpty()) {
            JLabel emptyLabel = new JLabel("Cart is empty");
            emptyLabel.setFont(StyleUtil.FONT_SMALL);
            emptyLabel.setForeground(Color.GRAY);
            cartPanel.add(emptyLabel);
            checkoutButton.setEnabled(false);
        } else {
            checkoutButton.setEnabled(true);
            for (Offer o : selectedOffers) {
                total += o.getTotalPrice();
                
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setOpaque(false);
                itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                
                JLabel nameLabel = new JLabel("• " + o.getProduct().getName() + " (x" + o.getQuantity() + ")");
                nameLabel.setFont(StyleUtil.FONT_BODY);
                
                JLabel priceLabel = new JLabel("Rp " + StyleUtil.formatRupiah(o.getTotalPrice()) + "   ");
                priceLabel.setFont(StyleUtil.FONT_BODY);
                
                JButton removeBtn = new JButton("X");
                removeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
                removeBtn.setForeground(Color.WHITE);
                removeBtn.setBackground(new Color(220, 53, 69)); // Red
                removeBtn.setBorderPainted(false);
                removeBtn.setFocusPainted(false);
                removeBtn.setOpaque(true);
                removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                removeBtn.setPreferredSize(new Dimension(25, 25));
                removeBtn.addActionListener(e -> removeOfferFromCart(o));
                
                JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                rightPanel.setOpaque(false);
                rightPanel.add(priceLabel);
                rightPanel.add(removeBtn);
                
                itemPanel.add(nameLabel, BorderLayout.CENTER);
                itemPanel.add(rightPanel, BorderLayout.EAST);
                
                cartPanel.add(itemPanel);
            }
        }
        
        totalLabel.setText("Total: Rp " + StyleUtil.formatRupiah(total));
        
        cartPanel.revalidate();
        cartPanel.repaint();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}
