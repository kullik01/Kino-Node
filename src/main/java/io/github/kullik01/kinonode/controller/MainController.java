/**
 * BSD 3-Clause License
 *
 * <p>
 * Copyright (c) 2026, Hannah Kullik
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.controller;

import javafx.application.Platform;
import javafx.util.Duration;


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
 * The main shell controller of the application. Manages the layout container
 * (sidebar navigation menu, theme toggle, and main content area). Handles
 * switching between different application views (Library, Watchlists, Settings,
 * and About) and dynamically loads/caches their associated FXML files.
 */
public class MainController {
  private static final Logger LOGGER =
      Logger.getLogger(MainController.class.getName());

  @FXML
  private StackPane rootPane;
  @FXML
  private StackPane contentArea;
  @FXML
  private Button btnLibrary;
  @FXML
  private Button btnWatchlists;
  @FXML
  private Button btnAnalytics;
  @FXML
  private Button btnImdb;
  @FXML
  private Button btnSettings;
  @FXML
  private Button btnAbout;
  @FXML
  private ToggleSwitch btnThemeToggle;

  private final Map<String, Parent> views = new HashMap<>();
  private final Map<String, Object> controllers = new HashMap<>();
  private Button activeButton;
  private double xOffset = 0;
  private double yOffset = 0;

  /**
   * Initializes the main controller layout. Restores the theme selection (light
   * or dark mode) from configuration, establishes window listeners for
   * maximization changes, and triggers navigation to the default Library view.
   */
  @FXML
  public void initialize() {
    AppContext.getInstance().setMainController(this);

    // Load initial theme button state from settings (Default is light)
    var tmpSettingsRepository =
        AppContext.getInstance().getSettingsRepository();
    String tmpTheme = tmpSettingsRepository.get("theme_mode", "light");

    // Unselected (false) = Light Mode (Default)
    // Selected (true) = Dark Mode
    if (tmpTheme != null && tmpTheme.equals("dark")) {
      btnThemeToggle.setSelected(true);
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip().setText("Dark Mode");
        btnThemeToggle.getTooltip().setShowDelay(Duration.millis(250));
      }
    } else {
      btnThemeToggle.setSelected(false);
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip().setText("Light Mode");
        btnThemeToggle.getTooltip().setShowDelay(Duration.millis(250));
      }
    }

    // Add selection listener AFTER setting initial values to avoid immediate
    // toggle firing on load
    btnThemeToggle.selectedProperty()
        .addListener((observable, oldValue, newValue) -> {
          toggleTheme(newValue);
        });

    // Apply application logo cursor to sidebar buttons
    javafx.scene.Cursor tmpLogoCursor =
        io.github.kullik01.kinonode.util.CursorUtils.getAppLogoCursor();
    btnLibrary.setCursor(tmpLogoCursor);
    btnWatchlists.setCursor(tmpLogoCursor);
    btnAnalytics.setCursor(tmpLogoCursor);
    btnImdb.setCursor(tmpLogoCursor);
    btnSettings.setCursor(tmpLogoCursor);
    btnAbout.setCursor(tmpLogoCursor);
    btnThemeToggle.setCursor(io.github.kullik01.kinonode.util.CursorUtils
        .getCenteredAppLogoCursor());

    // Show Library by default
    showLibrary();

    // Eagerly preload the IMDb view on startup to begin background loading
    // immediately
    preloadView("IMDb", "/io/github/kullik01/kinonode/fxml/ImdbView.fxml");

    // Apply application logo cursor globally to scene
    Platform.runLater(() -> {
      javafx.scene.Scene tmpScene = rootPane.getScene();
      if (tmpScene != null) {
        tmpScene.setCursor(tmpLogoCursor);
      }

      Stage tmpStage = (Stage) rootPane.getScene().getWindow();
      if (tmpStage != null) {
        tmpStage.maximizedProperty()
            .addListener((observable, oldValue, newValue) -> {
              if (newValue) {
                rootPane.getStyleClass().add("maximized");
              } else {
                rootPane.getStyleClass().remove("maximized");
              }
            });
      }
    });
  }

  /**
   * Switches the main view to the Library section.
   */
  @FXML
  private void showLibrary() {
    navigate("Library", "/io/github/kullik01/kinonode/fxml/LibraryView.fxml",
        btnLibrary);
  }

  /**
   * Switches the main view to the Watchlists section.
   */
  @FXML
  private void showWatchlists() {
    navigate("Watchlists",
        "/io/github/kullik01/kinonode/fxml/WatchlistsView.fxml", btnWatchlists);
  }

  /**
   * Switches the main view to the Analytics section.
   */
  @FXML
  private void showAnalytics() {
    navigate("Analytics",
        "/io/github/kullik01/kinonode/fxml/AnalyticsView.fxml", btnAnalytics);
  }

  /**
   * Switches the main view to the IMDb search section.
   * If already active, it triggers a refresh on the IMDb view.
   */
  @FXML
  private void showImdb() {
    if (activeButton == btnImdb) {
      Object tmpController = controllers.get("IMDb");
      if (tmpController instanceof Refreshable refreshable) {
        refreshable.refresh();
      }
      return;
    }
    navigate("IMDb", "/io/github/kullik01/kinonode/fxml/ImdbView.fxml",
        btnImdb);
  }

  /**
   * Switches the main view to the Settings section.
   */
  @FXML
  public void showSettings() {
    navigate("Settings", "/io/github/kullik01/kinonode/fxml/SettingsView.fxml",
        btnSettings);
  }

  /**
   * Switches the main view to the About section.
   */
  @FXML
  private void showAbout() {
    navigate("About", "/io/github/kullik01/kinonode/fxml/AboutView.fxml",
        btnAbout);
  }

  /**
   * Handles the common logic for navigating between major views in the main shell.
   *
   * @param name         The unique name identifier for the view.
   * @param fxmlPath     The classpath URL to the FXML layout file.
   * @param targetButton The sidebar button corresponding to this view.
   */
  private void navigate(String name, String fxmlPath, Button targetButton) {
    if (activeButton == targetButton) {
      return; // Already on this tab
    }

    loadView(name, fxmlPath);
    setActiveButton(targetButton);
  }

  /**
   * Toggles the application's visual theme between Light and Dark modes.
   * Saves the selection to user settings.
   *
   * @param dark True to apply the dark theme, false for the light theme.
   */
  private void toggleTheme(boolean dark) {
    var tmpSettingsRepository =
        AppContext.getInstance().getSettingsRepository();

    // Set AtlantaFX stylesheet globally depending on selected theme option
    if (dark) {
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null)
        btnThemeToggle.getTooltip().setText("Dark Mode");
      Application
          .setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
      tmpSettingsRepository.set("theme_mode", "dark");
    } else {
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null)
        btnThemeToggle.getTooltip().setText("Light Mode");
      Application
          .setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
      tmpSettingsRepository.set("theme_mode", "light");
    }
  }

  /**
   * Updates the visual state of the sidebar buttons to highlight the currently
   * active selection.
   *
   * @param button The button to mark as active.
   */
  private void setActiveButton(Button button) {
    if (activeButton != null) {
      activeButton.getStyleClass().remove("active");
    }
    activeButton = button;
    if (activeButton != null) {
      activeButton.getStyleClass().add("active");
    }
  }

  /**
   * Loads an FXML view into the main content area. Caches loaded views and
   * controllers to optimize performance on subsequent navigations.
   * If the controller implements {@link Refreshable}, it triggers a refresh.
   *
   * @param name     The unique name identifier for the view.
   * @param fxmlPath The classpath URL to the FXML layout file.
   */
  private void loadView(String name, String fxmlPath) {
    try {
      Parent tmpView = views.get(name);
      // FXML file is loaded lazily and cached locally to avoid loading overhead
      // on tab switches
      if (tmpView == null) {
        URL tmpUrl = getClass().getResource(fxmlPath);
        if (tmpUrl == null) {
          throw new IOException("Cannot find FXML: " + fxmlPath);
        }
        FXMLLoader tmpLoader = new FXMLLoader(tmpUrl);
        tmpView = tmpLoader.load();
        views.put(name, tmpView);
        controllers.put(name, tmpLoader.getController());
      }

      contentArea.getChildren().setAll(tmpView);

      // Refresh content if controller is refreshable
      Object tmpController = controllers.get(name);
      if (tmpController instanceof Refreshable refreshable) {
        refreshable.refresh();
      }
    } catch (IOException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to load view " + name, tmpException);
    }
  }

  /**
   * Navigates directly to the IMDb view and loads a specific URL.
   *
   * @param url The IMDb URL to open in the integrated browser.
   */
  public void showImdbWithUrl(String url) {
    if (activeButton != btnImdb) {
      navigate("IMDb", "/io/github/kullik01/kinonode/fxml/ImdbView.fxml",
          btnImdb);
    }
    Object tmpController = controllers.get("IMDb");
    if (tmpController instanceof ImdbController imdbController) {
      imdbController.loadPage(url);
    }
  }

  /**
   * Pre-loads an FXML view in the background without displaying it.
   * Useful for heavy views like the web browser that cause lag on first load.
   *
   * @param name     The unique name identifier for the view.
   * @param fxmlPath The classpath URL to the FXML layout file.
   */
  private void preloadView(String name, String fxmlPath) {
    try {
      Parent tmpView = views.get(name);
      if (tmpView == null) {
        URL tmpUrl = getClass().getResource(fxmlPath);
        if (tmpUrl == null) {
          throw new IOException("Cannot find FXML: " + fxmlPath);
        }
        FXMLLoader tmpLoader = new FXMLLoader(tmpUrl);
        tmpView = tmpLoader.load();
        views.put(name, tmpView);
        controllers.put(name, tmpLoader.getController());
      }
    } catch (IOException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to preload view " + name, tmpException);
    }
  }
}
