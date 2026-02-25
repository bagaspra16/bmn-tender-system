package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Product;
import com.bnm.tender.model.Seller;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;

public class SellerInputCard extends JPanel {
    private Seller seller;
    private JTextField productNameField;
    private JTextField priceField;
    private JSpinner ratingSpinner;
    private JSpinner quantitySpinner;
    private JButton submitOfferButton;
    private TenderRequest currentRequest;

    public SellerInputCard(Seller seller) {
        this.seller = seller;
        
        setLayout(new BorderLayout(5, 5));
        setBackground(StyleUtil.getSellerColor(seller.getName()));
        setBorder(StyleUtil.createRoundedBorder(Color.GRAY));

        // Header with "My info" (opens seller contact popup)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel header = new JLabel(seller.getName() + " " + StyleUtil.ICON_SELLER);
        StyleUtil.styleHeader(header);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(header, BorderLayout.CENTER);
        JButton myInfoBtn = new JButton("My info & Map");
        myInfoBtn.setFont(StyleUtil.FONT_SMALL);
        myInfoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        myInfoBtn.setFocusPainted(false);
        myInfoBtn.setBorderPainted(false);
        myInfoBtn.setContentAreaFilled(false);
        myInfoBtn.setForeground(new Color(0, 102, 204));
        myInfoBtn.addActionListener(e -> SellerContactDialog.showFor(this, seller));
        headerPanel.add(myInfoBtn, BorderLayout.EAST);
        JLabel contactLabel = new JLabel("📞 " + (seller.getContactId().isEmpty() ? "—" : seller.getContactId()));
        contactLabel.setFont(StyleUtil.FONT_SMALL);
        contactLabel.setForeground(Color.GRAY);
        contactLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JPanel headerNorth = new JPanel(new BorderLayout());
        headerNorth.setOpaque(false);
        headerNorth.add(headerPanel, BorderLayout.NORTH);
        headerNorth.add(contactLabel, BorderLayout.SOUTH);
        add(headerNorth, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10)); // More rows for quantity
        formPanel.setOpaque(false);

        JLabel lblProduct = new JLabel("Product:");
        lblProduct.setFont(StyleUtil.FONT_BODY);
        formPanel.add(lblProduct);
        
        productNameField = new JTextField();
        productNameField.setFont(StyleUtil.FONT_BODY);
        formPanel.add(productNameField);

        JLabel lblPrice = new JLabel("Price (Rp):");
        lblPrice.setFont(StyleUtil.FONT_BODY);
        formPanel.add(lblPrice);
        
        priceField = new JTextField();
        priceField.setFont(StyleUtil.FONT_BODY);
        priceField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { formatPrice(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { formatPrice(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { formatPrice(); }
            
            private void formatPrice() {
                SwingUtilities.invokeLater(() -> {
                    String text = priceField.getText();
                    if (text.isEmpty()) return;
                    String clean = text.replaceAll("[^\\d]", "");
                    if (clean.isEmpty()) return;
                    try {
                        long value = Long.parseLong(clean);
                        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMANY);
                        String formatted = formatter.format(value);
                        if (!text.equals(formatted)) priceField.setText(formatted);
                    } catch (NumberFormatException ex) {}
                });
            }
        });
        formPanel.add(priceField);
        
        JLabel lblRating = new JLabel("Rating:");
        lblRating.setFont(StyleUtil.FONT_BODY);
        formPanel.add(lblRating);
        
        ratingSpinner = new JSpinner(new SpinnerNumberModel(4.5, 0.0, 5.0, 0.1));
        ratingSpinner.setFont(StyleUtil.FONT_BODY);
        formPanel.add(ratingSpinner);

        JLabel lblQty = new JLabel("Quantity:");
        lblQty.setFont(StyleUtil.FONT_BODY);
        formPanel.add(lblQty);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setFont(StyleUtil.FONT_BODY);
        formPanel.add(quantitySpinner);

        // Center: only the input form so card height stays consistent
        add(formPanel, BorderLayout.CENTER);

        // Footer Action
        submitOfferButton = new JButton("Send Offer " + StyleUtil.ICON_SEND);
        Color btnColor = getBackground().darker();
        StyleUtil.styleActionButton(submitOfferButton, new Color(255, 255, 255, 200)); // Whiter button for contrast
        submitOfferButton.addActionListener(e -> submitOffer());
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        footer.add(submitOfferButton);
        add(footer, BorderLayout.SOUTH);
    }
    
    public void setRequest(TenderRequest request) {
        this.currentRequest = request;
        submitOfferButton.setEnabled(request != null);
        if (request == null) {
            productNameField.setText("");
            priceField.setText("");
        }
    }

    private void submitOffer() {
        if (currentRequest == null) {
            JOptionPane.showMessageDialog(this, "No active request selected!");
            return;
        }

        String name = productNameField.getText().trim();
        String priceStr = priceField.getText().trim();
        double rating = (Double) ratingSpinner.getValue();

        if (name.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in Product Name and Price!");
            return;
        }

        try {
            // Remove dots for parsing (100.000 -> 100000)
            String cleanPrice = priceStr.replaceAll("\\.", "");
            double price = Double.parseDouble(cleanPrice);
            
            int quantity = (Integer) quantitySpinner.getValue();
            
            Product product = new Product(name, name);
            Offer offer = new Offer(seller, product, price, quantity, rating, "", currentRequest);
            
            TenderController.getInstance().submitOffer(currentRequest.getRequestId(), offer);
            
            // Visual feedback
            submitOfferButton.setText("Sent! ✅");
            Timer t = new Timer(2000, e -> submitOfferButton.setText("Send Offer 🚀"));
            t.setRepeats(false);
            t.start();
            
            // Clear inputs
            productNameField.setText("");
            priceField.setText("");
            ratingSpinner.setValue(4.5);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Price!");
        }
    }
}
