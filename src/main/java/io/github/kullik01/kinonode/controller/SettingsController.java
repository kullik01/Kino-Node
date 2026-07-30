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

import io.github.kullik01.kinonode.util.CursorUtils;
import io.github.kullik01.kinonode.util.CustomAlert;
import atlantafx.base.controls.ToggleSwitch;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import javafx.util.Duration;


import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.AppContext;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.VBox;

/**
 * Controller for the application Settings view. Handles configuration of
 * workspace directories and external media player paths. Implements
 * {@link Refreshable} to support reloading configurations dynamically. Features
 * live validations, custom ListView graphic cells, and automated tab exit
 * saving.
 */
public class SettingsController implements Refreshable {

  private ListView<String> lstWorkspaces;
  private Button btnAddFolder;
  private Button btnRemoveFolder;
  private TextField txtPlayerPath;
  private Label lblStatus;
  private Label lblPlayerStatus;
  private Label lblPlayerError;

  private SettingsRepository settingsRepository;
  private String loadedWorkspaces = "";
  private String loadedPlayerPath = "";
  private String loadedCursorMode = "";

  private ToggleSwitch btnCursorToggle;
  private ToggleSwitch btnCompactToggle;
  private ToggleSwitch btnMaxToggle;

  private javafx.animation.PauseTransition statusTimer;

  /**
   * Displays a temporary status message in the UI that automatically disappears
   * after a few seconds.
   *
   * @param message The text to display.
   */
  private void showStatus(String message) {
    lblStatus.setText(message);
    if (statusTimer != null) {
      statusTimer.stop();
    }
    statusTimer = new javafx.animation.PauseTransition(Duration.seconds(5));
    statusTimer.setOnFinished(e -> lblStatus.setText(""));
    statusTimer.play();
  }

  public Parent buildView() {
    VBox rootPane = new VBox();
    rootPane.setSpacing(20.0);
    rootPane.getStyleClass().add("detail-pane");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());
    rootPane.setPadding(new Insets(30.0, 30.0, 30.0, 30.0));

    Label titleLbl = new Label("Application Settings");
    titleLbl.getStyleClass().add("detail-title");

    // Workspaces Section
    VBox workspaceSection = new VBox(10.0);
    Label wsTitle = new Label("Workspace Folders");
    wsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    Label wsSub = new Label("Root folders recursively scanned for video files");
    wsSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");

    HBox wsBox = new HBox(10.0);
    lstWorkspaces = new ListView<>();
    lstWorkspaces.setId("lstWorkspaces");
    lstWorkspaces.setPrefHeight(150.0);
    HBox.setHgrow(lstWorkspaces, Priority.ALWAYS);

    VBox wsBtnBox = new VBox(10.0);
    btnAddFolder = new Button("➕");
    btnAddFolder.setId("btnAddFolder");
    btnAddFolder.getStyleClass().add("button-secondary");
    btnAddFolder.setMinWidth(Control.USE_PREF_SIZE);
    btnAddFolder.setOnAction(e -> addWorkspace());
    Tooltip addTooltip = new Tooltip("Add");
    addTooltip.setShowDelay(Duration.millis(250));
    btnAddFolder.setTooltip(addTooltip);

    btnRemoveFolder = new Button("➖");
    btnRemoveFolder.setId("btnRemoveFolder");
    btnRemoveFolder.getStyleClass().add("button-secondary");
    btnRemoveFolder.setMinWidth(Control.USE_PREF_SIZE);
    btnRemoveFolder.setOnAction(e -> removeWorkspace());
    Tooltip removeTooltip = new Tooltip("Remove");
    removeTooltip.setShowDelay(Duration.millis(250));
    btnRemoveFolder.setTooltip(removeTooltip);

    wsBtnBox.getChildren().addAll(btnAddFolder, btnRemoveFolder);
    wsBox.getChildren().addAll(lstWorkspaces, wsBtnBox);
    workspaceSection.getChildren().addAll(wsTitle, wsSub, wsBox);

    // Player Section
    GridPane playerSection = new GridPane();
    playerSection.setHgap(15.0);
    playerSection.setVgap(15.0);

    ColumnConstraints col1 = new ColumnConstraints();
    col1.setMinWidth(150.0);
    col1.setPrefWidth(180.0);
    ColumnConstraints col2 = new ColumnConstraints();
    col2.setHgrow(Priority.ALWAYS);
    ColumnConstraints col3 = new ColumnConstraints();
    col3.setMinWidth(100.0);
    playerSection.getColumnConstraints().addAll(col1, col2, col3);

    RowConstraints row1 = new RowConstraints();
    row1.setMinHeight(30.0);
    playerSection.getRowConstraints().add(row1);

    Label playerTitle = new Label("External Media Player:");
    playerTitle.setStyle("-fx-font-weight: bold;");
    GridPane.setConstraints(playerTitle, 0, 0, 1, 1, javafx.geometry.HPos.LEFT,
        javafx.geometry.VPos.CENTER);

    VBox playerPathBox = new VBox(4.0);
    GridPane.setConstraints(playerPathBox, 1, 0, 1, 1,
        javafx.geometry.HPos.LEFT, javafx.geometry.VPos.CENTER);

    HBox playerInputBox = new HBox(10.0);
    playerInputBox.setAlignment(Pos.CENTER_LEFT);
    txtPlayerPath = new TextField();
    txtPlayerPath.setId("txtPlayerPath");
    txtPlayerPath
        .setPromptText("Path to player binary (e.g. mpv.exe, vlc.exe)");
    HBox.setHgrow(txtPlayerPath, Priority.ALWAYS);

    lblPlayerStatus = new Label();
    lblPlayerStatus.setId("lblPlayerStatus");
    playerInputBox.getChildren().addAll(txtPlayerPath, lblPlayerStatus);

    lblPlayerError = new Label();
    lblPlayerError.setId("lblPlayerError");
    lblPlayerError
        .setStyle("-fx-text-fill: -color-danger-fg; -fx-font-size: 11px;");
    lblPlayerError.setVisible(false);
    lblPlayerError.setManaged(false);

    playerPathBox.getChildren().addAll(playerInputBox, lblPlayerError);

    Button btnBrowse = new Button("📂");
    btnBrowse.getStyleClass().add("button-secondary");
    btnBrowse.setMinWidth(Control.USE_PREF_SIZE);
    btnBrowse.setOnAction(e -> browsePlayer());
    Tooltip browseTooltip = new Tooltip("Browse");
    browseTooltip.setShowDelay(Duration.millis(250));
    btnBrowse.setTooltip(browseTooltip);
    GridPane.setConstraints(btnBrowse, 2, 0, 1, 1, javafx.geometry.HPos.LEFT,
        javafx.geometry.VPos.CENTER);

    playerSection.getChildren().addAll(playerTitle, playerPathBox, btnBrowse);

    lblStatus = new Label("");
    lblStatus.setId("lblStatus");
    lblStatus
        .setStyle("-fx-text-fill: -color-success-fg; -fx-font-size: 14px;");

    // Appearance Section
    Region tmpSpacer = new Region();
    VBox.setVgrow(tmpSpacer, Priority.ALWAYS);

    VBox tmpAppearanceSection = new VBox(10.0);
    Label tmpAppTitle = new Label("Appearance");
    tmpAppTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    Label tmpAppSub = new Label("Visual preferences and UI tweaks");
    tmpAppSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");

    HBox tmpCursorBox = new HBox(10.0);
    tmpCursorBox.setAlignment(Pos.CENTER_LEFT);
    Label tmpCursorLbl = new Label("Custom Mouse Pointer");
    tmpCursorLbl.setPrefWidth(250.0);
    btnCursorToggle = new ToggleSwitch();
    btnCursorToggle.setId("btnCursorToggle");
    Tooltip tmpCursorTooltip = new Tooltip("Enable custom application mouse pointer");
    tmpCursorTooltip.setShowDelay(Duration.millis(250));
    btnCursorToggle.setTooltip(tmpCursorTooltip);
    tmpCursorBox.getChildren().addAll(tmpCursorLbl, btnCursorToggle);

    HBox tmpCompactBox = new HBox(10.0);
    tmpCompactBox.setAlignment(Pos.CENTER_LEFT);
    Label tmpCompactLbl = new Label("Compact Mode (Denser Layout)");
    tmpCompactLbl.setPrefWidth(250.0);
    btnCompactToggle = new ToggleSwitch();
    btnCompactToggle.setId("btnCompactToggle");
    Tooltip tmpCompactTooltip = new Tooltip("Enable a denser UI layout for more content");
    tmpCompactTooltip.setShowDelay(Duration.millis(250));
    btnCompactToggle.setTooltip(tmpCompactTooltip);
    tmpCompactBox.getChildren().addAll(tmpCompactLbl, btnCompactToggle);

    HBox tmpMaxBox = new HBox(10.0);
    tmpMaxBox.setAlignment(Pos.CENTER_LEFT);
    Label tmpMaxLbl = new Label("Always Start Full-Screen");
    tmpMaxLbl.setPrefWidth(250.0);
    btnMaxToggle = new ToggleSwitch();
    btnMaxToggle.setId("btnMaxToggle");
    Tooltip tmpMaxTooltip = new Tooltip("Launch the application in full-screen mode");
    tmpMaxTooltip.setShowDelay(Duration.millis(250));
    btnMaxToggle.setTooltip(tmpMaxTooltip);
    tmpMaxBox.getChildren().addAll(tmpMaxLbl, btnMaxToggle);

    tmpAppearanceSection.getChildren().addAll(tmpAppTitle, tmpAppSub, tmpCursorBox, tmpCompactBox, tmpMaxBox);

    rootPane.getChildren().addAll(titleLbl, workspaceSection, playerSection, tmpSpacer, tmpAppearanceSection,
        lblStatus);

    return rootPane;
  }

  /**
   * Initializes the settings view, setting up control actions, listeners, and
   * validators. Registers scene-detach hooks for automated saves.
   */
  public void initialize() {
    settingsRepository = AppContext.getInstance().getSettingsRepository();

    btnRemoveFolder.setDisable(true);
    lstWorkspaces.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          btnRemoveFolder.setDisable(newValue == null);
        });

    setupValidationListeners();
    setupWorkspaceCellFactory();
    loadSettings();

    // Auto-save when changing tabs (which removes lstWorkspaces from the scene)
    lstWorkspaces.sceneProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (oldValue != null && newValue == null) {
            if (hasUnsavedChanges() && !hasValidationErrors()) {
              saveSettings();
            }
          }
        });
  }

  /** Reloads settings details and clears status warnings. */
  @Override
  public void refresh() {
    loadSettings();
    lblStatus.setText("");
  }

  /**
   * Loads the current settings from the repository and populates the UI fields.
   * Also captures a snapshot of the loaded state to enable dirty checking for
   * unsaved changes.
   */
  private void loadSettings() {
    // Load workspaces
    lstWorkspaces.getItems().clear();
    String tmpWorkspaces = settingsRepository.get("workspace_folders", "");
    if (!tmpWorkspaces.isBlank()) {
      List<String> tmpFolderList =
          Arrays.stream(tmpWorkspaces.split(";")).map(String::trim)
              .filter(s -> !s.isEmpty()).collect(Collectors.toList());
      lstWorkspaces.getItems().addAll(tmpFolderList);
    }

    // Load player path
    String tmpPlayer = settingsRepository.get("player_executable_path", "");
    txtPlayerPath.setText(tmpPlayer);

    // Load cursor mode
    String tmpCursor = settingsRepository.get("custom_cursor", "true");
    btnCursorToggle.setSelected("true".equals(tmpCursor));

    // Load compact mode
    String tmpCompactMode = settingsRepository.get("compact_mode", "false");
    btnCompactToggle.setSelected("true".equals(tmpCompactMode));

    // Load start maximized
    String tmpMaxMode = settingsRepository.get("always_start_maximized", "false");
    btnMaxToggle.setSelected("true".equals(tmpMaxMode));

    // Store loaded state snapshots for dirty checking
    loadedWorkspaces = tmpWorkspaces;
    loadedPlayerPath = tmpPlayer;
    loadedCursorMode = tmpCursor;

    // Initial validation trigger
    validateAll();
  }

  /**
   * Opens a directory chooser dialog to let the user select a new workspace
   * folder. If a valid, non-duplicate directory is selected, it is added to the
   * list and saved.
   */
  private void addWorkspace() {
    DirectoryChooser tmpChooser = new DirectoryChooser();
    tmpChooser.setTitle("Select Workspace Folder");
    Window tmpWindow = btnAddFolder.getScene().getWindow();
    File tmpSelectedDirectory = tmpChooser.showDialog(tmpWindow);

    if (tmpSelectedDirectory != null) {
      String tmpPath =
          tmpSelectedDirectory.getAbsolutePath().replace("\\", "/");
      if (!lstWorkspaces.getItems().contains(tmpPath)) {
        lstWorkspaces.getItems().add(tmpPath);
        saveWorkspacesOnly();
        showStatus("Workspace folder added.");
      }
    }
  }

  /**
   * Prompts for confirmation and removes the currently selected workspace from
   * the settings. Also triggers the deletion of all movie records associated
   * with that workspace from the local database.
   */
  private void removeWorkspace() {
    String tmpSelected = lstWorkspaces.getSelectionModel().getSelectedItem();
    if (tmpSelected != null) {
      boolean confirmed = CustomAlert.showConfirmation(
          lstWorkspaces.getScene().getWindow(), "Remove Workspace",
          "Remove workspace: " + tmpSelected,
          "Are you sure you want to remove this workspace? Movies from this workspace will be removed from your library, but files will not be deleted from your disk.",
          "/io/github/kullik01/kinonode/images/removal_icon.png");
      if (confirmed) {
        lstWorkspaces.getItems().remove(tmpSelected);
        saveWorkspacesOnly();
        showStatus("Workspace folder removed.");

        // Clear movies belonging to the removed workspace from local database
        // records
        io.github.kullik01.kinonode.repository.MovieRepository tmpMovieRepository =
            AppContext.getInstance().getMovieRepository();
        tmpMovieRepository.deleteByFilePathPrefix(tmpSelected);
      }
    }
  }

  /**
   * Opens a file chooser dialog to select an external media player executable.
   * Restricts file types based on the underlying operating system.
   */
  private void browsePlayer() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Select External Media Player Executable");

    // Add extension filters for executables based on the operating system
    String tmpOperatingSystem = System.getProperty("os.name").toLowerCase();
    if (tmpOperatingSystem.contains("win")) {
      tmpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
          "Executable Files", "*.exe", "*.bat", "*.cmd"));
    } else {
      tmpChooser.getExtensionFilters()
          .add(new FileChooser.ExtensionFilter("All Files", "*"));
    }

    Window tmpWindow = txtPlayerPath.getScene().getWindow();
    File tmpFile = tmpChooser.showOpenDialog(tmpWindow);
    if (tmpFile != null) {
      txtPlayerPath.setText(tmpFile.getAbsolutePath().replace("\\", "/"));
    }
  }

  /**
   * Persists all current settings from the UI to the repository and updates the
   * local snapshots for dirty checking.
   */
  private void saveSettings() {
    // Save paths
    saveWorkspacesOnly();

    // Save player
    String tmpPlayer = txtPlayerPath.getText().trim().replace("\\", "/");
    settingsRepository.set("player_executable_path", tmpPlayer);

    // Save cursor mode
    String tmpCursor = btnCursorToggle.isSelected() ? "true" : "false";
    settingsRepository.set("custom_cursor", tmpCursor);
    io.github.kullik01.kinonode.util.CursorUtils.updateGlobalCursor();

    // Update dirty checking snapshots
    loadedPlayerPath = tmpPlayer;
    loadedCursorMode = tmpCursor;

    showStatus("Settings saved successfully.");
  }

  /**
   * Saves only the workspace folder list to the repository. Used for immediate
   * persistence when adding or removing folders.
   */
  private void saveWorkspacesOnly() {
    String tmpPathsStr = String.join(";", lstWorkspaces.getItems());
    settingsRepository.set("workspace_folders", tmpPathsStr);
    loadedWorkspaces = tmpPathsStr;
  }

  /**
   * Initializes listeners on input fields and lists to trigger real-time
   * validation and change detection.
   */
  private void setupValidationListeners() {
    txtPlayerPath.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          validatePlayerPath(newValue);
        });

    lstWorkspaces.getItems().addListener((
        javafx.collections.ListChangeListener.Change<? extends String> tmpChange) -> {
      // Unsaved changes detected
    });

    btnCursorToggle.selectedProperty().addListener((tmpObservable, tmpOldValue, tmpNewValue) -> {
      String tmpCursor = tmpNewValue ? "true" : "false";
      settingsRepository.set("custom_cursor", tmpCursor);
      loadedCursorMode = tmpCursor;
      CursorUtils.updateGlobalCursor();
    });

    btnCompactToggle.selectedProperty().addListener((tmpObservable, tmpOldValue, tmpNewValue) -> {
      String tmpCompact = tmpNewValue ? "true" : "false";
      settingsRepository.set("compact_mode", tmpCompact);
      io.github.kullik01.kinonode.KinoNodeApp.updateGlobalDensity();
    });

    btnMaxToggle.selectedProperty().addListener((tmpObservable, tmpOldValue, tmpNewValue) -> {
      String tmpMax = tmpNewValue ? "true" : "false";
      settingsRepository.set("always_start_maximized", tmpMax);
    });
  }

  /**
   * Triggers a full validation pass across all configurable settings.
   */
  private void validateAll() {
    validatePlayerPath(txtPlayerPath.getText());
  }

  /**
   * Validates the provided file path to ensure it points to an existing file if
   * it is not empty. Updates the visual validation state of the player path
   * input field.
   *
   * @param path The absolute path to the executable file.
   */
  private void validatePlayerPath(String path) {
    if (path == null || path.isBlank()) {
      // Empty path is considered valid (meaning no custom external player
      // configured)
      setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, true,
          null);
    } else {
      File tmpFile = new File(path);
      if (tmpFile.exists() && tmpFile.isFile()) {
        setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError,
            true, null);
      } else {
        setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError,
            false, "Executable file does not exist.");
      }
    }
  }

  /**
   * Updates the UI styling and feedback labels of an input field based on its
   * validity.
   *
   * @param field The TextField being validated.
   * @param statusLabel The Label used to display validation icons (e.g.,
   *        cross).
   * @param errorLabel The Label used to display the textual error message.
   * @param isValid True if the input is valid, false otherwise.
   * @param errorMessage The message to show if the input is invalid.
   */
  private void setValidationStatus(TextField field, Label statusLabel,
      Label errorLabel, boolean isValid, String errorMessage) {
    if (isValid) {
      field.setStyle(""); // Clean default style
      statusLabel.setGraphic(null); // No check icon

      if (errorLabel != null) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
      }
    } else {
      // Highlight the text field with AtlantaFX/CSS danger styling
      field.setStyle("-fx-border-color: -color-danger-fg;");

      SVGPath tmpCrossIcon = new SVGPath();
      tmpCrossIcon.setContent(
          "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
      tmpCrossIcon.setStyle("-fx-fill: -color-danger-fg;");
      tmpCrossIcon.setScaleX(0.85);
      tmpCrossIcon.setScaleY(0.85);
      statusLabel.setGraphic(tmpCrossIcon);

      if (errorLabel != null) {
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
      }
    }
  }

  /**
   * Verifies if any inputs have errors that prevent settings from saving.
   *
   * @return {@code true} if errors exist, {@code false} if settings are ready
   *         to be saved
   */
  public boolean hasValidationErrors() {
    boolean tmpIsPlayerValid = true;
    String tmpPlayerPath = txtPlayerPath.getText();
    if (tmpPlayerPath != null && !tmpPlayerPath.isBlank()) {
      File tmpFile = new File(tmpPlayerPath);
      tmpIsPlayerValid = tmpFile.exists() && tmpFile.isFile();
    }

    return !tmpIsPlayerValid;
  }

  /**
   * Checks if current input configurations differ from loaded settings.
   *
   * @return {@code true} if unsaved changes exist; {@code false} otherwise
   */
  public boolean hasUnsavedChanges() {
    String tmpCurrentWorkspaces = String.join(";", lstWorkspaces.getItems());
    String tmpCurrentPlayer = txtPlayerPath.getText() != null
        ? txtPlayerPath.getText().trim().replace("\\", "/")
        : "";
    String tmpCurrentCursor = btnCursorToggle.isSelected() ? "true" : "false";

    return !tmpCurrentWorkspaces.equals(loadedWorkspaces)
        || !tmpCurrentPlayer.equals(loadedPlayerPath)
        || !tmpCurrentCursor.equals(loadedCursorMode);
  }

  /**
   * Configures the visual presentation of the workspace list items. Displays
   * the folder path along with an icon, and adds a warning indicator if the
   * configured directory no longer exists on the filesystem.
   */
  private void setupWorkspaceCellFactory() {
    lstWorkspaces.setCellFactory(
        tmpListView -> new javafx.scene.control.ListCell<String>() {
          @Override
          protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
              setText(null);
              setGraphic(null);
            } else {
              HBox tmpBox = new HBox(10);
              tmpBox.setAlignment(Pos.CENTER_LEFT);

              // Folder icon visual layout setup
              SVGPath tmpFolderIcon = new SVGPath();
              tmpFolderIcon.setContent(
                  "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z");
              tmpFolderIcon.setStyle("-fx-fill: #2178dd;");
              tmpFolderIcon.setScaleX(0.95);
              tmpFolderIcon.setScaleY(0.95);

              Label tmpLabelPath = new Label(item);
              tmpLabelPath.setMaxWidth(400);
              tmpLabelPath.setWrapText(false);

              Region tmpSpacer = new Region();
              HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

              // Validate directory folder existence and show a warning SVG if
              // missing
              File tmpDir = new File(item);
              if (tmpDir.exists() && tmpDir.isDirectory()) {
                tmpLabelPath.setStyle("");
                tmpBox.getChildren().addAll(tmpFolderIcon, tmpLabelPath);
              } else {
                SVGPath tmpStatusIcon = new SVGPath();
                tmpStatusIcon.setContent(
                    "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
                tmpStatusIcon.setStyle("-fx-fill: -color-danger-fg;");
                tmpStatusIcon.setScaleX(0.8);
                tmpStatusIcon.setScaleY(0.8);
                tmpLabelPath.setStyle("-fx-text-fill: -color-danger-fg;");

                tmpBox.getChildren().addAll(tmpFolderIcon, tmpLabelPath,
                    tmpSpacer, tmpStatusIcon);
              }
              setGraphic(tmpBox);
            }
          }
        });
  }
}
