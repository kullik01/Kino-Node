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
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.util.PathUtils;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for the Movie Detail View. Displays details of a {@link Movie} and allows editing the
 * metadata, rating, cover art, and watchlist association. Supports playing the movie via external
 * player and opening trailer links in a web browser.
 */
public class MovieDetailController {
  private static final Logger LOGGER = Logger.getLogger(MovieDetailController.class.getName());
  private static Image PLACEHOLDER_IMAGE;

  @FXML private Label lblHeaderTitle;
  @FXML private ImageView imgPoster;
  @FXML private Button btnPlay;
  @FXML private Button btnTrailer;

  @FXML private TextField txtTitle;
  @FXML private TextField txtYear;
  @FXML private TextField txtGenre;
  @FXML private TextField txtDirector;
  @FXML private TextField txtWriter;
  @FXML private TextField txtProducer;
  @FXML private TextField txtCinematographer;
  @FXML private TextField txtComposer;
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

  private javafx.animation.PauseTransition statusTimer;

  private void showStatus(String message) {
    lblStatus.setText(message);
    if (statusTimer != null) {
      statusTimer.stop();
    }
    // Automatically clear status labels after 5 seconds
    statusTimer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
    statusTimer.setOnFinished(e -> lblStatus.setText(""));
    statusTimer.play();
  }

  /** Initializes the controller. Sets up search/trailer binding listeners. */
  @FXML
  public void initialize() {
    movieRepository = AppContext.getInstance().getMovieRepository();
    watchlistRepository = AppContext.getInstance().getWatchlistRepository();

    // Listeners for trailer URL to dynamically disable/enable Watch Trailer button
    txtTrailerUrl
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              btnTrailer.setDisable(newVal == null || newVal.trim().isEmpty());
            });
  }

  /**
   * Binds movie details to all input fields and controls in the view. Registers a scene-property
   * listener to auto-save any changes when navigating away.
   *
   * @param movie the {@link Movie} instance to load
   */
  public void setMovie(Movie movie) {
    this.movie = movie;
    this.currentRating = movie.getRating();

    lblHeaderTitle.setText(movie.getTitle());
    txtTitle.setText(movie.getTitle());
    txtYear.setText(movie.getYear() != null ? String.valueOf(movie.getYear()) : "");
    txtGenre.setText(movie.getGenre() != null ? movie.getGenre() : "");
    txtDirector.setText(movie.getDirector() != null ? movie.getDirector() : "");
    txtWriter.setText(movie.getWriter() != null ? movie.getWriter() : "");
    txtProducer.setText(movie.getProducer() != null ? movie.getProducer() : "");
    txtCinematographer.setText(
        movie.getCinematographer() != null ? movie.getCinematographer() : "");
    txtComposer.setText(movie.getComposer() != null ? movie.getComposer() : "");
    txtDescription.setText(movie.getDescription() != null ? movie.getDescription() : "");
    txtTrailerUrl.setText(movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "");

    btnTrailer.setDisable(txtTrailerUrl.getText().trim().isEmpty());

    String filePath = movie.getFilePath();
    btnPlay.setDisable(
        filePath == null || filePath.trim().isEmpty() || !new File(filePath).exists());

    loadPoster();
    buildRatingWidget();
    refreshWatchlistStatus();

    // Auto-save when changing tabs (which removes the view from the scene)
    lblHeaderTitle
        .sceneProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (oldVal != null && newVal == null) {
                if (hasUnsavedChanges()) {
                  performSave();
                }
              }
            });
  }

  private void loadPoster() {
    String path = movie.getCoverImagePath();
    Image image = null;
    if (path != null && !path.isBlank()) {
      File f = new File(path);
      if (f.exists()) {
        try {
          image = new Image(f.toURI().toURL().toExternalForm(), 240, 360, false, true, true);
        } catch (MalformedURLException e) {
          LOGGER.log(Level.WARNING, "Failed to load poster in details: " + path, e);
        }
      }
    }

    if (image == null) {
      if (PLACEHOLDER_IMAGE == null) {
        try {
          var resource =
              getClass().getResource("/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
          if (resource != null) {
            PLACEHOLDER_IMAGE = new Image(resource.toExternalForm(), 240, 360, false, true, true);
          }
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Failed to load details placeholder cover", e);
        }
      }
      image = PLACEHOLDER_IMAGE;
    }

    if (image != null) {
      imgPoster.setImage(image);
    }
  }

  private void buildRatingWidget() {
    ratingContainer.getChildren().clear();

    // Clear Rating button (Reset to unrated)
    Button btnClear = new Button("❌");
    btnClear.setMinSize(
        javafx.scene.control.Button.USE_PREF_SIZE, javafx.scene.control.Button.USE_PREF_SIZE);
    btnClear.setStyle(
        "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2px; -fx-font-size: 11px;");
    Tooltip clearTooltip = new Tooltip("Clear rating (unrated)");
    clearTooltip.setShowDelay(javafx.util.Duration.millis(250));
    btnClear.setTooltip(clearTooltip);
    btnClear.setOnAction(
        e -> {
          currentRating = null;
          updateRatingUI();
        });
    ratingContainer.getChildren().add(btnClear);

    // 10 Movie Camera icons (1 to 10) representing the rating value
    List<Label> reels = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      final int score = i;
      Label reel = new Label("🎥");
      reel.setMinSize(
          javafx.scene.control.Label.USE_PREF_SIZE, javafx.scene.control.Label.USE_PREF_SIZE);
      reel.getStyleClass().add("rating-reel");
      reel.setStyle("-fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 0 1px;");
      Tooltip reelTooltip = new Tooltip("Rate " + score + "/10");
      reelTooltip.setShowDelay(javafx.util.Duration.millis(250));
      reel.setTooltip(reelTooltip);
      reel.setOnMouseClicked(
          e -> {
            currentRating = score;
            updateRatingUI();
          });
      reels.add(reel);
      ratingContainer.getChildren().add(reel);
    }

    // Numerical display label positioned at the end of the rating container
    Label lblRatingVal = new Label();
    lblRatingVal.setMinSize(
        javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
    lblRatingVal.setStyle(
        "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    ratingContainer.getChildren().add(lblRatingVal);

    updateRatingUI();
  }

  private void updateRatingUI() {
    // Get the numerical label node at the very end
    Label lblRatingVal =
        (Label) ratingContainer.getChildren().get(ratingContainer.getChildren().size() - 1);

    Button btnClear = (Button) ratingContainer.getChildren().get(0);

    if (currentRating == null) {
      btnClear.setVisible(false);
      // Gray out all film reels when unrated
      for (int i = 1; i <= 10; i++) {
        Label reel = (Label) ratingContainer.getChildren().get(i);
        reel.setStyle(
            "-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #444444; -fx-opacity: 0.3;");
      }
      lblRatingVal.setText("");
    } else {
      btnClear.setVisible(true);
      // Highlight rating reels up to the current selected rating level
      for (int i = 1; i <= 10; i++) {
        Label reel = (Label) ratingContainer.getChildren().get(i);
        if (i <= currentRating) {
          reel.setStyle(
              "-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #ED8B00; -fx-opacity: 1.0;");
        } else {
          reel.setStyle(
              "-fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: #444444; -fx-opacity: 0.3;");
        }
      }
      lblRatingVal.setText(currentRating + "/10");
      lblRatingVal.setStyle(
          "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    }
  }

  private void refreshWatchlistStatus() {
    List<Watchlist> all = watchlistRepository.findAll();
    List<String> inWatchlists =
        all.stream()
            .filter(w -> w.getMovies().stream().anyMatch(m -> m.getId().equals(movie.getId())))
            .map(Watchlist::getName)
            .collect(Collectors.toList());

    if (inWatchlists.isEmpty()) {
      lblWatchlistsList.setText("None");
    } else {
      lblWatchlistsList.setText(String.join(", ", inWatchlists));
    }

    // Rebuild Watchlists Context Menu
    menuWatchlists.getItems().clear();
    for (Watchlist w : all) {
      boolean alreadyIn = w.getMovies().stream().anyMatch(m -> m.getId().equals(movie.getId()));
      if (!alreadyIn) {
        MenuItem item = new MenuItem("Add to: " + w.getName());
        item.setOnAction(
            e -> {
              watchlistRepository.addMovieToWatchlist(w.getId(), movie.getId());
              refreshWatchlistStatus();
              showStatus("Added to watchlist " + w.getName());
            });
        menuWatchlists.getItems().add(item);
      } else {
        MenuItem item = new MenuItem("➖ Remove from: " + w.getName());
        item.setOnAction(
            e -> {
              // Confirm removal with warning dialog
              Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
              javafx.stage.Stage stage =
                  (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
              stage
                  .getIcons()
                  .add(
                      new javafx.scene.image.Image(
                          getClass()
                              .getResourceAsStream(
                                  "/io/github/kullik01/kinonode/images/logo.png")));
              alert.setTitle("Remove Movie");
              alert.setHeaderText("Remove from Watchlist");
              alert.setContentText(
                  "Are you sure you want to remove this movie from " + w.getName() + "?");

              javafx.scene.image.ImageView customIcon =
                  new javafx.scene.image.ImageView(
                      new javafx.scene.image.Image(
                          getClass()
                              .getResourceAsStream(
                                  "/io/github/kullik01/kinonode/images/removal_icon.png")));
              customIcon.setFitHeight(48);
              customIcon.setFitWidth(48);
              alert.getDialogPane().setGraphic(customIcon);

              alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
              Optional<ButtonType> alertResult = alert.showAndWait();
              if (alertResult.isPresent() && alertResult.get() == ButtonType.OK) {
                watchlistRepository.removeMovieFromWatchlist(w.getId(), movie.getId());
                refreshWatchlistStatus();
                showStatus("Removed from watchlist " + w.getName());
              }
            });
        menuWatchlists.getItems().add(item);
      }
    }

    // Option to create new watchlist on the fly
    MenuItem createNew = new MenuItem("➕ Create New Watchlist...");
    createNew.setOnAction(
        e -> {
          Optional<String> result =
              showWatchlistDialog("New Watchlist", "Create a new watchlist", "", -1);
          result.ifPresent(
              name -> {
                String cleanName = name.trim();
                if (!cleanName.isEmpty()) {
                  if (validateWatchlistName(cleanName, -1)) {
                    Watchlist nw = watchlistRepository.insert(new Watchlist(cleanName));
                    if (nw.getId() != null) {
                      watchlistRepository.addMovieToWatchlist(nw.getId(), movie.getId());
                      refreshWatchlistStatus();
                      showStatus("Created watchlist and added movie.");
                    }
                  }
                }
              });
        });
    menuWatchlists.getItems().add(createNew);
  }

  private Optional<String> showWatchlistDialog(
      String title, String header, String defaultText, int currentId) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(header);

    dialog.getDialogPane().setPrefSize(480, 280);
    dialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
    stage
        .getIcons()
        .add(
            new javafx.scene.image.Image(
                getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

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

    textField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue.length() > 30) {
                textField.setText(oldValue);
                return;
              }
              // Enforce ASCII-only characters in watchlist names
              if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
                errorLabel.setText(
                    "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
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
                  errorLabel.setText(
                      "A watchlist with this name already exists. Please choose a different name.");
                  errorLabel.setVisible(true);
                  okButton.setDisable(true);
                } else {
                  errorLabel.setVisible(false);
                  okButton.setDisable(newValue.trim().isEmpty());
                }
              }
            });

    dialog.setResultConverter(
        dialogButton -> {
          if (dialogButton == okButtonType) {
            return textField.getText();
          }
          return null;
        });

    return dialog.showAndWait();
  }

  private boolean validateWatchlistName(String cleanName, int currentId) {
    List<Watchlist> watchlists = watchlistRepository.findAll();
    for (Watchlist w : watchlists) {
      if (w.getName().equalsIgnoreCase(cleanName)) {
        if (w.getId() != null && w.getId() != currentId) {
          Alert alert = new Alert(Alert.AlertType.ERROR);
          javafx.stage.Stage stage =
              (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
          stage
              .getIcons()
              .add(
                  new javafx.scene.image.Image(
                      getClass()
                          .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
          alert.setTitle("Validation Error");
          alert.setHeaderText("Duplicate Watchlist");
          alert.setContentText(
              "A watchlist with this name already exists. Please choose a different name.");

          javafx.scene.image.ImageView customIcon =
              new javafx.scene.image.ImageView(
                  new javafx.scene.image.Image(
                      getClass()
                          .getResourceAsStream(
                              "/io/github/kullik01/kinonode/images/error_icon.png")));
          customIcon.setFitHeight(48);
          customIcon.setFitWidth(48);
          alert.getDialogPane().setGraphic(customIcon);

          alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
          alert.showAndWait();
          return false;
        }
      }
    }
    return true;
  }

  @FXML
  private void changePoster() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Choose Poster Image");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

    File selected = chooser.showOpenDialog(imgPoster.getScene().getWindow());
    if (selected != null) {
      try {
        Path dbPath = AppContext.getInstance().getDatabaseService().getDbPath();
        Path coversDir = dbPath.getParent().resolve("covers");
        // Ensure covers folder directory is present beside the database file
        if (!Files.exists(coversDir)) {
          Files.createDirectories(coversDir);
        }

        String ext = PathUtils.getFileExtension(selected);
        if (ext.isEmpty()) ext = "jpg";

        // Generate unique filename with timestamp to bypass cover art browser caching issues
        String filename = "cover_" + movie.getId() + "_" + System.currentTimeMillis() + "." + ext;
        Path destPath = coversDir.resolve(filename);

        Files.copy(selected.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

        String newPath = destPath.toAbsolutePath().toString().replace("\\", "/");
        movie.setCoverImagePath(newPath);

        // Reload image directly in details screen
        imgPoster.setImage(
            new Image(destPath.toUri().toURL().toExternalForm(), 240, 360, false, true, true));
        showStatus("Poster image updated.");
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to copy cover poster file", e);
        showStatus("Error saving poster: " + e.getMessage());
      }
    }
  }

  @FXML
  private void playMovie() {
    try {
      // Trigger PlaybackService which handles configuring and executing external media player
      // process
      AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
      showStatus("Opening external player...");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to launch external media player", e);
      Alert alert = new Alert(Alert.AlertType.ERROR);
      javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
      stage
          .getIcons()
          .add(
              new javafx.scene.image.Image(
                  getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
      alert.setTitle("Playback Failed");
      alert.setHeaderText("Could not launch external media player");
      alert.setContentText(e.getMessage());

      javafx.scene.image.ImageView customIcon =
          new javafx.scene.image.ImageView(
              new javafx.scene.image.Image(
                  getClass()
                      .getResourceAsStream("/io/github/kullik01/kinonode/images/error_icon.png")));
      customIcon.setFitHeight(48);
      customIcon.setFitWidth(48);
      alert.getDialogPane().setGraphic(customIcon);

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
      // Open default web browser pointing to trailer link
      if (java.awt.Desktop.isDesktopSupported()
          && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
        java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        showStatus("Opening trailer link in browser...");
      } else {
        showStatus("System web browser action not supported on this platform.");
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to open browser trailer link", e);
      showStatus("Failed to open browser.");
    }
  }

  private enum InputConstraint {
    ANY,
    NUMBERS_ONLY,
    NO_NUMBERS
  }

  private void showEditDialog(
      String title,
      String fieldName,
      TextInputControl control,
      InputConstraint constraint,
      java.util.function.Consumer<String> updater) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Edit " + title);
    dialog.setHeaderText("Edit " + fieldName);

    dialog.getDialogPane().setPrefSize(480, 280);
    dialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
    stage
        .getIcons()
        .add(
            new javafx.scene.image.Image(
                getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    javafx.scene.image.ImageView customIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(
                getClass()
                    .getResourceAsStream("/io/github/kullik01/kinonode/images/question_mark.png")));
    customIcon.setFitHeight(48);
    customIcon.setFitWidth(48);
    dialog.getDialogPane().setGraphic(customIcon);

    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

    VBox vbox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String defaultText = control.getText() != null ? control.getText().trim() : "";

    if (!defaultText.isEmpty()) {
      errorLabel.setText("This is the current " + fieldName + " and must be updated by a new one.");
      errorLabel.setVisible(true);
    } else {
      errorLabel.setVisible(false);
    }

    TextInputControl inputField;
    if (control instanceof TextArea) {
      TextArea ta = new TextArea(defaultText);
      ta.setWrapText(true);
      ta.setPrefHeight(100);
      inputField = ta;
    } else {
      inputField = new TextField(defaultText);
    }

    // Set formatting constraints on user input
    if (constraint == InputConstraint.NUMBERS_ONLY) {
      inputField.setTextFormatter(
          new TextFormatter<>(
              change -> {
                if (change.getText().matches("\\d*")) {
                  return change;
                }
                return null;
              }));
    } else if (constraint == InputConstraint.NO_NUMBERS) {
      inputField.setTextFormatter(
          new TextFormatter<>(
              change -> {
                if (change.getText().matches("[^0-9]*")) {
                  return change;
                }
                return null;
              }));
    }

    vbox.getChildren()
        .addAll(new Label("Enter the new " + fieldName + ":"), errorLabel, inputField);
    dialog.getDialogPane().setContent(vbox);

    javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
    okButton.setDisable(true);

    inputField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              String val = newValue.trim();
              boolean isUnchanged = val.equals(defaultText);

              // Require input content to be ASCII only
              if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
                errorLabel.setText(
                    "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
              } else if (isUnchanged) {
                if (!defaultText.isEmpty()) {
                  errorLabel.setText(
                      "This is the current " + fieldName + " and must be updated by a new one.");
                  errorLabel.setVisible(true);
                }
                okButton.setDisable(true);
              } else {
                errorLabel.setVisible(false);
                okButton.setDisable(false);
              }
            });

    dialog.setResultConverter(
        dialogButton -> {
          if (dialogButton == okButtonType) {
            return inputField.getText();
          }
          return null;
        });

    java.util.Optional<String> result = dialog.showAndWait();
    result.ifPresent(
        val -> {
          String newVal = val.trim();
          control.setText(newVal);
          updater.accept(newVal);
          movieRepository.update(movie);
          showStatus(title + " updated!");
        });
  }

  @FXML
  private void editTitle() {
    showEditDialog(
        "Title",
        "Movie Title",
        txtTitle,
        InputConstraint.ANY,
        val -> movie.setTitle(val.isEmpty() ? null : val));
  }

  @FXML
  private void editYear() {
    showEditDialog(
        "Year",
        "Release Year",
        txtYear,
        InputConstraint.NUMBERS_ONLY,
        val -> {
          try {
            movie.setYear(val.isEmpty() ? null : Integer.parseInt(val));
          } catch (NumberFormatException e) {
            showStatus("Year must be a number!");
          }
        });
  }

  @FXML
  private void editGenre() {
    showEditDialog(
        "Genre",
        "Genre",
        txtGenre,
        InputConstraint.NO_NUMBERS,
        val -> movie.setGenre(val.isEmpty() ? null : val));
  }

  @FXML
  private void editDirector() {
    showEditDialog(
        "Director",
        "Director(s)",
        txtDirector,
        InputConstraint.NO_NUMBERS,
        val -> movie.setDirector(val.isEmpty() ? null : val));
  }

  @FXML
  private void editWriter() {
    showEditDialog(
        "Writer",
        "Writer(s)",
        txtWriter,
        InputConstraint.NO_NUMBERS,
        val -> movie.setWriter(val.isEmpty() ? null : val));
  }

  @FXML
  private void editProducer() {
    showEditDialog(
        "Producer",
        "Producer(s)",
        txtProducer,
        InputConstraint.NO_NUMBERS,
        val -> movie.setProducer(val.isEmpty() ? null : val));
  }

  @FXML
  private void editCinematographer() {
    showEditDialog(
        "Cinematographer",
        "Cinematographer(s)",
        txtCinematographer,
        InputConstraint.NO_NUMBERS,
        val -> movie.setCinematographer(val.isEmpty() ? null : val));
  }

  @FXML
  private void editComposer() {
    showEditDialog(
        "Composer",
        "Composer(s)",
        txtComposer,
        InputConstraint.NO_NUMBERS,
        val -> movie.setComposer(val.isEmpty() ? null : val));
  }

  @FXML
  private void editDescription() {
    showEditDialog(
        "Description",
        "Description",
        txtDescription,
        InputConstraint.NO_NUMBERS,
        val -> movie.setDescription(val.isEmpty() ? null : val));
  }

  @FXML
  private void editTrailerUrl() {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Edit Trailer Link");
    dialog.setHeaderText("YouTube Trailer URL");

    dialog.getDialogPane().setPrefSize(480, 280);
    dialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
    stage
        .getIcons()
        .add(
            new javafx.scene.image.Image(
                getClass().getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    javafx.scene.image.ImageView customIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(
                getClass()
                    .getResourceAsStream("/io/github/kullik01/kinonode/images/question_mark.png")));
    customIcon.setFitHeight(48);
    customIcon.setFitWidth(48);
    dialog.getDialogPane().setGraphic(customIcon);

    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

    VBox vbox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String defaultText = txtTrailerUrl.getText().trim();

    if (!defaultText.isEmpty()) {
      errorLabel.setText("This is the current YouTube link and must be updated by a new one.");
      errorLabel.setVisible(true);
    } else {
      errorLabel.setVisible(false);
    }

    TextField textField = new TextField(defaultText);
    textField.setPromptText("https://www.youtube.com/watch?v=...");

    vbox.getChildren()
        .addAll(new Label("Enter the new YouTube Trailer Link:"), errorLabel, textField);
    dialog.getDialogPane().setContent(vbox);

    javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
    okButton.setDisable(true);

    textField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              String url = newValue.trim();
              boolean isUnchanged = url.equalsIgnoreCase(defaultText);

              if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
                errorLabel.setText(
                    "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
              } else if (isUnchanged && !defaultText.isEmpty()) {
                errorLabel.setText(
                    "This is the current YouTube link and must be updated by a new one.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
              } else if (!url.isEmpty()
                  && !url.contains("youtube.com")
                  && !url.contains("youtu.be")) {
                errorLabel.setText("Trailer Link must be a valid YouTube URL.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
              } else {
                errorLabel.setVisible(false);
                okButton.setDisable(url.isEmpty());
              }
            });

    dialog.setResultConverter(
        dialogButton -> {
          if (dialogButton == okButtonType) {
            return textField.getText();
          }
          return null;
        });

    java.util.Optional<String> result = dialog.showAndWait();
    result.ifPresent(
        url -> {
          String newUrl = url.trim();
          txtTrailerUrl.setText(newUrl);
          movie.setTrailerUrl(newUrl.isEmpty() ? null : newUrl);
          movieRepository.update(movie);
          showStatus("Trailer Link updated!");
        });
  }

  /**
   * Evaluates if any of the UI input fields have unsaved modifications compared to the database.
   *
   * @return {@code true} if unsaved changes exist; {@code false} otherwise
   */
  public boolean hasUnsavedChanges() {
    if (movie == null) return false;
    if (!java.util.Objects.equals(movie.getTitle(), txtTitle.getText().trim())) return true;

    String movieYear = movie.getYear() != null ? String.valueOf(movie.getYear()) : "";
    if (!java.util.Objects.equals(movieYear, txtYear.getText().trim())) return true;

    String movieGenre = movie.getGenre() != null ? movie.getGenre() : "";
    if (!java.util.Objects.equals(movieGenre, txtGenre.getText().trim())) return true;

    String movieDirector = movie.getDirector() != null ? movie.getDirector() : "";
    if (!java.util.Objects.equals(movieDirector, txtDirector.getText().trim())) return true;

    String movieWriter = movie.getWriter() != null ? movie.getWriter() : "";
    if (!java.util.Objects.equals(movieWriter, txtWriter.getText().trim())) return true;

    String movieProducer = movie.getProducer() != null ? movie.getProducer() : "";
    if (!java.util.Objects.equals(movieProducer, txtProducer.getText().trim())) return true;

    String movieCinematographer =
        movie.getCinematographer() != null ? movie.getCinematographer() : "";
    if (!java.util.Objects.equals(movieCinematographer, txtCinematographer.getText().trim()))
      return true;

    String movieComposer = movie.getComposer() != null ? movie.getComposer() : "";
    if (!java.util.Objects.equals(movieComposer, txtComposer.getText().trim())) return true;

    String movieDesc = movie.getDescription() != null ? movie.getDescription() : "";
    if (!java.util.Objects.equals(movieDesc, txtDescription.getText().trim())) return true;

    String movieTrailer = movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "";
    if (!java.util.Objects.equals(movieTrailer, txtTrailerUrl.getText().trim())) return true;

    if (!java.util.Objects.equals(movie.getRating(), currentRating)) return true;

    return false;
  }

  /**
   * Performs validation and persists all modified fields to the database. Prints feedback status to
   * the status label.
   *
   * @return {@code true} if save is completed successfully; {@code false} if validation failed
   */
  public boolean performSave() {
    String title = txtTitle.getText().trim();
    if (title.isEmpty()) {
      showStatus("Title cannot be empty.");
      return false;
    }

    Integer year = null;
    if (!txtYear.getText().trim().isEmpty()) {
      try {
        year = Integer.parseInt(txtYear.getText().trim());
      } catch (NumberFormatException e) {
        showStatus("Release year must be a valid integer number.");
        return false;
      }
    }

    String trailer = txtTrailerUrl.getText().trim();
    if (!trailer.isEmpty()) {
      if (!trailer.contains("youtube.com") && !trailer.contains("youtu.be")) {
        showStatus("Trailer Link must be a valid YouTube URL.");
        return false;
      }
    }

    movie.setTitle(title);
    movie.setYear(year);
    movie.setGenre(txtGenre.getText().trim().isEmpty() ? null : txtGenre.getText().trim());
    movie.setDirector(txtDirector.getText().trim().isEmpty() ? null : txtDirector.getText().trim());
    movie.setWriter(txtWriter.getText().trim().isEmpty() ? null : txtWriter.getText().trim());
    movie.setProducer(txtProducer.getText().trim().isEmpty() ? null : txtProducer.getText().trim());
    movie.setCinematographer(
        txtCinematographer.getText().trim().isEmpty() ? null : txtCinematographer.getText().trim());
    movie.setComposer(txtComposer.getText().trim().isEmpty() ? null : txtComposer.getText().trim());
    movie.setDescription(
        txtDescription.getText().trim().isEmpty() ? null : txtDescription.getText().trim());
    movie.setTrailerUrl(trailer.isEmpty() ? null : trailer);
    movie.setRating(currentRating);

    movieRepository.update(movie);
    showStatus("Movie details saved successfully.");
    return true;
  }

  @FXML
  private void goBack() {
    if (hasUnsavedChanges()) {
      performSave();
    }
    executeBack();
  }

  private Runnable onBack;

  /**
   * Sets the callback routine to execute when backing out of the details view.
   *
   * @param onBack the back-action {@link Runnable}
   */
  public void setOnBack(Runnable onBack) {
    this.onBack = onBack;
  }

  private void executeBack() {
    if (onBack != null) {
      onBack.run();
    } else {
      // Fallback just in case
      try {
        URL url = getClass().getResource("/io/github/kullik01/kinonode/fxml/LibraryView.fxml");
        FXMLLoader loader = new FXMLLoader(url);
        Parent libraryView = loader.load();

        StackPane contentArea = (StackPane) lblHeaderTitle.getScene().lookup("#contentArea");
        if (contentArea != null) {
          contentArea.getChildren().setAll(libraryView);
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to load LibraryView FXML", e);
      }
    }
  }
}
