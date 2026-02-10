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
    private JButton submitOfferButton;
    private TenderRequest currentRequest;

    public SellerInputCard(Seller seller) {
        this.seller = seller;
        
        setLayout(new BorderLayout(5, 5));
        setBackground(StyleUtil.getSellerColor(seller.getName()));
        setBorder(StyleUtil.createRoundedBorder(Color.GRAY));

        // Header
        JLabel header = new JLabel(seller.getName() + " " + StyleUtil.ICON_SELLER);
        StyleUtil.styleHeader(header);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(header, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10)); // More spacing
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
        // Optionally clear fields or keep them? Keeping them might be easier for bulk entry.
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
            
            Product product = new Product(name, name);
            Offer offer = new Offer(seller, product, price, rating, "", currentRequest);
            
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
