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

import io.github.kullik01.kinonode.KinoNodeApp;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.service.DatabaseService;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.PlaybackService;
import com.google.inject.Inject;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.view.pages.AboutView;
import javafx.scene.Parent;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the "About" view of the application. This class fetches system
 * info, database path/size, and statistics, injecting them into the View.
 */
public class AboutController implements Refreshable {

  private AboutView view;

  private final SettingsRepository settingsRepository;
  private final DatabaseService databaseService;
  private final MovieRepository movieRepository;
  private final WatchlistRepository watchlistRepository;
  private final PlaybackService playbackService;
  private final ResourceBundle resources;

  @Inject
  public AboutController(SettingsRepository settingsRepository,
      DatabaseService databaseService, MovieRepository movieRepository,
      WatchlistRepository watchlistRepository,
      PlaybackService playbackService,
      ResourceBundle resources) {
    this.settingsRepository = settingsRepository;
    this.databaseService = databaseService;
    this.movieRepository = movieRepository;
    this.watchlistRepository = watchlistRepository;
    this.playbackService = playbackService;
    this.resources = resources;
  }

  public Parent buildView() {
    view = new AboutView(resources);

    // Wire up event handlers
    view.btnGitHub.setOnAction(e -> openRepository());
    view.btnBug.setOnAction(e -> openIssueTracker());
    view.btnUpdate.setOnAction(e -> checkForUpdates());
    view.btnDbLoc.setOnAction(e -> openDatabaseLocation());
    view.btnPlayerLoc.setOnAction(e -> openPlayerLocation());

    return view.getRoot();
  }

  public void initialize() {
    loadAboutInfo();
  }

  @Override
  public void refresh() {
    loadAboutInfo();
  }

  private void loadAboutInfo() {
    if (view == null)
      return;

    view.lblVersion.setText("1.0.0");

    // 1. Extensions
    String tmpExtensions = settingsRepository.get("video_extensions",
        "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");
    view.lblExtensions.setText(tmpExtensions);

    // 2. Database path & size
    try {
      Path tmpDatabasePath = databaseService.getDbPath();
      view.lblDbPath.setText(
          tmpDatabasePath.toAbsolutePath().toString().replace("\\", "/"));

      Path tmpCurrentDir = Path.of(System.getProperty("user.dir"));
      if (tmpDatabasePath.toAbsolutePath().startsWith(tmpCurrentDir)) {
        view.lblStorageMode.setText("(Single Directory / Portable Mode)");
      } else {
        view.lblStorageMode.setText("(OS Application Data Directory)");
      }

      if (Files.exists(tmpDatabasePath)) {
        long tmpSizeBytes = Files.size(tmpDatabasePath);
        double tmpSizeMegabytes = tmpSizeBytes / (1024.0 * 1024.0);
        view.lblDbSize.setText(String.format("%.2f MB", tmpSizeMegabytes));
      } else {
        view.lblDbSize.setText("Unknown");
      }
    } catch (Exception exception) {
      view.lblDbPath.setText("Unknown database path");
    }

    // 3. System properties
    try {
      String tmpJavaVersion = System.getProperty("java.version");
      String tmpJavaVendor = System.getProperty("java.vendor");
      view.lblJavaRuntime.setText(tmpJavaVersion + " (" + tmpJavaVendor + ")");

      String tmpOperatingSystemName = System.getProperty("os.name");
      String tmpOperatingSystemArchitecture = System.getProperty("os.arch");
      String tmpOperatingSystemVersion = System.getProperty("os.version");
      view.lblOsDetails
          .setText(tmpOperatingSystemName + " " + tmpOperatingSystemVersion
              + " (" + tmpOperatingSystemArchitecture + ")");
    } catch (Exception exception) {
      view.lblJavaRuntime.setText("Unknown");
      view.lblOsDetails.setText("Unknown");
    }

    // 4. Statistics
    try {
      int tmpMovieCount = movieRepository.findAll().size();
      view.lblMovieCount
          .setText(tmpMovieCount + (tmpMovieCount == 1 ? " movie" : " movies"));

      List<Watchlist> allWatchlists = watchlistRepository.findAll();
      long tmpSmartCount =
          allWatchlists.stream().filter(Watchlist::isSmart).count();
      long tmpRegularCount = allWatchlists.size() - tmpSmartCount;

      view.lblRegularWatchlistCount.setText(String.valueOf(tmpRegularCount));
      view.lblSmartWatchlistCount.setText(String.valueOf(tmpSmartCount));
    } catch (Exception exception) {
      view.lblMovieCount.setText("Unknown");
      view.lblRegularWatchlistCount.setText("Unknown");
      view.lblSmartWatchlistCount.setText("Unknown");
    }

    // 4.5 Workspace Size
    view.lblWorkspaceSize.setText("Calculating...");
    new Thread(() -> {
      try {
        String tmpWorkspaces = settingsRepository.get("workspace_folders", "");
        long totalWorkspaceSizeBytes = 0;
        if (!tmpWorkspaces.isBlank()) {
          String[] tmpFolderList = tmpWorkspaces.split(";");
          for (String folderPath : tmpFolderList) {
            Path path = Path.of(folderPath.trim());
            if (Files.exists(path) && Files.isDirectory(path)) {
              try (
                  java.util.stream.Stream<Path> pathStream = Files.walk(path)) {
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
              view.lblWorkspaceSize
                  .setText(String.format("%.2f GB", tmpSizeGb));
            } else {
              double tmpSizeMb = finalSize / (1024.0 * 1024.0);
              view.lblWorkspaceSize
                  .setText(String.format("%.2f MB", tmpSizeMb));
            }
          } else {
            view.lblWorkspaceSize.setText("0.00 MB");
          }
        });
      } catch (Exception exception) {
        javafx.application.Platform.runLater(() -> {
          view.lblWorkspaceSize.setText("Unknown");
        });
      }
    }).start();

    // 5. Media Player Executable
    try {
      Optional<Path> tmpPlayerPathOptional = playbackService.getPlayerPath();
      if (tmpPlayerPathOptional.isPresent()) {
        view.lblPlayerPath.setText(tmpPlayerPathOptional.get().toAbsolutePath()
            .toString().replace("\\", "/"));
      } else {
        view.lblPlayerPath
            .setText("No player configured (using auto-detect/none)");
      }
    } catch (Exception exception) {
      view.lblPlayerPath.setText("Unknown");
    }
  }

  private void openPlayerLocation() {
    try {
      Optional<Path> tmpPlayerPathOptional = playbackService.getPlayerPath();
      if (tmpPlayerPathOptional.isPresent()
          && Files.exists(tmpPlayerPathOptional.get().getParent())) {
        if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          Desktop.getDesktop()
              .open(tmpPlayerPathOptional.get().getParent().toFile());
        }
      }
    } catch (Exception exception) {
      // Ignore
    }
  }

  private void openDatabaseLocation() {
    try {
      Path tmpDatabasePath = databaseService.getDbPath();
      if (tmpDatabasePath != null
          && Files.exists(tmpDatabasePath.getParent())) {
        if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          Desktop.getDesktop().open(tmpDatabasePath.getParent().toFile());
        }
      }
    } catch (Exception exception) {
      // Ignore
    }
  }

  private void openRepository() {
    openWebpage("https://github.com/kullik01/Kino-Node");
  }

  private void openIssueTracker() {
    openWebpage("https://github.com/kullik01/Kino-Node/issues/new");
  }

  private void openWebpage(String url) {
    try {
      KinoNodeApp.getInstance().getHostServices().showDocument(url);
    } catch (Exception exception) {
      // Ignore
    }
  }

  private void checkForUpdates() {
    openWebpage("https://github.com/kullik01/Kino-Node/releases");
  }
}
