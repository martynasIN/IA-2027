package com.ia.ia_base.controllers;

import com.ia.ia_base.database.dao.UserDAO;
import com.ia.ia_base.models.User;
import com.ia.ia_base.util.AlertManager;
import com.ia.ia_base.util.PasswordHasher;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Handles the Registration screen.
 *
 * Before saving to the database, we validate:
 *   - All fields are filled in
 *   - Email matches the expected format (checked with a regular expression)
 *   - Password is at least 6 characters
 *   - Both password fields match
 *
 * IB security: the password is hashed before it is stored — never plain text.
 */
public class RegistrationController extends BaseController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Hyperlink loginLink;

    private UserDAO userDAO;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        userDAO = new UserDAO();
    }

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            AlertManager.showError("Error", "Please fill in all fields");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            AlertManager.showError("Invalid Email", "Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            AlertManager.showError("Weak Password", "Password must be at least 6 characters long");
            return;
        }

        if (!password.equals(confirmPassword)) {
            AlertManager.showError("Password Mismatch", "Passwords do not match");
            return;
        }

        try {
            // Make sure no account with this email already exists
            User existing = userDAO.findByEmail(email);
            if (existing != null) {
                AlertManager.showError("Registration Failed", "Email already registered");
                return;
            }

            // Build the User object and save it to the database
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPasswordHash(PasswordHasher.hashPassword(password));
            userDAO.create(newUser);

            AlertManager.showInfo("Success", "Registration successful! You can now log in.");
            handleLoginLink();

        } catch (SQLException e) {
            AlertManager.showError("Database Error", "Failed to register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLoginLink() {
        if (stage == null && loginLink != null && loginLink.getScene() != null) {
            stage = (javafx.stage.Stage) loginLink.getScene().getWindow();
        }
        changeScene("views/LoginView.fxml");
        if (stage != null) {
            stage.setTitle("Events App - Login");
        }
    }
}