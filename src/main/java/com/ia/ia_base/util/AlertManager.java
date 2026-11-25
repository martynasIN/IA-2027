package com.ia.ia_base.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Shows dialog boxes (alerts) to the user.
 * Use these methods instead of creating Alert objects manually.
 *
 * Quick reference:
 *   showInfo()         — information message
 *   showError()        — error message
 *   showWarning()      — warning message
 *   showConfirmation() — OK / Cancel dialog, returns true if OK clicked
 *   confirmDelete()    — standard "Are you sure?" before deleting
 */
public class AlertManager {
    
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /** Shows an OK / Cancel dialog. Returns true if the user clicked OK. */
    public static boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /** Asks "Are you sure you want to delete: [name]?" Returns true if confirmed. */
    public static boolean confirmDelete(String itemName) {
        return showConfirmation("Confirmation", "Delete Record", 
                              "Are you sure you want to delete: " + itemName + "?");
    }
}

