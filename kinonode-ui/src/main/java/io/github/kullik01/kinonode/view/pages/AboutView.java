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
package io.github.kullik01.kinonode.view.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.util.ResourceBundle;

public class AboutView {

  private final ScrollPane rootPane;

  public final Label lblVersion = new Label("1.0.0");
  public final TextField lblExtensions = new TextField();
  public final TextField lblDbPath = new TextField();
  public final Label lblStorageMode = new Label("");
  public final Label lblJavaRuntime = new Label("Unknown");
  public final Label lblOsDetails = new Label("Unknown");
  public final Label lblMovieCount = new Label("0 movies");
  public final Label lblDbSize = new Label("Unknown");
  public final Label lblRegularWatchlistCount = new Label("0");
  public final Label lblSmartWatchlistCount = new Label("0");
  public final Label lblWorkspaceSize = new Label("Unknown");
  public final TextField lblPlayerPath = new TextField("None");

  public final Button btnGitHub;
  public final Button btnBug;
  public final Button btnUpdate;
  public final Button btnPlayerLoc = new Button("📂");
  public final Button btnDbLoc = new Button("📂");

  public AboutView(ResourceBundle resources) {
    btnGitHub = new Button(resources.getString("about.btn.github"));
    btnBug = new Button(resources.getString("about.btn.bug"));
    btnUpdate = new Button(resources.getString("about.btn.update"));

    rootPane = new ScrollPane();
    rootPane.setFitToWidth(true);
    rootPane.setFitToHeight(true);
    rootPane.setStyle("-fx-background-color: transparent;");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    VBox mainContent = new VBox(25.0);
    mainContent.getStyleClass().add("detail-pane");
    mainContent.setPadding(new Insets(30.0, 30.0, 30.0, 30.0));

    // Header Section
    VBox headerSection = new VBox(10.0);
    headerSection.setAlignment(Pos.CENTER);

    ImageView logoView = new ImageView();
    logoView.setFitHeight(100.0);
    logoView.setFitWidth(100.0);
    logoView.setPreserveRatio(true);
    logoView.setSmooth(true);
    Image logoImage = new Image(getClass()
        .getResource("/io/github/kullik01/kinonode/images/logos/logo.png")
        .toExternalForm(), 200, 200, true, true);
    logoView.setImage(logoImage);

    Label appTitle = new Label(resources.getString("about.title"));
    appTitle.getStyleClass().add("title-1");

    Label appSubtitle = new Label(resources.getString("about.subtitle"));
    appSubtitle.getStyleClass().add("text-muted");
    appSubtitle.setStyle("-fx-font-size: 16px;");

    HBox btnBox = new HBox(15.0);
    btnBox.setAlignment(Pos.CENTER);
    btnBox.setPadding(new Insets(15.0, 0, 15.0, 0));

    btnGitHub.getStyleClass().add("accent");
    btnGitHub.setStyle("-fx-text-fill: white;");
    btnBug.setStyle("-fx-text-fill: white;");
    btnUpdate.setStyle("-fx-text-fill: white;");

    btnBox.getChildren().addAll(btnGitHub, btnBug, btnUpdate);
    headerSection.getChildren().addAll(logoView, appTitle, appSubtitle, btnBox);

    // Cards Row
    HBox cardsRow = new HBox(20.0);
    cardsRow.setAlignment(Pos.TOP_LEFT);

    // Left Card
    VBox leftCard = new VBox();
    leftCard.getStyleClass().add("about-card");
    HBox.setHgrow(leftCard, Priority.ALWAYS);

    Label leftCardTitle = new Label(resources.getString("about.card.details"));
    leftCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region sep1 = new Region();
    sep1.setMinHeight(Region.USE_PREF_SIZE);
    sep1.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane grid1 = new GridPane();
    grid1.setHgap(15.0);
    grid1.setVgap(12.0);
    ColumnConstraints gc1 = new ColumnConstraints();
    gc1.setMinWidth(110.0);
    gc1.setPrefWidth(140.0);
    ColumnConstraints gc2 = new ColumnConstraints();
    gc2.setHgrow(Priority.ALWAYS);
    grid1.getColumnConstraints().addAll(gc1, gc2);

    lblVersion.setId("lblVersion");
    addGridRow(grid1, 0, resources.getString("about.label.version"),
        lblVersion);
    addGridRow(grid1, 1, resources.getString("about.label.developer"),
        new Label(resources.getString("about.val.developer")));
    addGridRow(grid1, 2, resources.getString("about.label.designers"),
        new Label(resources.getString("about.val.designers")));
    addGridRow(grid1, 3, resources.getString("about.label.license"),
        new Label(resources.getString("about.val.license")));
    addGridRow(grid1, 4, resources.getString("about.label.copyright"),
        new Label(resources.getString("about.val.copyright")));

    Label envTitle = new Label(resources.getString("about.card.env"));
    envTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(envTitle, new Insets(15.0, 0, 0, 0));
    Region sep2 = new Region();
    sep2.setMinHeight(Region.USE_PREF_SIZE);
    sep2.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane grid2 = new GridPane();
    grid2.setHgap(15.0);
    grid2.setVgap(12.0);
    grid2.getColumnConstraints().addAll(gc1, gc2);

    lblJavaRuntime.setId("lblJavaRuntime");
    addGridRow(grid2, 0, resources.getString("about.label.java"),
        lblJavaRuntime);
    lblOsDetails.setId("lblOsDetails");
    addGridRow(grid2, 1, resources.getString("about.label.os"), lblOsDetails);

    Label builtTitle = new Label(resources.getString("about.card.built"));
    builtTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(builtTitle, new Insets(15.0, 0, 0, 0));
    Region sep3 = new Region();
    sep3.setMinHeight(Region.USE_PREF_SIZE);
    sep3.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox builtBox = new VBox(4.0);
    builtBox.getChildren().addAll(
        createMutedLabel(resources.getString("about.built.java")),
        createMutedLabel(resources.getString("about.built.javafx")),
        createMutedLabel(resources.getString("about.built.sqlite")),
        createMutedLabel(resources.getString("about.built.atlantafx")),
        createMutedLabel(resources.getString("about.built.gson")));

    leftCard.getChildren().addAll(leftCardTitle, sep1, grid1, envTitle, sep2,
        grid2, builtTitle, sep3, builtBox);

    // Right Card
    VBox rightCard = new VBox();
    rightCard.getStyleClass().add("about-card");
    HBox.setHgrow(rightCard, Priority.ALWAYS);

    Label rightCardTitle = new Label(resources.getString("about.card.storage"));
    rightCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region sep4 = new Region();
    sep4.setMinHeight(Region.USE_PREF_SIZE);
    sep4.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane grid3 = new GridPane();
    grid3.setHgap(15.0);
    grid3.setVgap(12.0);
    ColumnConstraints gc3 = new ColumnConstraints();
    gc3.setMinWidth(130.0);
    gc3.setPrefWidth(140.0);
    grid3.getColumnConstraints().addAll(gc3, gc2);

    lblWorkspaceSize.setId("lblWorkspaceSize");
    addGridRow(grid3, 0, resources.getString("about.label.workspace"),
        lblWorkspaceSize);
    lblDbSize.setId("lblDbSize");
    addGridRow(grid3, 1, resources.getString("about.label.db_size"), lblDbSize);
    lblMovieCount.setId("lblMovieCount");
    addGridRow(grid3, 2, resources.getString("about.label.movies"),
        lblMovieCount);
    lblRegularWatchlistCount.setId("lblRegularWatchlistCount");
    addGridRow(grid3, 3, resources.getString("about.label.regular_watchlists"),
        lblRegularWatchlistCount);
    lblSmartWatchlistCount.setId("lblSmartWatchlistCount");
    addGridRow(grid3, 4, resources.getString("about.label.smart_watchlists"),
        lblSmartWatchlistCount);

    rightCard.getChildren().addAll(rightCardTitle, sep4, grid3);

    cardsRow.getChildren().addAll(leftCard, rightCard);

    // Full-width card
    VBox fullCard = new VBox();
    fullCard.getStyleClass().add("about-card");

    Label fullCardTitle = new Label(resources.getString("about.card.paths"));
    fullCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region sep5 = new Region();
    sep5.setMinHeight(Region.USE_PREF_SIZE);
    sep5.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox playerBox = new VBox(8.0);
    Label playerLabel =
        new Label(resources.getString("about.label.player_path"));
    playerLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    HBox playerInputBox = new HBox(10.0);
    playerInputBox.setAlignment(Pos.CENTER_LEFT);

    lblPlayerPath.setId("lblPlayerPath");
    lblPlayerPath.setEditable(false);
    lblPlayerPath.setFocusTraversable(false);
    lblPlayerPath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(lblPlayerPath, Priority.ALWAYS);
    lblPlayerPath.setMaxWidth(Double.MAX_VALUE);

    btnPlayerLoc.getStyleClass().add("button-secondary");
    btnPlayerLoc.setMinWidth(Region.USE_PREF_SIZE);
    btnPlayerLoc.setStyle("-fx-text-fill: white;");
    Tooltip playerLocTip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    playerLocTip.setShowDelay(Duration.millis(250));
    btnPlayerLoc.setTooltip(playerLocTip);
    playerInputBox.getChildren().addAll(lblPlayerPath, btnPlayerLoc);
    playerBox.getChildren().addAll(playerLabel, playerInputBox);

    Region sep6 = new Region();
    sep6.setMinHeight(Region.USE_PREF_SIZE);
    sep6.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox extBox = new VBox(8.0);
    Label extLabel = new Label(resources.getString("about.label.extensions"));
    extLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblExtensions.setId("lblExtensions");
    lblExtensions.setEditable(false);
    lblExtensions.setFocusTraversable(false);
    lblExtensions.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    lblExtensions.setMaxWidth(Double.MAX_VALUE);
    extBox.getChildren().addAll(extLabel, lblExtensions);

    Region sep7 = new Region();
    sep7.setMinHeight(Region.USE_PREF_SIZE);
    sep7.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox dbBox = new VBox(8.0);
    HBox dbLabelBox = new HBox(8.0);
    dbLabelBox.setAlignment(Pos.CENTER_LEFT);
    Label dbLabel = new Label(resources.getString("about.label.db_location"));
    dbLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblStorageMode.setId("lblStorageMode");
    lblStorageMode
        .setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
    dbLabelBox.getChildren().addAll(dbLabel, lblStorageMode);

    HBox dbInputBox = new HBox(10.0);
    dbInputBox.setAlignment(Pos.CENTER_LEFT);
    lblDbPath.setId("lblDbPath");
    lblDbPath.setEditable(false);
    lblDbPath.setFocusTraversable(false);
    lblDbPath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(lblDbPath, Priority.ALWAYS);
    lblDbPath.setMaxWidth(Double.MAX_VALUE);

    btnDbLoc.getStyleClass().add("button-secondary");
    btnDbLoc.setMinWidth(Region.USE_PREF_SIZE);
    btnDbLoc.setStyle("-fx-text-fill: white;");
    Tooltip dbLocTip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    dbLocTip.setShowDelay(Duration.millis(250));
    btnDbLoc.setTooltip(dbLocTip);
    dbInputBox.getChildren().addAll(lblDbPath, btnDbLoc);
    dbBox.getChildren().addAll(dbLabelBox, dbInputBox);

    fullCard.getChildren().addAll(fullCardTitle, sep5, playerBox, sep6, extBox,
        sep7, dbBox);

    mainContent.getChildren().addAll(headerSection, cardsRow, fullCard);
    rootPane.setContent(mainContent);
  }

  public Parent getRoot() {
    return rootPane;
  }

  private void addGridRow(GridPane grid, int row, String name, Label valLabel) {
    Label nameLabel = new Label(name);
    nameLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    GridPane.setConstraints(nameLabel, 0, row);
    valLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    GridPane.setConstraints(valLabel, 1, row);
    grid.getChildren().addAll(nameLabel, valLabel);
  }

  private Label createMutedLabel(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
    return label;
  }
}
