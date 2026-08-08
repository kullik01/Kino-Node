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
package io.github.kullik01.kinonode;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.kullik01.kinonode.di.AppContext;
import java.io.InputStream;
import java.util.logging.LogManager;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for KinoNode. Bootstraps services and sets up window
 * presentation. This class handles database service initialization,
 * configuration-based theme loading, programmatic layout loading, window setup,
 * and cleanup on exit.
 */
public class KinoNodeApp extends Application {

  private static KinoNodeApp instance;

  public KinoNodeApp() {
    instance = this;
  }

  public static KinoNodeApp getInstance() {
    return instance;
  }

  /**
   * Starts the primary stage of the JavaFX application. This method initializes
   * the application context, reads the user's preferred theme, configures the
   * global stylesheet, loads the main view layout, and displays the main
   * window.
   *
   * @param primaryStage the primary Stage for this application, onto which the
   *        application scene can be set
   * @throws Exception if programmatic loading fails or context initialization
   *         encounters an error
   */
  @Override
  public void start(Stage primaryStage) throws Exception {
    try {
      // Initialize Database and Repositories via the application context
      // singleton
      AppContext.getInstance();

      // Retrieve user's configured theme and apply the corresponding stylesheet
      var tmpSettingsRepository =
          AppContext.getInstance().getSettingsRepository();
      String tmpTheme = tmpSettingsRepository.get("theme_mode", "light");
      if ("dark".equals(tmpTheme)) {
        // Use AtlantaFX PrimerDark theme for dark mode
        Application
            .setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
      } else {
        // Default to AtlantaFX PrimerLight theme for light mode
        Application
            .setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
      }

      // Set high-DPI resolution properties for crisp rendering on HiDPI / 4K /
      // Retina screens
      System.setProperty("prism.allowhidpi", "true");
      System.setProperty("prism.lcdtext", "true");
      System.setProperty("prism.text", "t2k");
      System.setProperty("glass.win.uiScale.enabled", "true");
      System.setProperty("sun.java2d.dpiaware", "true");

      // Build the JavaFX node tree programmatically using Guice Injector
      io.github.kullik01.kinonode.controller.MainController tmpController =
          AppContext.getInstance().getInjector().getInstance(
              io.github.kullik01.kinonode.controller.MainController.class);
      Parent tmpRoot = tmpController.buildView();
      tmpController.initialize();

      // Set the application logo and title
      primaryStage.getIcons()
          .add(new javafx.scene.image.Image(getClass().getResourceAsStream(
              "/io/github/kullik01/kinonode/images/logos/logo.png")));
      primaryStage.setTitle("KinoNode - Personal Movie Library");

      // Calculate adaptive, high-resolution initial window dimensions based on
      // screen bounds
      javafx.geometry.Rectangle2D tmpVisualBounds =
          javafx.stage.Screen.getPrimary().getVisualBounds();
      double tmpTargetWidth =
          Math.max(960, Math.min(1600, tmpVisualBounds.getWidth() * 0.80));
      double tmpTargetHeight =
          Math.max(680, Math.min(1000, tmpVisualBounds.getHeight() * 0.80));

      primaryStage.setMinWidth(Math.min(800, tmpVisualBounds.getWidth() * 0.5));
      primaryStage
          .setMinHeight(Math.min(600, tmpVisualBounds.getHeight() * 0.5));

      // Initialize the main scene, associate it with the stage, and display the
      // window
      Scene tmpScene = new Scene(tmpRoot, tmpTargetWidth, tmpTargetHeight);
      primaryStage.setScene(tmpScene);

      // Check maximized state settings
      String tmpAlwaysMax =
          tmpSettingsRepository.get("always_start_maximized", "false");
      if ("true".equals(tmpAlwaysMax)) {
        primaryStage.setMaximized(true);
      } else {
        String tmpIsMax =
            tmpSettingsRepository.get("window_is_maximized", "false");
        primaryStage.setMaximized("true".equals(tmpIsMax));
      }

      // Track maximized state changes
      primaryStage.maximizedProperty().addListener((tmpObs, tmpOld, tmpNew) -> {
        tmpSettingsRepository.set("window_is_maximized",
            tmpNew ? "true" : "false");
      });

      primaryStage.show();

      Platform.runLater(() -> {
        primaryStage.setIconified(false);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.toFront();
        primaryStage.requestFocus();
        primaryStage.setAlwaysOnTop(false);
      });
    } catch (Throwable tmpThrowable) {
      java.util.logging.Logger.getLogger(KinoNodeApp.class.getName()).log(
          java.util.logging.Level.SEVERE,
          "Fatal exception encountered during application startup",
          tmpThrowable);
      showFatalStartupErrorDialog(primaryStage, tmpThrowable);
    }
  }

  private void showFatalStartupErrorDialog(Stage stage, Throwable throwable) {
    try {
      javafx.scene.control.Alert tmpAlert = new javafx.scene.control.Alert(
          javafx.scene.control.Alert.AlertType.ERROR);
      tmpAlert.setTitle("KinoNode - Startup Error");
      tmpAlert.setHeaderText("Failed to initialize application context");
      tmpAlert.setContentText("An unexpected error occurred during startup:\n\n"
          + throwable.getClass().getName() + ": " + throwable.getMessage()
          + "\n\nPlease verify file permissions and system configuration.");
      tmpAlert.showAndWait();
    } catch (Exception tmpIgnored) {
      System.err.println("Fatal startup error: " + throwable.getMessage());
      throwable.printStackTrace();
    }
  }

  /**
   * Performs clean shutdown operations when the application stops. This ensures
   * the database connections and other application context resources are closed
   * properly on app exit.
   *
   * @throws Exception if an error occurs during shutdown
   */
  @Override
  public void stop() throws Exception {
    // Shut down database service cleanly on app exit
    AppContext.getInstance().close();
    super.stop();
  }

  /**
   * Main entry point for the application. Launches the JavaFX application
   * framework.
   *
   * @param arguments the command-line arguments
   */
  public static void main(String[] arguments) {
    Thread.setDefaultUncaughtExceptionHandler((tmpThread, tmpThrowable) -> {
      java.util.logging.Logger.getLogger(KinoNodeApp.class.getName()).log(
          java.util.logging.Level.SEVERE,
          "Uncaught exception on thread " + tmpThread.getName(), tmpThrowable);
    });

    try (InputStream tmpConfigStream = KinoNodeApp.class.getResourceAsStream(
        "/io/github/kullik01/kinonode/config/logging.properties")) {
      if (tmpConfigStream != null) {
        LogManager.getLogManager().readConfiguration(tmpConfigStream);
      } else {
        System.err
            .println("Could not find logging.properties configuration file");
      }
    } catch (Exception tmpException) {
      System.err
          .println("Error initializing logger: " + tmpException.getMessage());
    }
    launch(arguments);
  }
}
