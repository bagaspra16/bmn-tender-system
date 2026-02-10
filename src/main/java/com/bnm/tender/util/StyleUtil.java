package com.bnm.tender.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StyleUtil {
    // Mac-like Pastel Palette
    public static final Color BG_CREAM = new Color(253, 251, 247); // Warm background
    
    // Vibrant Fanstastic Palette
    public static final Color VIBRANT_MINT = new Color(163, 228, 215);
    public static final Color VIBRANT_BLUE = new Color(169, 204, 227);
    public static final Color VIBRANT_PURPLE = new Color(210, 180, 222);
    public static final Color VIBRANT_ORANGE = new Color(245, 183, 177);
    public static final Color VIBRANT_YELLOW = new Color(249, 231, 159);
    public static final Color VIBRANT_TEAL = new Color(118, 215, 196);
    
    // Text Colors
    public static final Color TEXT_DARK = new Color(40, 40, 40); // Darker for contrast
    public static final Color TEXT_MUTED = new Color(100, 100, 100);

    // Fonts - BOLDER as requested
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 22); // Larger, Bold
    public static final Font FONT_BODY = new Font("SansSerif", Font.BOLD, 14);   // Bold body text
    public static final Font FONT_SMALL = new Font("SansSerif", Font.BOLD, 12);  // Bold small text
    
    // Icons
    public static final String ICON_USER = "👤";
    public static final String ICON_SELLER = "🏪";
    public static final String ICON_PRODUCT = "📦";
    public static final String ICON_PRICE = "🏷️";
    public static final String ICON_RATING = "⭐";
    public static final String ICON_CART = "🛒";
    public static final String ICON_SEND = "🚀";

    /**
     * Creates a rounded border with padding.
     */
    public static Border createRoundedBorder(Color color) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true), // Thicker border
            BorderFactory.createEmptyBorder(15, 15, 15, 15) // More padding
        );
    }
    
    /**
     * Styles a label as a header.
     */
    public static void styleHeader(JLabel label) {
        label.setFont(FONT_HEADER);
        label.setForeground(TEXT_DARK);
    }

    /**
     * Styles a button with a primary color action look.
     */
    public static void styleActionButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(TEXT_DARK);
        button.setFont(FONT_BODY);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20) // Larger buttons
        ));
        
        // Hover effect helper could be added here if we passed JComponent, 
        // but typically done via MouseListener. 
        // FlatLaf handles some hovering automatically.
    }
    
    /**
     * Getting a specific color for a seller based on index/hash to make it colorful.
     */
    public static Color getSellerColor(String sellerName) {
        int hash = Math.abs(sellerName.hashCode());
        Color[] colors = {VIBRANT_MINT, VIBRANT_BLUE, VIBRANT_PURPLE, VIBRANT_ORANGE, VIBRANT_YELLOW, VIBRANT_TEAL};
        return colors[hash % colors.length];
    }

    /**
     * Formats a double price to Indonesian Rupiah standard (e.g. 10000 -> "10.000").
     */
    public static String formatRupiah(double amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMANY);
        return formatter.format(amount);
    }
}
