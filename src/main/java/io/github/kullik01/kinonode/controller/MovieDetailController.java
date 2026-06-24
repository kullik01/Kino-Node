package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.util.PathUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MovieDetailController {
    private static final Logger LOGGER = Logger.getLogger(MovieDetailController.class.getName());

    @FXML private Label lblHeaderTitle;
    @FXML private ImageView imgPoster;
    @FXML private Button btnPlay;
    @FXML private Button btnTrailer;
    
    @FXML private TextField txtTitle;
    @FXML private TextField txtYear;
    @FXML private TextField txtGenre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtTrailerUrl;
    
    @FXML private HBox ratingContainer;
    @FXML private Label lblWatchlistsList;
    @FXML private MenuButton menuWatchlists;
    @FXML private Label lblStatus;

    private Movie movie;
    private Integer currentRating; // Nullable
    
    private MovieRepository movieRepository;
    private WatchlistRepository watchlistRepository;

    @FXML
    public void initialize() {
        movieRepository = AppContext.getInstance().getMovieRepository();
        watchlistRepository = AppContext.getInstance().getWatchlistRepository();
        
        // Listeners for trailer URL to dynamically disable/enable Watch Trailer button
        txtTrailerUrl.textProperty().addListener((obs, oldVal, newVal) -> {
            btnTrailer.setDisable(newVal == null || newVal.trim().isEmpty());
        });
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
        this.currentRating = movie.getRating();
        
        lblHeaderTitle.setText(movie.getTitle());
        txtTitle.setText(movie.getTitle());
        txtYear.setText(movie.getYear() != null ? String.valueOf(movie.getYear()) : "");
        txtGenre.setText(movie.getGenre() != null ? movie.getGenre() : "");
        txtDescription.setText(movie.getDescription() != null ? movie.getDescription() : "");
        txtTrailerUrl.setText(movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "");
        
        btnTrailer.setDisable(txtTrailerUrl.getText().trim().isEmpty());
        
        loadPoster();
        buildRatingWidget();
        refreshWatchlistStatus();
    }

    private void loadPoster() {
        String path = movie.getCoverImagePath();
        Image image = null;
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    image = new Image(f.toURI().toURL().toExternalForm(), 240, 360, false, true);
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "Failed to load poster in details: " + path, e);
                }
            }
        }

        if (image == null) {
            try {
                var resource = getClass().getResource("/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
                if (resource != null) {
                    image = new Image(resource.toExternalForm(), 240, 360, false, true);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load details placeholder cover", e);
            }
        }
        
        if (image != null) {
            imgPoster.setImage(image);
        }
    }

    private void buildRatingWidget() {
        ratingContainer.getChildren().clear();
        
        // Clear Rating button (Reset to unrated)
        Button btnClear = new Button("❌");
        btnClear.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2px; -fx-font-size: 11px;");
        Tooltip clearTooltip = new Tooltip("Clear rating (unrated)");
        clearTooltip.setShowDelay(javafx.util.Duration.millis(250));
        btnClear.setTooltip(clearTooltip);
        btnClear.setOnAction(e -> {
            currentRating = null;
            updateRatingUI();
        });
        ratingContainer.getChildren().add(btnClear);

        // '0' Rating button
        Button btnZero = new Button("0");
        btnZero.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2px 5px; -fx-font-size: 13px; -fx-font-weight: bold;");
        Tooltip zeroTooltip = new Tooltip("Rate 0/10");
        zeroTooltip.setShowDelay(javafx.util.Duration.millis(250));
        btnZero.setTooltip(zeroTooltip);
        btnZero.setOnAction(e -> {
            currentRating = 0;
            updateRatingUI();
        });
        ratingContainer.getChildren().add(btnZero);

        // 10 Film Clapperboard icons (1 to 10)
        List<Label> reels = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            final int score = i;
            Label reel = new Label("🎬");
            reel.getStyleClass().add("rating-reel");
            reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 0 1px;");
            Tooltip reelTooltip = new Tooltip("Rate " + score + "/10");
            reelTooltip.setShowDelay(javafx.util.Duration.millis(250));
            reel.setTooltip(reelTooltip);
            reel.setOnMouseClicked(e -> {
                currentRating = score;
                updateRatingUI();
            });
            reels.add(reel);
            ratingContainer.getChildren().add(reel);
        }

        // Numerical display label
        Label lblRatingVal = new Label();
        lblRatingVal.setStyle("-fx-text-fill: #ffb703; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
        ratingContainer.getChildren().add(lblRatingVal);

        updateRatingUI();
    }

    private void updateRatingUI() {
        // Find rating nodes
        Button btnZero = (Button) ratingContainer.getChildren().get(1);
        Label lblRatingVal = (Label) ratingContainer.getChildren().get(ratingContainer.getChildren().size() - 1);
        
        if (currentRating == null) {
            btnZero.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-weight: bold;");
            for (int i = 2; i <= 11; i++) {
                Label reel = (Label) ratingContainer.getChildren().get(i);
                reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #444444;");
            }
            lblRatingVal.setText("Unrated");
        } else if (currentRating == 0) {
            btnZero.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb703; -fx-font-weight: bold;");
            for (int i = 2; i <= 11; i++) {
                Label reel = (Label) ratingContainer.getChildren().get(i);
                reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #444444;");
            }
            lblRatingVal.setText("0/10");
        } else {
            btnZero.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-weight: bold;");
            for (int i = 1; i <= 10; i++) {
                Label reel = (Label) ratingContainer.getChildren().get(i + 1);
                if (i <= currentRating) {
                    reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #ffb703;");
                } else {
                    reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #444444;");
                }
            }
            lblRatingVal.setText(currentRating + "/10");
        }
    }

    private void refreshWatchlistStatus() {
        List<Watchlist> all = watchlistRepository.findAll();
        List<String> inWatchlists = all.stream()
                .filter(w -> w.getMovies().stream().anyMatch(m -> m.getId().equals(movie.getId())))
                .map(Watchlist::getName)
                .collect(Collectors.toList());

        if (inWatchlists.isEmpty()) {
            lblWatchlistsList.setText("None");
        } else {
            lblWatchlistsList.setText(String.join(", ", inWatchlists));
        }

        // Rebuild Menu
        menuWatchlists.getItems().clear();
        for (Watchlist w : all) {
            boolean alreadyIn = w.getMovies().stream().anyMatch(m -> m.getId().equals(movie.getId()));
            if (!alreadyIn) {
                MenuItem item = new MenuItem("Add to: " + w.getName());
                item.setOnAction(e -> {
                    watchlistRepository.addMovieToWatchlist(w.getId(), movie.getId());
                    refreshWatchlistStatus();
                    lblStatus.setText("Added to watchlist " + w.getName());
                });
                menuWatchlists.getItems().add(item);
            } else {
                MenuItem item = new MenuItem("🗑 Remove from: " + w.getName());
                item.setOnAction(e -> {
                    watchlistRepository.removeMovieFromWatchlist(w.getId(), movie.getId());
                    refreshWatchlistStatus();
                    lblStatus.setText("Removed from watchlist " + w.getName());
                });
                menuWatchlists.getItems().add(item);
            }
        }

        // Option to create new watchlist
        MenuItem createNew = new MenuItem("➕ Create New Watchlist...");
        createNew.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Watchlist");
            dialog.setHeaderText("Create a new watchlist");
            dialog.setContentText("Watchlist Name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                String cleanName = name.trim();
                if (!cleanName.isEmpty()) {
                    Watchlist nw = watchlistRepository.insert(new Watchlist(cleanName));
                    if (nw.getId() != null) {
                        watchlistRepository.addMovieToWatchlist(nw.getId(), movie.getId());
                        refreshWatchlistStatus();
                        lblStatus.setText("Created watchlist and added movie.");
                    }
                }
            });
        });
        menuWatchlists.getItems().add(createNew);
    }

    @FXML
    private void changePoster() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Poster Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        
        File selected = chooser.showOpenDialog(imgPoster.getScene().getWindow());
        if (selected != null) {
            try {
                Path dbPath = AppContext.getInstance().getDatabaseService().getDbPath();
                Path coversDir = dbPath.getParent().resolve("covers");
                if (!Files.exists(coversDir)) {
                    Files.createDirectories(coversDir);
                }

                String ext = PathUtils.getFileExtension(selected);
                if (ext.isEmpty()) ext = "jpg";

                String filename = "cover_" + movie.getId() + "_" + System.currentTimeMillis() + "." + ext;
                Path destPath = coversDir.resolve(filename);

                Files.copy(selected.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                String newPath = destPath.toAbsolutePath().toString().replace("\\", "/");
                movie.setCoverImagePath(newPath);
                
                // Reload image
                imgPoster.setImage(new Image(destPath.toUri().toURL().toExternalForm(), 240, 360, false, true));
                lblStatus.setText("Poster image updated.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to copy cover poster file", e);
                lblStatus.setText("Error saving poster: " + e.getMessage());
            }
        }
    }

    @FXML
    private void playMovie() {
        try {
            AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
            lblStatus.setText("Opening external player...");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to launch external media player", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback Failed");
            alert.setHeaderText("Could not launch external media player");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void watchTrailer() {
        String url = txtTrailerUrl.getText().trim();
        if (url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                lblStatus.setText("Opening trailer link in browser...");
            } else {
                lblStatus.setText("System web browser action not supported on this platform.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to open browser trailer link", e);
            lblStatus.setText("Failed to open browser.");
        }
    }

    @FXML
    private void saveChanges() {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            lblStatus.setText("Title cannot be empty.");
            return;
        }

        Integer year = null;
        if (!txtYear.getText().trim().isEmpty()) {
            try {
                year = Integer.parseInt(txtYear.getText().trim());
            } catch (NumberFormatException e) {
                lblStatus.setText("Release year must be a valid integer number.");
                return;
            }
        }

        String trailer = txtTrailerUrl.getText().trim();
        if (!trailer.isEmpty()) {
            if (!trailer.contains("youtube.com") && !trailer.contains("youtu.be")) {
                lblStatus.setText("Trailer Link must be a valid YouTube URL.");
                return;
            }
        }

        movie.setTitle(title);
        movie.setYear(year);
        movie.setGenre(txtGenre.getText().trim().isEmpty() ? null : txtGenre.getText().trim());
        movie.setDescription(txtDescription.getText().trim().isEmpty() ? null : txtDescription.getText().trim());
        movie.setTrailerUrl(trailer.isEmpty() ? null : trailer);
        movie.setRating(currentRating);

        movieRepository.update(movie);
        goBack();
    }

    @FXML
    private void cancelEdit() {
        goBack();
    }

    @FXML
    private void goBack() {
        try {
            URL url = getClass().getResource("/io/github/kullik01/kinonode/fxml/LibraryView.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent libraryView = loader.load();

            StackPane contentArea = (StackPane) lblHeaderTitle.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(libraryView);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to library grid", e);
        }
    }
}
