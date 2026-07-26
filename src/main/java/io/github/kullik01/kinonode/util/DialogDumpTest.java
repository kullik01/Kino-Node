package io.github.kullik01.kinonode.util;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;

public class DialogDumpTest extends Application {
  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(DialogDumpTest.class.getName());

  @Override
  public void start(Stage primaryStage) {
    Dialog<ButtonType> d = new Dialog<>();
    d.getDialogPane().getButtonTypes().add(ButtonType.OK);
    d.show();
    Parent root = d.getDialogPane().getScene().getRoot();
    dump(root, 0);
    Platform.exit();
  }

  private void dump(Node n, int level) {
    for (int i = 0; i < level; i++)
      System.out.print("  ");
    LOGGER.info(
        n.getClass().getName() + " [styleClass=" + n.getStyleClass() + "]");
    if (n instanceof Parent) {
      for (Node child : ((Parent) n).getChildrenUnmodifiable()) {
        dump(child, level + 1);
      }
    }
  }
}
