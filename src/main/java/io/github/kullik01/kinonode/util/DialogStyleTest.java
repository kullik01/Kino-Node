package io.github.kullik01.kinonode.util;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogStyleTest extends Application {
  @Override
  public void start(Stage primaryStage) {
    try {
      Dialog<Void> d = new Dialog<>();
      d.initStyle(StageStyle.TRANSPARENT);
      System.out.println("INIT_STYLE_SUCCESS");
    } catch (Exception e) {
      e.printStackTrace();
    }
    Platform.exit();
  }
}
