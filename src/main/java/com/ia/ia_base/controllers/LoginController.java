package com.ia.ia_base.controllers;

import com.ia.ia_base.database.dao.UserDAO;
import com.ia.ia_base.models.User;
import com.ia.ia_base.util.AlertManager;
import com.ia.ia_base.util.PasswordHasher;
import com.ia.ia_base.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/**
 * Handles the Login screen.
 *
 * When the user clicks Login:
 *   1. Read email and password from the form.
 *   2. Look up the user in the database (UserDAO.findByEmail).
 *   3. Verify the password with BCrypt (PasswordHasher.verifyPassword).
 *   4. Save the user to SessionManager and open the Events screen.
 */
public class LoginController extends BaseController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Hyperlink registerLink;

    private UserDAO userDAO;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        userDAO = new UserDAO();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            AlertManager.showError("Error", "Please enter email and password");
            return;
        }

        try {
            User user = userDAO.findByEmail(email);

            if (user == null || !PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
                AlertManager.showError("Login Failed", "Invalid email or password");
                return;
            }

            // Login success — save the user so other controllers can access it
            SessionManager.getInstance().setCurrentUser(user);

            // Switch to the Events screen in the same window
            changeScene("views/EventsView.fxml");
            if (stage != null) {
                stage.setTitle("Events App - My Events");
                stage.setResizable(true);    // Allow resizing and maximising
                stage.setMinWidth(700);      // Prevent the window from becoming too narrow
                stage.setMinHeight(500);     // Prevent the window from becoming too short
                stage.setWidth(950);         // Default opening size
                stage.setHeight(650);
            }

        } catch (SQLException e) {
            AlertManager.showError("Database Error", "Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterLink() {
        if (stage == null && registerLink != null && registerLink.getScene() != null) {
            stage = (javafx.stage.Stage) registerLink.getScene().getWindow();
        }
        changeScene("views/RegistrationView.fxml");
        if (stage != null) {
            stage.setTitle("Events App - Registration");
        }
    }
}