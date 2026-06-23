package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WatchlistsController implements Refreshable {
    private static final Logger LOGGER = Logger.getLogger(WatchlistsController.class.getName());

    @FXML private ListView<Watchlist> lstWatchlists;
    @FXML private VBox paneDetails;
    @FXML private Label lblWatchlistName;
    @FXML private Label lblMovieCount;
    @FXML private ListView<Movie> lstMovies;

    @FXML private Button btnRemoveMovie;

    private WatchlistRepository watchlistRepository;
    private Watchlist selectedWatchlist;

    @FXML
    public void initialize() {
        watchlistRepository = AppContext.getInstance().getWatchlistRepository();

        // Custom Cell Factory for Watchlists
        lstWatchlists.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Watchlist item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // Watchlist Selection Listener
        lstWatchlists.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedWatchlist = newVal;
            loadWatchlistMovies();
        });

        // Custom Cell Factory for Movies inside Watchlist
        lstMovies.setCellFactory(lv -> new ListCell<>() {
            private final HBox content = new HBox(15);
            private final ImageView thumbnail = new ImageView();
            private final Label titleLabel = new Label();
            private final Label ratingLabel = new Label();
            
            {
                content.setAlignment(Pos.CENTER_LEFT);
                thumbnail.setFitWidth(30);
                thumbnail.setFitHeight(45);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                ratingLabel.setStyle("-fx-text-fill: #ffb703;");
                
                // Allow title to expand, keeping rating aligned to right
                HBox.setHgrow(titleLabel, Priority.ALWAYS);
                content.getChildren().addAll(thumbnail, titleLabel, ratingLabel);
            }
            
            @Override
            protected void updateItem(Movie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    titleLabel.setText(item.getTitle() + (item.getYear() != null ? " (" + item.getYear() + ")" : ""));
                    ratingLabel.setText(item.getRating() != null ? "🎬 " + item.getRating() + "/10" : "Unrated");
                    
                    // Thumbnail
                    Image img = null;
                    if (item.getCoverImagePath() != null) {
                        File f = new File(item.getCoverImagePath());
                        if (f.exists()) {
                            try {
                                img = new Image(f.toURI().toURL().toExternalForm(), 30, 45, false, true);
                            } catch (MalformedURLException ignored) {}
                        }
                    }
                    if (img == null) {
                        try {
                            var resource = getClass().getResource("/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
                            if (resource != null) {
                                img = new Image(resource.toExternalForm(), 30, 45, false, true);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (img != null) {
                        thumbnail.setImage(img);
                    }
                    setGraphic(content);
                }
            }
        });

        // Double-click to Play movie in list
        lstMovies.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Movie selectedMovie = lstMovies.getSelectionModel().getSelectedItem();
                if (selectedMovie != null) {
                    playMovie(selectedMovie);
                }
            }
        });

        // Update button states on selection changes
        lstMovies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates();
        });

        updateButtonStates();
        refresh();
    }

    @Override
    public void refresh() {
        int selectedIdx = lstWatchlists.getSelectionModel().getSelectedIndex();
        List<Watchlist> watchlists = watchlistRepository.findAll();
        lstWatchlists.getItems().setAll(watchlists);

        if (selectedIdx >= 0 && selectedIdx < watchlists.size()) {
            lstWatchlists.getSelectionModel().select(selectedIdx);
        } else if (!watchlists.isEmpty()) {
            lstWatchlists.getSelectionModel().selectFirst();
        } else {
            selectedWatchlist = null;
            loadWatchlistMovies();
        }
    }

    private void loadWatchlistMovies() {
        if (selectedWatchlist == null) {
            lblWatchlistName.setText("Select a Watchlist");
            lblMovieCount.setText("0 movies");
            lstMovies.getItems().clear();
            paneDetails.setDisable(true);
        } else {
            // Re-fetch latest from db to avoid stale data
            Watchlist refreshed = watchlistRepository.findById(selectedWatchlist.getId());
            if (refreshed != null) {
                selectedWatchlist = refreshed;
            }
            
            lblWatchlistName.setText(selectedWatchlist.getName());
            List<Movie> movies = selectedWatchlist.getMovies();
            lblMovieCount.setText(movies.size() + (movies.size() == 1 ? " movie" : " movies"));
            lstMovies.getItems().setAll(movies);
            paneDetails.setDisable(false);
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean selected = lstMovies.getSelectionModel().getSelectedItem() != null;
        btnRemoveMovie.setDisable(!selected);
    }

    @FXML
    private void createWatchlist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Watchlist");
        dialog.setHeaderText("Create a new watchlist");
        dialog.setContentText("Watchlist Name:");
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(name -> {
            String cleanName = name.trim();
            if (!cleanName.isEmpty()) {
                watchlistRepository.insert(new Watchlist(cleanName));
                refresh();
            }
        });
    }

    @FXML
    private void renameWatchlist() {
        if (selectedWatchlist == null) return;

        TextInputDialog dialog = new TextInputDialog(selectedWatchlist.getName());
        dialog.setTitle("Rename Watchlist");
        dialog.setHeaderText("Rename watchlist: " + selectedWatchlist.getName());
        dialog.setContentText("New Name:");
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(name -> {
            String cleanName = name.trim();
            if (!cleanName.isEmpty() && !cleanName.equals(selectedWatchlist.getName())) {
                selectedWatchlist.setName(cleanName);
                watchlistRepository.update(selectedWatchlist);
                refresh();
            }
        });
    }

    @FXML
    private void deleteWatchlist() {
        if (selectedWatchlist == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Watchlist");
        alert.setHeaderText("Delete watchlist: " + selectedWatchlist.getName());
        alert.setContentText("Are you sure you want to delete this watchlist? Movies will not be deleted from your disk or library.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            watchlistRepository.delete(selectedWatchlist.getId());
            refresh();
        }
    }



    @FXML
    private void removeMovie() {
        Movie selectedMovie = lstMovies.getSelectionModel().getSelectedItem();
        if (selectedMovie == null || selectedWatchlist == null) return;

        watchlistRepository.removeMovieFromWatchlist(selectedWatchlist.getId(), selectedMovie.getId());
        loadWatchlistMovies();
    }

    private void playMovie(Movie movie) {
        try {
            AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to play movie from watchlist", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback Failed");
            alert.setHeaderText("Cannot open media player");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
