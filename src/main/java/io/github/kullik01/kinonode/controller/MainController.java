package io.github.kullik01.kinonode.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.kullik01.kinonode.service.AppContext;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML private StackPane contentArea;
    @FXML private Button btnLibrary;
    @FXML private Button btnWatchlists;
    @FXML private Button btnSettings;
    @FXML private ToggleSwitch btnThemeToggle;

    private final Map<String, Parent> views = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    private Button activeButton;

    @FXML
    public void initialize() {
        // Load initial theme button state from settings (Default is light)
        var settingsRepo = AppContext.getInstance().getSettingsRepository();
        String theme = settingsRepo.get("theme_mode", "light");
        
        // Unselected (false) = Light Mode (Default)
        // Selected (true) = Dark Mode
        if ("dark".equals(theme)) {
            btnThemeToggle.setSelected(true);
            btnThemeToggle.setText("🌙 Dark Mode");
        } else {
            btnThemeToggle.setSelected(false);
            btnThemeToggle.setText("☀ Light Mode");
        }

        // Add selection listener AFTER setting initial values to avoid immediate toggle firing on load
        btnThemeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            toggleTheme(newVal);
        });

        // Show Library by default
        showLibrary();
    }

    @FXML
    private void showLibrary() {
        navigate("Library", "/io/github/kullik01/kinonode/fxml/LibraryView.fxml", btnLibrary);
    }

    @FXML
    private void showWatchlists() {
        navigate("Watchlists", "/io/github/kullik01/kinonode/fxml/WatchlistsView.fxml", btnWatchlists);
    }

    @FXML
    private void showSettings() {
        navigate("Settings", "/io/github/kullik01/kinonode/fxml/SettingsView.fxml", btnSettings);
    }

    private void navigate(String name, String fxmlPath, Button targetButton) {
        if (activeButton == targetButton) {
            return; // Already on this tab
        }
        
        Runnable proceed = () -> {
            loadView(name, fxmlPath);
            setActiveButton(targetButton);
        };
        
        if (activeButton == btnSettings) {
            SettingsController settingsController = (SettingsController) controllers.get("Settings");
            if (settingsController != null && settingsController.hasUnsavedChanges()) {
                showUnsavedChangesDialog(settingsController, proceed);
                return;
            }
        }
        
        proceed.run();
    }

    private void showUnsavedChangesDialog(SettingsController settingsController, Runnable proceed) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Settings");
        alert.setHeaderText("You have unsaved settings.");
        alert.setContentText("Do you want to save your changes before leaving?");
        
        javafx.scene.control.ButtonType btnTypeSave = new javafx.scene.control.ButtonType("Save");
        javafx.scene.control.ButtonType btnTypeDiscard = new javafx.scene.control.ButtonType("Discard");
        javafx.scene.control.ButtonType btnTypeCancel = new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(btnTypeSave, btnTypeDiscard, btnTypeCancel);
        
        if (contentArea.getScene() != null && contentArea.getScene().getWindow() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().addAll(((javafx.stage.Stage) contentArea.getScene().getWindow()).getIcons());
        }
        
        var result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnTypeSave) {
                if (settingsController.hasValidationErrors()) {
                    javafx.scene.control.Alert errAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errAlert.setTitle("Validation Error");
                    errAlert.setHeaderText("Cannot save settings.");
                    errAlert.setContentText("Please fix validation errors before saving.");
                    errAlert.showAndWait();
                } else {
                    settingsController.saveSettings();
                    proceed.run();
                }
            } else if (result.get() == btnTypeDiscard) {
                settingsController.refresh(); // Discard changes by reloading
                proceed.run();
            }
            // If Cancel, do nothing (stays on Settings page)
        }
    }

    private void toggleTheme(boolean dark) {
        var settingsRepo = AppContext.getInstance().getSettingsRepository();
        
        if (dark) {
            btnThemeToggle.setText("🌙 Dark Mode");
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            settingsRepo.set("theme_mode", "dark");
        } else {
            btnThemeToggle.setText("☀ Light Mode");
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            settingsRepo.set("theme_mode", "light");
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void loadView(String name, String fxmlPath) {
        try {
            Parent view = views.get(name);
            if (view == null) {
                URL url = getClass().getResource(fxmlPath);
                if (url == null) {
                    throw new IOException("Cannot find FXML: " + fxmlPath);
                }
                FXMLLoader loader = new FXMLLoader(url);
                view = loader.load();
                views.put(name, view);
                controllers.put(name, loader.getController());
            }

            contentArea.getChildren().setAll(view);
            
            // Refresh content if controller is refreshable
            Object controller = controllers.get(name);
            if (controller instanceof Refreshable refreshable) {
                refreshable.refresh();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view " + name, e);
        }
    }
}
