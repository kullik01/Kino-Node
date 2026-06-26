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
    private static Image PLACEHOLDER_IMAGE;

    @FXML private ListView<Watchlist> lstWatchlists;
    @FXML private VBox paneDetails;
    @FXML private Label lblWatchlistName;
    @FXML private Label lblMovieCount;
    @FXML private ListView<Movie> lstMovies;
    @FXML private Button btnRenameWatchlist;
    @FXML private Button btnDeleteWatchlist;
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
            updateWatchlistButtonStates(newVal != null);
        });
        
        updateWatchlistButtonStates(false);

        // Custom Cell Factory for Movies inside Watchlist
        lstMovies.setCellFactory(lv -> new ListCell<>() {
            private final HBox content = new HBox(15);
            private final ImageView thumbnail = new ImageView();
            private final Label titleLabel = new Label();
            private final HBox ratingBox = new HBox(2);
            private final Label ratingTextLabel = new Label();
            
            {
                content.setAlignment(Pos.CENTER_LEFT);
                thumbnail.setFitWidth(30);
                thumbnail.setFitHeight(45);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
                titleLabel.setWrapText(true);
                titleLabel.setMinWidth(0);
                ratingTextLabel.setPrefWidth(50);
                ratingTextLabel.setMinWidth(50);
                ratingTextLabel.setMaxWidth(50);
                ratingTextLabel.setAlignment(Pos.CENTER_RIGHT);
                ratingTextLabel.setStyle("-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 5px;");
                ratingBox.setAlignment(Pos.CENTER_RIGHT);
                
                // Allow title to expand, keeping rating aligned to right
                HBox.setHgrow(titleLabel, Priority.ALWAYS);
                ratingBox.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                ratingTextLabel.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                
                content.maxWidthProperty().bind(lv.widthProperty().subtract(40));
                
                content.getChildren().addAll(thumbnail, titleLabel, ratingBox, ratingTextLabel);
            }
            
            @Override
            protected void updateItem(Movie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String text = item.getTitle() + (item.getYear() != null ? " (" + item.getYear() + ")" : "");
                    if (item.getCinematographer() != null && !item.getCinematographer().isBlank()) {
                        text += " - " + item.getCinematographer();
                    }
                    titleLabel.setText(text);
                    
                    ratingBox.getChildren().clear();
                    for (int i = 1; i <= 10; i++) {
                        Label reel = new Label("🎥");
                        reel.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE, javafx.scene.control.Label.USE_PREF_SIZE);
                        if (item.getRating() != null && i <= item.getRating()) {
                            reel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ED8B00; -fx-opacity: 1.0;");
                        } else {
                            reel.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444; -fx-opacity: 0.3;");
                        }
                        ratingBox.getChildren().add(reel);
                    }
                    if (item.getRating() != null) {
                        ratingTextLabel.setText(item.getRating() + "/10");
                        ratingTextLabel.setStyle("-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 5px;");
                    } else {
                        ratingTextLabel.setText("");
                    }
                    
                    // Thumbnail
                    Image img = null;
                    if (item.getCoverImagePath() != null) {
                        File f = new File(item.getCoverImagePath());
                        if (f.exists()) {
                            try {
                                img = new Image(f.toURI().toURL().toExternalForm(), 30, 45, false, true, true);
                            } catch (MalformedURLException ignored) {}
                        }
                    }
                    if (img == null) {
                        if (PLACEHOLDER_IMAGE == null) {
                            try {
                                var resource = getClass().getResource("/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
                                if (resource != null) {
                                    PLACEHOLDER_IMAGE = new Image(resource.toExternalForm(), 30, 45, false, true, true);
                                }
                            } catch (Exception ignored) {}
                        }
                        img = PLACEHOLDER_IMAGE;
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
            selectedWatchlist = lstWatchlists.getSelectionModel().getSelectedItem();
            loadWatchlistMovies();
        } else if (!watchlists.isEmpty()) {
            lstWatchlists.getSelectionModel().selectFirst();
            selectedWatchlist = lstWatchlists.getSelectionModel().getSelectedItem();
            loadWatchlistMovies();
        } else {
            selectedWatchlist = null;
            loadWatchlistMovies();
        }
    }

    private void loadWatchlistMovies() {
        if (selectedWatchlist == null) {
            lblWatchlistName.setText("Select a Movie");
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
    
    private void updateWatchlistButtonStates(boolean hasSelection) {
        if (btnRenameWatchlist != null) btnRenameWatchlist.setDisable(!hasSelection);
        if (btnDeleteWatchlist != null) btnDeleteWatchlist.setDisable(!hasSelection);
    }

    private Optional<String> showWatchlistDialog(String title, String header, String defaultText, int currentId) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        dialog.getDialogPane().setPrefSize(480, 280);
        dialog.getDialogPane().setMinSize(480, 280);
        
        javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(5);
        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setPrefWidth(430);
        errorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        TextField textField = new TextField(defaultText);
        textField.setPromptText("Watchlist Name");
        
        vbox.getChildren().addAll(errorLabel, textField);
        dialog.getDialogPane().setContent(vbox);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(defaultText.trim().isEmpty() || defaultText.length() > 30);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 30) {
                textField.setText(oldValue);
                return;
            }
            if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
                errorLabel.setText("The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
            } else {
                boolean duplicate = false;
                List<Watchlist> watchlists = watchlistRepository.findAll();
                for (Watchlist w : watchlists) {
                    if (w.getName().equalsIgnoreCase(newValue.trim())) {
                        if (w.getId() != null && w.getId() != currentId) {
                            duplicate = true;
                            break;
                        }
                    }
                }
                
                if (duplicate) {
                    errorLabel.setText("A watchlist with this name already exists. Please choose a different name.");
                    errorLabel.setVisible(true);
                    okButton.setDisable(true);
                } else {
                    errorLabel.setVisible(false);
                    okButton.setDisable(newValue.trim().isEmpty());
                }
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return textField.getText();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @FXML
    private void createWatchlist() {
        Optional<String> result = showWatchlistDialog("New Watchlist", "Create a new watchlist", "", -1);
        
        result.ifPresent(name -> {
            String cleanName = name.trim();
            if (!cleanName.isEmpty()) {
                if (validateWatchlistName(cleanName, -1)) {
                    watchlistRepository.insert(new Watchlist(cleanName));
                    refresh();
                }
            }
        });
    }

    @FXML
    private void renameWatchlist() {
        if (selectedWatchlist == null) return;

        Optional<String> result = showWatchlistDialog("Rename Watchlist", "Rename watchlist: " + selectedWatchlist.getName(), selectedWatchlist.getName(), selectedWatchlist.getId() == null ? -1 : selectedWatchlist.getId());
        
        result.ifPresent(name -> {
            String cleanName = name.trim();
            if (!cleanName.isEmpty() && !cleanName.equals(selectedWatchlist.getName())) {
                if (validateWatchlistName(cleanName, selectedWatchlist.getId() == null ? -1 : selectedWatchlist.getId())) {
                    selectedWatchlist.setName(cleanName);
                    watchlistRepository.update(selectedWatchlist);
                    refresh();
                }
            }
        });
    }

    private boolean validateWatchlistName(String cleanName, int currentId) {
        List<Watchlist> watchlists = watchlistRepository.findAll();
        for (Watchlist w : watchlists) {
            if (w.getName().equalsIgnoreCase(cleanName)) {
                if (w.getId() != null && w.getId() != currentId) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
                    stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
                    alert.setTitle("Validation Error");
                    alert.setHeaderText("Duplicate Watchlist");
                    alert.setContentText("A watchlist with this name already exists. Please choose a different name.");
                    alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                    alert.showAndWait();
                    return false;
                }
            }
        }
        return true;
    }

    @FXML
    private void deleteWatchlist() {
        if (selectedWatchlist == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
        alert.setTitle("Delete Watchlist");
        alert.setHeaderText("Delete watchlist: " + selectedWatchlist.getName());
        alert.setContentText("Are you sure you want to delete this watchlist? Movies will not be deleted from your disk or library.");
        alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        
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

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
        alert.setTitle("Remove Movie");
        alert.setHeaderText("Remove from Watchlist");
        alert.setContentText("Are you sure you want to remove this movie from " + selectedWatchlist.getName() + "?");
        alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            watchlistRepository.removeMovieFromWatchlist(selectedWatchlist.getId(), selectedMovie.getId());
            refresh();
        }
    }

    private void playMovie(Movie movie) {
        try {
            AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to play movie from watchlist", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
            alert.setTitle("Playback Failed");
            alert.setHeaderText("Cannot open media player");
            alert.setContentText(e.getMessage());
            alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
    }
}
