package io.github.kullik01.kinonode.controller;

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

    private final Map<String, Parent> views = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    private Button activeButton;

    @FXML
    public void initialize() {
        // Show Library by default
        showLibrary();
    }

    @FXML
    private void showLibrary() {
        loadView("Library", "/io/github/kullik01/kinonode/fxml/LibraryView.fxml");
        setActiveButton(btnLibrary);
    }

    @FXML
    private void showWatchlists() {
        loadView("Watchlists", "/io/github/kullik01/kinonode/fxml/WatchlistsView.fxml");
        setActiveButton(btnWatchlists);
    }

    @FXML
    private void showSettings() {
        loadView("Settings", "/io/github/kullik01/kinonode/fxml/SettingsView.fxml");
        setActiveButton(btnSettings);
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
