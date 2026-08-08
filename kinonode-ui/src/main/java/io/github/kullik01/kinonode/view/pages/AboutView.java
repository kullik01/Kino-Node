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

    VBox tmpMainContent = new VBox(25.0);
    tmpMainContent.getStyleClass().add("detail-pane");
    tmpMainContent.setPadding(new Insets(30.0, 30.0, 30.0, 30.0));

    // Header Section
    VBox tmpHeaderSection = new VBox(10.0);
    tmpHeaderSection.setAlignment(Pos.CENTER);

    ImageView tmpLogoView = new ImageView();
    tmpLogoView.setFitHeight(100.0);
    tmpLogoView.setFitWidth(100.0);
    tmpLogoView.setPreserveRatio(true);
    tmpLogoView.setSmooth(true);
    Image tmpLogoImage = new Image(getClass()
        .getResource("/io/github/kullik01/kinonode/images/logos/logo.png")
        .toExternalForm(), 200, 200, true, true);
    tmpLogoView.setImage(tmpLogoImage);

    Label tmpAppTitle = new Label(resources.getString("about.title"));
    tmpAppTitle.getStyleClass().add("title-1");

    Label tmpAppSubtitle = new Label(resources.getString("about.subtitle"));
    tmpAppSubtitle.getStyleClass().add("text-muted");
    tmpAppSubtitle.setStyle("-fx-font-size: 16px;");

    HBox tmpBtnBox = new HBox(15.0);
    tmpBtnBox.setAlignment(Pos.CENTER);
    tmpBtnBox.setPadding(new Insets(15.0, 0, 15.0, 0));

    btnGitHub.getStyleClass().add("accent");
    btnGitHub.setStyle("-fx-text-fill: white;");
    btnBug.setStyle("-fx-text-fill: white;");
    btnUpdate.setStyle("-fx-text-fill: white;");

    tmpBtnBox.getChildren().addAll(btnGitHub, btnBug, btnUpdate);
    tmpHeaderSection.getChildren().addAll(tmpLogoView, tmpAppTitle,
        tmpAppSubtitle, tmpBtnBox);

    // Cards Row
    HBox tmpCardsRow = new HBox(20.0);
    tmpCardsRow.setAlignment(Pos.TOP_LEFT);

    // Left Card
    VBox tmpLeftCard = new VBox();
    tmpLeftCard.getStyleClass().add("about-card");
    HBox.setHgrow(tmpLeftCard, Priority.ALWAYS);

    Label tmpLeftCardTitle =
        new Label(resources.getString("about.card.details"));
    tmpLeftCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region tmpSep1 = new Region();
    tmpSep1.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep1.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid1 = new GridPane();
    tmpGrid1.setHgap(15.0);
    tmpGrid1.setVgap(12.0);
    ColumnConstraints tmpGc1 = new ColumnConstraints();
    tmpGc1.setMinWidth(110.0);
    tmpGc1.setPrefWidth(140.0);
    ColumnConstraints tmpGc2 = new ColumnConstraints();
    tmpGc2.setHgrow(Priority.ALWAYS);
    tmpGrid1.getColumnConstraints().addAll(tmpGc1, tmpGc2);

    lblVersion.setId("lblVersion");
    addGridRow(tmpGrid1, 0, resources.getString("about.label.version"),
        lblVersion);
    addGridRow(tmpGrid1, 1, resources.getString("about.label.developer"),
        new Label(resources.getString("about.val.developer")));
    addGridRow(tmpGrid1, 2, resources.getString("about.label.designers"),
        new Label(resources.getString("about.val.designers")));
    addGridRow(tmpGrid1, 3, resources.getString("about.label.license"),
        new Label(resources.getString("about.val.license")));
    addGridRow(tmpGrid1, 4, resources.getString("about.label.copyright"),
        new Label(resources.getString("about.val.copyright")));

    Label tmpEnvTitle = new Label(resources.getString("about.card.env"));
    tmpEnvTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(tmpEnvTitle, new Insets(15.0, 0, 0, 0));
    Region tmpSep2 = new Region();
    tmpSep2.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep2.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid2 = new GridPane();
    tmpGrid2.setHgap(15.0);
    tmpGrid2.setVgap(12.0);
    tmpGrid2.getColumnConstraints().addAll(tmpGc1, tmpGc2);

    lblJavaRuntime.setId("lblJavaRuntime");
    addGridRow(tmpGrid2, 0, resources.getString("about.label.java"),
        lblJavaRuntime);
    lblOsDetails.setId("lblOsDetails");
    addGridRow(tmpGrid2, 1, resources.getString("about.label.os"),
        lblOsDetails);

    Label tmpBuiltTitle = new Label(resources.getString("about.card.built"));
    tmpBuiltTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(tmpBuiltTitle, new Insets(15.0, 0, 0, 0));
    Region tmpSep3 = new Region();
    tmpSep3.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep3.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpBuiltBox = new VBox(4.0);
    tmpBuiltBox.getChildren().addAll(
        createMutedLabel(resources.getString("about.built.java")),
        createMutedLabel(resources.getString("about.built.javafx")),
        createMutedLabel(resources.getString("about.built.sqlite")),
        createMutedLabel(resources.getString("about.built.atlantafx")),
        createMutedLabel(resources.getString("about.built.gson")));

    tmpLeftCard.getChildren().addAll(tmpLeftCardTitle, tmpSep1, tmpGrid1,
        tmpEnvTitle, tmpSep2, tmpGrid2, tmpBuiltTitle, tmpSep3, tmpBuiltBox);

    // Right Card
    VBox tmpRightCard = new VBox();
    tmpRightCard.getStyleClass().add("about-card");
    HBox.setHgrow(tmpRightCard, Priority.ALWAYS);

    Label tmpRightCardTitle =
        new Label(resources.getString("about.card.storage"));
    tmpRightCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region tmpSep4 = new Region();
    tmpSep4.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep4.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid3 = new GridPane();
    tmpGrid3.setHgap(15.0);
    tmpGrid3.setVgap(12.0);
    ColumnConstraints tmpGc3 = new ColumnConstraints();
    tmpGc3.setMinWidth(130.0);
    tmpGc3.setPrefWidth(140.0);
    tmpGrid3.getColumnConstraints().addAll(tmpGc3, tmpGc2);

    lblWorkspaceSize.setId("lblWorkspaceSize");
    addGridRow(tmpGrid3, 0, resources.getString("about.label.workspace"),
        lblWorkspaceSize);
    lblDbSize.setId("lblDbSize");
    addGridRow(tmpGrid3, 1, resources.getString("about.label.db_size"),
        lblDbSize);
    lblMovieCount.setId("lblMovieCount");
    addGridRow(tmpGrid3, 2, resources.getString("about.label.movies"),
        lblMovieCount);
    lblRegularWatchlistCount.setId("lblRegularWatchlistCount");
    addGridRow(tmpGrid3, 3,
        resources.getString("about.label.regular_watchlists"),
        lblRegularWatchlistCount);
    lblSmartWatchlistCount.setId("lblSmartWatchlistCount");
    addGridRow(tmpGrid3, 4, resources.getString("about.label.smart_watchlists"),
        lblSmartWatchlistCount);

    tmpRightCard.getChildren().addAll(tmpRightCardTitle, tmpSep4, tmpGrid3);

    tmpCardsRow.getChildren().addAll(tmpLeftCard, tmpRightCard);

    // Full-width card
    VBox tmpFullCard = new VBox();
    tmpFullCard.getStyleClass().add("about-card");

    Label tmpFullCardTitle = new Label(resources.getString("about.card.paths"));
    tmpFullCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region tmpSep5 = new Region();
    tmpSep5.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep5.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpPlayerBox = new VBox(8.0);
    Label tmpPlayerLabel =
        new Label(resources.getString("about.label.player_path"));
    tmpPlayerLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    HBox tmpPlayerInputBox = new HBox(10.0);
    tmpPlayerInputBox.setAlignment(Pos.CENTER_LEFT);

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
    Tooltip tmpPlayerLocTip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    tmpPlayerLocTip.setShowDelay(Duration.millis(250));
    btnPlayerLoc.setTooltip(tmpPlayerLocTip);
    tmpPlayerInputBox.getChildren().addAll(lblPlayerPath, btnPlayerLoc);
    tmpPlayerBox.getChildren().addAll(tmpPlayerLabel, tmpPlayerInputBox);

    Region tmpSep6 = new Region();
    tmpSep6.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep6.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpExtBox = new VBox(8.0);
    Label tmpExtLabel =
        new Label(resources.getString("about.label.extensions"));
    tmpExtLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblExtensions.setId("lblExtensions");
    lblExtensions.setEditable(false);
    lblExtensions.setFocusTraversable(false);
    lblExtensions.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    lblExtensions.setMaxWidth(Double.MAX_VALUE);
    tmpExtBox.getChildren().addAll(tmpExtLabel, lblExtensions);

    Region tmpSep7 = new Region();
    tmpSep7.setMinHeight(Region.USE_PREF_SIZE);
    tmpSep7.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpDbBox = new VBox(8.0);
    HBox tmpDbLabelBox = new HBox(8.0);
    tmpDbLabelBox.setAlignment(Pos.CENTER_LEFT);
    Label tmpDbLabel =
        new Label(resources.getString("about.label.db_location"));
    tmpDbLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblStorageMode.setId("lblStorageMode");
    lblStorageMode
        .setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
    tmpDbLabelBox.getChildren().addAll(tmpDbLabel, lblStorageMode);

    HBox tmpDbInputBox = new HBox(10.0);
    tmpDbInputBox.setAlignment(Pos.CENTER_LEFT);
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
    Tooltip tmpDbLocTip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    tmpDbLocTip.setShowDelay(Duration.millis(250));
    btnDbLoc.setTooltip(tmpDbLocTip);
    tmpDbInputBox.getChildren().addAll(lblDbPath, btnDbLoc);
    tmpDbBox.getChildren().addAll(tmpDbLabelBox, tmpDbInputBox);

    tmpFullCard.getChildren().addAll(tmpFullCardTitle, tmpSep5, tmpPlayerBox,
        tmpSep6, tmpExtBox, tmpSep7, tmpDbBox);

    tmpMainContent.getChildren().addAll(tmpHeaderSection, tmpCardsRow,
        tmpFullCard);
    rootPane.setContent(tmpMainContent);
  }

  public Parent getRoot() {
    return rootPane;
  }

  private void addGridRow(GridPane grid, int row, String name, Label valLabel) {
    Label tmpNameLabel = new Label(name);
    tmpNameLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    GridPane.setConstraints(tmpNameLabel, 0, row);
    valLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    GridPane.setConstraints(valLabel, 1, row);
    grid.getChildren().addAll(tmpNameLabel, valLabel);
  }

  private Label createMutedLabel(String text) {
    Label tmpLabel = new Label(text);
    tmpLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
    return tmpLabel;
  }
}
