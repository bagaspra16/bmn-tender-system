package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyerPanel extends JPanel implements TenderController.TenderListener {
    private JPanel chatContentPanel;
    private JScrollPane chatScrollPane;
    
    // Input Area
    private JTextField requestInput;
    private JButton sendButton;
    
    // Cart / Selected Items Area
    private JPanel cartPanel;
    private JLabel totalLabel;
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
        
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.setBackground(new Color(250, 250, 250));
        totalLabel = new JLabel("Total: Rp 0");
        totalLabel.setFont(StyleUtil.FONT_HEADER); // Effectively bold now
        totalPanel.add(totalLabel);
        
        JPanel cartWrapper = new JPanel(new BorderLayout());
        JLabel cartHeader = new JLabel("  " + StyleUtil.ICON_CART + " Your Selection:");
        StyleUtil.styleHeader(cartHeader);
        cartHeader.setFont(StyleUtil.FONT_BODY); // Keep it slightly smaller than main header
        cartWrapper.add(cartHeader, BorderLayout.NORTH);
        cartWrapper.add(cartPanel, BorderLayout.CENTER);
        cartWrapper.add(totalPanel, BorderLayout.SOUTH);
        
        // 2b. Input Area (Bottom of Bottom)
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        
        requestInput = new JTextField();
        requestInput.setFont(StyleUtil.FONT_BODY);
        requestInput.putClientProperty("JTextField.placeholderText", "Type your request here...");
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
        
        // Initial State: user starts fresh, no welcome message
        updateCartDisplay();
    }

    private void sendRequest() {
        String text = requestInput.getText().trim();
        if (!text.isEmpty()) {
            addUserMessage(text);
            TenderController.getInstance().postRequest(text, "");
            requestInput.setText("");
        }
    }

    private void addUserMessage(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 50)); // Indent right
        
        JLabel label = new JLabel("<html><body style='width: 250px; padding: 10px; background-color: #E3F2FD; border-radius: 10px; color: #333;'>" 
             + text + "</body></html>");
        label.setFont(StyleUtil.FONT_BODY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Add to Left
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

    @Override
    public void onNewRequest(TenderRequest request) {
        // Create a placeholder for offers on the RIGHT
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        
        JPanel offerContainer = new JPanel();
        offerContainer.setLayout(new BoxLayout(offerContainer, BoxLayout.Y_AXIS));
        offerContainer.setBackground(Color.WHITE);
        
        // Wrapper to align RIGHT
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
        
        // Add initial "Waiting" label
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
        container.removeAll();
        
        List<Offer> offers = TenderController.getInstance().getBestOffers(requestId);
        if (offers.isEmpty()) return;

        // Container bubble style
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(StyleUtil.BG_CREAM);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            StyleUtil.createRoundedBorder(StyleUtil.VIBRANT_MINT),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel header = new JLabel("Found " + offers.size() + " offers! 🍽️");
        header.setFont(StyleUtil.FONT_HEADER);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(header);
        bubble.add(Box.createVerticalStrut(10));

        // List offers with checkboxes
        List<JCheckBox> checkBoxes = new ArrayList<>();
        
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            JCheckBox checkBox = new JCheckBox();
            checkBox.setOpaque(false);
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            String prefix = "";
            String style = "font-family: sans-serif; font-size: 12px;"; // Reduced from 14px
            if (i == 0) {
                prefix = "👑 <b>KING'S CHOICE:</b> ";
                style += "background-color: #FFF8DC; border: 2px solid #FFD700; border-radius: 5px;"; 
            }
            
            checkBox.setText("<html><div style='" + style + " padding: 8px; width: 220px;'>" + prefix + 
                "<b>" + o.getProduct().getName() + "</b>" + 
                " <span style='color:gray; font-size: 10px;'>(" + o.getSeller().getName() + ")</span><br>" +
                StyleUtil.ICON_PRICE + " <b>Rp " + StyleUtil.formatRupiah(o.getPrice()) + "</b> | " + 
                StyleUtil.ICON_RATING + " <b>" + o.getRating() + "</b></div></html>");
            
            checkBox.putClientProperty("offer", o);
            checkBoxes.add(checkBox);
            bubble.add(checkBox);
            bubble.add(Box.createVerticalStrut(8));
        }
        
        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton chooseButton = new JButton("Add to Selection ✅");
        StyleUtil.styleActionButton(chooseButton, StyleUtil.VIBRANT_MINT);
        chooseButton.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                     addOfferToCart((Offer) cb.getClientProperty("offer"));
                }
            }
        });
        
        btnPanel.add(chooseButton);
        bubble.add(Box.createVerticalStrut(5));
        bubble.add(btnPanel);

        container.add(bubble);
        container.revalidate();
        container.repaint();
        scrollToBottom();
    }
    
    private void addOfferToCart(Offer offer) {
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
        } else {
            for (Offer o : selectedOffers) {
                total += o.getPrice();
                
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setOpaque(false);
                itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                
                JLabel nameLabel = new JLabel("• " + o.getProduct().getName() + " (" + o.getSeller().getName() + ")");
                nameLabel.setFont(StyleUtil.FONT_BODY);
                
                JLabel priceLabel = new JLabel("Rp " + StyleUtil.formatRupiah(o.getPrice()) + "   ");
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
        
        // Repaint
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
