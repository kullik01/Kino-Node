package io.github.kullik01.kinonode.controller;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.application.Application;

public class ModalTest extends Application {
  @Override
  public void start(Stage primaryStage) {
    try {
      Stage tmpStage = new Stage();
      tmpStage.initModality(Modality.WINDOW_MODAL);
      // No owner set!
      tmpStage.showAndWait();
      System.out.println("Success!");
    } catch (Exception e) {
      e.printStackTrace();
    }
    Platform.exit();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
