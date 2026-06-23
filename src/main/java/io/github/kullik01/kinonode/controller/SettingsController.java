package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

    private SettingsRepository settingsRepository;

    @FXML
    public void initialize() {
        settingsRepository = AppContext.getInstance().getSettingsRepository();
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
    private void saveSettings() {
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

        lblStatus.setText("Settings saved successfully.");
    }

    private void saveWorkspacesOnly() {
        String pathsStr = String.join(";", lstWorkspaces.getItems());
        settingsRepository.set("workspace_folders", pathsStr);
    }
}
