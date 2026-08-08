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

import io.github.kullik01.kinonode.uiutil.CursorUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.di.AppContext;
import io.github.kullik01.kinonode.service.LibraryScanService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.github.kullik01.kinonode.uiutil.CustomAlert;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import io.github.kullik01.kinonode.service.ImdbService;
import io.github.kullik01.kinonode.view.pages.DisambiguationView;
import io.github.kullik01.kinonode.view.pages.SyncOverviewView;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.VBox;

/**
 * Controller for the library view of the application. Manages displaying,
 * filtering, searching, and sorting the movies in the user's library.
 * Implements {@link Refreshable} to allow refreshing the movie catalog display.
 * Caches view cards and controllers to optimize rendering performance when
 * filtering/sorting.
 */
public class LibraryController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(LibraryController.class.getName());

  private static final double MIN_CARD_WIDTH = 200.0;
  private static final double CARD_GAP = 15.0;
  private static final double GRID_PADDING = 10.0;

  private VBox libraryRoot;
  private TextField txtSearch;
  private ComboBox<String> cmbSort;
  private FlowPane gridPane;
  private Button btnSyncLibrary;
  private HBox progressArea;
  private ProgressBar progressBar;

  private final MovieRepository movieRepository;
  private final LibraryScanService libraryScanService;
  private final SettingsRepository settingsRepository;
  private final AppContext appContext;
  private final Provider<MovieDetailController> movieDetailControllerProvider;
  private final ResourceBundle resources;
  private final ImdbService imdbService;

  @Inject
  public LibraryController(MovieRepository movieRepository,
      LibraryScanService libraryScanService,
      SettingsRepository settingsRepository, AppContext appContext,
      Provider<MovieDetailController> movieDetailControllerProvider,
      ResourceBundle resources, ImdbService imdbService) {
    this.movieRepository = movieRepository;
    this.libraryScanService = libraryScanService;
    this.settingsRepository = settingsRepository;
    this.appContext = appContext;
    this.movieDetailControllerProvider = movieDetailControllerProvider;
    this.resources = resources;
    this.imdbService = imdbService;
  }

  private List<Movie> allMovies = new ArrayList<>();
  private Task<LibraryScanService.ScanResult> activeScanTask;
  private MovieDetailController currentMovieDetailController;
  private final Map<Integer, Parent> cardCache = new HashMap<>();
  private final Map<Integer, MovieCardController> controllerCache =
      new HashMap<>();

  public Parent buildView() {
    StackPane rootPane = new StackPane();
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    libraryRoot = new VBox();
    libraryRoot.setId("libraryRoot");
    libraryRoot.setSpacing(15.0);
    libraryRoot.setPadding(new Insets(20.0, 20.0, 20.0, 20.0));

    HBox toolbar = new HBox();
    toolbar.setAlignment(Pos.CENTER_LEFT);
    toolbar.setSpacing(15.0);

    Label titleLbl = new Label(resources.getString("library.title"));
    titleLbl.setStyle(
        "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2178dd;");
    titleLbl.setMinWidth(Control.USE_PREF_SIZE);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    txtSearch = new TextField();
    txtSearch.setId("txtSearch");
    txtSearch.setPromptText(resources.getString("library.search.prompt"));
    txtSearch.setPrefWidth(250.0);

    cmbSort = new ComboBox<>();
    cmbSort.setId("cmbSort");
    cmbSort.setPrefWidth(200.0);
    cmbSort.getStyleClass().add("button-secondary");

    btnSyncLibrary = new Button();
    btnSyncLibrary.setId("btnSyncLibrary");
    btnSyncLibrary.getStyleClass().addAll("button-success", "button-icon");
    btnSyncLibrary.setMinWidth(36.0);
    btnSyncLibrary.setMaxWidth(36.0);
    btnSyncLibrary.setMinHeight(36.0);
    btnSyncLibrary.setOnAction(e -> syncLibrary());

    SVGPath syncGraphic = new SVGPath();
    syncGraphic.setContent(
        "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0020 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 004 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z");
    syncGraphic.setFill(Color.WHITE);
    btnSyncLibrary.setGraphic(syncGraphic);

    Tooltip syncTooltip =
        new Tooltip(resources.getString("library.empty.sync_btn"));
    syncTooltip.setShowDelay(Duration.millis(250));
    btnSyncLibrary.setTooltip(syncTooltip);

    Button btnAddMovie = new Button();
    btnAddMovie.setId("btnAddMovie");
    btnAddMovie.getStyleClass().addAll("button-secondary", "button-icon");
    btnAddMovie.setMinWidth(36.0);
    btnAddMovie.setMaxWidth(36.0);
    btnAddMovie.setMinHeight(36.0);
    btnAddMovie.setOnAction(e -> showAddMovieDialog());

    SVGPath addGraphic = new SVGPath();
    addGraphic.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
    addGraphic.setFill(Color.WHITE);
    btnAddMovie.setGraphic(addGraphic);

    Tooltip addTooltip =
        new Tooltip(resources.getString("library.add.movie.title"));
    addTooltip.setShowDelay(Duration.millis(250));
    btnAddMovie.setTooltip(addTooltip);

    progressArea = new HBox();
    progressArea.setId("progressArea");
    progressArea.setAlignment(Pos.CENTER_LEFT);
    progressArea.setSpacing(10.0);
    progressArea.setVisible(false);
    progressArea.managedProperty().bind(progressArea.visibleProperty());

    progressBar = new ProgressBar();
    progressBar.setId("progressBar");
    progressBar.setPrefWidth(250.0);

    Button btnCancelSync = new Button();
    btnCancelSync.getStyleClass().addAll("button-secondary", "button-icon");
    btnCancelSync.setMinWidth(36.0);
    btnCancelSync.setMaxWidth(36.0);
    btnCancelSync.setMinHeight(36.0);
    btnCancelSync.setOnAction(e -> cancelSync());

    SVGPath cancelGraphic = new SVGPath();
    cancelGraphic.setContent(
        "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z");
    cancelGraphic.setFill(Color.WHITE);
    btnCancelSync.setGraphic(cancelGraphic);

    Tooltip cancelTooltip = new Tooltip(resources.getString("button.cancel"));
    cancelTooltip.setShowDelay(Duration.millis(250));
    btnCancelSync.setTooltip(cancelTooltip);

    progressArea.getChildren().addAll(progressBar, btnCancelSync);
    toolbar.getChildren().addAll(titleLbl, spacer, txtSearch, cmbSort,
        btnAddMovie, btnSyncLibrary, progressArea);

    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setFitToWidth(true);
    scrollPane.setFitToHeight(true);
    scrollPane.getStyleClass().add("movie-grid-scroll");
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    gridPane = new FlowPane();
    gridPane.setId("gridPane");
    gridPane.getStyleClass().add("movie-grid");
    scrollPane.setContent(gridPane);

    scrollPane.widthProperty().addListener((tmpObs, tmpOld, tmpNew) -> {
      recalculateCardSizes(tmpNew.doubleValue());
    });

    libraryRoot.getChildren().addAll(toolbar, scrollPane);
    rootPane.getChildren().add(libraryRoot);

    return rootPane;
  }

  /**
   * Enforces consistent card sizes across all window GUI sizes, maintaining the
   * optimal default view dimensions regardless of full view / window resize.
   *
   * @param availableWidth the available width from the scroll pane
   */
  private void recalculateCardSizes(double availableWidth) {
    double tmpCardWidth = MovieCardController.STANDARD_CARD_WIDTH;

    for (Node tmpChild : gridPane.getChildren()) {
      if (tmpChild instanceof VBox) {
        ((Region) tmpChild).setPrefWidth(tmpCardWidth);
        ((Region) tmpChild).setMinWidth(tmpCardWidth);
        ((Region) tmpChild).setMaxWidth(tmpCardWidth);
      }
    }

    // Update card controllers with standard card width
    for (MovieCardController tmpController : controllerCache.values()) {
      tmpController.updateCardWidth(tmpCardWidth);
    }
  }

  /**
   * Initializes the controller. Sets up the sorting combo box, registers search
   * field and sorting listeners, and binds progress buttons.
   */
  public void initialize() {

    // Setup sort and filter options
    cmbSort.getItems().addAll(
        resources.getString("library.sort.cinematographer"),
        resources.getString("library.sort.composer"),
        resources.getString("library.sort.director"),
        resources.getString("library.sort.genre"),
        resources.getString("library.sort.duration"),
        resources.getString("library.sort.producer"),
        resources.getString("library.sort.rating"),
        resources.getString("library.sort.recently_added"),
        resources.getString("library.sort.year"),
        resources.getString("library.sort.shortest"),
        resources.getString("library.sort.title"),
        resources.getString("library.sort.writer"));

    cmbSort.setValue(resources.getString("library.sort.title"));

    // Real-time filters and sorts
    txtSearch.textProperty().addListener(
        (observable, oldValue, newValue) -> filterAndDisplayMovies());
    cmbSort.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        settingsRepository.set("library_sort_filter", newValue);
      }
      filterAndDisplayMovies();
    });

    btnSyncLibrary.disableProperty().bind(progressArea.visibleProperty());
    CursorUtils.applyCursor(btnSyncLibrary);
    CursorUtils.applyCursor(cmbSort);

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

  /**
   * Filters and sorts the movie library based on the current search text and
   * sort option. Caches view cards and controllers for efficient UI updates and
   * handles displaying either the filtered movie cards or an empty state view
   * if no movies match.
   */
  private void filterAndDisplayMovies() {
    String query = txtSearch.getText().toLowerCase().trim();
    String sortOption = cmbSort.getValue() != null ? cmbSort.getValue()
        : resources.getString("library.sort.recently_added");

    List<Movie> filteredMovies = allMovies.stream().filter(movie -> {
      if (query.isEmpty())
        return true;

      return switch (sortOption) {
        case "Title" -> movie.getTitle() != null
            && movie.getTitle().toLowerCase().contains(query);
        case "Genre" -> movie.getGenre() != null
            && movie.getGenre().toLowerCase().contains(query);
        case "Director" -> movie.getDirector() != null
            && movie.getDirector().toLowerCase().contains(query);
        case "Writer" -> movie.getWriter() != null
            && movie.getWriter().toLowerCase().contains(query);
        case "Producer" -> movie.getProducer() != null
            && movie.getProducer().toLowerCase().contains(query);
        case "Cinematographer" -> movie.getCinematographer() != null
            && movie.getCinematographer().toLowerCase().contains(query);
        case "Composer" -> movie.getComposer() != null
            && movie.getComposer().toLowerCase().contains(query);
        default -> (movie.getTitle() != null
            && movie.getTitle().toLowerCase().contains(query))
            || (movie.getGenre() != null
                && movie.getGenre().toLowerCase().contains(query))
            || (movie.getDescription() != null
                && movie.getDescription().toLowerCase().contains(query))
            || (movie.getDirector() != null
                && movie.getDirector().toLowerCase().contains(query))
            || (movie.getWriter() != null
                && movie.getWriter().toLowerCase().contains(query))
            || (movie.getProducer() != null
                && movie.getProducer().toLowerCase().contains(query))
            || (movie.getCinematographer() != null
                && movie.getCinematographer().toLowerCase().contains(query))
            || (movie.getComposer() != null
                && movie.getComposer().toLowerCase().contains(query))
            || (movie.getActors() != null
                && movie.getActors().toLowerCase().contains(query));
      };
    }).collect(Collectors.toList());

    // Sort movies according to selected sorting criteria with deterministic
    // tie-breakers
    if (sortOption != null) {
      Comparator<String> strComp =
          Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
      Comparator<Integer> idComp =
          Comparator.nullsLast(Comparator.naturalOrder());

      switch (sortOption) {
        case "Title (A-Z)" ->
          filteredMovies.sort(Comparator.comparing(Movie::getTitle, strComp)
              .thenComparing(Movie::getId, idComp));

        case "Title (Z-A)" ->
          filteredMovies.sort(Comparator.comparing(Movie::getTitle, strComp)
              .reversed().thenComparing(Movie::getId, idComp));

        case "Release Year" -> filteredMovies.sort(Comparator
            .comparing(Movie::getYear,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Rating (Highest)" -> filteredMovies.sort(Comparator
            .comparing(Movie::getRating,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Longest Movie" -> filteredMovies.sort(Comparator
            .comparing(Movie::getDuration,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        case "Shortest Movie" -> filteredMovies.sort(Comparator
            .comparing(Movie::getDuration,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Movie::getTitle, strComp)
            .thenComparing(Movie::getId, idComp));

        default -> filteredMovies.sort(Comparator
            .comparing(Movie::getDateAdded,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, strComp).thenComparing(Movie::getId,
                Comparator.nullsLast(Comparator.reverseOrder())));
      }
    }

    List<Node> newCards = new ArrayList<>();
    for (Movie movieItem : filteredMovies) {
      // Retrieve from cache first to avoid slow loading cycles
      Parent card = cardCache.get(movieItem.getId());
      MovieCardController controller = controllerCache.get(movieItem.getId());
      if (card == null) {
        try {
          controller = new MovieCardController();
          card = controller.buildView();

          cardCache.put(movieItem.getId(), card);
          controllerCache.put(movieItem.getId(), controller);
        } catch (Exception exception) {
          LOGGER.log(Level.SEVERE, "Failed to create MovieCard", exception);
          continue;
        }
      }

      if (controller != null) {
        controller.setMovie(movieItem, this::openMovieDetail);
      }
      newCards.add(card);
    }

    if (filteredMovies.isEmpty()) {
      gridPane.setAlignment(Pos.CENTER);
      gridPane.getChildren().setAll(createEmptyStateNode(allMovies.isEmpty()));
    } else {
      gridPane.setAlignment(Pos.TOP_LEFT);
      gridPane.getChildren().setAll(newCards);
    }

    // Ensure cards are sized correctly for current width
    if (gridPane.getParent() instanceof ScrollPane scroll) {
      recalculateCardSizes(scroll.getWidth());
    }
  }

  /**
   * Creates an empty state node to display when the library is empty or no
   * movies match the current search filters. Provides actionable buttons to
   * sync the library or configure workspaces if none are set.
   *
   * @param isLibraryEmpty true if the entire library is empty (no movies),
   *        false if movies exist but don't match the search.
   * @return A styled VBox node containing the empty state message and actions.
   */
  private Node createEmptyStateNode(boolean isLibraryEmpty) {
    VBox rootVerticalBox = new VBox();
    rootVerticalBox.setAlignment(Pos.CENTER);
    rootVerticalBox.setPadding(new Insets(30, 20, 30, 20));
    rootVerticalBox.prefHeightProperty()
        .bind(gridPane.heightProperty().subtract(30));
    rootVerticalBox.prefWidthProperty()
        .bind(gridPane.widthProperty().subtract(30));
    rootVerticalBox.setMaxWidth(Double.MAX_VALUE);
    rootVerticalBox.setMaxHeight(Double.MAX_VALUE);
    rootVerticalBox.setFillWidth(true);

    VBox card = new VBox(20);
    card.setAlignment(Pos.CENTER);
    card.setMaxWidth(520);
    card.setPadding(new Insets(36, 40, 36, 40));
    card.setStyle("-fx-background-color: -color-bg-overlay; "
        + "-fx-background-radius: 16px; "
        + "-fx-border-color: -color-border-default; "
        + "-fx-border-radius: 16px; "
        + "-fx-effect: dropshadow(three-pass-box, rgba(33, 120, 221, 0.18), 24, 0.0, 0, 8);");

    if (isLibraryEmpty) {
      // Check if workspaces are already configured
      String tmpWorkspaces = settingsRepository.get("workspace_folders", "");
      boolean tmpHasWorkspaces =
          tmpWorkspaces != null && !tmpWorkspaces.isBlank();

      if (tmpHasWorkspaces) {
        // Workspaces configured but library not yet synced
        Label tmpBadge =
            new Label(resources.getString("library.empty.sync_required"));
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
        VBox tmpSyncIconWrapper = new VBox(tmpSyncIcon);
        tmpSyncIconWrapper.setAlignment(Pos.CENTER);
        tmpSyncIconWrapper.setPadding(new Insets(10, 0, 10, 0));

        Label tmpTitle =
            new Label(resources.getString("library.empty.sync_title"));
        tmpTitle.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

        Label tmpText =
            new Label(resources.getString("library.empty.sync_text"));
        tmpText.setWrapText(true);
        tmpText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        tmpText.setMaxWidth(420);
        tmpText
            .setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");

        HBox tmpBtnBox = new HBox(14);
        tmpBtnBox.setAlignment(Pos.CENTER);

        Button tmpBtnSync =
            new Button(resources.getString("library.empty.sync_btn"));
        tmpBtnSync.getStyleClass().addAll("button-success");
        tmpBtnSync.setStyle(
            "-fx-padding: 8px 18px; -fx-font-size: 14px; -fx-font-weight: bold;");
        CursorUtils.applyCursor(tmpBtnSync);

        SVGPath tmpSyncBtnIcon = new SVGPath();
        tmpSyncBtnIcon.setContent(
            "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0020 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 004 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z");
        tmpSyncBtnIcon.setScaleX(0.85);
        tmpSyncBtnIcon.setScaleY(0.85);
        tmpSyncBtnIcon.setStyle("-fx-fill: white;");
        tmpBtnSync.setGraphic(tmpSyncBtnIcon);

        Tooltip tmpSyncTooltip =
            new Tooltip(resources.getString("library.empty.sync_tooltip"));
        tmpSyncTooltip.setShowDelay(Duration.millis(250));
        tmpSyncTooltip.getStyleClass().add("chart-popup-tooltip");
        Tooltip.install(tmpBtnSync, tmpSyncTooltip);

        tmpBtnSync.setOnAction(e -> syncLibrary());

        tmpBtnBox.getChildren().add(tmpBtnSync);
        card.getChildren().addAll(tmpBadge, tmpSyncIconWrapper, tmpTitle,
            tmpText, tmpBtnBox);
      } else {
        // No workspaces configured yet
        Label tmpBadge =
            new Label(resources.getString("library.empty.config_required"));
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
        VBox tmpCameraIconWrapper = new VBox(tmpCameraIcon);
        tmpCameraIconWrapper.setAlignment(Pos.CENTER);
        tmpCameraIconWrapper.setPadding(new Insets(10, 0, 10, 0));

        Label tmpTitle =
            new Label(resources.getString("library.empty.config_title"));
        tmpTitle.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

        Label tmpText =
            new Label(resources.getString("library.empty.config_text"));
        tmpText.setWrapText(true);
        tmpText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        tmpText.setMaxWidth(420);
        tmpText
            .setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");

        HBox tmpBtnBox = new HBox(14);
        tmpBtnBox.setAlignment(Pos.CENTER);

        Button tmpBtnSettings =
            new Button(resources.getString("library.empty.config_btn"));
        tmpBtnSettings.getStyleClass().addAll("button-success");
        tmpBtnSettings.setStyle(
            "-fx-padding: 8px 18px; -fx-font-size: 14px; -fx-font-weight: bold;");
        CursorUtils.applyCursor(tmpBtnSettings);

        SVGPath tmpGear = new SVGPath();
        tmpGear.setContent(
            "M12 15.5A3.5 3.5 0 0 1 8.5 12 3.5 3.5 0 0 1 12 8.5a3.5 3.5 0 0 1 3.5 3.5 3.5 3.5 0 0 1-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97 0-.33-.03-.66-.07-1l2.11-1.63c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.37-2.65A.506.506 0 0 0 14 2h-4c-.25 0-.46.18-.5.42l-.37 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64L4.57 11c-.04.34-.07.67-.07 1 0 .33.03.65.07.97l-2.11 1.66c-.19.15-.25.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1.01c.52.4 1.06.74 1.69.99l.37 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.37-2.65c.63-.26 1.17-.59 1.69-.99l2.49 1.01c.22.08.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.66Z");
        tmpGear.setScaleX(0.85);
        tmpGear.setScaleY(0.85);
        tmpGear.setStyle("-fx-fill: white;");
        tmpBtnSettings.setGraphic(tmpGear);

        Tooltip tmpTooltip =
            new Tooltip(resources.getString("library.empty.config_tooltip"));
        tmpTooltip.setShowDelay(Duration.millis(250));
        tmpTooltip.getStyleClass().add("chart-popup-tooltip");
        Tooltip.install(tmpBtnSettings, tmpTooltip);

        tmpBtnSettings.setOnAction(e -> {
          MainController tmpMain = appContext.getMainController();
          if (tmpMain != null) {
            tmpMain.showSettings();
          }
        });

        tmpBtnBox.getChildren().add(tmpBtnSettings);
        card.getChildren().addAll(tmpBadge, tmpCameraIconWrapper, tmpTitle,
            tmpText, tmpBtnBox);
      }
    } else {
      SVGPath tmpSearchIcon = new SVGPath();
      tmpSearchIcon.setContent(
          "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
      tmpSearchIcon.setScaleX(2.0);
      tmpSearchIcon.setScaleY(2.0);
      tmpSearchIcon.setStyle("-fx-fill: -color-fg-muted;");

      Label tmpTitle =
          new Label(resources.getString("library.empty.no_match_title"));
      tmpTitle.setStyle(
          "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
      Label tmpSub =
          new Label(resources.getString("library.empty.no_match_text"));
      tmpSub.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");
      card.getChildren().addAll(tmpSearchIcon, tmpTitle, tmpSub);
    }

    rootVerticalBox.getChildren().add(card);
    return rootVerticalBox;
  }

  /**
   * Opens the detailed view for a specific movie, dynamically replacing the
   * library grid with the movie's detail view in the parent StackPane.
   *
   * @param movie The movie object to display details for.
   */
  private void openMovieDetail(Movie movie) {
    try {
      MovieDetailController controller = movieDetailControllerProvider.get();
      Parent detailView = controller.buildView();
      controller.initialize();

      currentMovieDetailController = controller;
      controller.setMovie(movie);

      // Replaces library root with the detail view dynamically in StackPane
      StackPane rootPane = (StackPane) libraryRoot.getParent();
      if (rootPane != null) {
        controller.setOnBack(() -> {
          currentMovieDetailController = null;
          rootPane.getChildren().setAll(libraryRoot);
          refresh();
        });
        rootPane.getChildren().setAll(detailView);
      }
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "Failed to create MovieDetailView view",
          exception);
    }
  }

  /**
   * Initiates a background task to scan configured workspaces and sync the
   * movie library. Binds UI progress elements to the task and handles success,
   * failure, and cancellation events.
   */
  private void syncLibrary() {
    if (activeScanTask != null && activeScanTask.isRunning()) {
      return;
    }

    activeScanTask = new Task<>() {
      @Override
      protected LibraryScanService.ScanResult call() throws Exception {
        return libraryScanService
            .scan(new LibraryScanService.ProgressCallback() {
              @Override
              public void updateProgress(int workDone, int max) {
                callUpdateProgress(workDone, max);
              }

              @Override
              public void updateMessage(String message) {
                callUpdateMessage(message);
              }

              @Override
              public boolean isCancelled() {
                return isTaskCancelled();
              }
            });
      }

      private void callUpdateProgress(int workDone, int max) {
        updateProgress(workDone, max);
      }

      private void callUpdateMessage(String message) {
        updateMessage(message);
      }

      private boolean isTaskCancelled() {
        return isCancelled();
      }
    };

    // Bind UI progress elements to the background task and lock GUI
    // interactions during sync
    progressBar.progressProperty().bind(activeScanTask.progressProperty());
    progressArea.setVisible(true);
    gridPane.setDisable(true);
    if (gridPane.getScene() != null) {
      gridPane.getScene().setCursor(Cursor.WAIT);
    }

    activeScanTask.setOnSucceeded(event -> {
      LibraryScanService.ScanResult tmpResult = activeScanTask.getValue();
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
      gridPane.setDisable(false);
      if (gridPane.getScene() != null) {
        gridPane.getScene().setCursor(Cursor.DEFAULT);
      }

      Stage tmpOwnerStage = (Stage) gridPane.getScene().getWindow();
      if (tmpResult.unresolvedMovies() != null
          && !tmpResult.unresolvedMovies().isEmpty()) {
        DisambiguationView tmpDisambiguationView =
            new DisambiguationView(tmpOwnerStage, resources,
                tmpResult.unresolvedMovies(), imdbService, movieRepository);
        tmpDisambiguationView.showAndWait();
      }

      SyncOverviewView tmpOverviewView = new SyncOverviewView(tmpOwnerStage,
          resources, tmpResult, imdbService, movieRepository);
      tmpOverviewView.showAndWait();

      refresh();
      appContext.notifyLibraryChanged();
    });

    activeScanTask.setOnFailed(event -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
      gridPane.setDisable(false);
      if (gridPane.getScene() != null) {
        gridPane.getScene().setCursor(Cursor.DEFAULT);
      }
      Throwable exception = activeScanTask.getException();
      CustomAlert.showInformation(gridPane.getScene().getWindow(),
          "Sync Failed", "An error occurred during search",
          exception != null ? exception.getMessage()
              : "Unknown database or file error");
    });

    activeScanTask.setOnCancelled(event -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      progressArea.setVisible(false);
      gridPane.setDisable(false);
      if (gridPane.getScene() != null) {
        gridPane.getScene().setCursor(Cursor.DEFAULT);
      }
    });

    // Run sync task in a daemon thread so it doesn't block JavaFX application
    // exit
    Thread tmpThread = new Thread(activeScanTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  /**
   * Cancels the currently active library sync background task, if one is
   * running.
   */
  private void cancelSync() {
    if (activeScanTask != null) {
      activeScanTask.cancel();
    }
  }

  private void showAddMovieDialog() {
    Stage tmpDialogStage = new Stage();
    tmpDialogStage.setTitle(resources.getString("library.add.movie.title"));
    tmpDialogStage.setMinWidth(780);
    tmpDialogStage.setMinHeight(620);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setPadding(new Insets(20));
    tmpRoot.setStyle("-fx-background-color: -color-bg-default;");

    Label tmpHeaderLabel =
        new Label("Enhanced Movie Import & Match Management");
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    tmpRoot.setTop(tmpHeaderLabel);

    // Interactive Title Search & Match Options List
    VBox tmpSection1 = new VBox(10);
    tmpSection1.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 15; -fx-background-radius: 8;");

    Label tmpSec1Title = new Label("Interactive Match Selection");
    tmpSec1Title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    TextField tmpSearchInput = new TextField();
    tmpSearchInput.setPromptText("Enter movie title to search candidates...");
    Button btnSearchCandidates = new Button("Search Candidates");
    btnSearchCandidates.getStyleClass().add("button-primary");
    HBox tmpSearchBox = new HBox(10, tmpSearchInput, btnSearchCandidates);
    HBox.setHgrow(tmpSearchInput, Priority.ALWAYS);

    VBox tmpCandidateContainer = new VBox(10);
    ScrollPane tmpScroll = new ScrollPane(tmpCandidateContainer);
    tmpScroll.setFitToWidth(true);
    tmpScroll.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(tmpScroll, Priority.ALWAYS);

    tmpSection1.getChildren().addAll(tmpSec1Title, tmpSearchBox, tmpScroll);
    VBox.setVgrow(tmpSection1, Priority.ALWAYS);

    // Manual Search via IMDb ID
    VBox tmpSection2 = new VBox(10);
    tmpSection2.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 15; -fx-background-radius: 8;");

    Label tmpSec2Title = new Label("Manual Search via IMDb ID");
    tmpSec2Title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    TextField tmpImdbInput = new TextField();
    tmpImdbInput.setPromptText("e.g. tt0111161");
    Button btnImdbImport = new Button("Instant Import");
    btnImdbImport.getStyleClass().add("button-success");
    HBox tmpImdbBox = new HBox(10, tmpImdbInput, btnImdbImport);
    HBox.setHgrow(tmpImdbInput, Priority.ALWAYS);

    tmpSection2.getChildren().addAll(tmpSec2Title, tmpImdbBox);

    VBox tmpCenter = new VBox(15, tmpSection1, tmpSection2);
    VBox.setVgrow(tmpCenter, Priority.ALWAYS);
    BorderPane.setMargin(tmpCenter, new Insets(12, 0, 12, 0));
    tmpRoot.setCenter(tmpCenter);

    // OK Button MUST always occur in the right bottom corner
    Button btnClose = new Button("OK");
    btnClose.getStyleClass().add("button-secondary");
    btnClose.setStyle("-fx-font-weight: bold; -fx-padding: 8px 20px;");
    btnClose.setOnAction(e -> tmpDialogStage.close());

    HBox tmpBottomBox = new HBox(btnClose);
    tmpBottomBox.setAlignment(Pos.BOTTOM_RIGHT);
    tmpRoot.setBottom(tmpBottomBox);

    Runnable tmpPerformSearch = () -> {
      String tmpQuery = tmpSearchInput.getText().trim();
      if (tmpQuery.isEmpty())
        return;
      tmpCandidateContainer.getChildren()
          .setAll(new Label("Searching IMDb for matches..."));

      Task<List<Movie>> tmpTask = new Task<>() {
        @Override
        protected List<Movie> call() {
          return imdbService.searchSuggestions(tmpQuery);
        }
      };

      tmpTask.setOnSucceeded(evt -> {
        List<Movie> tmpResults = tmpTask.getValue();
        tmpCandidateContainer.getChildren().clear();
        if (tmpResults.isEmpty()) {
          tmpCandidateContainer.getChildren()
              .add(new Label("No candidate movie matches found."));
        } else {
          for (Movie tmpCand : tmpResults) {
            HBox tmpRow = new HBox(15);
            tmpRow.setAlignment(Pos.CENTER_LEFT);
            tmpRow.setStyle(
                "-fx-background-color: -color-bg-subtle; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: -color-border-default; -fx-border-radius: 8;");

            ImageView tmpPoster = new ImageView();
            tmpPoster.setFitWidth(70);
            tmpPoster.setFitHeight(105);
            tmpPoster.setPreserveRatio(true);

            if (tmpCand.getCoverImagePath() != null) {
              File tmpImgFile = new File(tmpCand.getCoverImagePath());
              if (tmpImgFile.exists()) {
                try {
                  tmpPoster.setImage(
                      new Image(tmpImgFile.toURI().toURL().toExternalForm(), 70,
                          105, true, true, true));
                } catch (Exception ignored) {
                }
              }
            }

            VBox tmpInfo = new VBox(4);
            Label tmpT = new Label(tmpCand.getTitle());
            tmpT.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-fg-default;");
            String tmpSubText =
                (tmpCand.getYear() != null ? "Year: " + tmpCand.getYear() : "")
                    + (tmpCand.getImdbId() != null
                        ? "  •  IMDb ID: " + tmpCand.getImdbId()
                        : "");
            Label tmpSub = new Label(tmpSubText);
            tmpSub.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");
            tmpInfo.getChildren().addAll(tmpT, tmpSub);

            if (tmpCand.getActors() != null && !tmpCand.getActors().isBlank()) {
              Label tmpAct = new Label("Cast: " + tmpCand.getActors());
              tmpAct.setStyle(
                  "-fx-font-size: 11px; -fx-text-fill: -color-fg-subtle;");
              tmpInfo.getChildren().add(tmpAct);
            }

            Region tmpSp = new Region();
            HBox.setHgrow(tmpSp, Priority.ALWAYS);

            Button btnSelectMatch = new Button("Select & Import");
            btnSelectMatch.getStyleClass().add("button-primary");
            btnSelectMatch.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 16px;");
            btnSelectMatch.setOnAction(e -> {
              String tmpTargetId = tmpCand.getImdbId();
              if (tmpTargetId == null || tmpTargetId.isBlank())
                return;

              tmpRoot.setDisable(true);
              if (tmpDialogStage.getScene() != null) {
                tmpDialogStage.getScene().setCursor(Cursor.WAIT);
              }

              Task<Void> tmpImportTask = new Task<>() {
                @Override
                protected Void call() {
                  Movie tmpMovie =
                      new Movie("manual://" + tmpTargetId, tmpCand.getTitle());
                  tmpMovie.setMatchStatus("MANUAL");
                  imdbService.enrichMovieDataById(tmpTargetId, tmpMovie);
                  if (tmpMovie.getTitle() != null) {
                    movieRepository.insert(tmpMovie);
                  }
                  return null;
                }
              };
              tmpImportTask.setOnSucceeded(e2 -> {
                tmpDialogStage.close();
                refresh();
                appContext.notifyLibraryChanged();
              });
              tmpImportTask.setOnFailed(e2 -> {
                tmpRoot.setDisable(false);
                if (tmpDialogStage.getScene() != null) {
                  tmpDialogStage.getScene().setCursor(Cursor.DEFAULT);
                }
              });
              Thread t = new Thread(tmpImportTask);
              t.setDaemon(true);
              t.start();
            });

            tmpRow.getChildren().addAll(tmpPoster, tmpInfo, tmpSp,
                btnSelectMatch);
            tmpCandidateContainer.getChildren().add(tmpRow);
          }
        }
      });

      Thread t = new Thread(tmpTask);
      t.setDaemon(true);
      t.start();
    };

    btnSearchCandidates.setOnAction(e -> tmpPerformSearch.run());
    tmpSearchInput.setOnAction(e -> tmpPerformSearch.run());

    btnImdbImport.setOnAction(e -> {
      String tmpId = tmpImdbInput.getText().trim();
      if (!tmpId.matches("^tt\\d{7,}$")) {
        tmpImdbInput.setStyle("-fx-border-color: red;");
        return;
      }
      tmpImdbInput.setStyle("");

      tmpRoot.setDisable(true);
      if (tmpDialogStage.getScene() != null) {
        tmpDialogStage.getScene().setCursor(Cursor.WAIT);
      }

      Task<Void> tmpAdd = new Task<>() {
        @Override
        protected Void call() {
          Movie tmpMovie = new Movie("manual://" + tmpId, "Importing...");
          tmpMovie.setMatchStatus("MANUAL");
          imdbService.enrichMovieDataById(tmpId, tmpMovie);
          if (tmpMovie.getTitle() != null) {
            movieRepository.insert(tmpMovie);
          }
          return null;
        }
      };
      tmpAdd.setOnSucceeded(evt -> {
        tmpDialogStage.close();
        refresh();
        appContext.notifyLibraryChanged();
      });
      tmpAdd.setOnFailed(evt -> {
        tmpRoot.setDisable(false);
        if (tmpDialogStage.getScene() != null) {
          tmpDialogStage.getScene().setCursor(Cursor.DEFAULT);
        }
      });
      Thread t = new Thread(tmpAdd);
      t.setDaemon(true);
      t.start();
    });

    Scene tmpScene = new Scene(tmpRoot, 780, 620);
    tmpScene.getStylesheets().addAll(gridPane.getScene().getStylesheets());
    tmpDialogStage.setScene(tmpScene);
    tmpDialogStage.initOwner(gridPane.getScene().getWindow());
    tmpDialogStage.initModality(Modality.APPLICATION_MODAL);
    tmpDialogStage.showAndWait();
  }
}
