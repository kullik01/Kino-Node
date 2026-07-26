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
    showInformation(owner, title, header, content, "/io/github/kullik01/kinonode/images/info_icon.png");
  }

  public static void showInformation(Window owner, String title, String header,
      String content, String iconPath) {
    Stage tmpStage = new Stage();

    // Inherit the owner stage characteristics if available
    if (owner instanceof Stage ownerStage) {
      tmpStage.initOwner(ownerStage);
      tmpStage.getIcons().addAll(ownerStage.getIcons());
    }

    // Configure stage modality and remove default OS decorations
    tmpStage.initModality(Modality.WINDOW_MODAL);
    tmpStage.initStyle(StageStyle.TRANSPARENT);

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
    tmpLogoView.setFitHeight(24);
    tmpLogoView.setSmooth(true);
    tmpLogoView.setFitWidth(24);
    javafx.scene.layout.HBox.setMargin(tmpLogoView,
        new javafx.geometry.Insets(0, 0, 0, -5));

    tmpTitleBar.getChildren().add(tmpLogoView);
    tmpTitleBar.setSpacing(5);

    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

    tmpTitleBar.getChildren().addAll(tmpTitleLabel, tmpSpacer);

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
    tmpTextLabel.setPrefWidth(350);
    tmpTextLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

    javafx.scene.Node contentNode = tmpTextLabel;
    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        javafx.scene.Node tmpIconView = createIconNode(iconPath);
        HBox tmpIconBox = new HBox(15, tmpTextLabel, tmpIconView);
        tmpIconBox.setAlignment(Pos.CENTER_LEFT);
        contentNode = tmpIconBox;
      } catch (Exception ignored) {
      }
    }

    Button tmpOkButton = new Button("OK");
    tmpOkButton.getStyleClass().add("button-primary");
    tmpOkButton.setOnAction(tmpEvent -> tmpStage.close());

    HBox tmpButtonBox = new HBox(tmpOkButton);
    tmpButtonBox.setAlignment(Pos.CENTER_RIGHT);

    javafx.scene.layout.Region tmpVSpacer = new javafx.scene.layout.Region();
    javafx.scene.layout.VBox.setVgrow(tmpVSpacer,
        javafx.scene.layout.Priority.ALWAYS);

    tmpContentBox.getChildren().addAll(tmpHeaderLabel, contentNode, tmpVSpacer,
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

    tmpStage.sizeToScene();

    CursorUtils.centerOnOwnerOrScreen(tmpStage, owner);


    tmpStage.showAndWait();
  }

  public static boolean showConfirmation(Window owner, String title,
      String header, String content) {
    return showConfirmation(owner, title, header, content, null);
  }

  public static boolean showConfirmation(Window owner, String title,
      String header, String content, String iconPath) {
    Stage tmpStage = new Stage();
    final boolean[] result = {false};

    if (owner instanceof Stage ownerStage) {
      tmpStage.initOwner(ownerStage);
      tmpStage.getIcons().addAll(ownerStage.getIcons());
    }

    tmpStage.initModality(Modality.WINDOW_MODAL);
    tmpStage.initStyle(StageStyle.TRANSPARENT);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setStyle(
        "-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
    tmpRoot.getStylesheets()
        .add(CustomAlert.class
            .getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    HBox tmpTitleBar = new HBox();
    tmpTitleBar.getStyleClass().add("title-bar");
    tmpTitleBar.setAlignment(Pos.CENTER_LEFT);

    Label tmpTitleLabel = new Label(title);
    tmpTitleLabel.getStyleClass().add("title-bar-label");

    javafx.scene.image.ImageView tmpLogoView = new javafx.scene.image.ImageView(
        new javafx.scene.image.Image(CustomAlert.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/logo.png")));
    tmpLogoView.setFitHeight(24);
    tmpLogoView.setSmooth(true);
    tmpLogoView.setFitWidth(24);
    javafx.scene.layout.HBox.setMargin(tmpLogoView,
        new javafx.geometry.Insets(0, 0, 0, -5));

    tmpTitleBar.getChildren().add(tmpLogoView);
    tmpTitleBar.setSpacing(5);

    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

    tmpTitleBar.getChildren().addAll(tmpTitleLabel, tmpSpacer);

    VBox tmpContentBox = new VBox(15);
    tmpContentBox.setPadding(new Insets(20));
    tmpContentBox.setStyle("-fx-background-radius: 0 0 8px 8px;");

    Label tmpHeaderLabel = new Label(header);
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

    Label tmpTextLabel = new Label(content);
    tmpTextLabel.setWrapText(true);
    tmpTextLabel.setStyle("-fx-text-fill: -color-fg-muted;");
    tmpTextLabel.setPrefWidth(350);
    tmpTextLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

    javafx.scene.Node contentNode = tmpTextLabel;
    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        javafx.scene.Node tmpIconView = createIconNode(iconPath);
        HBox tmpIconBox = new HBox(15, tmpTextLabel, tmpIconView);
        tmpIconBox.setAlignment(Pos.CENTER_LEFT);
        contentNode = tmpIconBox;
      } catch (Exception ignored) {
      }
    }

    Button tmpOkButton = new Button("OK");
    tmpOkButton.getStyleClass().add("button-primary");
    tmpOkButton.setOnAction(tmpEvent -> {
      result[0] = true;
      tmpStage.close();
    });

    Button tmpCancelButton = new Button("Cancel");
    tmpCancelButton.getStyleClass().add("button-primary");
    tmpCancelButton.setOnAction(tmpEvent -> tmpStage.close());

    HBox tmpButtonBox = new HBox(10, tmpCancelButton, tmpOkButton);
    tmpButtonBox.setAlignment(Pos.CENTER_RIGHT);

    javafx.scene.layout.Region tmpVSpacer = new javafx.scene.layout.Region();
    javafx.scene.layout.VBox.setVgrow(tmpVSpacer,
        javafx.scene.layout.Priority.ALWAYS);

    tmpContentBox.getChildren().addAll(tmpHeaderLabel, contentNode, tmpVSpacer,
        tmpButtonBox);

    tmpRoot.setTop(tmpTitleBar);
    tmpRoot.setCenter(tmpContentBox);

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

    tmpStage.sizeToScene();

    CursorUtils.centerOnOwnerOrScreen(tmpStage, owner);


    tmpStage.showAndWait();
    return result[0];
  }

  public static void showError(Window owner, String title, String header,
      String content) {
    showError(owner, title, header, content, null);
  }

  public static void showError(Window owner, String title, String header,
      String content, String iconPath) {
    Stage tmpStage = new Stage();

    if (owner instanceof Stage ownerStage) {
      tmpStage.initOwner(ownerStage);
      tmpStage.getIcons().addAll(ownerStage.getIcons());
    }

    tmpStage.initModality(Modality.WINDOW_MODAL);
    tmpStage.initStyle(StageStyle.TRANSPARENT);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setStyle(
        "-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
    tmpRoot.getStylesheets()
        .add(CustomAlert.class
            .getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    HBox tmpTitleBar = new HBox();
    tmpTitleBar.getStyleClass().add("title-bar");
    tmpTitleBar.setAlignment(Pos.CENTER_LEFT);

    Label tmpTitleLabel = new Label(title);
    tmpTitleLabel.getStyleClass().add("title-bar-label");

    javafx.scene.image.ImageView tmpLogoView = new javafx.scene.image.ImageView(
        new javafx.scene.image.Image(CustomAlert.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/error_icon.png")));
    tmpLogoView.setFitHeight(24);
    tmpLogoView.setSmooth(true);
    tmpLogoView.setFitWidth(24);
    javafx.scene.layout.HBox.setMargin(tmpLogoView,
        new javafx.geometry.Insets(0, 0, 0, -5));

    tmpTitleBar.getChildren().add(tmpLogoView);
    tmpTitleBar.setSpacing(5);

    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

    tmpTitleBar.getChildren().addAll(tmpTitleLabel, tmpSpacer);

    VBox tmpContentBox = new VBox(15);
    tmpContentBox.setPadding(new Insets(20));
    tmpContentBox.setStyle("-fx-background-radius: 0 0 8px 8px;");

    Label tmpHeaderLabel = new Label(header);
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

    Label tmpTextLabel = new Label(content);
    tmpTextLabel.setWrapText(true);
    tmpTextLabel.setStyle("-fx-text-fill: -color-fg-muted;");
    tmpTextLabel.setPrefWidth(350);
    tmpTextLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

    javafx.scene.Node contentNode = tmpTextLabel;
    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        javafx.scene.Node tmpIconView = createIconNode(iconPath);
        HBox tmpIconBox = new HBox(15, tmpTextLabel, tmpIconView);
        tmpIconBox.setAlignment(Pos.CENTER_LEFT);
        contentNode = tmpIconBox;
      } catch (Exception ignored) {
      }
    }

    Button tmpOkButton = new Button("OK");
    tmpOkButton.getStyleClass().add("button-primary");
    tmpOkButton.setOnAction(tmpEvent -> tmpStage.close());

    HBox tmpButtonBox = new HBox(tmpOkButton);
    tmpButtonBox.setAlignment(Pos.CENTER_RIGHT);

    javafx.scene.layout.Region tmpVSpacer = new javafx.scene.layout.Region();
    javafx.scene.layout.VBox.setVgrow(tmpVSpacer,
        javafx.scene.layout.Priority.ALWAYS);

    tmpContentBox.getChildren().addAll(tmpHeaderLabel, contentNode, tmpVSpacer,
        tmpButtonBox);

    tmpRoot.setTop(tmpTitleBar);
    tmpRoot.setCenter(tmpContentBox);

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

    tmpStage.sizeToScene();

    CursorUtils.centerOnOwnerOrScreen(tmpStage, owner);


    tmpStage.showAndWait();
  }

  private static javafx.scene.Node createIconNode(String iconPath) {
    if (iconPath.contains("info_icon")) {
      javafx.scene.shape.SVGPath svg = new javafx.scene.shape.SVGPath();
      svg.setContent("M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z");
      svg.setStyle("-fx-fill: #2178dd;");
      svg.setScaleX(4.0);
      svg.setScaleY(4.0);
      javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(svg);
      pane.setPrefSize(96, 96);
      return pane;
    } else if (iconPath.contains("removal_icon")) {
      javafx.scene.shape.SVGPath svg = new javafx.scene.shape.SVGPath();
      svg.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
      svg.setStyle("-fx-fill: #DC3545;");
      svg.setScaleX(4.0);
      svg.setScaleY(4.0);
      javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(svg);
      pane.setPrefSize(96, 96);
      return pane;
    } else {
      javafx.scene.image.ImageView imgView =
          new javafx.scene.image.ImageView(new javafx.scene.image.Image(
              CustomAlert.class.getResourceAsStream(iconPath)));
      imgView.setFitHeight(96);
      imgView.setFitWidth(96);
      imgView.setSmooth(true);
      return imgView;
    }
  }

}
