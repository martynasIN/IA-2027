package com.ia.ia_base.controllers;

import com.ia.ia_base.database.dao.CategoryDAO;
import com.ia.ia_base.database.dao.EventDAO;
import com.ia.ia_base.models.Category;
import com.ia.ia_base.models.Event;
import com.ia.ia_base.util.AlertManager;
import com.ia.ia_base.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for the Add / Edit Event form (modal window).
 *
 * setEvent(null)  → Add mode: empty form, creates a new event on save.
 * setEvent(event) → Edit mode: form pre-filled with existing data.
 *
 * After saving, closeWindow() closes the modal.
 * EventsController then refreshes the table automatically
 * because showAndWait() was used to open this form.
 */
public class EventFormController extends BaseController {

    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextArea descriptionArea;

    private Event eventToEdit; // null = add mode, non-null = edit mode
    private EventDAO eventDAO;
    private CategoryDAO categoryDAO;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        eventDAO = new EventDAO();
        categoryDAO = new CategoryDAO();
        loadCategories();
    }

    /**
     * Call this after loading the FXML, before calling stage.show().
     * Pass null for Add mode, or an existing Event for Edit mode.
     */
    public void setEvent(Event event) {
        this.eventToEdit = event;

        if (event != null) {
            // Edit mode: populate fields with existing data
            formTitleLabel.setText("Edit Event");
            titleField.setText(event.getTitle());
            descriptionArea.setText(event.getDescription() != null ? event.getDescription() : "");
            datePicker.setValue(event.getDate());

            // Select the matching category in the ComboBox
            for (Category cat : categoryComboBox.getItems()) {
                if (cat.getId() == event.getCategory().getId()) {
                    categoryComboBox.setValue(cat);
                    break;
                }
            }
        } else {
            // Add mode
            formTitleLabel.setText("Add Event");
        }
    }

    /**
     * Loads categories from the database and puts them in the ComboBox.
     * Category.toString() returns the name, so the ComboBox displays it correctly.
     */
    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.findAll();
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            AlertManager.showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        // --- Validation ---
        String title = titleField.getText().trim();
        Category category = categoryComboBox.getValue();

        if (title.isEmpty()) {
            AlertManager.showError("Validation Error", "Please enter a title");
            return;
        }

        if (category == null) {
            AlertManager.showError("Validation Error", "Please select a category");
            return;
        }

        try {
            if (eventToEdit == null) {
                // ADD mode — build a new Event and insert it
                Event newEvent = new Event();
                newEvent.setTitle(title);
                newEvent.setDescription(descriptionArea.getText().trim());
                newEvent.setDate(datePicker.getValue());
                newEvent.setUserId(SessionManager.getInstance().getCurrentUser().getId());
                newEvent.setCategory(category);

                eventDAO.create(newEvent);
            } else {
                // EDIT mode — update the existing event in the database
                eventToEdit.setTitle(title);
                eventToEdit.setDescription(descriptionArea.getText().trim());
                eventToEdit.setDate(datePicker.getValue());
                eventToEdit.setCategory(category);

                eventDAO.update(eventToEdit);
            }

            closeWindow(); // Close the form after saving

        } catch (SQLException e) {
            AlertManager.showError("Database Error", "Failed to save event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }
}