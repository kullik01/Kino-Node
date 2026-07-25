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
 * A utility class to display custom, undecorated modal alerts in the
 * application. The alert dialog utilizes the application's CSS theme for
 * styling and implements custom title bar drag logic to allow moving the
 * undecorated window.
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
  public static void showInformation(Window owner, String title, String header,
      String content) {
    Stage tmpStage = new Stage();

    // Inherit the owner stage characteristics if available
    if (owner instanceof Stage ownerStage) {
      tmpStage.initOwner(ownerStage);
      tmpStage.getIcons().addAll(ownerStage.getIcons());
    }

    // Configure stage modality and remove default OS decorations
    tmpStage.initModality(Modality.WINDOW_MODAL);
    tmpStage.initStyle(StageStyle.UNDECORATED);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setStyle(
        "-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
    tmpRoot.getStylesheets()
        .add(CustomAlert.class
            .getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    // Build the Title Bar
    HBox tmpTitleBar = new HBox();
    tmpTitleBar.getStyleClass().add("title-bar");
    tmpTitleBar.setAlignment(Pos.CENTER_LEFT);

    Label tmpTitleLabel = new Label(title);
    tmpTitleLabel.getStyleClass().add("title-bar-label");

    javafx.scene.image.ImageView tmpLogoView = new javafx.scene.image.ImageView(
        new javafx.scene.image.Image(CustomAlert.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/logo.png")));
    tmpLogoView.setFitHeight(16);
    tmpLogoView.setFitWidth(16);

    tmpTitleBar.getChildren().add(tmpLogoView);
    tmpTitleBar.setSpacing(5);

    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

    Button tmpCloseButton = new Button("✕");
    tmpCloseButton.getStyleClass().addAll("window-button", "close-button");
    tmpCloseButton.setOnAction(tmpEvent -> tmpStage.close());

    tmpTitleBar.getChildren().addAll(tmpTitleLabel, tmpSpacer, tmpCloseButton);

    // Build the Content Area
    VBox tmpContentBox = new VBox(15);
    tmpContentBox.setPadding(new Insets(20));
    tmpContentBox.setStyle("-fx-background-radius: 0 0 8px 8px;");

    Label tmpHeaderLabel = new Label(header);
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

    Label tmpTextLabel = new Label(content);
    tmpTextLabel.setWrapText(true);
    tmpTextLabel.setStyle("-fx-text-fill: -color-fg-muted;");

    Button tmpOkButton = new Button("OK");
    tmpOkButton.getStyleClass().add("button-primary");
    tmpOkButton.setOnAction(tmpEvent -> tmpStage.close());
    HBox tmpButtonBox = new HBox(tmpOkButton);
    tmpButtonBox.setAlignment(Pos.CENTER_RIGHT);

    tmpContentBox.getChildren().addAll(tmpHeaderLabel, tmpTextLabel,
        tmpButtonBox);

    tmpRoot.setTop(tmpTitleBar);
    tmpRoot.setCenter(tmpContentBox);

    // Implement custom window dragging logic for undecorated stages
    final double[] tmpXOffset = {0};
    final double[] tmpYOffset = {0};
    tmpTitleBar.setOnMousePressed(event -> {
      tmpXOffset[0] = tmpStage.getX() - event.getScreenX();
      tmpYOffset[0] = tmpStage.getY() - event.getScreenY();
    });
    tmpTitleBar.setOnMouseDragged(event -> {
      tmpStage.setX(event.getScreenX() + tmpXOffset[0]);
      tmpStage.setY(event.getScreenY() + tmpYOffset[0]);
    });

    StackPane tmpWrapper = new StackPane(tmpRoot);
    tmpWrapper.setStyle("-fx-background-color: transparent; -fx-padding: 0px;");

    Scene tmpScene = new Scene(tmpWrapper);
    tmpScene.setFill(Color.TRANSPARENT);
    tmpScene.setCursor(CursorUtils.getAppLogoCursor());
    tmpStage.setScene(tmpScene);

    tmpStage.setWidth(450);
    tmpStage.setHeight(250);

    CursorUtils.centerOnOwnerOrScreen(tmpStage, owner);

    tmpStage.showAndWait();
  }
}
