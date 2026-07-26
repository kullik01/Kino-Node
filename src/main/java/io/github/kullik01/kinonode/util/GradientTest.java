package io.github.kullik01.kinonode.util;

import javafx.scene.shape.SVGPath;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.application.Platform;

public class GradientTest extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    SVGPath tmpIcon = new SVGPath();
    tmpIcon.setContent(
        "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z");
    tmpIcon.setStyle(
        "-fx-fill: linear-gradient(to right, #ED8B00 50%, #666666 50%);");
    System.out.println("Gradient test compiled and ran.");
    Platform.exit();
  }
}
