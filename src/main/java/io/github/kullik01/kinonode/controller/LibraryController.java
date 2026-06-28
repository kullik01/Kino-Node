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
 * Controller for the library view of the application. Manages displaying, filtering, searching, and
 * sorting the movies in the user's library. Implements {@link Refreshable} to allow refreshing the
 * movie catalog display. Caches FXML cards and controllers to optimize rendering performance when
 * filtering/sorting.
 */
public class LibraryController implements Refreshable {
  private static final Logger LOGGER = Logger.getLogger(LibraryController.class.getName());

  @FXML private VBox libraryRoot;
  @FXML private TextField txtSearch;
  @FXML private ComboBox<String> cmbSort;
  @FXML private FlowPane gridPane;
  @FXML private Button btnSyncLibrary;

  @FXML private HBox progressArea;
  @FXML private ProgressBar progressBar;

  private MovieRepository movieRepository;
  private LibraryScanService libraryScanService;
  private List<Movie> allMovies = new ArrayList<>();
  private Task<LibraryScanService.ScanResult> activeScanTask;
  private MovieDetailController currentMovieDetailController;
  private final java.util.Map<Integer, Parent> cardCache = new java.util.HashMap<>();
  private final java.util.Map<Integer, MovieCardController> controllerCache =
      new java.util.HashMap<>();

  /**
   * Initializes the controller. Sets up the sorting combo box, registers search field and sorting
   * listeners, and binds progress buttons.
   */
  @FXML
  public void initialize() {
    movieRepository = AppContext.getInstance().getMovieRepository();
    libraryScanService = AppContext.getInstance().getLibraryScanService();

    // Setup sort and filter options
    cmbSort
        .getItems()
        .addAll(
            "Cinematographer",
            "Composer",
            "Director",
            "Genre",
            "Producer",
            "Rating (Highest)",
            "Recently Added",
            "Release Year",
            "Title",
            "Title (A-Z)",
            "Title (Z-A)",
            "Writer");

    String savedSortFilter =
        AppContext.getInstance()
            .getSettingsRepository()
            .get("library_sort_filter", "Recently Added");
    cmbSort.setValue(savedSortFilter);

    // Real-time filters and sorts
    txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndDisplayMovies());
    cmbSort
        .valueProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                AppContext.getInstance().getSettingsRepository().set("library_sort_filter", newVal);
              }
              filterAndDisplayMovies();
            });

    btnSyncLibrary.disableProperty().bind(progressArea.visibleProperty());

    refresh();
  }

  /**
   * Refreshes the library list from the repository. Preserves the active movie details view, if
   * open, instead of resetting it.
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
    String query = txtSearch.getText().toLowerCase().trim();
    String sortOption = cmbSort.getValue() != null ? cmbSort.getValue() : "Recently Added";

    List<Movie> filtered =
        allMovies.stream()
            .filter(
                m -> {
                  if (query.isEmpty()) return true;

                  return switch (sortOption) {
                    case "Title" ->
                        m.getTitle() != null && m.getTitle().toLowerCase().contains(query);
                    case "Genre" ->
                        m.getGenre() != null && m.getGenre().toLowerCase().contains(query);
                    case "Director" ->
                        m.getDirector() != null && m.getDirector().toLowerCase().contains(query);
                    case "Writer" ->
                        m.getWriter() != null && m.getWriter().toLowerCase().contains(query);
                    case "Producer" ->
                        m.getProducer() != null && m.getProducer().toLowerCase().contains(query);
                    case "Cinematographer" ->
                        m.getCinematographer() != null
                            && m.getCinematographer().toLowerCase().contains(query);
                    case "Composer" ->
                        m.getComposer() != null && m.getComposer().toLowerCase().contains(query);
                    default ->
                        (m.getTitle() != null && m.getTitle().toLowerCase().contains(query))
                            || (m.getGenre() != null && m.getGenre().toLowerCase().contains(query))
                            || (m.getDescription() != null
                                && m.getDescription().toLowerCase().contains(query))
                            || (m.getDirector() != null
                                && m.getDirector().toLowerCase().contains(query))
                            || (m.getWriter() != null
                                && m.getWriter().toLowerCase().contains(query))
                            || (m.getProducer() != null
                                && m.getProducer().toLowerCase().contains(query))
                            || (m.getCinematographer() != null
                                && m.getCinematographer().toLowerCase().contains(query))
                            || (m.getComposer() != null
                                && m.getComposer().toLowerCase().contains(query));
                  };
                })
            .collect(Collectors.toList());

    // Sort movies according to selected sorting criteria
    if (sortOption != null) {
      switch (sortOption) {
        case "Title (A-Z)" ->
            filtered.sort(
                Comparator.comparing(
                    Movie::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        case "Title (Z-A)" ->
            filtered.sort(
                Comparator.comparing(
                        Movie::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .reversed());
        case "Release Year" ->
            filtered.sort(
                (m1, m2) -> {
                  // Safe sorting of year that places null-valued release years at the bottom
                  if (m1.getYear() == null && m2.getYear() == null) return 0;
                  if (m1.getYear() == null) return 1;
                  if (m2.getYear() == null) return -1;
                  return Integer.compare(m1.getYear(), m2.getYear());
                });
        case "Rating (Highest)" ->
            filtered.sort(
                (m1, m2) -> {
                  // Safe sorting of ratings in descending order, putting nulls at the end
                  if (m1.getRating() == null && m2.getRating() == null) return 0;
                  if (m1.getRating() == null) return 1;
                  if (m2.getRating() == null) return -1;
                  return Integer.compare(m2.getRating(), m1.getRating()); // Descending
                });
        default ->
            filtered.sort(
                Comparator.comparing(
                        Movie::getDateAdded, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed()); // Default sort for Filters and Recently Added
      }
    }

    java.util.List<javafx.scene.Node> newCards = new java.util.ArrayList<>();
    for (Movie m : filtered) {
      // Retrieve from cache first to avoid slow FXML loading cycles
      Parent card = cardCache.get(m.getId());
      MovieCardController controller = controllerCache.get(m.getId());
      if (card == null) {
        try {
          URL url = getClass().getResource("/io/github/kullik01/kinonode/fxml/MovieCard.fxml");
          FXMLLoader loader = new FXMLLoader(url);
          card = loader.load();
          controller = loader.getController();

          cardCache.put(m.getId(), card);
          controllerCache.put(m.getId(), controller);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to load MovieCard FXML", e);
          continue;
        }
      }

      if (controller != null) {
        controller.setMovie(m, this::openMovieDetail);
      }
      newCards.add(card);
    }
    gridPane.getChildren().setAll(newCards);
  }

  private void openMovieDetail(Movie movie) {
    try {
      URL url = getClass().getResource("/io/github/kullik01/kinonode/fxml/MovieDetailView.fxml");
      FXMLLoader loader = new FXMLLoader(url);
      Parent detailView = loader.load();

      MovieDetailController controller = loader.getController();
      currentMovieDetailController = controller;
      controller.setMovie(movie);

      // Replaces library root with the detail view dynamically in StackPane
      StackPane rootPane = (StackPane) libraryRoot.getParent();
      if (rootPane != null) {
        controller.setOnBack(
            () -> {
              currentMovieDetailController = null;
              rootPane.getChildren().setAll(libraryRoot);
              refresh();
            });
        rootPane.getChildren().setAll(detailView);
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to load MovieDetailView FXML", e);
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

    activeScanTask.setOnSucceeded(
        event -> {
          LibraryScanService.ScanResult result = activeScanTask.getValue();
          progressBar.progressProperty().unbind();
          progressBar.setProgress(0);
          progressArea.setVisible(false);
          refresh();

          CustomAlert.showInformation(
              gridPane.getScene().getWindow(),
              "Sync Completed",
              "Sync Completed Successfully",
              "Total files scanned: "
                  + result.totalScanned()
                  + "\nNew movies added: "
                  + result.newAdded());
        });

    activeScanTask.setOnFailed(
        event -> {
          progressBar.progressProperty().unbind();
          progressBar.setProgress(0);
          progressArea.setVisible(false);
          Throwable e = activeScanTask.getException();
          CustomAlert.showInformation(
              gridPane.getScene().getWindow(),
              "Sync Failed",
              "An error occurred during search",
              e != null ? e.getMessage() : "Unknown database or file error");
        });

    activeScanTask.setOnCancelled(
        event -> {
          progressBar.progressProperty().unbind();
          progressBar.setProgress(0);
          progressArea.setVisible(false);
        });

    // Run sync task in a daemon thread so it doesn't block JavaFX application exit
    Thread th = new Thread(activeScanTask);
    th.setDaemon(true);
    th.start();
  }

  @FXML
  private void cancelSync() {
    if (activeScanTask != null) {
      activeScanTask.cancel();
    }
  }
}
