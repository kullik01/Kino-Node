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

import io.github.kullik01.kinonode.uiutil.CursorUtils;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import javafx.application.Platform;
import javafx.util.Duration;


import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.kullik01.kinonode.di.AppContext;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The main shell controller of the application. Manages the layout container
 * (sidebar navigation menu, theme toggle, and main content area). Handles
 * switching between different application views (Library, Watchlists, Settings,
 * and About) and dynamically loads/caches their associated view nodes.
 */
public class MainController {
  private static final Logger LOGGER =
      Logger.getLogger(MainController.class.getName());

  private StackPane rootPane;
  private StackPane contentArea;
  private Button btnLibrary;
  private Button btnWatchlists;
  private Button btnAnalytics;
  private Button buttonImdb;
  private Button btnSettings;
  private Button btnAbout;
  private ToggleSwitch btnThemeToggle;

  private final Map<String, Parent> views = new HashMap<>();
  private final Map<String, Object> controllers = new HashMap<>();
  private Button activeButton;
  private double xOffset = 0;
  private double yOffset = 0;

  private final Provider<LibraryController> libraryProvider;
  private final Provider<WatchlistsController> watchlistsProvider;
  private final Provider<SettingsController> settingsProvider;
  private final Provider<ImdbController> imdbProvider;
  private final Provider<AboutController> aboutProvider;
  private final Provider<AnalyticsController> analyticsProvider;
  private final AppContext appContext;
  private final SettingsRepository settingsRepository;
  private final ResourceBundle resources;

  @Inject
  public MainController(Provider<LibraryController> libraryProvider,
      Provider<WatchlistsController> watchlistsProvider,
      Provider<SettingsController> settingsProvider,
      Provider<ImdbController> imdbProvider,
      Provider<AboutController> aboutProvider,
      Provider<AnalyticsController> analyticsProvider, AppContext appContext,
      SettingsRepository settingsRepository, ResourceBundle resources) {
    this.libraryProvider = libraryProvider;
    this.watchlistsProvider = watchlistsProvider;
    this.settingsProvider = settingsProvider;
    this.imdbProvider = imdbProvider;
    this.aboutProvider = aboutProvider;
    this.analyticsProvider = analyticsProvider;
    this.appContext = appContext;
    this.settingsRepository = settingsRepository;
    this.resources = resources;
  }

  public Parent buildView() {
    appContext.setMainController(this);

    rootPane = new StackPane();
    rootPane.setId("rootPane");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());
    rootPane.setStyle("-fx-background-color: transparent;");

    BorderPane mainContainer = new BorderPane();
    mainContainer.setId("mainContainer");
    mainContainer.setPrefHeight(768.0);
    mainContainer.setPrefWidth(1280.0);
    mainContainer.getStyleClass().add("main-window");

    VBox sidebar = new VBox();
    sidebar.getStyleClass().add("sidebar");

    btnLibrary = createSidebarButton(resources.getString("main.library"),
        "btnLibrary", "🎬", this::showLibrary, true);
    btnWatchlists = createSidebarButton(resources.getString("main.watchlists"),
        "btnWatchlists",
        "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 9H10V9h8v2zm-4 4h-4v-2h4v2zm4-8h-8V5h8v2z",
        this::showWatchlists, false);
    btnAnalytics = createSidebarButton(resources.getString("main.analytics"),
        "btnAnalytics",
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z",
        this::showAnalytics, false);
    buttonImdb = createSidebarButton(resources.getString("main.imdb"),
        "buttonImdb",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z",
        this::showImdb, false);
    btnSettings = createSidebarButton(resources.getString("main.settings"),
        "btnSettings",
        "M12 15.5A3.5 3.5 0 0 1 8.5 12 3.5 3.5 0 0 1 12 8.5a3.5 3.5 0 0 1 3.5 3.5 3.5 3.5 0 0 1-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97 0-.33-.03-.66-.07-1l2.11-1.63c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.37-2.65A.506.506 0 0 0 14 2h-4c-.25 0-.46.18-.5.42l-.37 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64L4.57 11c-.04.34-.07.67-.07 1 0 .33.03.65.07.97l-2.11 1.66c-.19.15-.25.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1.01c.52.4 1.06.74 1.69.99l.37 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.37-2.65c.63-.26 1.17-.59 1.69-.99l2.49 1.01c.22.08.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.66Z",
        this::showSettings, false);
    btnAbout = createSidebarButton(resources.getString("main.about"),
        "btnAbout",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
        this::showAbout, false);

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    HBox themeBox = new HBox();
    themeBox.setAlignment(Pos.CENTER);
    themeBox.setStyle("-fx-padding: 0 0 10 0;");

    btnThemeToggle = new ToggleSwitch("☀");
    btnThemeToggle.setId("btnThemeToggle");
    btnThemeToggle.setMinWidth(Control.USE_PREF_SIZE);
    Tooltip themeTooltip = new Tooltip(resources.getString("main.theme.light"));
    themeTooltip.setShowDelay(Duration.millis(250));
    btnThemeToggle.setTooltip(themeTooltip);
    themeBox.getChildren().add(btnThemeToggle);

    sidebar.getChildren().addAll(btnLibrary, btnWatchlists, btnAnalytics,
        buttonImdb, btnSettings, btnAbout, spacer, themeBox);
    mainContainer.setLeft(sidebar);

    VBox centerVBox = new VBox();
    centerVBox.getStyleClass().add("main-content-vbox");

    contentArea = new StackPane();
    contentArea.setId("contentArea");
    VBox.setVgrow(contentArea, Priority.ALWAYS);

    centerVBox.getChildren().add(contentArea);
    mainContainer.setCenter(centerVBox);

    rootPane.getChildren().add(mainContainer);

    return rootPane;
  }

  private Button createSidebarButton(String text, String id, String svgContent,
      Runnable action, boolean isTextGraphic) {
    Button btn = new Button(text);
    btn.setId(id);
    btn.getStyleClass().add("sidebar-button");
    btn.setOnAction(e -> action.run());
    if (isTextGraphic) {
      btn.setMinWidth(Control.USE_PREF_SIZE);
      javafx.scene.text.Text graphic = new javafx.scene.text.Text(svgContent);
      graphic.getStyleClass().add("settings-svg");
      graphic.setStyle("-fx-font-size: 16px;");
      btn.setGraphic(graphic);
    } else {
      SVGPath graphic = new SVGPath();
      graphic.getStyleClass().add("settings-svg");
      graphic.setScaleX(0.75);
      graphic.setScaleY(0.75);
      graphic.setContent(svgContent);
      if (id.equals("buttonImdb"))
        graphic.setId("svgImageImdb");
      if (id.equals("btnSettings"))
        graphic.setId("svgSettings");
      btn.setGraphic(graphic);
      if (id.equals("btnWatchlists") || id.equals("btnAnalytics")) {
        btn.setMinWidth(Control.USE_PREF_SIZE);
      }
    }
    return btn;
  }

  /**
   * Initializes the main controller layout. Restores the theme selection (light
   * or dark mode) from configuration, establishes window listeners for
   * maximization changes, and triggers navigation to the default Library view.
   */
  public void initialize() {

    // Load initial theme button state from settings (Default is light)
    var tmpSettingsRepository = settingsRepository;
    String tmpTheme = tmpSettingsRepository.get("theme_mode", "light");

    // Unselected (false) = Light Mode (Default)
    // Selected (true) = Dark Mode
    if (tmpTheme != null && tmpTheme.equals("dark")) {
      btnThemeToggle.setSelected(true);
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip()
            .setText(resources.getString("main.theme.dark"));
        btnThemeToggle.getTooltip().setShowDelay(Duration.millis(250));
      }
    } else {
      btnThemeToggle.setSelected(false);
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null) {
        btnThemeToggle.getTooltip()
            .setText(resources.getString("main.theme.light"));
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
    CursorUtils.applyCursor(btnLibrary);
    CursorUtils.applyCursor(btnWatchlists);
    CursorUtils.applyCursor(btnAnalytics);
    CursorUtils.applyCursor(buttonImdb);
    CursorUtils.applyCursor(btnSettings);
    CursorUtils.applyCursor(btnAbout);
    CursorUtils.applyCenteredCursor(btnThemeToggle);

    // Show Library by default
    showLibrary();

    // Eagerly preload the IMDb view on startup to begin background loading
    // immediately
    preloadView("IMDb");

    // Apply application logo cursor globally to scene
    Platform.runLater(() -> {
      Scene tmpScene = rootPane.getScene();
      if (tmpScene != null) {
        Cursor tmpCursor = CursorUtils.getAppLogoCursor();
        tmpScene.setCursor(tmpCursor);
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
  private void showLibrary() {
    navigate("Library", btnLibrary);
  }

  /**
   * Switches the main view to the Watchlists section.
   */
  private void showWatchlists() {
    navigate("Watchlists", btnWatchlists);
  }

  /**
   * Switches the main view to the Analytics section.
   */
  private void showAnalytics() {
    navigate("Analytics", btnAnalytics);
  }

  /**
   * Switches the main view to the IMDb search section. If already active, it
   * triggers a refresh on the IMDb view.
   */
  private void showImdb() {
    if (activeButton == buttonImdb) {
      Object controller = controllers.get("IMDb");
      if (controller instanceof Refreshable refreshable) {
        refreshable.refresh();
      }
      return;
    }
    navigate("IMDb", buttonImdb);
  }

  /**
   * Switches the main view to the Settings section.
   */
  public void showSettings() {
    navigate("Settings", btnSettings);
  }

  /**
   * Switches the main view to the About section.
   */
  private void showAbout() {
    navigate("About", btnAbout);
  }

  /**
   * Handles the common logic for navigating between major views in the main
   * shell.
   *
   * @param name The unique name identifier for the view.
   * @param targetButton The sidebar button corresponding to this view.
   */
  private void navigate(String name, Button targetButton) {
    if (activeButton == targetButton) {
      return; // Already on this tab
    }

    loadView(name);
    setActiveButton(targetButton);
  }

  /**
   * Toggles the application's visual theme between Light and Dark modes. Saves
   * the selection to user settings.
   *
   * @param dark True to apply the dark theme, false for the light theme.
   */
  private void toggleTheme(boolean dark) {
    var tmpSettingsRepository = settingsRepository;

    // Set AtlantaFX stylesheet globally depending on selected theme option
    if (dark) {
      btnThemeToggle.setText("🌙");
      if (btnThemeToggle.getTooltip() != null)
        btnThemeToggle.getTooltip()
            .setText(resources.getString("main.theme.dark"));
      Application
          .setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
      tmpSettingsRepository.set("theme_mode", "dark");
    } else {
      btnThemeToggle.setText("☀");
      if (btnThemeToggle.getTooltip() != null)
        btnThemeToggle.getTooltip()
            .setText(resources.getString("main.theme.light"));
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
   * Loads a view into the main content area. Caches loaded views and
   * controllers to optimize performance on subsequent navigations. If the
   * controller implements {@link Refreshable}, it triggers a refresh.
   *
   * @param name The unique name identifier for the view.
   */
  private void loadView(String name) {
    try {
      Parent view = views.get(name);
      // View is loaded lazily and cached locally to avoid loading overhead
      // on tab switches
      if (view == null) {
        view = createView(name);
      }

      contentArea.getChildren().setAll(view);

      // Refresh content if controller is refreshable
      Object controller = controllers.get(name);
      if (controller instanceof Refreshable refreshable) {
        refreshable.refresh();
      }
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "Failed to load view " + name, exception);
    }
  }

  private Parent createView(String name) {
    Parent view = null;
    if ("Library".equals(name)) {
      LibraryController controller = libraryProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else if ("Watchlists".equals(name)) {
      WatchlistsController controller = watchlistsProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else if ("Settings".equals(name)) {
      SettingsController controller = settingsProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else if ("IMDb".equals(name)) {
      ImdbController controller = imdbProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else if ("About".equals(name)) {
      AboutController controller = aboutProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else if ("Analytics".equals(name)) {
      AnalyticsController controller = analyticsProvider.get();
      view = controller.buildView();
      controller.initialize();
      views.put(name, view);
      controllers.put(name, controller);
    } else {
      throw new IllegalArgumentException("Unknown view: " + name);
    }
    return view;
  }

  /**
   * Navigates directly to the IMDb view and loads a specific URL.
   *
   * @param url The IMDb URL to open in the integrated browser.
   */
  public void showImdbWithUrl(String url) {
    if (activeButton != buttonImdb) {
      navigate("IMDb", buttonImdb);
    }
    Object controller = controllers.get("IMDb");
    if (controller instanceof ImdbController imdbController) {
      imdbController.loadPage(url);
    }
  }

  /**
   * Pre-loads a view in the background without displaying it. Useful for heavy
   * views like the web browser that cause lag on first load.
   *
   * @param name The unique name identifier for the view.
   * @param name The unique name identifier for the view.
   */
  private void preloadView(String name) {
    try {
      Parent view = views.get(name);
      if (view == null) {
        createView(name);
      }
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "Failed to preload view " + name, exception);
    }
  }
}
