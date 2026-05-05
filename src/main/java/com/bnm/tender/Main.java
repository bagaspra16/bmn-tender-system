package com.bnm.tender;

import com.formdev.flatlaf.FlatLightLaf;
import com.bnm.tender.view.MainFrame;
import com.bnm.tender.db.Database;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Database::shutdown, "db-shutdown"));

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
