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

  public final Label labelVersion = new Label("1.0.1");
  public final TextField labelExtensions = new TextField();
  public final TextField labelDatabasePath = new TextField();
  public final Label labelStorageMode = new Label("");
  public final Label labelJavaRuntime = new Label("Unknown");
  public final Label labelOsDetails = new Label("Unknown");
  public final Label labelMovieCount = new Label("0 movies");
  public final Label labelDatabaseSize = new Label("Unknown");
  public final Label labelRegularWatchlistCount = new Label("0");
  public final Label labelSmartWatchlistCount = new Label("0");
  public final Label labelWorkspaceSize = new Label("Unknown");
  public final TextField labelPlayerPath = new TextField("None");

  public final Button buttonGitHub;
  public final Button buttonBug;
  public final Button buttonUpdate;
  public final Button buttonPlayerLocation = new Button("📂");
  public final Button buttonDatabaseLocation = new Button("📂");

  public AboutView(ResourceBundle resources) {
    buttonGitHub = new Button(resources.getString("about.btn.github"));
    buttonBug = new Button(resources.getString("about.btn.bug"));
    buttonUpdate = new Button(resources.getString("about.btn.update"));

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

    HBox tmpButtonBox = new HBox(15.0);
    tmpButtonBox.setAlignment(Pos.CENTER);
    tmpButtonBox.setPadding(new Insets(15.0, 0, 15.0, 0));

    buttonGitHub.getStyleClass().add("accent");
    buttonGitHub.setStyle("-fx-text-fill: white;");
    buttonBug.setStyle("-fx-text-fill: white;");
    buttonUpdate.setStyle("-fx-text-fill: white;");

    tmpButtonBox.getChildren().addAll(buttonGitHub, buttonBug, buttonUpdate);
    tmpHeaderSection.getChildren().addAll(tmpLogoView, tmpAppTitle,
        tmpAppSubtitle, tmpButtonBox);

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
    Region tmpSeparator1 = new Region();
    tmpSeparator1.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator1.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid1 = new GridPane();
    tmpGrid1.setHgap(15.0);
    tmpGrid1.setVgap(12.0);
    ColumnConstraints tmpColumnConstraints1 = new ColumnConstraints();
    tmpColumnConstraints1.setMinWidth(110.0);
    tmpColumnConstraints1.setPrefWidth(140.0);
    ColumnConstraints tmpColumnConstraints2 = new ColumnConstraints();
    tmpColumnConstraints2.setHgrow(Priority.ALWAYS);
    tmpGrid1.getColumnConstraints().addAll(tmpColumnConstraints1,
        tmpColumnConstraints2);

    labelVersion.setId("labelVersion");
    addGridRow(tmpGrid1, 0, resources.getString("about.label.version"),
        labelVersion);
    addGridRow(tmpGrid1, 1, resources.getString("about.label.developer"),
        new Label(resources.getString("about.val.developer")));
    addGridRow(tmpGrid1, 2, resources.getString("about.label.designers"),
        new Label(resources.getString("about.val.designers")));
    addGridRow(tmpGrid1, 3, resources.getString("about.label.license"),
        new Label(resources.getString("about.val.license")));
    addGridRow(tmpGrid1, 4, resources.getString("about.label.copyright"),
        new Label(resources.getString("about.val.copyright")));

    Label tmpEnvironmentTitle =
        new Label(resources.getString("about.card.env"));
    tmpEnvironmentTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(tmpEnvironmentTitle, new Insets(15.0, 0, 0, 0));
    Region tmpSeparator2 = new Region();
    tmpSeparator2.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator2.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid2 = new GridPane();
    tmpGrid2.setHgap(15.0);
    tmpGrid2.setVgap(12.0);
    tmpGrid2.getColumnConstraints().addAll(tmpColumnConstraints1,
        tmpColumnConstraints2);

    labelJavaRuntime.setId("labelJavaRuntime");
    addGridRow(tmpGrid2, 0, resources.getString("about.label.java"),
        labelJavaRuntime);
    labelOsDetails.setId("labelOsDetails");
    addGridRow(tmpGrid2, 1, resources.getString("about.label.os"),
        labelOsDetails);

    Label tmpBuiltTitle = new Label(resources.getString("about.card.built"));
    tmpBuiltTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(tmpBuiltTitle, new Insets(15.0, 0, 0, 0));
    Region tmpSeparator3 = new Region();
    tmpSeparator3.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator3.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpBuiltBox = new VBox(4.0);
    tmpBuiltBox.getChildren().addAll(
        createMutedLabel(resources.getString("about.built.java")),
        createMutedLabel(resources.getString("about.built.javafx")),
        createMutedLabel(resources.getString("about.built.sqlite")),
        createMutedLabel(resources.getString("about.built.atlantafx")),
        createMutedLabel(resources.getString("about.built.gson")));

    tmpLeftCard.getChildren().addAll(tmpLeftCardTitle, tmpSeparator1, tmpGrid1,
        tmpEnvironmentTitle, tmpSeparator2, tmpGrid2, tmpBuiltTitle,
        tmpSeparator3, tmpBuiltBox);

    // Right Card
    VBox tmpRightCard = new VBox();
    tmpRightCard.getStyleClass().add("about-card");
    HBox.setHgrow(tmpRightCard, Priority.ALWAYS);

    Label tmpRightCardTitle =
        new Label(resources.getString("about.card.storage"));
    tmpRightCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region tmpSeparator4 = new Region();
    tmpSeparator4.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator4.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    GridPane tmpGrid3 = new GridPane();
    tmpGrid3.setHgap(15.0);
    tmpGrid3.setVgap(12.0);
    ColumnConstraints tmpColumnConstraints3 = new ColumnConstraints();
    tmpColumnConstraints3.setMinWidth(130.0);
    tmpColumnConstraints3.setPrefWidth(140.0);
    tmpGrid3.getColumnConstraints().addAll(tmpColumnConstraints3,
        tmpColumnConstraints2);

    labelWorkspaceSize.setId("labelWorkspaceSize");
    addGridRow(tmpGrid3, 0, resources.getString("about.label.workspace"),
        labelWorkspaceSize);
    labelDatabaseSize.setId("labelDatabaseSize");
    addGridRow(tmpGrid3, 1, resources.getString("about.label.db_size"),
        labelDatabaseSize);
    labelMovieCount.setId("labelMovieCount");
    addGridRow(tmpGrid3, 2, resources.getString("about.label.movies"),
        labelMovieCount);
    labelRegularWatchlistCount.setId("labelRegularWatchlistCount");
    addGridRow(tmpGrid3, 3,
        resources.getString("about.label.regular_watchlists"),
        labelRegularWatchlistCount);
    labelSmartWatchlistCount.setId("labelSmartWatchlistCount");
    addGridRow(tmpGrid3, 4, resources.getString("about.label.smart_watchlists"),
        labelSmartWatchlistCount);

    tmpRightCard.getChildren().addAll(tmpRightCardTitle, tmpSeparator4,
        tmpGrid3);

    tmpCardsRow.getChildren().addAll(tmpLeftCard, tmpRightCard);

    // Full-width card
    VBox tmpFullCard = new VBox();
    tmpFullCard.getStyleClass().add("about-card");

    Label tmpFullCardTitle = new Label(resources.getString("about.card.paths"));
    tmpFullCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region tmpSeparator5 = new Region();
    tmpSeparator5.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator5.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpPlayerBox = new VBox(8.0);
    Label tmpPlayerLabel =
        new Label(resources.getString("about.label.player_path"));
    tmpPlayerLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    HBox tmpPlayerInputBox = new HBox(10.0);
    tmpPlayerInputBox.setAlignment(Pos.CENTER_LEFT);

    labelPlayerPath.setId("labelPlayerPath");
    labelPlayerPath.setEditable(false);
    labelPlayerPath.setFocusTraversable(false);
    labelPlayerPath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(labelPlayerPath, Priority.ALWAYS);
    labelPlayerPath.setMaxWidth(Double.MAX_VALUE);

    buttonPlayerLocation.getStyleClass().add("button-secondary");
    buttonPlayerLocation.setMinWidth(Region.USE_PREF_SIZE);
    buttonPlayerLocation.setStyle("-fx-text-fill: white;");
    Tooltip tmpPlayerLocationTooltip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    tmpPlayerLocationTooltip.setShowDelay(Duration.millis(250));
    buttonPlayerLocation.setTooltip(tmpPlayerLocationTooltip);
    tmpPlayerInputBox.getChildren().addAll(labelPlayerPath,
        buttonPlayerLocation);
    tmpPlayerBox.getChildren().addAll(tmpPlayerLabel, tmpPlayerInputBox);

    Region tmpSeparator6 = new Region();
    tmpSeparator6.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator6.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpExtensionBox = new VBox(8.0);
    Label tmpExtensionLabel =
        new Label(resources.getString("about.label.extensions"));
    tmpExtensionLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    labelExtensions.setId("labelExtensions");
    labelExtensions.setEditable(false);
    labelExtensions.setFocusTraversable(false);
    labelExtensions.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    labelExtensions.setMaxWidth(Double.MAX_VALUE);
    tmpExtensionBox.getChildren().addAll(tmpExtensionLabel, labelExtensions);

    Region tmpSeparator7 = new Region();
    tmpSeparator7.setMinHeight(Region.USE_PREF_SIZE);
    tmpSeparator7.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox tmpDatabaseBox = new VBox(8.0);
    HBox tmpDatabaseLabelBox = new HBox(8.0);
    tmpDatabaseLabelBox.setAlignment(Pos.CENTER_LEFT);
    Label tmpDatabaseLabel =
        new Label(resources.getString("about.label.db_location"));
    tmpDatabaseLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    labelStorageMode.setId("labelStorageMode");
    labelStorageMode
        .setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
    tmpDatabaseLabelBox.getChildren().addAll(tmpDatabaseLabel,
        labelStorageMode);

    HBox tmpDatabaseInputBox = new HBox(10.0);
    tmpDatabaseInputBox.setAlignment(Pos.CENTER_LEFT);
    labelDatabasePath.setId("labelDatabasePath");
    labelDatabasePath.setEditable(false);
    labelDatabasePath.setFocusTraversable(false);
    labelDatabasePath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(labelDatabasePath, Priority.ALWAYS);
    labelDatabasePath.setMaxWidth(Double.MAX_VALUE);

    buttonDatabaseLocation.getStyleClass().add("button-secondary");
    buttonDatabaseLocation.setMinWidth(Region.USE_PREF_SIZE);
    buttonDatabaseLocation.setStyle("-fx-text-fill: white;");
    Tooltip tmpDatabaseLocationTooltip =
        new Tooltip(resources.getString("about.tooltip.open_location"));
    tmpDatabaseLocationTooltip.setShowDelay(Duration.millis(250));
    buttonDatabaseLocation.setTooltip(tmpDatabaseLocationTooltip);
    tmpDatabaseInputBox.getChildren().addAll(labelDatabasePath,
        buttonDatabaseLocation);
    tmpDatabaseBox.getChildren().addAll(tmpDatabaseLabelBox,
        tmpDatabaseInputBox);

    tmpFullCard.getChildren().addAll(tmpFullCardTitle, tmpSeparator5,
        tmpPlayerBox, tmpSeparator6, tmpExtensionBox, tmpSeparator7,
        tmpDatabaseBox);

    tmpMainContent.getChildren().addAll(tmpHeaderSection, tmpCardsRow,
        tmpFullCard);
    rootPane.setContent(tmpMainContent);
  }

  public Parent getRoot() {
    return rootPane;
  }

  private void addGridRow(GridPane grid, int row, String name,
      Label valueLabel) {
    Label tmpNameLabel = new Label(name);
    tmpNameLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    GridPane.setConstraints(tmpNameLabel, 0, row);
    valueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    GridPane.setConstraints(valueLabel, 1, row);
    grid.getChildren().addAll(tmpNameLabel, valueLabel);
  }

  private Label createMutedLabel(String text) {
    Label tmpLabel = new Label(text);
    tmpLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
    return tmpLabel;
  }
}
