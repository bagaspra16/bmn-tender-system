package com.bnm.tender.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static volatile HikariDataSource dataSource;

    private Database() {}

    public static synchronized void init() {
        if (dataSource != null) return;

        Properties props = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                    "application.properties not found on classpath. " +
                    "Copy application.properties.example and fill in your Supabase credentials.");
            }
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(required(props, "db.url"));
        cfg.setUsername(required(props, "db.user"));
        cfg.setPassword(required(props, "db.password"));
        cfg.setMaximumPoolSize(intProp(props, "db.pool.maxSize", 5));
        cfg.setMinimumIdle(intProp(props, "db.pool.minIdle", 1));
        cfg.setConnectionTimeout(intProp(props, "db.pool.connectionTimeoutMs", 10000));
        cfg.setPoolName("BMN-Tender-Pool");
        cfg.addDataSourceProperty("ApplicationName", "BMN-Tender-System");

        dataSource = new HikariDataSource(cfg);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String required(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return v.trim();
    }

    private static int intProp(Properties p, String key, int def) {
        String v = p.getProperty(key);
        return (v == null || v.isBlank()) ? def : Integer.parseInt(v.trim());
    }
}
