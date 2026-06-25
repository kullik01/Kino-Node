package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.service.LibraryScanService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import io.github.kullik01.kinonode.util.CustomAlert;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private final java.util.Map<Integer, MovieCardController> controllerCache = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        movieRepository = AppContext.getInstance().getMovieRepository();
        libraryScanService = AppContext.getInstance().getLibraryScanService();

        // Setup sort options
        cmbSort.getItems().addAll(
                "Title (A-Z)",
                "Title (Z-A)",
                "Release Year",
                "Rating (Highest)",
                "Recently Added"
        );
        cmbSort.setValue("Recently Added");

        // Real-time filters and sorts
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndDisplayMovies());
        cmbSort.valueProperty().addListener((obs, oldVal, newVal) -> filterAndDisplayMovies());
        
        btnSyncLibrary.disableProperty().bind(progressArea.visibleProperty());

        refresh();
    }

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
        List<Movie> filtered = allMovies.stream()
                .filter(m -> query.isEmpty() ||
                        m.getTitle().toLowerCase().contains(query) ||
                        (m.getGenre() != null && m.getGenre().toLowerCase().contains(query)) ||
                        (m.getDescription() != null && m.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        // Sort movies
        String sortOption = cmbSort.getValue();
        if (sortOption != null) {
            switch (sortOption) {
                case "Title (A-Z)" -> filtered.sort(Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER));
                case "Title (Z-A)" -> filtered.sort(Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER).reversed());
                case "Release Year" -> filtered.sort((m1, m2) -> {
                    if (m1.getYear() == null && m2.getYear() == null) return 0;
                    if (m1.getYear() == null) return 1;
                    if (m2.getYear() == null) return -1;
                    return Integer.compare(m1.getYear(), m2.getYear());
                });
                case "Rating (Highest)" -> filtered.sort((m1, m2) -> {
                    if (m1.getRating() == null && m2.getRating() == null) return 0;
                    if (m1.getRating() == null) return 1;
                    if (m2.getRating() == null) return -1;
                    return Integer.compare(m2.getRating(), m1.getRating()); // Descending
                });
                case "Recently Added" -> filtered.sort(Comparator.comparing(Movie::getDateAdded).reversed());
            }
        }

        java.util.List<javafx.scene.Node> newCards = new java.util.ArrayList<>();
        for (Movie m : filtered) {
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

            StackPane rootPane = (StackPane) libraryRoot.getParent();
            if (rootPane != null) {
                controller.setOnBack(() -> {
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
        
        progressBar.progressProperty().bind(activeScanTask.progressProperty());
        progressArea.setVisible(true);

        activeScanTask.setOnSucceeded(event -> {
            LibraryScanService.ScanResult result = activeScanTask.getValue();
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressArea.setVisible(false);
            refresh();
            
            CustomAlert.showInformation(
                gridPane.getScene().getWindow(),
                "Sync Completed",
                "Sync Completed Successfully",
                "Total files scanned: " + result.totalScanned() + "\nNew movies added: " + result.newAdded()
            );
        });

        activeScanTask.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressArea.setVisible(false);
            Throwable e = activeScanTask.getException();
            CustomAlert.showInformation(
                gridPane.getScene().getWindow(),
                "Sync Failed",
                "An error occurred during search",
                e != null ? e.getMessage() : "Unknown database or file error"
            );
        });

        activeScanTask.setOnCancelled(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressArea.setVisible(false);
        });

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
