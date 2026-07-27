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

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogBuilder {

  /**
   * Creates a customized JavaFX Dialog with application branding, transparent window styling,
   * and draggable/resizable behavior.
   *
   * @param <T>        The return type of the dialog.
   * @param title      The window title.
   * @param headerText The primary header text.
   * @param iconPath   The classpath route to an optional icon graphic.
   * @return The configured Dialog instance.
   */
  public static <T> Dialog<T> createDialog(String title, String headerText,
      String iconPath) {
    Dialog<T> tmpDialog = new Dialog<>();

    // Critical: Set transparent stage style BEFORE stage is realized to avoid
    // white corners on rounded dialogs
    try {
      tmpDialog.initStyle(StageStyle.TRANSPARENT);
    } catch (Exception ignored) {
    }

    tmpDialog.setTitle(title);
    tmpDialog.setHeaderText(headerText);

    // Setup default icons
    try {
      Stage tmpStage = (Stage) tmpDialog.getDialogPane().getScene().getWindow();
      tmpStage.getIcons().add(new Image(DialogBuilder.class.getResourceAsStream(
          "/io/github/kullik01/kinonode/images/logo.png")));
    } catch (Exception ignored) {
    }

    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        ImageView tmpCustomIcon = new ImageView(
            new Image(DialogBuilder.class.getResourceAsStream(iconPath)));
        tmpCustomIcon.setFitHeight(48);
        tmpCustomIcon.setFitWidth(48);
        tmpDialog.getDialogPane().setGraphic(tmpCustomIcon);
      } catch (Exception ignored) {
      }
    }

    tmpDialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

    // Let CursorUtils handle the rest of the transparent scene setup and
    // styling
    CursorUtils.applyCursor(tmpDialog);

    // Add resize support
    tmpDialog.setOnShown(e -> {
      Stage stage = (Stage) tmpDialog.getDialogPane().getScene().getWindow();
      ResizeHelper.addResizeListener(stage);
    });

    return tmpDialog;
  }

  /**
   * Displays a simple, branded informational alert dialog.
   *
   * @param title       The window title.
   * @param headerText  The primary header text.
   * @param contentText The detailed message body.
   */
  public static void showInfo(String title, String headerText,
      String contentText) {
    Dialog<ButtonType> tmpDialog = createDialog(title, headerText,
        "/io/github/kullik01/kinonode/images/logo.png");
    tmpDialog.setContentText(contentText);
    tmpDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
    tmpDialog.showAndWait();
  }

  /**
   * Displays a branded error alert dialog with an error icon.
   *
   * @param title       The window title.
   * @param headerText  The primary header text.
   * @param contentText The detailed error message body.
   */
  public static void showError(String title, String headerText,
      String contentText) {
    Dialog<ButtonType> tmpDialog = createDialog(title, headerText,
        "/io/github/kullik01/kinonode/images/error_icon.png");
    tmpDialog.setContentText(contentText);
    tmpDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
    tmpDialog.showAndWait();
  }
}
