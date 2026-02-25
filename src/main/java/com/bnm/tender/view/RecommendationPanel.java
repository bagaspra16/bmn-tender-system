package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Seller;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Middle window: shows recommended offers for the buyer,
 * grouped by seller, in a dedicated column.
 * Read-only (selection still happens in BuyerPanel cart),
 * but visually separates "Recommended for Buyer" from Buyer input.
 */
public class RecommendationPanel extends JPanel implements TenderController.TenderListener {
    private final BuyerPanel buyerPanel;
    private JPanel contentPanel;
    private Map<String, JPanel> requestPanels;

    public RecommendationPanel(BuyerPanel buyerPanel) {
        this.buyerPanel = buyerPanel;
        setLayout(new BorderLayout(10, 10));
        setBackground(StyleUtil.BG_CREAM);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        TenderController.getInstance().addListener(this);
        requestPanels = new LinkedHashMap<>();

        JLabel header = new JLabel("Recommended for Buyer");
        header.setFont(StyleUtil.FONT_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(header, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(0, 2, 10, 10)); // 2-column grid of cards
        contentPanel.setBackground(StyleUtil.BG_CREAM);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onNewRequest(TenderRequest request) {
        JPanel requestBox = new JPanel();
        requestBox.setLayout(new BoxLayout(requestBox, BoxLayout.Y_AXIS));
        requestBox.setBackground(Color.WHITE);
        requestBox.setBorder(BorderFactory.createCompoundBorder(
            StyleUtil.createRoundedBorder(StyleUtil.VIBRANT_BLUE),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("<html><div style='width:220px;'><b>Request:</b> " + request.getQuery() + "</div></html>");
        title.setFont(StyleUtil.FONT_BODY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        requestBox.add(title);
        requestBox.add(Box.createVerticalStrut(6));

        JLabel info = new JLabel("<html><span style='font-size:11px;color:#555;'>Waiting for best offers...</span></html>");
        info.setFont(StyleUtil.FONT_SMALL);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        requestBox.add(info);

        contentPanel.add(requestBox);
        contentPanel.revalidate();
        contentPanel.repaint();

        requestPanels.put(request.getRequestId(), requestBox);
    }

    @Override
    public void onNewOffer(String requestId, Offer offer) {
        JPanel box = requestPanels.get(requestId);
        if (box != null) {
            renderOffers(box, requestId);
        }
    }

    private void renderOffers(JPanel box, String requestId) {
        box.removeAll();

        List<Offer> offers = TenderController.getInstance().getBestOffers(requestId);
        if (offers.isEmpty()) {
            JLabel empty = new JLabel("No offers yet.");
            empty.setFont(StyleUtil.FONT_SMALL);
            empty.setForeground(Color.GRAY);
            box.add(empty);
            box.revalidate();
            box.repaint();
            return;
        }

        JLabel header = new JLabel("<html><div style='width:220px;'>Best offers for this request<br><span style='font-size:11px;color:#777;'>Tap package or items to add to cart</span></div></html>");
        header.setFont(StyleUtil.FONT_BODY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(header);
        box.add(Box.createVerticalStrut(8));

        Map<Seller, List<Offer>> bySeller = new LinkedHashMap<>();
        for (Offer o : offers) {
            bySeller.computeIfAbsent(o.getSeller(), k -> new ArrayList<>()).add(o);
        }

        for (Map.Entry<Seller, List<Offer>> entry : bySeller.entrySet()) {
            Seller seller = entry.getKey();
            List<Offer> sellerOffers = entry.getValue();

            double total = sellerOffers.stream().mapToDouble(Offer::getTotalPrice).sum();

            JLabel sellerLabel = new JLabel(
                "<html><div style='width:220px;'><b>" + seller.getName() + "</b><br/>" +
                    "<span style='font-size:11px;color:#555;'>Package total: Rp "
                    + StyleUtil.formatRupiah(total) + "</span></div></html>");
            sellerLabel.setFont(StyleUtil.FONT_BODY);
            sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(sellerLabel);

            // Package controls
            JPanel sellerControlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            sellerControlRow.setOpaque(false);

            JButton addWholeBtn = new JButton("Add whole package");
            StyleUtil.styleActionButton(addWholeBtn, StyleUtil.VIBRANT_YELLOW);
            addWholeBtn.setFont(StyleUtil.FONT_SMALL);
            addWholeBtn.addActionListener(e -> {
                for (Offer o : sellerOffers) {
                    buyerPanel.addOfferToCart(o);
                }
            });
            sellerControlRow.add(addWholeBtn);

            box.add(sellerControlRow);
            box.add(Box.createVerticalStrut(4));

            // Individual items
            List<JCheckBox> itemChecks = new ArrayList<>();
            JPanel itemsPanel = new JPanel();
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
            itemsPanel.setOpaque(false);

            for (Offer o : sellerOffers) {
                JCheckBox cb = new JCheckBox(
                    "<html><div style='width:210px;'>• " + o.getProduct().getName() +
                        " (x" + o.getQuantity() + ") — Rp " + StyleUtil.formatRupiah(o.getTotalPrice()) +
                        "</div></html>");
                cb.setOpaque(false);
                cb.setFont(StyleUtil.FONT_SMALL);
                cb.putClientProperty("offer", o);
                itemsPanel.add(cb);
                itemChecks.add(cb);
            }

            JButton addSelectedBtn = new JButton("Add selected items");
            StyleUtil.styleActionButton(addSelectedBtn, StyleUtil.VIBRANT_MINT);
            addSelectedBtn.setFont(StyleUtil.FONT_SMALL);
            addSelectedBtn.addActionListener(e -> {
                for (JCheckBox cb : itemChecks) {
                    if (cb.isSelected()) {
                        buyerPanel.addOfferToCart((Offer) cb.getClientProperty("offer"));
                    }
                }
            });
            JPanel addSelectedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            addSelectedRow.setOpaque(false);
            addSelectedRow.add(addSelectedBtn);

            box.add(itemsPanel);
            box.add(addSelectedRow);
            box.add(Box.createVerticalStrut(10));
        }

        box.revalidate();
        box.repaint();
    }
}

