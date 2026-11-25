package com.ia.ia_base.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Helper for opening and closing windows.
 *
 * In this project, EventsController opens the event form directly
 * (so it can pass data to the controller before showing the window).
 * Use WindowManager when you need to open a simple window without data passing.
 */
public class WindowManager {
    
    /**
     * Opens a new window from an FXML file.
     * @param fxmlPath path relative to resources/com/ia/ia_base/ (e.g., "views/EventsView.fxml")
     * @param title    window title shown in the title bar
     */
    public static Stage openWindow(String fxmlPath, String title) {
        return openWindow(fxmlPath, title, false);
    }
    
    /**
     * Opens a new window. If modal=true, the window blocks the one behind it
     * (the user must close it before continuing).
     */
    public static Stage openWindow(String fxmlPath, String title, boolean modal) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource("/com/ia/ia_base/" + fxmlPath));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            
            if (modal) {
                stage.initModality(Modality.APPLICATION_MODAL);
            }
            
            // Pass the Stage to the controller so it can close itself later
            Object controller = loader.getController();
            if (controller instanceof com.ia.ia_base.controllers.BaseController) {
                ((com.ia.ia_base.controllers.BaseController) controller).setStage(stage);
            }
            
            stage.show();
            return stage;
        } catch (IOException e) {
            System.err.println("Error opening window: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
    
    /** Opens a window with a fixed width and height. */
    public static Stage openWindow(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource("/com/ia/ia_base/" + fxmlPath));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
            return stage;
        } catch (IOException e) {
            System.err.println("Error opening window: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
    
    /** Closes a window. */
    public static void closeWindow(Stage stage) {
        if (stage != null) {
            stage.close();
        }
    }
}

