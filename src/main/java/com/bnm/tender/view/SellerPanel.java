package com.bnm.tender.view;

import com.bnm.tender.controller.TenderController;
import com.bnm.tender.model.Offer;
import com.bnm.tender.model.Seller;
import com.bnm.tender.model.TenderRequest;
import com.bnm.tender.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SellerPanel extends JPanel implements TenderController.TenderListener {
    private DefaultListModel<TenderRequest> requestModel;
    private JList<TenderRequest> requestList;
    private JPanel cardsContainer;
    private List<SellerInputCard> sellerCards;

    public SellerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(StyleUtil.BG_CREAM);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Registry
        TenderController.getInstance().addListener(this);
        sellerCards = new ArrayList<>();

        // Left: Incoming Requests List
        requestModel = new DefaultListModel<>();
        requestList = new JList<>(requestModel);
        requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestList.setFont(StyleUtil.FONT_BODY);
        requestList.addListSelectionListener(e -> updateCardsForRequest());
        requestList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TenderRequest) {
                    String text = "<html><b>" + ((TenderRequest) value).getQuery() + "</b><br>" +
                                  "<span style='font-size:10px; color:gray;'>" + 
                                  ((TenderRequest) value).getBuyerAddress() + "</span></html>";
                    setText(text);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                    if (isSelected) {
                        setBackground(new Color(255, 229, 180)); // Peach highlight
                        setForeground(Color.BLACK);
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }
                return this;
            }
        });
        
        JScrollPane requestScroll = new JScrollPane(requestList);
        requestScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true), "Incoming Requests"));
        requestScroll.setPreferredSize(new Dimension(250, 0));
        requestScroll.setOpaque(false);
        requestScroll.getViewport().setOpaque(false);
        
        add(requestScroll, BorderLayout.WEST);

        // Center: Grid of Seller Cards
        cardsContainer = new JPanel(new GridLayout(0, 1, 10, 10)); // 1 column, dynamic rows
        cardsContainer.setOpaque(false);
        
        // Initialize cards for all sellers
        List<Seller> sellers = TenderController.getInstance().getSellers();
        for (Seller seller : sellers) {
            SellerInputCard card = new SellerInputCard(seller);
            sellerCards.add(card);
            cardsContainer.add(card);
        }

        JScrollPane cardsScroll = new JScrollPane(cardsContainer);
        cardsScroll.setBorder(null);
        cardsScroll.setOpaque(false);
        cardsScroll.getViewport().setOpaque(false);
        cardsScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(cardsScroll, BorderLayout.CENTER);
    }

    private void updateCardsForRequest() {
        TenderRequest selected = requestList.getSelectedValue();
        for (SellerInputCard card : sellerCards) {
             card.setRequest(selected);
        }
    }

    @Override
    public void onNewRequest(TenderRequest request) {
        requestModel.addElement(request);
        // Auto-select if first one to make it easier
        if (requestModel.size() == 1) {
            requestList.setSelectedIndex(0);
        }
    }

    @Override
    public void onNewOffer(String requestId, Offer offer) {
        // Optional: Show status on the specific card
    }
}
