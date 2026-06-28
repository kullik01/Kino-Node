/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.kullik01.kinonode.service.AppContext;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The main shell controller of the application. Manages the layout container (sidebar navigation
 * menu, theme toggle, and main content area). Handles switching between different application views
 * (Library, Watchlists, Settings, and About) and dynamically loads/caches their associated FXML
 * files.
 */
public class MainController {
  private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

  @FXML private StackPane rootPane;
  @FXML private StackPane contentArea;
  @FXML private Button btnLibrary;
  @FXML private Button btnWatchlists;
  @FXML private Button btnSettings;
  @FXML private Button btnAbout;
  @FXML private ToggleSwitch btnThemeToggle;

  private final Map<String, Parent> views = new HashMap<>();
  private final Map<String, Object> controllers = new HashMap<>();
  private Button activeButton;
  private double xOffset = 0;
  private double yOffset = 0;

  /**
   * Initializes the main controller layout. Restores the theme selection (light or dark mode) from
   * configuration, establishes window listeners for maximization changes, and triggers navigation
   * to the default Library view.
   */
  @FXML
  public void initialize() {

    // Load initial theme button state from settings (Default is light)
    var settingsRepo = AppContext.getInstance().getSettingsRepository();
    String theme = settingsRepo.get("theme_mode", "light");

    // Unselected (false) = Light Mode (Default)
    // Selected (true) = Dark Mode
    if (theme != null && theme.equals("dark")) {
      btnThemeToggle.setSelected(true);
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip().setText("Dark Mode");
        btnThemeToggle.getTooltip().setShowDelay(javafx.util.Duration.millis(250));
      }
    } else {
      btnThemeToggle.setSelected(false);
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip().setText("Light Mode");
        btnThemeToggle.getTooltip().setShowDelay(javafx.util.Duration.millis(250));
      }
    }

    // Add selection listener AFTER setting initial values to avoid immediate toggle firing on load
    btnThemeToggle
        .selectedProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              toggleTheme(newVal);
            });

    // Show Library by default
    showLibrary();

    // Query and listen to the window stage once it is rendered to apply maximized styling
    javafx.application.Platform.runLater(
        () -> {
          Stage stage = (Stage) rootPane.getScene().getWindow();
          stage
              .maximizedProperty()
              .addListener(
                  (obs, oldVal, newVal) -> {
                    if (newVal) {
                      rootPane.getStyleClass().add("maximized");
                    } else {
                      rootPane.getStyleClass().remove("maximized");
                    }
                  });
        });
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

  @FXML
  private void showAbout() {
    navigate("About", "/io/github/kullik01/kinonode/fxml/AboutView.fxml", btnAbout);
  }

  private void navigate(String name, String fxmlPath, Button targetButton) {
    if (activeButton == targetButton) {
      return; // Already on this tab
    }

    loadView(name, fxmlPath);
    setActiveButton(targetButton);
  }

  private void toggleTheme(boolean dark) {
    var settingsRepo = AppContext.getInstance().getSettingsRepository();

    // Set AtlantaFX stylesheet globally depending on selected theme option
    if (dark) {
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null) btnThemeToggle.getTooltip().setText("Dark Mode");
      Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
      settingsRepo.set("theme_mode", "dark");
    } else {
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null) btnThemeToggle.getTooltip().setText("Light Mode");
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
      // FXML file is loaded lazily and cached locally to avoid loading overhead on tab switches
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
