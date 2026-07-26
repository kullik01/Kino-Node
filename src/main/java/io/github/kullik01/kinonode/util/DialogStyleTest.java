package io.github.kullik01.kinonode.util;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogStyleTest extends Application {
  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(DialogStyleTest.class.getName());

  @Override
  public void start(Stage primaryStage) {
    try {
      Dialog<Void> d = new Dialog<>();
      d.initStyle(StageStyle.TRANSPARENT);
      LOGGER.info("INIT_STYLE_SUCCESS");
    } catch (Exception e) {
      LOGGER.log(java.util.logging.Level.SEVERE, "Exception", e);
    }
    Platform.exit();
  }
}
