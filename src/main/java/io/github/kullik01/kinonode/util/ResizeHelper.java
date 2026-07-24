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

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Utility class to make an undecorated/transparent stage resizable. It attaches
 * mouse event handlers to the Stage's Scene and recursively down the Node
 * hierarchy to capture mouse interactions and resize the stage.
 */
public class ResizeHelper {

  /**
   * Attaches a resize listener to the specified JavaFX Stage. This method
   * registers mouse handlers on the stage's scene and all current child nodes
   * within the scene graph.
   *
   * @param stage the JavaFX Stage to be made resizable
   */
  public static void addResizeListener(Stage stage) {
    ResizeListener tmpResizeListener = new ResizeListener(stage);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, tmpResizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED,
        tmpResizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED,
        tmpResizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED,
        tmpResizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED_TARGET,
        tmpResizeListener);
    ObservableList<Node> tmpChildren =
        stage.getScene().getRoot().getChildrenUnmodifiable();
    for (Node tmpChild : tmpChildren) {
      addListenerDeeply(tmpChild, tmpResizeListener);
    }
  }

  private static void addListenerDeeply(Node node,
      EventHandler<MouseEvent> listener) {
    node.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
    node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
    node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
    node.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
    node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener);
    if (node instanceof Parent) {
      Parent tmpParent = (Parent) node;
      ObservableList<Node> tmpChildren = tmpParent.getChildrenUnmodifiable();
      for (Node tmpChild : tmpChildren) {
        addListenerDeeply(tmpChild, listener);
      }
    }
  }

  /**
   * Event handler responsible for capturing mouse events and calculating the
   * appropriate window cursor and resize dimensions.
   */
  static class ResizeListener implements EventHandler<MouseEvent> {
    private final Stage stage;
    private Cursor cursorEvent = Cursor.DEFAULT;
    private int border = 8;
    private double startX = 0;
    private double startY = 0;
    private double startScreenX = 0;
    private double startScreenY = 0;

    /**
     * Constructs a new ResizeListener for the given Stage.
     *
     * @param stage the JavaFX Stage that this listener will resize
     */
    public ResizeListener(Stage stage) {
      this.stage = stage;
    }

    /**
     * Handles incoming MouseEvents, determining cursor visual states, recording
     * mouse positions on click, and performing the window resizing on drag.
     *
     * @param mouseEvent the MouseEvent dispatched to the handler
     */
    @Override
    public void handle(MouseEvent mouseEvent) {
      EventType<? extends MouseEvent> tmpMouseEventType =
          mouseEvent.getEventType();
      Scene tmpScene = stage.getScene();

      double tmpMouseEventX = mouseEvent.getSceneX(),
          tmpMouseEventY = mouseEvent.getSceneY(),
          tmpSceneWidth = tmpScene.getWidth(),
          tmpSceneHeight = tmpScene.getHeight();

      // Adjust the mouse cursor representation based on the hover position
      // relative to border
      // boundaries
      if (MouseEvent.MOUSE_MOVED.equals(tmpMouseEventType)) {
        if (tmpMouseEventX < border && tmpMouseEventY < border) {
          cursorEvent = Cursor.NW_RESIZE;
        } else if (tmpMouseEventX < border
            && tmpMouseEventY > tmpSceneHeight - border) {
          cursorEvent = Cursor.SW_RESIZE;
        } else if (tmpMouseEventX > tmpSceneWidth - border
            && tmpMouseEventY < border) {
          cursorEvent = Cursor.NE_RESIZE;
        } else if (tmpMouseEventX > tmpSceneWidth - border
            && tmpMouseEventY > tmpSceneHeight - border) {
          cursorEvent = Cursor.SE_RESIZE;
        } else if (tmpMouseEventX < border) {
          cursorEvent = Cursor.W_RESIZE;
        } else if (tmpMouseEventX > tmpSceneWidth - border) {
          cursorEvent = Cursor.E_RESIZE;
        } else if (tmpMouseEventY < border) {
          cursorEvent = Cursor.N_RESIZE;
        } else if (tmpMouseEventY > tmpSceneHeight - border) {
          cursorEvent = Cursor.S_RESIZE;
        if (!Cursor.DEFAULT.equals(cursorEvent)) {
          tmpScene.setCursor(cursorEvent);
        } else {
          tmpScene.setCursor(CursorUtils.getAppLogoCursor());
        }
      } else if (MouseEvent.MOUSE_EXITED.equals(tmpMouseEventType)
          || MouseEvent.MOUSE_EXITED_TARGET.equals(tmpMouseEventType)) {
        tmpScene.setCursor(CursorUtils.getAppLogoCursor());
      } else if (MouseEvent.MOUSE_PRESSED.equals(tmpMouseEventType)) {
        // Store mouse press anchor offsets used during resize calculation
        startX = stage.getWidth() - tmpMouseEventX;
        startY = stage.getHeight() - tmpMouseEventY;
        startScreenX = mouseEvent.getScreenX();
        startScreenY = mouseEvent.getScreenY();
      } else if (MouseEvent.MOUSE_DRAGGED.equals(tmpMouseEventType)) {
        if (!Cursor.DEFAULT.equals(cursorEvent)) {
          tmpScene.setCursor(cursorEvent);
        }
          double tmpMinWidth =
              stage.getMinWidth() > 0 ? stage.getMinWidth() : 400;
          double tmpMinHeight =
              stage.getMinHeight() > 0 ? stage.getMinHeight() : 300;

          // Handle vertical resizing (North / South edges)
          if (!Cursor.W_RESIZE.equals(cursorEvent)
              && !Cursor.E_RESIZE.equals(cursorEvent)) {
            // Dragging from the top (North) requires adjusting both the height
            // and the Y position
            // of the stage
            if (Cursor.NW_RESIZE.equals(cursorEvent)
                || Cursor.N_RESIZE.equals(cursorEvent)
                || Cursor.NE_RESIZE.equals(cursorEvent)) {
              double tmpNewHeight =
                  stage.getHeight() - (mouseEvent.getScreenY() - stage.getY());
              if (tmpNewHeight >= tmpMinHeight) {
                stage.setHeight(tmpNewHeight);
                stage.setY(mouseEvent.getScreenY());
              }
            } else {
              // Dragging from the bottom (South) only requires adjusting the
              // height
              double tmpNewHeight =
                  mouseEvent.getScreenY() - stage.getY() + startY;
              if (tmpNewHeight >= tmpMinHeight) {
                stage.setHeight(tmpNewHeight);
              }
            }
          }

          // Handle horizontal resizing (West / East edges)
          if (!Cursor.N_RESIZE.equals(cursorEvent)
              && !Cursor.S_RESIZE.equals(cursorEvent)) {
            // Dragging from the left (West) requires adjusting both the width
            // and the X position of
            // the stage
            if (Cursor.NW_RESIZE.equals(cursorEvent)
                || Cursor.W_RESIZE.equals(cursorEvent)
                || Cursor.SW_RESIZE.equals(cursorEvent)) {
              double tmpNewWidth =
                  stage.getWidth() - (mouseEvent.getScreenX() - stage.getX());
              if (tmpNewWidth >= tmpMinWidth) {
                stage.setWidth(tmpNewWidth);
                stage.setX(mouseEvent.getScreenX());
              }
            } else {
              // Dragging from the right (East) only requires adjusting the
              // width
              double tmpNewWidth =
                  mouseEvent.getScreenX() - stage.getX() + startX;
              if (tmpNewWidth >= tmpMinWidth) {
                stage.setWidth(tmpNewWidth);
              }
            }
          }
        }
      }
    }
  }
}
