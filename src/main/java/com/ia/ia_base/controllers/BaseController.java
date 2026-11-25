package com.ia.ia_base.controllers;

import com.ia.ia_base.util.WindowManager;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * IB Topic D.2 — Inheritance
 *
 * All controllers extend this class and inherit its methods.
 * It holds a reference to the current window (stage) and provides
 * changeScene() for switching screens inside the same window.
 */
public abstract class BaseController implements Initializable {
    protected Stage stage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Empty by default — override in child controllers if needed
    }

    /** Called by MainApplication or WindowManager after the FXML is loaded. */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Replaces the content of the current window with a new FXML screen.
     * Used to switch between Login, Registration, and Events screens
     * without opening extra windows.
     */
    protected void changeScene(String fxmlPath) {
        if (stage == null) {
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/ia/ia_base/" + fxmlPath)
            );
            Parent root = loader.load();
            
            // Set Stage object in new controller
            BaseController controller = loader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }
            
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Error changing window: " + fxmlPath);
            e.printStackTrace();
        }
    }
    
    /** Closes the current window. */
    protected void closeWindow() {
        if (stage != null) {
            WindowManager.closeWindow(stage);
        }
    }
}

