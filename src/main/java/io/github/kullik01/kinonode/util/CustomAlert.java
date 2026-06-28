/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * A utility class to display custom, undecorated modal alerts in the application. The alert dialog
 * utilizes the application's CSS theme for styling and implements custom title bar drag logic to
 * allow moving the undecorated window.
 */
public class CustomAlert {

  /**
   * Displays a styled, undecorated modal dialog showing information.
   *
   * @param owner the owner window of this dialog
   * @param title the title text to display in the custom title bar
   * @param header the header text for the dialog content section
   * @param content the detailed message content for the dialog
   */
  public static void showInformation(Window owner, String title, String header, String content) {
    Stage stage = new Stage();

    // Inherit the owner stage characteristics if available
    if (owner instanceof Stage ownerStage) {
      stage.initOwner(ownerStage);
      stage.getIcons().addAll(ownerStage.getIcons());
    }

    // Configure stage modality and remove default OS decorations
    stage.initModality(Modality.WINDOW_MODAL);
    stage.initStyle(StageStyle.UNDECORATED);

    BorderPane root = new BorderPane();
    root.setStyle(
        "-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
    root.getStylesheets()
        .add(
            CustomAlert.class
                .getResource("/io/github/kullik01/kinonode/css/theme.css")
                .toExternalForm());

    // Build the Title Bar
    HBox titleBar = new HBox();
    titleBar.getStyleClass().add("title-bar");
    titleBar.setAlignment(Pos.CENTER_LEFT);

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("title-bar-label");

    javafx.scene.image.ImageView logoView =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(
                CustomAlert.class.getResourceAsStream(
                    "/io/github/kullik01/kinonode/images/logo.png")));
    logoView.setFitHeight(16);
    logoView.setFitWidth(16);

    titleBar.getChildren().add(logoView);
    titleBar.setSpacing(5);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Button closeBtn = new Button("✕");
    closeBtn.getStyleClass().addAll("window-button", "close-button");
    closeBtn.setOnAction(e -> stage.close());

    titleBar.getChildren().addAll(titleLabel, spacer, closeBtn);

    // Build the Content Area
    VBox contentBox = new VBox(15);
    contentBox.setPadding(new Insets(20));
    contentBox.setStyle("-fx-background-radius: 0 0 8px 8px;");

    Label headerLabel = new Label(header);
    headerLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

    Label textLabel = new Label(content);
    textLabel.setWrapText(true);
    textLabel.setStyle("-fx-text-fill: -color-fg-muted;");

    Button okBtn = new Button("OK");
    okBtn.getStyleClass().add("button-primary");
    okBtn.setOnAction(e -> stage.close());
    HBox btnBox = new HBox(okBtn);
    btnBox.setAlignment(Pos.CENTER_RIGHT);

    contentBox.getChildren().addAll(headerLabel, textLabel, btnBox);

    root.setTop(titleBar);
    root.setCenter(contentBox);

    // Implement custom window dragging logic for undecorated stages
    final double[] xOffset = {0};
    final double[] yOffset = {0};
    titleBar.setOnMousePressed(
        event -> {
          xOffset[0] = stage.getX() - event.getScreenX();
          yOffset[0] = stage.getY() - event.getScreenY();
        });
    titleBar.setOnMouseDragged(
        event -> {
          stage.setX(event.getScreenX() + xOffset[0]);
          stage.setY(event.getScreenY() + yOffset[0]);
        });

    StackPane wrapper = new StackPane(root);
    wrapper.setStyle("-fx-background-color: transparent; -fx-padding: 0px;");

    Scene scene = new Scene(wrapper);
    scene.setFill(Color.TRANSPARENT);
    stage.setScene(scene);

    stage.setWidth(450);
    stage.setHeight(250);

    stage.showAndWait();
  }
}
