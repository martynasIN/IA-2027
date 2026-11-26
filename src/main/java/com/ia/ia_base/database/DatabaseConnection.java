package com.ia.ia_base.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * IB Topic D.2 — Singleton design pattern
 *
 * Only ONE database connection exists for the whole app.
 * getInstance() always returns the same object — never creates a new one.
 *
 * Setup:
 *   1. Copy .env.example → rename to .env
 *   2. Fill in your MySQL credentials
 *   3. .env is in .gitignore — your password will not be committed to Git
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    // -----------------------------------------------------------------------
    // Credentials are loaded from the .env file — NOT hardcoded here.
    // Edit the .env file in the project root to change the connection settings.
    // -----------------------------------------------------------------------
    private static final String DB_URL;
    private static final String DB_NAME;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        Properties env = loadEnvFile();
        DB_URL      = env.getProperty("DB_URL",      "jdbc:mysql://localhost:3306/");
        DB_NAME     = env.getProperty("DB_NAME",     "");
        DB_USER     = env.getProperty("DB_USER",     "");
        DB_PASSWORD = env.getProperty("DB_PASSWORD", "");
    }

    private DatabaseConnection() {
        // Private constructor — Singleton pattern
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Returns the active database connection, opening it if necessary.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = DB_URL + DB_NAME + "?useSSL=false&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
            System.out.println("Connected to database: " + DB_NAME);
        }
        return connection;
    }

    /**
     * Closes the database connection.
     * Called from MainApplication.stop() when the application exits.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /** Returns true if DB_NAME and DB_USER were loaded from .env (not empty). */
    public static boolean isConfigured() {
        return !DB_NAME.isBlank() && !DB_USER.isBlank();
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // .env file parser
    // Reads key=value pairs from a .env file in the current working directory.
    // Lines starting with # are treated as comments and are ignored.
    // -----------------------------------------------------------------------
    private static Properties loadEnvFile() {
        Properties props = new Properties();
        File envFile = new File(".env");

        if (!envFile.exists()) {
            System.err.println("WARNING: .env file not found in " + envFile.getAbsolutePath());
            System.err.println("  → Copy .env.example to .env and fill in your credentials.");
            return props;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip comments

                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key   = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    props.setProperty(key, value);
                }
            }
            System.out.println("Loaded database settings from .env");
        } catch (IOException e) {
            System.err.println("Error reading .env file: " + e.getMessage());
        }

        return props;
    }
}