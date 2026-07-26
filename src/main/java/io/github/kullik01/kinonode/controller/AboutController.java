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

import java.awt.Desktop;


import io.github.kullik01.kinonode.KinoNodeApp;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.model.Watchlist;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
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

  @FXML
  private Label lblVersion;
  @FXML
  private TextField lblExtensions;
  @FXML
  private TextField lblDbPath;
  @FXML
  private Label lblStorageMode;

  @FXML
  private Label lblJavaRuntime;
  @FXML
  private Label lblOsDetails;


  @FXML
  private Label lblMovieCount;
  @FXML
  private Label lblDbSize;
  @FXML
  private Label lblRegularWatchlistCount;
  @FXML
  private Label lblSmartWatchlistCount;
  @FXML
  private TextField lblPlayerPath;

  /**
   * Initializes the About view. Automatically invoked by JavaFX after the FXML
   * layout has been loaded and injected.
   */
  @FXML
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
    } catch (Exception tmpException) {
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

    } catch (Exception tmpException) {
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
    } catch (Exception tmpException) {
      lblMovieCount.setText("Unknown");
      lblRegularWatchlistCount.setText("Unknown");
      lblSmartWatchlistCount.setText("Unknown");
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
    } catch (Exception tmpException) {
      lblPlayerPath.setText("Unknown");
    }
  }

  @FXML
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
    } catch (Exception tmpException) {
      // Ignore if it cannot be opened
    }
  }

  @FXML
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
    } catch (Exception tmpException) {
      // Ignore if it cannot be opened
    }
  }

  @FXML
  private void openRepository() {
    openWebpage("https://github.com/kullik01/Kino-Node");
  }

  @FXML
  private void openIssueTracker() {
    openWebpage("https://github.com/kullik01/Kino-Node/issues/new");
  }

  private void openWebpage(String url) {
    try {
      KinoNodeApp.getInstance().getHostServices().showDocument(url);
    } catch (Exception tmpException) {
      // Ignore errors opening browser
    }
  }

  @FXML
  private void checkForUpdates() {
    openWebpage("https://github.com/kullik01/Kino-Node/releases");
  }
}
