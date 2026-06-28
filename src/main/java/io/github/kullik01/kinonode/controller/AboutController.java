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

import io.github.kullik01.kinonode.service.AppContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the "About" view of the application. This class displays system info, database
 * path/size, configuring video extensions, movie/watchlist statistics, Java and OS version info,
 * and configured media player details. It also provides options to open folder paths in the
 * operating system's default file browser.
 */
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

  /**
   * Initializes the About view. Automatically invoked by JavaFX after the FXML layout has been
   * loaded and injected.
   */
  @FXML
  public void initialize() {
    loadAboutInfo();
  }

  /**
   * Refreshes the display information of the About page. Part of the Refreshable interface
   * contract.
   */
  @Override
  public void refresh() {
    loadAboutInfo();
  }

  private void loadAboutInfo() {
    lblVersion.setText("1.0.0");

    // 1. Extensions
    String extensions =
        AppContext.getInstance()
            .getSettingsRepository()
            .get(
                "video_extensions", "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");
    lblExtensions.setText(extensions);

    // 2. Database path & size
    try {
      Path dbPath = AppContext.getInstance().getDatabaseService().getDbPath();
      // Display path with forward slashes for cross-platform consistency
      lblDbPath.setText(dbPath.toAbsolutePath().toString().replace("\\", "/"));

      if (Files.exists(dbPath)) {
        // Calculate file size in MB
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
      lblWatchlistCount.setText(
          watchlistCount + (watchlistCount == 1 ? " watchlist" : " watchlists"));
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

  @FXML
  private void openPlayerLocation() {
    try {
      Optional<Path> playerPathOpt = AppContext.getInstance().getPlaybackService().getPlayerPath();
      // Check if player is configured and its parent directory exists
      if (playerPathOpt.isPresent() && Files.exists(playerPathOpt.get().getParent())) {
        // Ensure Desktop API is supported on the current platform before opening the explorer
        if (java.awt.Desktop.isDesktopSupported()
            && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
          java.awt.Desktop.getDesktop().open(playerPathOpt.get().getParent().toFile());
        }
      }
    } catch (Exception e) {
      // Ignore if it cannot be opened
    }
  }

  @FXML
  private void openDatabaseLocation() {
    try {
      Path dbPath = AppContext.getInstance().getDatabaseService().getDbPath();
      // Verify that the database path and its parent folder exist
      if (dbPath != null && Files.exists(dbPath.getParent())) {
        // Open the database folder using system's default file browser
        if (java.awt.Desktop.isDesktopSupported()
            && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
          java.awt.Desktop.getDesktop().open(dbPath.getParent().toFile());
        }
      }
    } catch (Exception e) {
      // Ignore if it cannot be opened
    }
  }
}
