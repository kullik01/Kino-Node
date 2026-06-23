package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
import java.util.stream.Collectors;

public class SettingsController implements Refreshable {

    @FXML private ListView<String> lstWorkspaces;
    @FXML private Button btnAddFolder;
    @FXML private Button btnRemoveFolder;
    @FXML private TextField txtPlayerPath;
    @FXML private TextField txtExtensions;
    @FXML private Label lblStatus;
    @FXML private Label lblPlayerStatus;
    @FXML private Label lblPlayerError;
    @FXML private Label lblExtensionsStatus;
    @FXML private Label lblExtensionsError;
    @FXML private Button btnSave;

    private SettingsRepository settingsRepository;
    private String loadedWorkspaces = "";
    private String loadedPlayerPath = "";
    private String loadedExtensions = "";

    @FXML
    public void initialize() {
        settingsRepository = AppContext.getInstance().getSettingsRepository();
        setupValidationListeners();
        setupWorkspaceCellFactory();
        loadSettings();
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

        // Load video extensions
        String extensions = settingsRepository.get("video_extensions", "mp4,mkv,avi,mov,m4v");
        txtExtensions.setText(extensions);

        // Store loaded state snapshots for dirty checking
        loadedWorkspaces = workspaces;
        loadedPlayerPath = player;
        loadedExtensions = extensions;

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
                lblStatus.setText("Workspace folder added.");
            }
        }
    }

    @FXML
    private void removeWorkspace() {
        String selected = lstWorkspaces.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lstWorkspaces.getItems().remove(selected);
            saveWorkspacesOnly();
            lblStatus.setText("Workspace folder removed.");
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

    @FXML
    public void saveSettings() {
        // Save paths
        saveWorkspacesOnly();

        // Save player
        String player = txtPlayerPath.getText().trim().replace("\\", "/");
        settingsRepository.set("player_executable_path", player);

        // Save extensions
        String extRaw = txtExtensions.getText().trim();
        String extensionsClean = Arrays.stream(extRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        
        if (extensionsClean.isEmpty()) {
            extensionsClean = "mp4,mkv,avi,mov,m4v";
        }
        settingsRepository.set("video_extensions", extensionsClean);
        txtExtensions.setText(extensionsClean);

        // Update dirty checking snapshots
        loadedPlayerPath = player;
        loadedExtensions = extensionsClean;

        lblStatus.setText("Settings saved successfully.");
    }

    private void saveWorkspacesOnly() {
        String pathsStr = String.join(";", lstWorkspaces.getItems());
        settingsRepository.set("workspace_folders", pathsStr);
        loadedWorkspaces = pathsStr;
    }

    private void setupValidationListeners() {
        txtPlayerPath.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePlayerPath(newVal);
            updateSaveButtonState();
        });

        txtExtensions.textProperty().addListener((obs, oldVal, newVal) -> {
            validateExtensions(newVal);
            updateSaveButtonState();
        });
    }

    private void validateAll() {
        validatePlayerPath(txtPlayerPath.getText());
        validateExtensions(txtExtensions.getText());
        updateSaveButtonState();
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

    private void validateExtensions(String ext) {
        if (ext == null || ext.isBlank()) {
            setValidationStatus(txtExtensions, lblExtensionsStatus, lblExtensionsError, false, "Extensions list cannot be empty.");
        } else {
            String clean = ext.trim();
            boolean valid = true;
            String[] parts = clean.split(",");
            if (parts.length == 0) {
                valid = false;
            } else {
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty() || !trimmed.matches("^[a-zA-Z0-9]+$")) {
                        valid = false;
                        break;
                    }
                }
            }

            if (valid) {
                setValidationStatus(txtExtensions, lblExtensionsStatus, lblExtensionsError, true, null);
            } else {
                setValidationStatus(txtExtensions, lblExtensionsStatus, lblExtensionsError, false, "Invalid format. Use comma-separated alphanumeric values (e.g. mp4,mkv).");
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

    private void updateSaveButtonState() {
        btnSave.setDisable(hasValidationErrors());
    }

    public boolean hasValidationErrors() {
        boolean isPlayerValid = true;
        String playerPath = txtPlayerPath.getText();
        if (playerPath != null && !playerPath.isBlank()) {
            File file = new File(playerPath);
            isPlayerValid = file.exists() && file.isFile();
        }

        boolean isExtensionsValid = false;
        String ext = txtExtensions.getText();
        if (ext != null && !ext.isBlank()) {
            String[] parts = ext.split(",");
            if (parts.length > 0) {
                isExtensionsValid = true;
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty() || !trimmed.matches("^[a-zA-Z0-9]+$")) {
                        isExtensionsValid = false;
                        break;
                    }
                }
            }
        }
        
        return !isPlayerValid || !isExtensionsValid;
    }

    public boolean hasUnsavedChanges() {
        String currentWorkspaces = String.join(";", lstWorkspaces.getItems());
        String currentPlayer = txtPlayerPath.getText() != null ? txtPlayerPath.getText().trim().replace("\\", "/") : "";
        
        String extRaw = txtExtensions.getText() != null ? txtExtensions.getText().trim() : "";
        String currentExtensions = Arrays.stream(extRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        if (currentExtensions.isEmpty()) {
            currentExtensions = "mp4,mkv,avi,mov,m4v";
        }
        
        return !currentWorkspaces.equals(loadedWorkspaces)
                || !currentPlayer.equals(loadedPlayerPath)
                || !currentExtensions.equals(loadedExtensions);
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
                    folderIcon.setStyle("-fx-fill: -color-accent-emphasis;");
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
