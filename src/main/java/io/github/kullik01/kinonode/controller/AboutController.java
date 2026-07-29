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
package io.github.kullik01.kinonode.controller;

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

import java.awt.Desktop;


import io.github.kullik01.kinonode.KinoNodeApp;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.model.Watchlist;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller for the "About" view of the application. This class displays
 * system info, database path/size, configuring video extensions,
 * movie/watchlist statistics, Java and OS version info, and configured media
 * player details. It also provides options to open folder paths in the
 * operating system's default file browser.
 */
public class AboutController implements Refreshable {

  private Label lblVersion;
  private TextField lblExtensions;
  private TextField lblDbPath;
  private Label lblStorageMode;

  private Label lblJavaRuntime;
  private Label lblOsDetails;


  private Label lblMovieCount;
  private Label lblDbSize;
  private Label lblRegularWatchlistCount;
  private Label lblSmartWatchlistCount;
  private Label lblWorkspaceSize;
  private TextField lblPlayerPath;

  public Parent buildView() {
    ScrollPane rootPane = new ScrollPane();
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
    Image logoImage = new Image(
        getClass().getResource("/io/github/kullik01/kinonode/images/logo.png")
            .toExternalForm(),
        200, 200, true, true);
    logoView.setImage(logoImage);

    Label appTitle = new Label("KinoNode");
    appTitle.getStyleClass().add("title-1");

    Label appSubtitle = new Label("Personal Movie Library Manager");
    appSubtitle.getStyleClass().add("text-muted");
    appSubtitle.setStyle("-fx-font-size: 16px;");

    HBox btnBox = new HBox(15.0);
    btnBox.setAlignment(Pos.CENTER);
    btnBox.setPadding(new Insets(15.0, 0, 15.0, 0));

    javafx.scene.control.Button btnGitHub =
        new javafx.scene.control.Button("GitHub Repository");
    btnGitHub.setOnAction(e -> openRepository());
    btnGitHub.getStyleClass().add("accent");
    btnGitHub.setStyle("-fx-text-fill: white;");

    javafx.scene.control.Button btnBug =
        new javafx.scene.control.Button("Report a Bug");
    btnBug.setOnAction(e -> openIssueTracker());
    btnBug.setStyle("-fx-text-fill: white;");

    javafx.scene.control.Button btnUpdate =
        new javafx.scene.control.Button("Check for Updates");
    btnUpdate.setOnAction(e -> checkForUpdates());
    btnUpdate.setStyle("-fx-text-fill: white;");

    btnBox.getChildren().addAll(btnGitHub, btnBug, btnUpdate);
    headerSection.getChildren().addAll(logoView, appTitle, appSubtitle, btnBox);

    // Cards Row
    HBox cardsRow = new HBox(20.0);
    cardsRow.setAlignment(Pos.TOP_LEFT);

    // Left Card: Application Details
    VBox leftCard = new VBox();
    leftCard.getStyleClass().add("about-card");
    HBox.setHgrow(leftCard, Priority.ALWAYS);

    Label leftCardTitle = new Label("Application Details");
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

    addGridRow(grid1, 0, "Version:", lblVersion = new Label("1.0.0"));
    lblVersion.setId("lblVersion");
    addGridRow(grid1, 1, "Developer:", new Label("Hannah Kullik"));
    addGridRow(grid1, 2, "Logo Designers:",
        new Label("Martin Urban & Hannah Kullik"));
    addGridRow(grid1, 3, "License:", new Label("BSD 3-Clause License"));
    addGridRow(grid1, 4, "Copyright:", new Label("© 2026 Hannah Kullik"));

    Label envTitle = new Label("System Environment");
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

    addGridRow(grid2, 0, "Java Runtime:",
        lblJavaRuntime = new Label("Unknown"));
    lblJavaRuntime.setId("lblJavaRuntime");
    addGridRow(grid2, 1, "Operating System:",
        lblOsDetails = new Label("Unknown"));
    lblOsDetails.setId("lblOsDetails");

    Label builtTitle = new Label("Built With");
    builtTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    VBox.setMargin(builtTitle, new Insets(15.0, 0, 0, 0));
    Region sep3 = new Region();
    sep3.setMinHeight(Region.USE_PREF_SIZE);
    sep3.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox builtBox = new VBox(4.0);
    builtBox.getChildren().addAll(createMutedLabel("• Java 25 — Core runtime"),
        createMutedLabel("• JavaFX 25 — UI framework"),
        createMutedLabel("• SQLite — Data persistence"),
        createMutedLabel("• AtlantaFX — Modern CSS theme"),
        createMutedLabel("• Google Gson — JSON parsing"));

    leftCard.getChildren().addAll(leftCardTitle, sep1, grid1, envTitle, sep2,
        grid2, builtTitle, sep3, builtBox);

    // Right Card: System Configuration & Storage
    VBox rightCard = new VBox();
    rightCard.getStyleClass().add("about-card");
    HBox.setHgrow(rightCard, Priority.ALWAYS);

    Label rightCardTitle = new Label("System Configuration & Storage");
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

    addGridRow(grid3, 0, "Workspace Size:",
        lblWorkspaceSize = new Label("Unknown"));
    lblWorkspaceSize.setId("lblWorkspaceSize");
    addGridRow(grid3, 1, "Database Size:", lblDbSize = new Label("Unknown"));
    lblDbSize.setId("lblDbSize");
    addGridRow(grid3, 2, "Library Movies:",
        lblMovieCount = new Label("0 movies"));
    lblMovieCount.setId("lblMovieCount");
    addGridRow(grid3, 3, "Regular Watchlists:",
        lblRegularWatchlistCount = new Label("0"));
    lblRegularWatchlistCount.setId("lblRegularWatchlistCount");
    addGridRow(grid3, 4, "Smart Watchlists:",
        lblSmartWatchlistCount = new Label("0"));
    lblSmartWatchlistCount.setId("lblSmartWatchlistCount");

    rightCard.getChildren().addAll(rightCardTitle, sep4, grid3);

    cardsRow.getChildren().addAll(leftCard, rightCard);

    // Full-width card: Paths & Configuration
    VBox fullCard = new VBox();
    fullCard.getStyleClass().add("about-card");

    Label fullCardTitle = new Label("Paths & Configuration");
    fullCardTitle.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    Region sep5 = new Region();
    sep5.setMinHeight(Region.USE_PREF_SIZE);
    sep5.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox playerBox = new VBox(8.0);
    Label playerLabel = new Label("Media Player Path:");
    playerLabel
        .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    HBox playerInputBox = new HBox(10.0);
    playerInputBox.setAlignment(Pos.CENTER_LEFT);
    lblPlayerPath = new TextField("None");
    lblPlayerPath.setId("lblPlayerPath");
    lblPlayerPath.setEditable(false);
    lblPlayerPath.setFocusTraversable(false);
    lblPlayerPath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(lblPlayerPath, Priority.ALWAYS);
    lblPlayerPath.setMaxWidth(Double.MAX_VALUE);
    javafx.scene.control.Button btnPlayerLoc =
        new javafx.scene.control.Button("📂");
    btnPlayerLoc.getStyleClass().add("button-secondary");
    btnPlayerLoc.setMinWidth(Region.USE_PREF_SIZE);
    btnPlayerLoc.setStyle("-fx-text-fill: white;");
    btnPlayerLoc.setOnAction(e -> openPlayerLocation());
    Tooltip playerLocTip = new Tooltip("Open Location");
    playerLocTip.setShowDelay(Duration.millis(250));
    btnPlayerLoc.setTooltip(playerLocTip);
    playerInputBox.getChildren().addAll(lblPlayerPath, btnPlayerLoc);
    playerBox.getChildren().addAll(playerLabel, playerInputBox);

    Region sep6 = new Region();
    sep6.setMinHeight(Region.USE_PREF_SIZE);
    sep6.setStyle(
        "-fx-background-color: -color-border-default; -fx-pref-height: 1;");

    VBox extBox = new VBox(8.0);
    Label extLabel = new Label("Supported Video Extensions:");
    extLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblExtensions = new TextField(
        "3gp, avi, flv, m4v, mkv, mov, mp4, mpeg, mpg, mts, ogg, ogv, ts, webm, wmv");
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
    Label dbLabel = new Label("Database Storage Location:");
    dbLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
    lblStorageMode = new Label("");
    lblStorageMode.setId("lblStorageMode");
    lblStorageMode
        .setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
    dbLabelBox.getChildren().addAll(dbLabel, lblStorageMode);

    HBox dbInputBox = new HBox(10.0);
    dbInputBox.setAlignment(Pos.CENTER_LEFT);
    lblDbPath = new TextField("");
    lblDbPath.setId("lblDbPath");
    lblDbPath.setEditable(false);
    lblDbPath.setFocusTraversable(false);
    lblDbPath.setStyle(
        "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: -color-fg-default; -fx-control-inner-background: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-radius: 6px;");
    HBox.setHgrow(lblDbPath, Priority.ALWAYS);
    lblDbPath.setMaxWidth(Double.MAX_VALUE);
    javafx.scene.control.Button btnDbLoc =
        new javafx.scene.control.Button("📂");
    btnDbLoc.getStyleClass().add("button-secondary");
    btnDbLoc.setMinWidth(Region.USE_PREF_SIZE);
    btnDbLoc.setStyle("-fx-text-fill: white;");
    btnDbLoc.setOnAction(e -> openDatabaseLocation());
    Tooltip dbLocTip = new Tooltip("Open Location");
    dbLocTip.setShowDelay(Duration.millis(250));
    btnDbLoc.setTooltip(dbLocTip);
    dbInputBox.getChildren().addAll(lblDbPath, btnDbLoc);
    dbBox.getChildren().addAll(dbLabelBox, dbInputBox);

    fullCard.getChildren().addAll(fullCardTitle, sep5, playerBox, sep6, extBox,
        sep7, dbBox);

    mainContent.getChildren().addAll(headerSection, cardsRow, fullCard);
    rootPane.setContent(mainContent);

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

  /**
   * Initializes the About view. Automatically invoked after the layout has been
   * instantiated.
   */
  public void initialize() {
    loadAboutInfo();
  }

  /**
   * Refreshes the display information of the About page. Part of the
   * Refreshable interface contract.
   */
  @Override
  public void refresh() {
    loadAboutInfo();
  }

  /**
   * Retrieves application statistics, configuration details, and system
   * properties, then updates the corresponding UI labels and text fields.
   */
  private void loadAboutInfo() {
    lblVersion.setText("1.0.0");

    // 1. Extensions
    String tmpExtensions =
        AppContext.getInstance().getSettingsRepository().get("video_extensions",
            "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");
    lblExtensions.setText(tmpExtensions);

    // 2. Database path & size
    try {
      Path tmpDatabasePath =
          AppContext.getInstance().getDatabaseService().getDbPath();
      // Display path with forward slashes for cross-platform consistency
      lblDbPath.setText(
          tmpDatabasePath.toAbsolutePath().toString().replace("\\", "/"));

      if (lblStorageMode != null) {
        Path tmpCurrentDir = Path.of(System.getProperty("user.dir"));
        if (tmpDatabasePath.toAbsolutePath().startsWith(tmpCurrentDir)) {
          lblStorageMode.setText("(Single Directory / Portable Mode)");
        } else {
          lblStorageMode.setText("(OS Application Data Directory)");
        }
      }

      if (Files.exists(tmpDatabasePath)) {
        // Calculate file size in MB
        long tmpSizeBytes = Files.size(tmpDatabasePath);
        double tmpSizeMegabytes = tmpSizeBytes / (1024.0 * 1024.0);
        if (lblDbSize != null) {
          lblDbSize.setText(String.format("%.2f MB", tmpSizeMegabytes));
        }
      } else {
        if (lblDbSize != null) {
          lblDbSize.setText("Unknown");
        }
      }
    } catch (Exception exception) {
      lblDbPath.setText("Unknown database path");
    }

    // 3. System properties
    try {
      String tmpJavaVersion = System.getProperty("java.version");
      String tmpJavaVendor = System.getProperty("java.vendor");
      lblJavaRuntime.setText(tmpJavaVersion + " (" + tmpJavaVendor + ")");

      String tmpOperatingSystemName = System.getProperty("os.name");
      String tmpOperatingSystemArchitecture = System.getProperty("os.arch");
      String tmpOperatingSystemVersion = System.getProperty("os.version");
      lblOsDetails
          .setText(tmpOperatingSystemName + " " + tmpOperatingSystemVersion
              + " (" + tmpOperatingSystemArchitecture + ")");

    } catch (Exception exception) {
      lblJavaRuntime.setText("Unknown");
      lblOsDetails.setText("Unknown");
    }

    // 4. Statistics (Movies and Watchlists)
    try {
      int tmpMovieCount =
          AppContext.getInstance().getMovieRepository().findAll().size();
      lblMovieCount
          .setText(tmpMovieCount + (tmpMovieCount == 1 ? " movie" : " movies"));

      List<Watchlist> allWatchlists =
          AppContext.getInstance().getWatchlistRepository().findAll();
      long tmpSmartCount =
          allWatchlists.stream().filter(Watchlist::isSmart).count();
      long tmpRegularCount = allWatchlists.size() - tmpSmartCount;

      lblRegularWatchlistCount.setText(String.valueOf(tmpRegularCount));
      lblSmartWatchlistCount.setText(String.valueOf(tmpSmartCount));
    } catch (Exception exception) {
      lblMovieCount.setText("Unknown");
      lblRegularWatchlistCount.setText("Unknown");
      lblSmartWatchlistCount.setText("Unknown");
    }

    // 4.5 Workspace Size
    if (lblWorkspaceSize != null) {
      lblWorkspaceSize.setText("Calculating...");
      new Thread(() -> {
        try {
          String tmpWorkspaces = AppContext.getInstance()
              .getSettingsRepository().get("workspace_folders", "");
          long totalWorkspaceSizeBytes = 0;
          if (!tmpWorkspaces.isBlank()) {
            String[] tmpFolderList = tmpWorkspaces.split(";");
            for (String folderPath : tmpFolderList) {
              Path path = Path.of(folderPath.trim());
              if (Files.exists(path) && Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> pathStream =
                    Files.walk(path)) {
                  totalWorkspaceSizeBytes +=
                      pathStream.filter(Files::isRegularFile).mapToLong(p -> {
                        try {
                          return Files.size(p);
                        } catch (Exception e) {
                          return 0L;
                        }
                      }).sum();
                }
              }
            }
          }
          final long finalSize = totalWorkspaceSizeBytes;
          javafx.application.Platform.runLater(() -> {
            if (finalSize > 0) {
              double tmpSizeGb = finalSize / (1024.0 * 1024.0 * 1024.0);
              if (tmpSizeGb >= 1.0) {
                lblWorkspaceSize.setText(String.format("%.2f GB", tmpSizeGb));
              } else {
                double tmpSizeMb = finalSize / (1024.0 * 1024.0);
                lblWorkspaceSize.setText(String.format("%.2f MB", tmpSizeMb));
              }
            } else {
              lblWorkspaceSize.setText("0.00 MB");
            }
          });
        } catch (Exception exception) {
          javafx.application.Platform.runLater(() -> {
            lblWorkspaceSize.setText("Unknown");
          });
        }
      }).start();
    }

    // 5. Media Player Executable
    try {
      Optional<Path> tmpPlayerPathOptional =
          AppContext.getInstance().getPlaybackService().getPlayerPath();
      if (tmpPlayerPathOptional.isPresent()) {
        lblPlayerPath.setText(tmpPlayerPathOptional.get().toAbsolutePath()
            .toString().replace("\\", "/"));
      } else {
        lblPlayerPath.setText("No player configured (using auto-detect/none)");
      }
    } catch (Exception exception) {
      lblPlayerPath.setText("Unknown");
    }
  }

  /**
   * Opens the directory containing the configured external media player in the
   * system's default file explorer.
   */
  private void openPlayerLocation() {
    try {
      Optional<Path> tmpPlayerPathOptional =
          AppContext.getInstance().getPlaybackService().getPlayerPath();
      // Check if player is configured and its parent directory exists
      if (tmpPlayerPathOptional.isPresent()
          && Files.exists(tmpPlayerPathOptional.get().getParent())) {
        // Ensure Desktop API is supported on the current platform before
        // opening the explorer
        if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          Desktop.getDesktop()
              .open(tmpPlayerPathOptional.get().getParent().toFile());
        }
      }
    } catch (Exception exception) {
      // Ignore if it cannot be opened
    }
  }

  /**
   * Opens the directory containing the local SQLite database file in the
   * system's default file explorer.
   */
  private void openDatabaseLocation() {
    try {
      Path tmpDatabasePath =
          AppContext.getInstance().getDatabaseService().getDbPath();
      // Verify that the database path and its parent folder exist
      if (tmpDatabasePath != null
          && Files.exists(tmpDatabasePath.getParent())) {
        // Open the database folder using system's default file browser
        if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          Desktop.getDesktop().open(tmpDatabasePath.getParent().toFile());
        }
      }
    } catch (Exception exception) {
      // Ignore if it cannot be opened
    }
  }

  /**
   * Opens the project's GitHub repository in the default web browser.
   */
  private void openRepository() {
    openWebpage("https://github.com/kullik01/Kino-Node");
  }

  /**
   * Opens the project's GitHub issue tracker for bug reporting in the default
   * web browser.
   */
  private void openIssueTracker() {
    openWebpage("https://github.com/kullik01/Kino-Node/issues/new");
  }

  /**
   * Opens a specified URL using the JavaFX HostServices API, which delegates to
   * the operating system's default web browser.
   *
   * @param url The fully qualified URL to open.
   */
  private void openWebpage(String url) {
    try {
      KinoNodeApp.getInstance().getHostServices().showDocument(url);
    } catch (Exception exception) {
      // Ignore errors opening browser
    }
  }

  /**
   * Opens the project's GitHub releases page in the default web browser to
   * allow the user to check for newer application versions.
   */
  private void checkForUpdates() {
    openWebpage("https://github.com/kullik01/Kino-Node/releases");
  }
}
