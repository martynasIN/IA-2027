package com.ia.ia_base;

import com.ia.ia_base.controllers.LoginController;
import com.ia.ia_base.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Main JavaFX class — the app starts here.
 *
 * start() runs when the app opens:
 *   1. Try to connect to the database (shows a warning if it fails).
 *   2. Load and show the Login screen.
 *
 * stop() runs automatically when the user closes the window.
 * We use it to close the database connection cleanly.
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Connect to the database
        try {
            DatabaseConnection.getInstance().getConnection();
            System.out.println("Database connection: OK");
        } catch (SQLException e) {
            System.err.println("WARNING: Could not connect to the database!");
            System.err.println("  → Check the settings in DatabaseConnection.java");
            System.err.println("  → Make sure MySQL is running and database_setup.sql was run");
            System.err.println("  → Error: " + e.getMessage());
        }

        // Load and show the Login screen
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("views/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 400, 300);

        LoginController controller = loader.getController();
        if (controller != null) {
            controller.setStage(stage);
        }

        stage.setTitle("Events App - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        // Close the database connection when the application exits
        DatabaseConnection.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch();
    }
}