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
package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.AppContext;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

/**
 * Controller for the application Settings view. Handles configuration of workspace directories and
 * external media player paths. Implements {@link Refreshable} to support reloading configurations
 * dynamically. Features live validations, custom ListView graphic cells, and automated tab exit
 * saving.
 */
public class SettingsController implements Refreshable {

  @FXML private ListView<String> lstWorkspaces;
  @FXML private Button btnAddFolder;
  @FXML private Button btnRemoveFolder;
  @FXML private TextField txtPlayerPath;
  @FXML private Label lblStatus;
  @FXML private Label lblPlayerStatus;
  @FXML private Label lblPlayerError;

  private SettingsRepository settingsRepository;
  private String loadedWorkspaces = "";
  private String loadedPlayerPath = "";

  private javafx.animation.PauseTransition statusTimer;

  private void showStatus(String message) {
    lblStatus.setText(message);
    if (statusTimer != null) {
      statusTimer.stop();
    }
    statusTimer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
    statusTimer.setOnFinished(e -> lblStatus.setText(""));
    statusTimer.play();
  }

  /**
   * Initializes the settings view, setting up control actions, listeners, and validators. Registers
   * scene-detach hooks for automated saves.
   */
  @FXML
  public void initialize() {
    settingsRepository = AppContext.getInstance().getSettingsRepository();

    btnRemoveFolder.setDisable(true);
    lstWorkspaces
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              btnRemoveFolder.setDisable(newVal == null);
            });

    setupValidationListeners();
    setupWorkspaceCellFactory();
    loadSettings();

    // Auto-save when changing tabs (which removes lstWorkspaces from the scene)
    lstWorkspaces
        .sceneProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (oldVal != null && newVal == null) {
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

  private void loadSettings() {
    // Load workspaces
    lstWorkspaces.getItems().clear();
    String tmpWorkspaces = settingsRepository.get("workspace_folders", "");
    if (!tmpWorkspaces.isBlank()) {
      List<String> tmpFolderList =
          Arrays.stream(tmpWorkspaces.split(";"))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
      lstWorkspaces.getItems().addAll(tmpFolderList);
    }

    // Load player path
    String tmpPlayer = settingsRepository.get("player_executable_path", "");
    txtPlayerPath.setText(tmpPlayer);

    // Store loaded state snapshots for dirty checking
    loadedWorkspaces = tmpWorkspaces;
    loadedPlayerPath = tmpPlayer;

    // Initial validation trigger
    validateAll();
  }

  @FXML
  private void addWorkspace() {
    DirectoryChooser tmpChooser = new DirectoryChooser();
    tmpChooser.setTitle("Select Workspace Folder");
    Window tmpWindow = btnAddFolder.getScene().getWindow();
    File tmpSelectedDirectory = tmpChooser.showDialog(tmpWindow);

    if (tmpSelectedDirectory != null) {
      String tmpPath = tmpSelectedDirectory.getAbsolutePath().replace("\\", "/");
      if (!lstWorkspaces.getItems().contains(tmpPath)) {
        lstWorkspaces.getItems().add(tmpPath);
        saveWorkspacesOnly();
        showStatus("Workspace folder added.");
      }
    }
  }

  @FXML
  private void removeWorkspace() {
    String tmpSelected = lstWorkspaces.getSelectionModel().getSelectedItem();
    if (tmpSelected != null) {
      Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
      javafx.stage.Stage tmpStage = (javafx.stage.Stage) tmpAlert.getDialogPane().getScene().getWindow();
      tmpStage
          .getIcons()
          .add(
              new javafx.scene.image.Image(
                  getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
      tmpAlert.setTitle("Remove Workspace");
      tmpAlert.setHeaderText("Remove workspace: " + tmpSelected);
      tmpAlert.setContentText(
          "Are you sure you want to remove this workspace? Movies from this workspace will be removed from your library, but files will not be deleted from your disk.");

      javafx.scene.image.ImageView tmpCustomIcon =
          new javafx.scene.image.ImageView(
              new javafx.scene.image.Image(
                  getClass()
                      .getResourceAsStream(
                          "/io/github/kullik01/kinonode/images/removal_icon.png")));
      tmpCustomIcon.setFitHeight(48);
      tmpCustomIcon.setFitWidth(48);
      tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

      tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

      Optional<ButtonType> tmpResult = tmpAlert.showAndWait();
      if (tmpResult.isPresent() && tmpResult.get() == ButtonType.OK) {
        lstWorkspaces.getItems().remove(tmpSelected);
        saveWorkspacesOnly();
        showStatus("Workspace folder removed.");

        // Clear movies belonging to the removed workspace from local database records
        io.github.kullik01.kinonode.repository.MovieRepository tmpMovieRepo =
            AppContext.getInstance().getMovieRepository();
        tmpMovieRepo.deleteByFilePathPrefix(tmpSelected);
      }
    }
  }

  @FXML
  private void browsePlayer() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Select External Media Player Executable");

    // Add extension filters for executables based on the operating system
    String tmpOs = System.getProperty("os.name").toLowerCase();
    if (tmpOs.contains("win")) {
      tmpChooser
          .getExtensionFilters()
          .add(new FileChooser.ExtensionFilter("Executable Files", "*.exe", "*.bat", "*.cmd"));
    } else {
      tmpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*"));
    }

    Window tmpWindow = txtPlayerPath.getScene().getWindow();
    File tmpFile = tmpChooser.showOpenDialog(tmpWindow);
    if (tmpFile != null) {
      txtPlayerPath.setText(tmpFile.getAbsolutePath().replace("\\", "/"));
    }
  }

  private void saveSettings() {
    // Save paths
    saveWorkspacesOnly();

    // Save player
    String tmpPlayer = txtPlayerPath.getText().trim().replace("\\", "/");
    settingsRepository.set("player_executable_path", tmpPlayer);

    // Update dirty checking snapshots
    loadedPlayerPath = tmpPlayer;

    showStatus("Settings saved successfully.");
  }

  private void saveWorkspacesOnly() {
    String tmpPathsStr = String.join(";", lstWorkspaces.getItems());
    settingsRepository.set("workspace_folders", tmpPathsStr);
    loadedWorkspaces = tmpPathsStr;
  }

  private void setupValidationListeners() {
    txtPlayerPath
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              validatePlayerPath(newVal);
            });

    lstWorkspaces
        .getItems()
        .addListener(
            (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
              // Unsaved changes detected
            });
  }

  private void validateAll() {
    validatePlayerPath(txtPlayerPath.getText());
  }

  private void validatePlayerPath(String path) {
    if (path == null || path.isBlank()) {
      // Empty path is considered valid (meaning no custom external player configured)
      setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, true, null);
    } else {
      File tmpFile = new File(path);
      if (tmpFile.exists() && tmpFile.isFile()) {
        setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, true, null);
      } else {
        setValidationStatus(
            txtPlayerPath,
            lblPlayerStatus,
            lblPlayerError,
            false,
            "Executable file does not exist.");
      }
    }
  }

  private void setValidationStatus(
      TextField field, Label statusLabel, Label errorLabel, boolean isValid, String errorMessage) {
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
   * @return {@code true} if errors exist, {@code false} if settings are ready to be saved
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
    String tmpCurrentPlayer =
        txtPlayerPath.getText() != null ? txtPlayerPath.getText().trim().replace("\\", "/") : "";

    return !tmpCurrentWorkspaces.equals(loadedWorkspaces) || !tmpCurrentPlayer.equals(loadedPlayerPath);
  }

  private void setupWorkspaceCellFactory() {
    lstWorkspaces.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<String>() {
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

                  Label tmpLblPath = new Label(item);
                  tmpLblPath.setMaxWidth(400);
                  tmpLblPath.setWrapText(false);

                  Region tmpSpacer = new Region();
                  HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

                  // Validate directory folder existence and show a warning SVG if missing
                  File tmpDir = new File(item);
                  if (tmpDir.exists() && tmpDir.isDirectory()) {
                    tmpLblPath.setStyle("");
                    tmpBox.getChildren().addAll(tmpFolderIcon, tmpLblPath);
                  } else {
                    SVGPath tmpStatusIcon = new SVGPath();
                    tmpStatusIcon.setContent(
                        "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
                    tmpStatusIcon.setStyle("-fx-fill: -color-danger-fg;");
                    tmpStatusIcon.setScaleX(0.8);
                    tmpStatusIcon.setScaleY(0.8);
                    tmpLblPath.setStyle("-fx-text-fill: -color-danger-fg;");

                    tmpBox.getChildren().addAll(tmpFolderIcon, tmpLblPath, tmpSpacer, tmpStatusIcon);
                  }
                  setGraphic(tmpBox);
                }
              }
            });
  }
}
