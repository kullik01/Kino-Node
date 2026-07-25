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
package io.github.kullik01.kinonode.util;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;
import javafx.stage.Window;
import javafx.stage.Stage;

/**
 * Utility class to provide custom mouse cursors across the application.
 */
public class CursorUtils {
  private static final Logger LOGGER =
      Logger.getLogger(CursorUtils.class.getName());

  private static Cursor appLogoCursor;
  private static Cursor centeredAppLogoCursor;

  /**
   * Gets the transparent application logo mouse cursor.
   *
   * @return custom application logo {@link Cursor}, or {@link Cursor#DEFAULT} as fallback
   */
  public static Cursor getAppLogoCursor() {
    if (appLogoCursor == null) {
      try {
        InputStream tmpStream = CursorUtils.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/app_logo_cursor.png");
        if (tmpStream != null) {
          Image tmpImage = new Image(tmpStream);
          appLogoCursor = new ImageCursor(tmpImage, tmpImage.getWidth() / 2.0,
              tmpImage.getHeight() / 2.0);
        } else {
          appLogoCursor = Cursor.DEFAULT;
        }
      } catch (Exception tmpException) {
        LOGGER.log(Level.WARNING,
            "Failed to load application logo cursor image", tmpException);
        appLogoCursor = Cursor.DEFAULT;
      }
    }
    return appLogoCursor;
  }

  /**
   * Gets the transparent application logo mouse cursor centered at the image.
   *
   * @return custom centered application logo {@link Cursor}, or {@link Cursor#DEFAULT} as fallback
   */
  public static Cursor getCenteredAppLogoCursor() {
    if (centeredAppLogoCursor == null) {
      try {
        InputStream tmpStream = CursorUtils.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/app_logo_cursor.png");
        if (tmpStream != null) {
          Image tmpImage = new Image(tmpStream);
          centeredAppLogoCursor = new ImageCursor(tmpImage, tmpImage.getWidth() / 2.0,
              tmpImage.getHeight() / 2.0);
        } else {
          centeredAppLogoCursor = Cursor.DEFAULT;
        }
      } catch (Exception tmpException) {
        LOGGER.log(Level.WARNING,
            "Failed to load centered application logo cursor image", tmpException);
        centeredAppLogoCursor = Cursor.DEFAULT;
      }
    }
    return centeredAppLogoCursor;
  }

  /**
   * Backwards compatible method returning the application logo cursor.
   *
   * @return custom application logo {@link Cursor}
   */
  public static Cursor getCameraCursor() {
    return getAppLogoCursor();
  }

  /**
   * Centers a window dead in the middle of its owner window or screen.
   *
   * @param window the {@link Window} to position
   * @param owner the owner {@link Window} to center over
   */
  public static void centerOnOwnerOrScreen(Window window, Window owner) {
    if (window == null) return;

    javafx.beans.value.ChangeListener<Number> positioner = new javafx.beans.value.ChangeListener<Number>() {
      private boolean adjusting = false;
      @Override
      public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
        if (adjusting) return;
        adjusting = true;
        try {
          Window activeOwner = owner;
          if (activeOwner == null && window instanceof javafx.stage.Stage stageWin) {
            activeOwner = stageWin.getOwner();
          }
          if (activeOwner == null) {
            for (Window win : Window.getWindows()) {
              if (win != window && win instanceof Stage stage && stage.isShowing()) {
                activeOwner = stage;
                break;
              }
            }
          }
          if (activeOwner != null && activeOwner.isShowing() && activeOwner.getWidth() > 0 && activeOwner.getHeight() > 0) {
            double ownerX = activeOwner.getX();
            double ownerY = activeOwner.getY();
            double ownerWidth = activeOwner.getWidth();
            double ownerHeight = activeOwner.getHeight();

            double winWidth = window.getWidth();
            double winHeight = window.getHeight();

            if (winWidth > 0 && winHeight > 0) {
              window.setX(ownerX + (ownerWidth - winWidth) / 2.0);
              window.setY(ownerY + (ownerHeight - winHeight) / 2.0);
              return;
            }
          }
          window.centerOnScreen();
        } finally {
          adjusting = false;
        }
      }
    };

    window.widthProperty().addListener(positioner);
    window.heightProperty().addListener(positioner);

    // Initial check to attach owner listeners
    Runnable attachOwnerListeners = () -> {
      Window activeOwner = owner;
      if (activeOwner == null && window instanceof javafx.stage.Stage stageWin) {
        activeOwner = stageWin.getOwner();
      }
      if (activeOwner == null) {
        for (Window win : Window.getWindows()) {
          if (win != window && win instanceof Stage stage && stage.isShowing()) {
            activeOwner = stage;
            break;
          }
        }
      }
      if (activeOwner != null) {
        activeOwner.xProperty().addListener(positioner);
        activeOwner.yProperty().addListener(positioner);
        activeOwner.widthProperty().addListener(positioner);
        activeOwner.heightProperty().addListener(positioner);

        Window finalOwner = activeOwner;
        window.showingProperty().addListener((obs, wasShowing, isShowing) -> {
          if (!isShowing) {
            window.widthProperty().removeListener(positioner);
            window.heightProperty().removeListener(positioner);
            finalOwner.xProperty().removeListener(positioner);
            finalOwner.yProperty().removeListener(positioner);
            finalOwner.widthProperty().removeListener(positioner);
            finalOwner.heightProperty().removeListener(positioner);
          }
        });
      }
    };

    if (window.isShowing()) {
      attachOwnerListeners.run();
      positioner.changed(null, null, null);
    } else {
      window.setOnShown(e -> {
        attachOwnerListeners.run();
        positioner.changed(null, null, null);
      });
    }
  }

  /**
   * Applies the high-resolution application logo cursor to a dialog window
   * and centers it over the owner window or screen.
   *
   * @param dialog the JavaFX {@link javafx.scene.control.Dialog} to configure
   */
  public static void applyCursor(javafx.scene.control.Dialog<?> dialog) {
    if (dialog != null && dialog.getDialogPane() != null) {
      javafx.scene.Node pane = dialog.getDialogPane();
      if (pane.getScene() != null) {
        pane.getScene().setCursor(getAppLogoCursor());
      } else {
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
          if (newScene != null) {
            newScene.setCursor(getAppLogoCursor());
          }
        });
      }
      dialog.showingProperty().addListener((obs, wasShowing, isShowing) -> {
        if (isShowing) {
          if (dialog.getDialogPane() != null && dialog.getDialogPane().getScene() != null) {
            centerOnOwnerOrScreen(dialog.getDialogPane().getScene().getWindow(), dialog.getOwner());
          }
          javafx.application.Platform.runLater(() -> {
            if (dialog.getDialogPane() != null) {
              for (javafx.scene.control.ButtonType type : dialog.getDialogPane().getButtonTypes()) {
                javafx.scene.Node button = dialog.getDialogPane().lookupButton(type);
                if (button != null && !button.getStyleClass().contains("button-primary")) {
                  button.getStyleClass().add("button-primary");
                }
              }
            }
          });
        }
      });
    }
  }

  /**
   * Applies the high-resolution application logo cursor to a stage window
   * and centers it over the owner window or screen.
   *
   * @param stage the JavaFX {@link javafx.stage.Stage} to configure
   */
  public static void applyCursor(javafx.stage.Stage stage) {
    if (stage != null) {
      if (stage.getScene() != null) {
        stage.getScene().setCursor(getAppLogoCursor());
      } else {
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
          if (newScene != null) {
            newScene.setCursor(getAppLogoCursor());
          }
        });
      }
      centerOnOwnerOrScreen(stage, stage.getOwner());
    }
  }
}
