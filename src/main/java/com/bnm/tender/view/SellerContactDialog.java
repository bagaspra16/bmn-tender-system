package com.bnm.tender.view;

import com.bnm.tender.model.Seller;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Popup dialog showing a specific seller's contact info and map integration.
 * Explains seller contact and includes "Open in Maps" (opens default browser)
 * and optional WhatsApp link for instant messaging.
 */
public class SellerContactDialog extends JDialog {
    private final Seller seller;
    private JLabel mapPreviewLabel;

    public SellerContactDialog(Frame owner, Seller seller) {
        super(owner, "Seller Contact — " + seller.getName(), true);
        this.seller = seller;
        setLayout(new BorderLayout(15, 15));
        setSize(720, 520); // Bigger popup for clearer info + map
        setLocationRelativeTo(owner);
        getContentPane().setBackground(StyleUtil.BG_CREAM);
        setResizable(true);
        buildUI();
    }

    public static void showFor(Component parent, Seller seller) {
        Frame frame = parent instanceof Frame ? (Frame) parent : (Frame) SwingUtilities.getWindowAncestor(parent);
        SellerContactDialog d = new SellerContactDialog(frame, seller);
        d.setVisible(true);
    }

    private void buildUI() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(StyleUtil.BG_CREAM);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Seller Contact & Info");
        header.setFont(StyleUtil.FONT_HEADER);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(header);
        main.add(Box.createVerticalStrut(8));

        JLabel explain = new JLabel("<html><div style='width:340px; color:#555;'>Contact this seller for questions, custom orders, or delivery details. Use the map to see their location.</div></html>");
        explain.setFont(StyleUtil.FONT_SMALL);
        explain.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(explain);
        main.add(Box.createVerticalStrut(16));

        // Seller info card
        JPanel infoCard = new JPanel(new GridLayout(0, 1, 6, 6));
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
            StyleUtil.createRoundedBorder(StyleUtil.VIBRANT_MINT),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        addInfoRow(infoCard, "Name", seller.getName());
        addInfoRow(infoCard, "Seller ID", seller.getId().length() > 12 ? seller.getId().substring(0, 12) + "…" : seller.getId());
        addInfoRow(infoCard, "Phone / WA", seller.getContactId().isEmpty() ? "—" : seller.getContactId());
        addInfoRow(infoCard, "Address", seller.getAddress().isEmpty() ? "—" : seller.getAddress());

        main.add(infoCard);
        main.add(Box.createVerticalStrut(20));

        // Map integration title
        JLabel mapLabel = new JLabel("Map integration (live preview)");
        mapLabel.setFont(StyleUtil.FONT_BODY);
        mapLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(mapLabel);
        main.add(Box.createVerticalStrut(8));

        // Map preview panel - shows a real static map image from an online map API
        mapPreviewLabel = new JLabel("Loading map preview...", SwingConstants.CENTER);
        mapPreviewLabel.setOpaque(true);
        mapPreviewLabel.setBackground(Color.WHITE);
        mapPreviewLabel.setBorder(BorderFactory.createCompoundBorder(
            StyleUtil.createRoundedBorder(StyleUtil.VIBRANT_BLUE),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mapPreviewLabel.setPreferredSize(new Dimension(420, 260));
        mapPreviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(mapPreviewLabel);
        main.add(Box.createVerticalStrut(10));

        // Map + messaging buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setBackground(StyleUtil.BG_CREAM);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton openMapBtn = new JButton("Open full map");
        StyleUtil.styleActionButton(openMapBtn, StyleUtil.VIBRANT_TEAL);
        openMapBtn.addActionListener(e -> openInMaps());
        btnPanel.add(openMapBtn);

        if (!seller.getContactId().isEmpty()) {
            JButton waBtn = new JButton("WhatsApp chat");
            StyleUtil.styleActionButton(waBtn, new Color(37, 211, 102));
            waBtn.addActionListener(e -> openWhatsApp());
            btnPanel.add(waBtn);
        }

        main.add(btnPanel);
        main.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // Load real map preview asynchronously
        loadMapPreview();

        // Close
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBackground(StyleUtil.BG_CREAM);
        JButton closeBtn = new JButton("Close");
        StyleUtil.styleActionButton(closeBtn, StyleUtil.VIBRANT_ORANGE);
        closeBtn.addActionListener(e -> setVisible(false));
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);
    }

    private void addInfoRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel l = new JLabel(label + ":");
        l.setFont(StyleUtil.FONT_SMALL);
        l.setForeground(Color.GRAY);
        JLabel v = new JLabel(value);
        v.setFont(StyleUtil.FONT_BODY);
        row.add(l, BorderLayout.NORTH);
        row.add(v, BorderLayout.CENTER);
        parent.add(row);
    }

    /**
     * Loads a static map image from an online map API (OpenStreetMap static map service)
     * and shows it inside the dialog as a real map preview.
     */
    private void loadMapPreview() {
        String location = seller.getAddress();
        if (location == null || location.trim().isEmpty()) {
            location = "Jakarta, Indonesia";
        }
        final String query = location.trim();

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    // Use OpenStreetMap static map service.
                    // Note: center uses plain text query; service will try to resolve it.
                    String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                    String urlStr = "https://staticmap.openstreetmap.de/staticmap.php?center="
                        + encoded + "&zoom=15&size=420x260&maptype=mapnik&markers=" + encoded + ",red-pushpin";
                    URL url = new URL(urlStr);
                    ImageIcon icon = new ImageIcon(url);
                    if (icon.getIconWidth() <= 0) {
                        throw new IllegalStateException("Empty icon");
                    }
                    return icon;
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        mapPreviewLabel.setText(null);
                        mapPreviewLabel.setIcon(icon);
                    } else {
                        mapPreviewLabel.setText("Map preview not available for this address.");
                    }
                } catch (Exception e) {
                    mapPreviewLabel.setText("Map preview not available.");
                }
            }
        };
        worker.execute();
    }

    private void openInMaps() {
        String location = seller.getAddress();
        if (location == null || location.trim().isEmpty()) {
            location = "Indonesia";
        }
        try {
            String encoded = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
            URI uri = URI.create("https://www.google.com/maps/search/?api=1&query=" + encoded);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                JOptionPane.showMessageDialog(this, "Open this URL in your browser:\n" + uri.toString());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open maps: " + ex.getMessage());
        }
    }

    private void openWhatsApp() {
        String phone = seller.getContactId().replaceAll("[^0-9]", "");
        if (phone.startsWith("0")) {
            phone = "62" + phone.substring(1);
        } else if (!phone.startsWith("62")) {
            phone = "62" + phone;
        }
        try {
            URI uri = URI.create("https://wa.me/" + phone);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                JOptionPane.showMessageDialog(this, "Open in browser: " + uri.toString());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open WhatsApp: " + ex.getMessage());
        }
    }
}
