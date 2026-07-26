package io.github.kullik01.kinonode.util;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResizeHelper {

  private static final int RESIZE_MARGIN = 10;
  private static boolean isRightEdge = false;
  private static boolean isBottomEdge = false;
  private static boolean isBottomRightCorner = false;
  private static double initX = -1;
  private static double initY = -1;
  private static double startWidth = -1;
  private static double startHeight = -1;

  public static void addResizeListener(Stage stage) {
    Scene scene = stage.getScene();
    if (scene == null)
      return;

    scene.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
      double x = event.getSceneX();
      double y = event.getSceneY();
      double width = scene.getWidth();
      double height = scene.getHeight();

      isRightEdge = x >= width - RESIZE_MARGIN;
      isBottomEdge = y >= height - RESIZE_MARGIN;
      isBottomRightCorner = isRightEdge && isBottomEdge;

      if (isBottomRightCorner) {
        scene.setCursor(Cursor.SE_RESIZE);
      } else if (isRightEdge) {
        scene.setCursor(Cursor.E_RESIZE);
      } else if (isBottomEdge) {
        scene.setCursor(Cursor.S_RESIZE);
      } else {
        scene.setCursor(CursorUtils.getAppLogoCursor());
      }
    });

    scene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
      if (isRightEdge || isBottomEdge || isBottomRightCorner) {
        initX = event.getScreenX();
        initY = event.getScreenY();
        startWidth = stage.getWidth();
        startHeight = stage.getHeight();
        event.consume();
      }
    });

    scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
      if (isRightEdge || isBottomRightCorner) {
        double newWidth = startWidth + (event.getScreenX() - initX);
        if (newWidth >= stage.getMinWidth()) {
          stage.setWidth(newWidth);
        }
      }
      if (isBottomEdge || isBottomRightCorner) {
        double newHeight = startHeight + (event.getScreenY() - initY);
        if (newHeight >= stage.getMinHeight()) {
          stage.setHeight(newHeight);
        }
      }
    });
  }
}
