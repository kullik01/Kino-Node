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
  private final java.util.Map<Integer, Parent> cardCache =
      new java.util.HashMap<>();
  private final java.util.Map<Integer, MovieCardController> controllerCache =
      new java.util.HashMap<>();

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
        "Genre", "Producer", "Rating (Highest)", "Recently Added",
        "Release Year", "Title", "Title (A-Z)", "Title (Z-A)", "Writer");

    String tmpSavedSortFilter = AppContext.getInstance().getSettingsRepository()
        .get("library_sort_filter", "Recently Added");
    cmbSort.setValue(tmpSavedSortFilter);

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

    refresh();
  }

  /**
   * Refreshes the library list from the repository. Preserves the active movie
   * details view, if open, instead of resetting it.
   */
  @Override
  public void refresh() {
    if (currentMovieDetailController != null) {
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

    // Sort movies according to selected sorting criteria
    if (tmpSortOption != null) {
      switch (tmpSortOption) {
        case "Title (A-Z)" ->
          tmpFiltered.sort(Comparator.comparing(Movie::getTitle,
              Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        case "Title (Z-A)" -> tmpFiltered.sort(Comparator
            .comparing(Movie::getTitle,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .reversed());
        case "Release Year" -> tmpFiltered.sort((movie1, movie2) -> {
          // Safe sorting of year that places null-valued release years at the
          // bottom
          if (movie1.getYear() == null && movie2.getYear() == null)
            return 0;
          if (movie1.getYear() == null)
            return 1;
          if (movie2.getYear() == null)
            return -1;
          return Integer.compare(movie1.getYear(), movie2.getYear());
        });
        case "Rating (Highest)" -> tmpFiltered.sort((movie1, movie2) -> {
          // Safe sorting of ratings in descending order, putting nulls at the
          // end
          if (movie1.getRating() == null && movie2.getRating() == null)
            return 0;
          if (movie1.getRating() == null)
            return 1;
          if (movie2.getRating() == null)
            return -1;
          return Integer.compare(movie2.getRating(), movie1.getRating()); // Descending
        });
        default -> tmpFiltered
            .sort(Comparator
                .comparing(Movie::getDateAdded,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()); // Default sort for Filters and Recently Added
      }
    }

    java.util.List<javafx.scene.Node> tmpNewCards = new java.util.ArrayList<>();
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
    gridPane.getChildren().setAll(tmpNewCards);
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
