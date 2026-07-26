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

  public static void showInfo(String title, String headerText,
      String contentText) {
    Dialog<ButtonType> tmpDialog = createDialog(title, headerText,
        "/io/github/kullik01/kinonode/images/logo.png");
    tmpDialog.setContentText(contentText);
    tmpDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
    tmpDialog.showAndWait();
  }

  public static void showError(String title, String headerText,
      String contentText) {
    Dialog<ButtonType> tmpDialog = createDialog(title, headerText,
        "/io/github/kullik01/kinonode/images/error_icon.png");
    tmpDialog.setContentText(contentText);
    tmpDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
    tmpDialog.showAndWait();
  }
}
