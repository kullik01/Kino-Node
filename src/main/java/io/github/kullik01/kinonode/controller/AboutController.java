package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.service.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class AboutController implements Refreshable {

    @FXML private Label lblVersion;
    @FXML private Label lblExtensions;
    @FXML private Label lblDbPath;

    @FXML private Label lblJavaRuntime;
    @FXML private Label lblOsDetails;
    @FXML private Label lblMovieCount;
    @FXML private Label lblDbSize;
    @FXML private Label lblWatchlistCount;
    @FXML private Label lblPlayerPath;

    @FXML
    public void initialize() {
        loadAboutInfo();
    }

    @Override
    public void refresh() {
        loadAboutInfo();
    }

    private void loadAboutInfo() {
        lblVersion.setText("1.0.0");
        
        // 1. Extensions
        String extensions = AppContext.getInstance().getSettingsRepository().get("video_extensions", "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");
        lblExtensions.setText(extensions);
        
        // 2. Database path & size
        try {
            Path dbPath = AppContext.getInstance().getDatabaseService().getDbPath();
            lblDbPath.setText(dbPath.toAbsolutePath().toString().replace("\\", "/"));
            
            if (Files.exists(dbPath)) {
                long sizeBytes = Files.size(dbPath);
                double sizeMB = sizeBytes / (1024.0 * 1024.0);
                if (lblDbSize != null) {
                    lblDbSize.setText(String.format("%.2f MB", sizeMB));
                }
            } else {
                if (lblDbSize != null) {
                    lblDbSize.setText("Unknown");
                }
            }
        } catch (Exception e) {
            lblDbPath.setText("Unknown database path");
        }

        // 3. System properties
        try {
            String javaVer = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");
            lblJavaRuntime.setText(javaVer + " (" + javaVendor + ")");
            
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String osVer = System.getProperty("os.version");
            lblOsDetails.setText(osName + " " + osVer + " (" + osArch + ")");
        } catch (Exception e) {
            lblJavaRuntime.setText("Unknown");
            lblOsDetails.setText("Unknown");
        }
        
        // 4. Statistics (Movies and Watchlists)
        try {
            int movieCount = AppContext.getInstance().getMovieRepository().findAll().size();
            lblMovieCount.setText(movieCount + (movieCount == 1 ? " movie" : " movies"));
            
            int watchlistCount = AppContext.getInstance().getWatchlistRepository().findAll().size();
            lblWatchlistCount.setText(watchlistCount + (watchlistCount == 1 ? " watchlist" : " watchlists"));
        } catch (Exception e) {
            lblMovieCount.setText("Unknown");
            lblWatchlistCount.setText("Unknown");
        }
        
        // 5. Media Player Executable
        try {
            Optional<Path> playerPathOpt = AppContext.getInstance().getPlaybackService().getPlayerPath();
            if (playerPathOpt.isPresent()) {
                lblPlayerPath.setText(playerPathOpt.get().toAbsolutePath().toString().replace("\\", "/"));
            } else {
                lblPlayerPath.setText("No player configured (using auto-detect/none)");
            }
        } catch (Exception e) {
            lblPlayerPath.setText("Unknown");
        }
    }
}
