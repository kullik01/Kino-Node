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

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.service.LibraryScanService;
import io.github.kullik01.kinonode.util.CustomAlert;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.VBox;

/**
 * Controller for the library view of the application. Manages displaying,
 * filtering, searching, and sorting the movies in the user's library.
 * Implements {@link Refreshable} to allow refreshing the movie catalog display.
 * Caches FXML cards and controllers to optimize rendering performance when
 * filtering/sorting.
 */
public class LibraryController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(LibraryController.class.getName());

  @FXML
  private VBox libraryRoot;
  @FXML
  private TextField txtSearch;
  @FXML
  private ComboBox<String> cmbSort;
  @FXML
  private FlowPane gridPane;
  @FXML
  private Button btnSyncLibrary;

  @FXML
  private HBox progressArea;
  @FXML
  private ProgressBar progressBar;

  private MovieRepository movieRepository;
  private LibraryScanService libraryScanService;
  private List<Movie> allMovies = new ArrayList<>();
  private Task<LibraryScanService.ScanResult> activeScanTask;
  private MovieDetailController currentMovieDetailController;
  private final Map<Integer, Parent> cardCache = new HashMap<>();
  private final Map<Integer, MovieCardController> controllerCache =
      new HashMap<>();

  /**
   * Initializes the controller. Sets up the sorting combo box, registers search
   * field and sorting listeners, and binds progress buttons.
   */
  @FXML
  public void initialize() {
    movieRepository = AppContext.getInstance().getMovieRepository();
    libraryScanService = AppContext.getInstance().getLibraryScanService();

    // Setup sort and filter options
    cmbSort.getItems().addAll("Cinematographer", "Composer", "Director",
        "Genre", "Longest Movie", "Producer", "Rating (Highest)",
        "Recently Added", "Release Year", "Shortest Movie", "Title (A-Z)",
        "Writer");

    cmbSort.setValue("Title (A-Z)");

    // Real-time filters and sorts
    txtSearch.textProperty().addListener(
        (observable, oldValue, newValue) -> filterAndDisplayMovies());
    cmbSort.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        AppContext.getInstance().getSettingsRepository()
            .set("library_sort_filter", newValue);
      }
      filterAndDisplayMovies();
    });

    btnSyncLibrary.disableProperty().bind(progressArea.visibleProperty());
    btnSyncLibrary.setCursor(
        io.github.kullik01.kinonode.util.CursorUtils.getCameraCursor());
    cmbSort.setCursor(
        io.github.kullik01.kinonode.util.CursorUtils.getCameraCursor());

    refresh();
  }

  /**
   * Refreshes the library list from the repository. Preserves the active movie
   * details view, if open, instead of resetting it.
   */
  @Override
  public void refresh() {
    if (currentMovieDetailController != null) {
      currentMovieDetailController.refresh();
      return; // Preserve the movie detail view when switching tabs
    }
    allMovies = movieRepository.findAll();
    filterAndDisplayMovies();
  }

  private void filterAndDisplayMovies() {
    String tmpQuery = txtSearch.getText().toLowerCase().trim();
    String tmpSortOption =
        cmbSort.getValue() != null ? cmbSort.getValue() : "Recently Added";

    List<Movie> tmpFiltered = allMovies.stream().filter(movie -> {
      if (tmpQuery.isEmpty())
        return true;

      return switch (tmpSortOption) {
        case "Title" -> movie.getTitle() != null
            && movie.getTitle().toLowerCase().contains(tmpQuery);
        case "Genre" -> movie.getGenre() != null
            && movie.getGenre().toLowerCase().contains(tmpQuery);
        case "Director" -> movie.getDirector() != null
            && movie.getDirector().toLowerCase().contains(tmpQuery);
        case "Writer" -> movie.getWriter() != null
            && movie.getWriter().toLowerCase().contains(tmpQuery);
        case "Producer" -> movie.getProducer() != null
            && movie.getProducer().toLowerCase().contains(tmpQuery);
        case "Cinematographer" -> movie.getCinematographer() != null
            && movie.getCinematographer().toLowerCase().contains(tmpQuery);
        case "Composer" -> movie.getComposer() != null
            && movie.getComposer().toLowerCase().contains(tmpQuery);
        default -> (movie.getTitle() != null
            && movie.getTitle().toLowerCase().contains(tmpQuery))
            || (movie.getGenre() != null
                && movie.getGenre().toLowerCase().contains(tmpQuery))
            || (movie.getDescription() != null
                && movie.getDescription().toLowerCase().contains(tmpQuery))
            || (movie.getDirector() != null
                && movie.getDirector().toLowerCase().contains(tmpQuery))
            || (movie.getWriter() != null
                && movie.getWriter().toLowerCase().contains(tmpQuery))
            || (movie.getProducer() != null
                && movie.getProducer().toLowerCase().contains(tmpQuery))
            || (movie.getCinematographer() != null
                && movie.getCinematographer().toLowerCase().contains(tmpQuery))
            || (movie.getComposer() != null
                && movie.getComposer().toLowerCase().contains(tmpQuery))
            || (movie.getActors() != null
                && movie.getActors().toLowerCase().contains(tmpQuery));
      };
    }).collect(Collectors.toList());

    // Sort movies according to selected sorting criteria with deterministic
    // tie-breakers
    if (tmpSortOption != null) {
      Comparator<String> strComp =
          Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
      Comparator<Integer> idComp =
          Comparator.nullsLast(Comparator.naturalOrder());

      switch (tmpSortOption) {
        case "Title (A-Z)" ->
          tmpFiltered.sort(Comparator.comparing(Movie::getTitle, strComp)
              .thenComparing(Movie::getId, idComp));

        case "Title (Z-A)" ->
          tmpFiltered.sort(Comparator.comparing(Movie::getTitle, strComp)
              .reversed().thenComparing(Movie::getId, idComp));

        case "Release Year" -> tmpFiltered.sort(Comparator
            .comparing(Movie::getYear,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Rating (Highest)" -> tmpFiltered.sort(Comparator
            .comparing(Movie::getRating,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Longest Movie" -> tmpFiltered.sort(Comparator
            .comparing(Movie::getDuration,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Shortest Movie" -> tmpFiltered.sort(Comparator
            .comparing(Movie::getDuration,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        default -> tmpFiltered.sort(Comparator
            .comparing(Movie::getDateAdded,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp).thenComparing(Movie::getId,
                Comparator.nullsLast(Comparator.reverseOrder())));
      }
    }

    List<Node> tmpNewCards = new ArrayList<>();
    for (Movie tmpMovie : tmpFiltered) {
      // Retrieve from cache first to avoid slow FXML loading cycles
      Parent tmpCard = cardCache.get(tmpMovie.getId());
      MovieCardController tmpController = controllerCache.get(tmpMovie.getId());
      if (tmpCard == null) {
        try {
          URL tmpUrl = getClass()
              .getResource("/io/github/kullik01/kinonode/fxml/MovieCard.fxml");
          FXMLLoader tmpLoader = new FXMLLoader(tmpUrl);
          tmpCard = tmpLoader.load();
          tmpController = tmpLoader.getController();

          cardCache.put(tmpMovie.getId(), tmpCard);
          controllerCache.put(tmpMovie.getId(), tmpController);
        } catch (IOException tmpException) {
          LOGGER.log(Level.SEVERE, "Failed to load MovieCard FXML",
              tmpException);
          continue;
        }
      }

      if (tmpController != null) {
        tmpController.setMovie(tmpMovie, this::openMovieDetail);
      }
      tmpNewCards.add(tmpCard);
    }

    if (tmpFiltered.isEmpty()) {
      gridPane.setAlignment(javafx.geometry.Pos.CENTER);
      gridPane.getChildren().setAll(createEmptyStateNode(allMovies.isEmpty()));
    } else {
      gridPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
      gridPane.getChildren().setAll(tmpNewCards);
    }
  }

  private Node createEmptyStateNode(boolean isLibraryEmpty) {
    javafx.scene.layout.VBox tmpRootVBox = new javafx.scene.layout.VBox();
    tmpRootVBox.setAlignment(javafx.geometry.Pos.CENTER);
    tmpRootVBox.setPadding(new javafx.geometry.Insets(30, 20, 30, 20));
    tmpRootVBox.prefHeightProperty()
        .bind(gridPane.heightProperty().subtract(30));
    tmpRootVBox.prefWidthProperty().bind(gridPane.widthProperty().subtract(30));
    tmpRootVBox.setMaxWidth(Double.MAX_VALUE);
    tmpRootVBox.setMaxHeight(Double.MAX_VALUE);
    tmpRootVBox.setFillWidth(true);

    javafx.scene.layout.VBox tmpCard = new javafx.scene.layout.VBox(20);
    tmpCard.setAlignment(javafx.geometry.Pos.CENTER);
    tmpCard.setMaxWidth(520);
    tmpCard.setPadding(new javafx.geometry.Insets(36, 40, 36, 40));
    tmpCard.setStyle("-fx-background-color: -color-bg-overlay; "
        + "-fx-background-radius: 16px; "
        + "-fx-border-color: -color-border-default; "
        + "-fx-border-radius: 16px; "
        + "-fx-effect: dropshadow(three-pass-box, rgba(33, 120, 221, 0.18), 24, 0.0, 0, 8);");

    if (isLibraryEmpty) {
      // Check if workspaces are already configured
      String tmpWorkspaces = AppContext.getInstance().getSettingsRepository()
          .get("workspace_folders", "");
      boolean tmpHasWorkspaces =
          tmpWorkspaces != null && !tmpWorkspaces.isBlank();

      if (tmpHasWorkspaces) {
        // Workspaces configured but library not yet synced
        Label tmpBadge = new Label("LIBRARY SYNC REQUIRED");
        tmpBadge.setStyle("-fx-background-color: rgba(237, 139, 0, 0.15); "
            + "-fx-text-fill: #ED8B00; " + "-fx-font-weight: bold; "
            + "-fx-font-size: 11px; " + "-fx-padding: 4px 14px; "
            + "-fx-background-radius: 12px;");

        SVGPath tmpSyncIcon = new SVGPath();
        tmpSyncIcon.setContent(
            "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0020 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 004 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z");
        tmpSyncIcon.setScaleX(2.4);
        tmpSyncIcon.setScaleY(2.4);
        tmpSyncIcon.setStyle(
            "-fx-fill: #ED8B00; -fx-effect: dropshadow(three-pass-box, rgba(237, 139, 0, 0.35), 10, 0, 0, 2);");
        javafx.scene.layout.VBox tmpSyncIconWrapper =
            new javafx.scene.layout.VBox(tmpSyncIcon);
        tmpSyncIconWrapper.setAlignment(javafx.geometry.Pos.CENTER);
        tmpSyncIconWrapper.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        Label tmpTitle = new Label("Sync Your Library");
        tmpTitle.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

        Label tmpText = new Label(
            "Your workspace is configured. Press the Sync button to scan your folder(s) and populate your movie library.");
        tmpText.setWrapText(true);
        tmpText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        tmpText.setMaxWidth(420);
        tmpText
            .setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");

        javafx.scene.layout.HBox tmpBtnBox = new javafx.scene.layout.HBox(14);
        tmpBtnBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button tmpBtnSync = new Button("Sync Library");
        tmpBtnSync.getStyleClass().addAll("button-success");
        tmpBtnSync.setStyle(
            "-fx-padding: 8px 18px; -fx-font-size: 14px; -fx-font-weight: bold;");
        tmpBtnSync.setCursor(
            io.github.kullik01.kinonode.util.CursorUtils.getAppLogoCursor());

        SVGPath tmpSyncBtnIcon = new SVGPath();
        tmpSyncBtnIcon.setContent(
            "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0020 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 004 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z");
        tmpSyncBtnIcon.setScaleX(0.85);
        tmpSyncBtnIcon.setScaleY(0.85);
        tmpSyncBtnIcon.setStyle("-fx-fill: white;");
        tmpBtnSync.setGraphic(tmpSyncBtnIcon);

        Tooltip tmpSyncTooltip = new Tooltip("Why sync?\n"
            + "Syncing scans your configured workspace folder(s) to discover, index, and catalog your movie files, posters, and metadata into your collection library.");
        tmpSyncTooltip.setShowDelay(Duration.millis(250));
        tmpSyncTooltip.getStyleClass().add("chart-popup-tooltip");
        Tooltip.install(tmpBtnSync, tmpSyncTooltip);

        tmpBtnSync.setOnAction(e -> syncLibrary());

        tmpBtnBox.getChildren().add(tmpBtnSync);
        tmpCard.getChildren().addAll(tmpBadge, tmpSyncIconWrapper, tmpTitle,
            tmpText, tmpBtnBox);
      } else {
        // No workspaces configured yet
        Label tmpBadge = new Label("WORKSPACE CONFIGURATION");
        tmpBadge.setStyle("-fx-background-color: rgba(33, 120, 221, 0.15); "
            + "-fx-text-fill: #2178dd; " + "-fx-font-weight: bold; "
            + "-fx-font-size: 11px; " + "-fx-padding: 4px 14px; "
            + "-fx-background-radius: 12px;");

        SVGPath tmpCameraIcon = new SVGPath();
        tmpCameraIcon.setContent(
            "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z");
        tmpCameraIcon.setScaleX(2.4);
        tmpCameraIcon.setScaleY(2.4);
        tmpCameraIcon.setStyle(
            "-fx-fill: #ED8B00; -fx-effect: dropshadow(three-pass-box, rgba(237, 139, 0, 0.35), 10, 0, 0, 2);");
        javafx.scene.layout.VBox tmpCameraIconWrapper =
            new javafx.scene.layout.VBox(tmpCameraIcon);
        tmpCameraIconWrapper.setAlignment(javafx.geometry.Pos.CENTER);
        tmpCameraIconWrapper
            .setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        Label tmpTitle = new Label("Start Your Movie Library");
        tmpTitle.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

        Label tmpText = new Label(
            "Please configure your workspace folder in Settings to scan, index, and organize your films.");
        tmpText.setWrapText(true);
        tmpText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        tmpText.setMaxWidth(420);
        tmpText
            .setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");

        javafx.scene.layout.HBox tmpBtnBox = new javafx.scene.layout.HBox(14);
        tmpBtnBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button tmpBtnSettings = new Button("Configure Workspaces");
        tmpBtnSettings.getStyleClass().addAll("button-success");
        tmpBtnSettings.setStyle(
            "-fx-padding: 8px 18px; -fx-font-size: 14px; -fx-font-weight: bold;");
        tmpBtnSettings.setCursor(
            io.github.kullik01.kinonode.util.CursorUtils.getAppLogoCursor());

        SVGPath tmpGear = new SVGPath();
        tmpGear.setContent(
            "M12 15.5A3.5 3.5 0 0 1 8.5 12 3.5 3.5 0 0 1 12 8.5a3.5 3.5 0 0 1 3.5 3.5 3.5 3.5 0 0 1-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97 0-.33-.03-.66-.07-1l2.11-1.63c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.37-2.65A.506.506 0 0 0 14 2h-4c-.25 0-.46.18-.5.42l-.37 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64L4.57 11c-.04.34-.07.67-.07 1 0 .33.03.65.07.97l-2.11 1.66c-.19.15-.25.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1.01c.52.4 1.06.74 1.69.99l.37 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.37-2.65c.63-.26 1.17-.59 1.69-.99l2.49 1.01c.22.08.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.66Z");
        tmpGear.setScaleX(0.85);
        tmpGear.setScaleY(0.85);
        tmpGear.setStyle("-fx-fill: white;");
        tmpBtnSettings.setGraphic(tmpGear);

        Tooltip tmpTooltip = new Tooltip("Why set a workspace?\n"
            + "KinoNode needs at least one workspace folder path to scan, locate, and catalog your movie files, posters, and metadata into your collection library.");
        tmpTooltip.setShowDelay(Duration.millis(250));
        tmpTooltip.getStyleClass().add("chart-popup-tooltip");
        Tooltip.install(tmpBtnSettings, tmpTooltip);

        tmpBtnSettings.setOnAction(e -> {
          MainController tmpMain = AppContext.getInstance().getMainController();
          if (tmpMain != null) {
            tmpMain.showSettings();
          }
        });

        tmpBtnBox.getChildren().add(tmpBtnSettings);
        tmpCard.getChildren().addAll(tmpBadge, tmpCameraIconWrapper, tmpTitle,
            tmpText, tmpBtnBox);
      }
    } else {
      SVGPath tmpSearchIcon = new SVGPath();
      tmpSearchIcon.setContent(
          "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
      tmpSearchIcon.setScaleX(2.0);
      tmpSearchIcon.setScaleY(2.0);
      tmpSearchIcon.setStyle("-fx-fill: -color-fg-muted;");

      Label tmpTitle = new Label("No Matching Movies Found");
      tmpTitle.setStyle(
          "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
      Label tmpSub = new Label(
          "Try clearing your search query or choosing a different filter.");
      tmpSub.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");
      tmpCard.getChildren().addAll(tmpSearchIcon, tmpTitle, tmpSub);
    }

    tmpRootVBox.getChildren().add(tmpCard);
    return tmpRootVBox;
  }

  private void openMovieDetail(Movie movie) {
    try {
      URL tmpUrl = getClass().getResource(
          "/io/github/kullik01/kinonode/fxml/MovieDetailView.fxml");
      FXMLLoader tmpLoader = new FXMLLoader(tmpUrl);
      Parent tmpDetailView = tmpLoader.load();

      MovieDetailController tmpController = tmpLoader.getController();
      currentMovieDetailController = tmpController;
      tmpController.setMovie(movie);

      // Replaces library root with the detail view dynamically in StackPane
      StackPane tmpRootPane = (StackPane) libraryRoot.getParent();
      if (tmpRootPane != null) {
        tmpController.setOnBack(() -> {
          currentMovieDetailController = null;
          tmpRootPane.getChildren().setAll(libraryRoot);
          refresh();
        });
        tmpRootPane.getChildren().setAll(tmpDetailView);
      }
    } catch (IOException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to load MovieDetailView FXML",
          tmpException);
    }
  }

  @FXML
  private void syncLibrary() {
    if (activeScanTask != null && activeScanTask.isRunning()) {
      return;
    }

    activeScanTask = libraryScanService.createScanTask();

    // Bind UI progress elements to the background task
    progressBar.progressProperty().bind(activeScanTask.progressProperty());
    progressArea.setVisible(true);

    activeScanTask.setOnSucceeded(event -> {
      LibraryScanService.ScanResult tmpResult = activeScanTask.getValue();
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
      refresh();
      AppContext.getInstance().notifyLibraryChanged();

      CustomAlert.showInformation(gridPane.getScene().getWindow(),
          "Sync Completed", "Sync Completed Successfully",
          "Total files scanned: " + tmpResult.totalScanned()
              + "\nNew movies added: " + tmpResult.newAdded());
    });

    activeScanTask.setOnFailed(event -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
      Throwable tmpException = activeScanTask.getException();
      CustomAlert.showInformation(gridPane.getScene().getWindow(),
          "Sync Failed", "An error occurred during search",
          tmpException != null ? tmpException.getMessage()
              : "Unknown database or file error");
    });

    activeScanTask.setOnCancelled(event -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
    });

    // Run sync task in a daemon thread so it doesn't block JavaFX application
    // exit
    Thread tmpThread = new Thread(activeScanTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  @FXML
  private void cancelSync() {
    if (activeScanTask != null) {
      activeScanTask.cancel();
    }
  }
}
