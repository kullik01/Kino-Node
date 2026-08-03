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
package io.github.kullik01.kinonode.uiutil;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Utility class providing logic to add manual resizing capabilities to
 * undecorated/transparent JavaFX Stages (e.g. custom dialogs).
 */
public class ResizeHelper {

  private static final int RESIZE_MARGIN = 10;
  private static boolean isRightEdge = false;
  private static boolean isBottomEdge = false;
  private static boolean isBottomRightCorner = false;
  private static double initX = -1;
  private static double initY = -1;
  private static double startWidth = -1;
  private static double startHeight = -1;

  /**
   * Attaches mouse event listeners to a given Stage's Scene to allow manual
   * resizing by dragging the right edge, bottom edge, or bottom-right corner.
   *
   * @param stage The undecorated JavaFX Stage to make resizable.
   */
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
