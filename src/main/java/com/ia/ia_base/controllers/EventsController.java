package com.ia.ia_base.controllers;

import com.ia.ia_base.database.dao.CategoryDAO;
import com.ia.ia_base.database.dao.EventDAO;
import com.ia.ia_base.models.Category;
import com.ia.ia_base.models.Event;
import com.ia.ia_base.util.AlertManager;
import com.ia.ia_base.util.ApiClient;
import com.ia.ia_base.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main screen shown after login — displays the user's events in a table.
 *
 * IB SQL concept: events are loaded with a JOIN query (see EventDAO)
 * so the category name is available without a second database call.
 *
 * Each row has Edit and Delete buttons built with a cell factory
 * (actionsColumn in setupTableColumns).
 */
public class EventsController extends BaseController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TableView<Event> eventsTable;

    @FXML
    private TableColumn<Event, String> titleColumn;

    @FXML
    private TableColumn<Event, String> categoryColumn;

    @FXML
    private TableColumn<Event, String> dateColumn;

    @FXML
    private TableColumn<Event, String> descriptionColumn;

    @FXML
    private TableColumn<Event, Void> actionsColumn;

    private EventDAO eventDAO;
    private CategoryDAO categoryDAO;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        eventDAO    = new EventDAO();
        categoryDAO = new CategoryDAO();

        // Show welcome message with the logged-in user's email
        String email = SessionManager.getInstance().getCurrentUser().getEmail();
        welcomeLabel.setText("Welcome, " + email + "!");

        setupTableColumns();
        loadEvents();
        checkAndImportFromApi();
    }

    /**
     * Connects each TableView column to a field in the Event object.
     * setCellValueFactory() tells JavaFX what value to show in each cell.
     * setCellFactory() (used for actionsColumn) lets us put buttons in a cell.
     */
    private void setupTableColumns() {
        // Columns fill the full table width.
        // The last column grows/shrinks when the window is resized.
        eventsTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        titleColumn.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getTitle())
        );

        // Category name comes from the SQL JOIN — no extra database call needed
        categoryColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            String name = event.getCategory() != null ? event.getCategory().getName() : "";
            return new SimpleStringProperty(name);
        });

        dateColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            String date = event.getDate() != null ? event.getDate().toString() : "";
            return new SimpleStringProperty(date);
        });

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            return new SimpleStringProperty(desc != null ? desc : "");
        });

        // Actions column — each row gets its own Edit and Delete buttons.
        // updateItem() is called by JavaFX every time a row is drawn.
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> openEventForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    if (AlertManager.confirmDelete(event.getTitle())) {
                        try {
                            eventDAO.delete(event.getId());
                            loadEvents();
                        } catch (java.sql.SQLException ex) {
                            AlertManager.showError("Database Error", "Failed to delete: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /**
     * Loads the current user's events from the database and fills the table.
     * Called once on startup and again after every add / edit / delete.
     */
    private void loadEvents() {
        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<Event> events = eventDAO.findByUserId(userId);
            eventsTable.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException e) {
            AlertManager.showError("Database Error", "Failed to load events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddEvent() {
        openEventForm(null);
    }

    /**
     * Opens the event form as a modal window.
     *
     * Pass null to open in Add mode, or an existing Event to open in Edit mode.
     * showAndWait() pauses execution until the modal is closed, so
     * loadEvents() below it runs only after the user has finished.
     */
    private void openEventForm(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/ia/ia_base/views/EventFormView.fxml")
            );
            Parent root = loader.load();

            // Pass the event to the form controller (null = add mode)
            EventFormController formController = loader.getController();
            formController.setEvent(event);

            // Create a modal window
            Stage formStage = new Stage();
            formStage.setTitle(event == null ? "Add Event" : "Edit Event");
            formStage.initModality(Modality.APPLICATION_MODAL); // blocks the main window
            formStage.setScene(new Scene(root, 420, 360));
            formStage.setResizable(false);

            // Give the form controller a reference to its own window
            formController.setStage(formStage);

            formStage.showAndWait(); // pauses here until the form is closed

            loadEvents(); // refresh the table with the latest data

        } catch (Exception e) {
            AlertManager.showError("Error", "Failed to open event form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called on startup. Checks if any API endpoints are configured in .env.
     * If yes, asks the user whether to import events from the API.
     *
     * Duplicate events (same title + date + user) are skipped automatically.
     */
    private void checkAndImportFromApi() {
        List<ApiClient.ApiEndpoint> endpoints = ApiClient.loadEndpoints();
        if (endpoints.isEmpty()) return; // no API configured — do nothing

        // Show one confirmation dialog listing all configured API names
        String names = endpoints.stream()
            .map(ApiClient.ApiEndpoint::name)
            .collect(Collectors.joining(", "));

        boolean confirmed = AlertManager.showConfirmation(
            "Import from API",
            "API source(s) found: " + names,
            "Would you like to import events from the API?"
        );
        if (!confirmed) return;

        int imported = 0;

        for (ApiClient.ApiEndpoint endpoint : endpoints) {
            try {
                String json = ApiClient.fetchJson(endpoint.url(), endpoint.key());

                List<Category> categories = categoryDAO.findAll();
                List<Event> apiEvents = ApiClient.parseEvents(json, categories);

                for (Event event : apiEvents) {
                    // Skip if this event was already imported before
                    if (!eventDAO.existsByTitleAndDate(event.getTitle(), event.getDate(), event.getUserId())) {
                        eventDAO.create(event);
                        imported++;
                    }
                }

            } catch (Exception e) {
                AlertManager.showError("API Error",
                    "Could not import from " + endpoint.name() + ":\n" + e.getMessage());
            }
        }

        if (imported > 0) {
            AlertManager.showInfo("Import Complete", imported + " new event(s) imported.");
            loadEvents(); // refresh the table
        } else {
            AlertManager.showInfo("Nothing New", "No new events found — everything is already up to date.");
        }
    }

    @FXML
    private void handleLogout() {
        if (AlertManager.showConfirmation("Logout", null, "Are you sure you want to log out?")) {
            SessionManager.getInstance().logout();
            changeScene("views/LoginView.fxml");
            if (stage != null) {
                stage.setTitle("Events App - Login");
                // Reset min size before disabling resize,
                // otherwise the small login window may be blocked by old min values
                stage.setMinWidth(0);
                stage.setMinHeight(0);
                stage.setResizable(false);
                stage.setWidth(400);
                stage.setHeight(300);
            }
        }
    }
}