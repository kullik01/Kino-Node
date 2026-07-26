package io.github.kullik01.kinonode.util;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class DialogTest extends Application {
  @Override
  public void start(Stage primaryStage) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    LOGGER.info("Root == DialogPane? " + (alert.getDialogPane().getScene()
        .getRoot() == alert.getDialogPane()));
    System.exit(0);
  }

  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(DialogTest.class.getName());

  public static void main(String[] args) {
    launch(args);
  }
}
