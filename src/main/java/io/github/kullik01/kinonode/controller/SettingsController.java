package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @FXML
    public void initialize() {
        settingsRepository = AppContext.getInstance().getSettingsRepository();
        
        btnRemoveFolder.setDisable(true);
        lstWorkspaces.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnRemoveFolder.setDisable(newVal == null);
        });

        setupValidationListeners();
        setupWorkspaceCellFactory();
        loadSettings();
        
        // Auto-save when changing tabs (which removes lstWorkspaces from the scene)
        lstWorkspaces.sceneProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal == null) {
                if (hasUnsavedChanges() && !hasValidationErrors()) {
                    saveSettings();
                }
            }
        });
    }

    @Override
    public void refresh() {
        loadSettings();
        lblStatus.setText("");
    }

    private void loadSettings() {
        // Load workspaces
        lstWorkspaces.getItems().clear();
        String workspaces = settingsRepository.get("workspace_folders", "");
        if (!workspaces.isBlank()) {
            List<String> folderList = Arrays.stream(workspaces.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            lstWorkspaces.getItems().addAll(folderList);
        }

        // Load player path
        String player = settingsRepository.get("player_executable_path", "");
        txtPlayerPath.setText(player);

        // Store loaded state snapshots for dirty checking
        loadedWorkspaces = workspaces;
        loadedPlayerPath = player;

        // Initial validation trigger
        validateAll();
    }

    @FXML
    private void addWorkspace() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Workspace Folder");
        Window window = btnAddFolder.getScene().getWindow();
        File selectedDirectory = chooser.showDialog(window);
        
        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath().replace("\\", "/");
            if (!lstWorkspaces.getItems().contains(path)) {
                lstWorkspaces.getItems().add(path);
                saveWorkspacesOnly();
                showStatus("Workspace folder added.");
            }
        }
    }

    @FXML
    private void removeWorkspace() {
        String selected = lstWorkspaces.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Remove Workspace");
            alert.setHeaderText("Remove workspace: " + selected);
            alert.setContentText("Are you sure you want to remove this workspace? Movies from this workspace will be removed from your library, but files will not be deleted from your disk.");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                lstWorkspaces.getItems().remove(selected);
                saveWorkspacesOnly();
                showStatus("Workspace folder removed.");
                
                io.github.kullik01.kinonode.repository.MovieRepository movieRepo = AppContext.getInstance().getMovieRepository();
                movieRepo.deleteByFilePathPrefix(selected);
            }
        }
    }

    @FXML
    private void browsePlayer() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select External Media Player Executable");
        
        // Add extension filters for executables
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable Files", "*.exe", "*.bat", "*.cmd"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*"));
        }

        Window window = txtPlayerPath.getScene().getWindow();
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            txtPlayerPath.setText(file.getAbsolutePath().replace("\\", "/"));
        }
    }

    private void saveSettings() {
        // Save paths
        saveWorkspacesOnly();

        // Save player
        String player = txtPlayerPath.getText().trim().replace("\\", "/");
        settingsRepository.set("player_executable_path", player);

        // Update dirty checking snapshots
        loadedPlayerPath = player;

        showStatus("Settings saved successfully.");
    }

    private void saveWorkspacesOnly() {
        String pathsStr = String.join(";", lstWorkspaces.getItems());
        settingsRepository.set("workspace_folders", pathsStr);
        loadedWorkspaces = pathsStr;
    }

    private void setupValidationListeners() {
        txtPlayerPath.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePlayerPath(newVal);
        });
        
        lstWorkspaces.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends String> c) -> {
            // Unsaved changes detected
        });
    }

    private void validateAll() {
        validatePlayerPath(txtPlayerPath.getText());
    }

    private void validatePlayerPath(String path) {
        if (path == null || path.isBlank()) {
            // Empty is valid
            setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, true, null);
        } else {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, true, null);
            } else {
                setValidationStatus(txtPlayerPath, lblPlayerStatus, lblPlayerError, false, "Executable file does not exist.");
            }
        }
    }

    private void setValidationStatus(TextField field, Label statusLabel, Label errorLabel, boolean isValid, String errorMessage) {
        if (isValid) {
            field.setStyle(""); // Clean default style
            statusLabel.setGraphic(null); // No check icon
            
            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
        } else {
            field.setStyle("-fx-border-color: -color-danger-fg;");
            
            SVGPath crossIcon = new SVGPath();
            crossIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
            crossIcon.setStyle("-fx-fill: -color-danger-fg;");
            crossIcon.setScaleX(0.85);
            crossIcon.setScaleY(0.85);
            statusLabel.setGraphic(crossIcon);
            
            if (errorLabel != null) {
                errorLabel.setText(errorMessage);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        }
    }

    public boolean hasValidationErrors() {
        boolean isPlayerValid = true;
        String playerPath = txtPlayerPath.getText();
        if (playerPath != null && !playerPath.isBlank()) {
            File file = new File(playerPath);
            isPlayerValid = file.exists() && file.isFile();
        }
        
        return !isPlayerValid;
    }

    public boolean hasUnsavedChanges() {
        String currentWorkspaces = String.join(";", lstWorkspaces.getItems());
        String currentPlayer = txtPlayerPath.getText() != null ? txtPlayerPath.getText().trim().replace("\\", "/") : "";
        
        return !currentWorkspaces.equals(loadedWorkspaces)
                || !currentPlayer.equals(loadedPlayerPath);
    }

    private void setupWorkspaceCellFactory() {
        lstWorkspaces.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    // Folder icon
                    SVGPath folderIcon = new SVGPath();
                    folderIcon.setContent("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z");
                    folderIcon.setStyle("-fx-fill: #2178dd;");
                    folderIcon.setScaleX(0.95);
                    folderIcon.setScaleY(0.95);
                    
                    Label lblPath = new Label(item);
                    lblPath.setMaxWidth(400);
                    lblPath.setWrapText(false);
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    // Status icon (only cross if missing, else empty)
                    File dir = new File(item);
                    if (dir.exists() && dir.isDirectory()) {
                        lblPath.setStyle("");
                        box.getChildren().addAll(folderIcon, lblPath);
                    } else {
                        SVGPath statusIcon = new SVGPath();
                        statusIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
                        statusIcon.setStyle("-fx-fill: -color-danger-fg;");
                        statusIcon.setScaleX(0.8);
                        statusIcon.setScaleY(0.8);
                        lblPath.setStyle("-fx-text-fill: -color-danger-fg;");
                        
                        box.getChildren().addAll(folderIcon, lblPath, spacer, statusIcon);
                    }
                    setGraphic(box);
                }
            }
        });
    }
}
