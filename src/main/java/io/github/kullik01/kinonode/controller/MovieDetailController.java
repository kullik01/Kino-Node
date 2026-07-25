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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

/**
 * Controller for the Movie Detail View. Displays details of a {@link Movie} and
 * allows editing the metadata, rating, cover art, and watchlist association.
 * Supports playing the movie via external player and opening trailer links in a
 * web browser.
 */
public class MovieDetailController {
  private static final Logger LOGGER =
      Logger.getLogger(MovieDetailController.class.getName());
  private static Image PLACEHOLDER_IMAGE;

  @FXML
  private Label lblHeaderTitle;
  @FXML
  private ImageView imgPoster;
  @FXML
  private Button btnPlay;
  @FXML
  private Button btnTrailer;
  @FXML
  private Button btnImdbPage;

  @FXML
  private TextField txtTitle;
  @FXML
  private TextField txtYear;
  @FXML
  private TextField txtDuration;
  @FXML
  private TextField txtGenre;
  @FXML
  private TextField txtDirector;
  @FXML
  private TextField txtWriter;
  @FXML
  private TextField txtProducer;
  @FXML
  private TextField txtCinematographer;
  @FXML
  private TextField txtComposer;
  @FXML
  private TextField txtActors;
  @FXML
  private ComboBox<String> comboDirector;
  @FXML
  private ComboBox<String> comboWriter;
  @FXML
  private ComboBox<String> comboProducer;
  @FXML
  private ComboBox<String> comboCinematographer;
  @FXML
  private ComboBox<String> comboComposer;
  @FXML
  private ComboBox<String> comboActors;
  @FXML
  private TextArea txtDescription;
  @FXML
  private TextField txtTrailerUrl;

  @FXML
  private FlowPane ratingContainer;
  @FXML
  private Label lblWatchlistsList;
  @FXML
  private MenuButton menuWatchlists;
  @FXML
  private Label lblStatus;

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
    statusTimer =
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
    statusTimer.setOnFinished(e -> lblStatus.setText(""));
    statusTimer.play();
  }

  /** Initializes the controller. Sets up search/trailer binding listeners. */
  @FXML
  public void initialize() {
    movieRepository = AppContext.getInstance().getMovieRepository();
    watchlistRepository = AppContext.getInstance().getWatchlistRepository();

    // Listeners for trailer URL to dynamically disable/enable Watch Trailer
    // button
    txtTrailerUrl.textProperty().addListener((obs, oldVal, newVal) -> {
      btnTrailer.setDisable(newVal == null || newVal.trim().isEmpty());
    });
  }

  /**
   * Binds movie details to all input fields and controls in the view. Registers
   * a scene-property listener to auto-save any changes when navigating away.
   *
   * @param movie the {@link Movie} instance to load
   */
  public void setMovie(Movie movie) {
    this.movie = movie;
    this.currentRating = movie.getRating();

    lblHeaderTitle.setText(movie.getTitle());
    txtTitle.setText(movie.getTitle());
    txtYear.setText(
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "");
    if (movie.getDuration() != null && movie.getDuration() > 0) {
      txtDuration.setText(movie.getFormattedDuration());
      txtDuration.setStyle("-fx-text-fill: -color-fg-default;");
    } else {
      txtDuration.setText("Not specified (Click 📝 to set duration)");
      txtDuration.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
    }
    txtGenre.setText(movie.getGenre() != null ? movie.getGenre() : "");
    setupMultiField(movie.getDirector(), txtDirector, comboDirector);
    setupMultiField(movie.getWriter(), txtWriter, comboWriter);
    setupMultiField(movie.getProducer(), txtProducer, comboProducer);
    setupMultiField(movie.getCinematographer(), txtCinematographer,
        comboCinematographer);
    setupMultiField(movie.getComposer(), txtComposer, comboComposer);
    setupMultiField(movie.getActors(), txtActors, comboActors);
    txtDescription
        .setText(movie.getDescription() != null ? movie.getDescription() : "");
    txtTrailerUrl
        .setText(movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "");

    btnTrailer.setDisable(txtTrailerUrl.getText().trim().isEmpty());

    String tmpFilePath = movie.getFilePath();
    btnPlay.setDisable(tmpFilePath == null || tmpFilePath.trim().isEmpty()
        || !new File(tmpFilePath).exists());

    loadPoster();
    buildRatingWidget();
    refreshWatchlistStatus();

    // Auto-save when changing tabs (which removes the view from the scene)
    lblHeaderTitle.sceneProperty().addListener((obs, oldVal, newVal) -> {
      if (oldVal != null && newVal == null) {
        if (hasUnsavedChanges()) {
          performSave();
        }
      }
    });
  }

  private void loadPoster() {
    String tmpPath = movie.getCoverImagePath();
    Image tmpImage = null;
    if (tmpPath != null && !tmpPath.isBlank()) {
      File tmpFile = new File(tmpPath);
      if (tmpFile.exists()) {
        try {
          tmpImage = new Image(tmpFile.toURI().toURL().toExternalForm(), 240,
              360, false, true, true);
        } catch (MalformedURLException tmpException) {
          LOGGER.log(Level.WARNING,
              "Failed to load poster in details: " + tmpPath, tmpException);
        }
      }
    }

    if (tmpImage == null) {
      if (PLACEHOLDER_IMAGE == null) {
        try {
          var tmpResource = getClass().getResource(
              "/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
          if (tmpResource != null) {
            PLACEHOLDER_IMAGE = new Image(tmpResource.toExternalForm(), 240,
                360, false, true, true);
          }
        } catch (Exception tmpException) {
          LOGGER.log(Level.WARNING, "Failed to load details placeholder cover",
              tmpException);
        }
      }
      tmpImage = PLACEHOLDER_IMAGE;
    }

    if (tmpImage != null) {
      imgPoster.setImage(tmpImage);
    }
  }

  private SVGPath createCameraSvg() {
    SVGPath tmpIcon = new SVGPath();
    tmpIcon.setContent(
        "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z");
    tmpIcon.setScaleX(0.75);
    tmpIcon.setScaleY(0.75);
    tmpIcon.setPickOnBounds(true);
    return tmpIcon;
  }

  private void setReelState(Label reelLabel, boolean active) {
    SVGPath tmpIcon = (SVGPath) reelLabel.getGraphic();
    if (tmpIcon != null) {
      if (active) {
        tmpIcon.setStyle(
            "-fx-fill: #ED8B00; -fx-opacity: 1.0; -fx-effect: dropshadow(three-pass-box, rgba(237, 139, 0, 0.4), 6, 0, 0, 0);");
      } else {
        tmpIcon.setStyle(
            "-fx-fill: #666666; -fx-opacity: 0.35; -fx-effect: none;");
      }
    }
  }

  private int detectHoveredScore(double mouseX) {
    for (int tmpIndex = 0; tmpIndex < 10
        && tmpIndex < ratingContainer.getChildren().size(); tmpIndex++) {
      javafx.scene.Node tmpChild = ratingContainer.getChildren().get(tmpIndex);
      double tmpMinX = tmpChild.getBoundsInParent().getMinX();
      double tmpMaxX = tmpChild.getBoundsInParent().getMaxX();
      if (mouseX >= tmpMinX && mouseX <= tmpMaxX) {
        return tmpIndex + 1;
      }
    }
    if (ratingContainer.getChildren().size() >= 10) {
      double tmpLastMaxX = ratingContainer.getChildren().get(9)
          .getBoundsInParent().getMaxX();
      if (mouseX > tmpLastMaxX) {
        return 10;
      }
    }
    return 1;
  }

  private void buildRatingWidget() {
    ratingContainer.getChildren().clear();

    // 10 Movie Camera icons (1 to 10) representing the rating value
    List<Label> tmpReels = new ArrayList<>();
    for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
      final int tmpScore = tmpIndex;
      Label tmpReel = new Label();
      tmpReel.setPickOnBounds(true);
      tmpReel.setGraphic(createCameraSvg());
      tmpReel.setMinSize(28.0, 28.0);
      tmpReel.setPrefSize(28.0, 28.0);
      tmpReel.setMaxSize(28.0, 28.0);
      tmpReel.setAlignment(javafx.geometry.Pos.CENTER);
      tmpReel.getStyleClass().add("rating-reel");
      tmpReel.setStyle("-fx-padding: 0;");
      Tooltip tmpReelTooltip = new Tooltip("Rate " + tmpScore + "/10");
      tmpReelTooltip.setShowDelay(javafx.util.Duration.millis(250));
      tmpReel.setTooltip(tmpReelTooltip);

      Runnable applyRating = () -> {
        if (currentRating != null && currentRating == tmpScore) {
          currentRating = null; // Toggle off to unrated
        } else {
          currentRating = tmpScore;
        }
        if (movie != null) {
          movie.setRating(currentRating);
          movieRepository.update(movie);
        }
        updateRatingUI();
      };

      tmpReel.setOnMousePressed(e -> applyRating.run());
      tmpReel.setOnMouseClicked(e -> applyRating.run());
      tmpReel.setOnMouseEntered(e -> previewRatingHover(tmpScore));
      tmpReel.setOnMouseMoved(e -> previewRatingHover(tmpScore));
      tmpReels.add(tmpReel);
      ratingContainer.getChildren().add(tmpReel);
    }

    ratingContainer.setOnMouseMoved(e -> {
      int score = detectHoveredScore(e.getX());
      previewRatingHover(score);
    });

    ratingContainer.setOnMouseDragged(e -> {
      int score = detectHoveredScore(e.getX());
      previewRatingHover(score);
    });

    ratingContainer.setOnMouseReleased(e -> {
      if (e.getX() >= 0 && e.getX() <= 320) {
        int score = detectHoveredScore(e.getX());
        if (currentRating != null && currentRating == score) {
          currentRating = null;
        } else {
          currentRating = score;
        }
        if (movie != null) {
          movie.setRating(currentRating);
          movieRepository.update(movie);
        }
        updateRatingUI();
      }
    });

    ratingContainer.setOnMouseExited(e -> updateRatingUI());

    // Numerical display label positioned at the end of the rating container
    Label tmpLblRatingVal = new Label();
    tmpLblRatingVal.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE,
        javafx.scene.layout.Region.USE_PREF_SIZE);
    tmpLblRatingVal.setStyle(
        "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    ratingContainer.getChildren().add(tmpLblRatingVal);

    updateRatingUI();
  }

  private void previewRatingHover(int hoverScore) {
    Label tmpLblRatingVal = (Label) ratingContainer.getChildren()
        .get(ratingContainer.getChildren().size() - 1);
    for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
      Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
      setReelState(tmpReel, tmpIndex <= hoverScore);
    }
    tmpLblRatingVal.setText(hoverScore + "/10");
    tmpLblRatingVal.setStyle(
        "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
  }

  private void updateRatingUI() {
    Label tmpLblRatingVal = (Label) ratingContainer.getChildren()
        .get(ratingContainer.getChildren().size() - 1);

    if (currentRating == null) {
      for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
        Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
        setReelState(tmpReel, false);
      }
      tmpLblRatingVal.setText("Unrated");
      tmpLblRatingVal.setStyle(
          "-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-padding: 0 0 0 10px;");
    } else {
      for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
        Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
        setReelState(tmpReel, tmpIndex <= currentRating);
      }
      tmpLblRatingVal.setText(currentRating + "/10");
      tmpLblRatingVal.setStyle(
          "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    }
  }

  private void refreshWatchlistStatus() {
    List<Watchlist> tmpAll = watchlistRepository.findAll();
    List<String> tmpInWatchlists = tmpAll.stream()
        .filter(w -> w.getMovies().stream()
            .anyMatch(m -> m.getId().equals(movie.getId())))
        .map(Watchlist::getName).collect(Collectors.toList());

    if (tmpInWatchlists.isEmpty()) {
      lblWatchlistsList.setText("None");
    } else {
      lblWatchlistsList.setText(String.join(", ", tmpInWatchlists));
    }

    // Rebuild Watchlists Context Menu
    menuWatchlists.getItems().clear();
    for (Watchlist tmpW : tmpAll) {
      boolean tmpAlreadyIn = tmpW.getMovies().stream()
          .anyMatch(m -> m.getId().equals(movie.getId()));
      if (!tmpAlreadyIn) {
        MenuItem tmpItem = new MenuItem("Add to: " + tmpW.getName());
        tmpItem.setOnAction(e -> {
          watchlistRepository.addMovieToWatchlist(tmpW.getId(), movie.getId());
          refreshWatchlistStatus();
          showStatus("Added to watchlist " + tmpW.getName());
        });
        menuWatchlists.getItems().add(tmpItem);
      } else {
        MenuItem tmpItem = new MenuItem("➖ Remove from: " + tmpW.getName());
        tmpItem.setOnAction(e -> {
          // Confirm removal with warning dialog
          Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
          javafx.stage.Stage tmpStage = (javafx.stage.Stage) tmpAlert
              .getDialogPane().getScene().getWindow();
          tmpStage.getIcons()
              .add(new javafx.scene.image.Image(getClass().getResourceAsStream(
                  "/io/github/kullik01/kinonode/images/logo.png")));
          tmpAlert.setTitle("Remove Movie");
          tmpAlert.setHeaderText("Remove from Watchlist");
          tmpAlert
              .setContentText("Are you sure you want to remove this movie from "
                  + tmpW.getName() + "?");

          javafx.scene.image.ImageView tmpCustomIcon =
              new javafx.scene.image.ImageView(
                  new javafx.scene.image.Image(getClass().getResourceAsStream(
                      "/io/github/kullik01/kinonode/images/removal_icon.png")));
          tmpCustomIcon.setFitHeight(48);
          tmpCustomIcon.setFitWidth(48);
          tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

          tmpAlert.getDialogPane()
              .setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
          io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpAlert);
          Optional<ButtonType> tmpAlertResult = tmpAlert.showAndWait();
          if (tmpAlertResult.isPresent()
              && tmpAlertResult.get() == ButtonType.OK) {
            watchlistRepository.removeMovieFromWatchlist(tmpW.getId(),
                movie.getId());
            refreshWatchlistStatus();
            showStatus("Removed from watchlist " + tmpW.getName());
          }
        });
        menuWatchlists.getItems().add(tmpItem);
      }
    }

    // Option to create new watchlist on the fly
    MenuItem tmpCreateNew = new MenuItem("➕ Create New Watchlist...");
    tmpCreateNew.setOnAction(e -> {
      Optional<String> tmpResult = showWatchlistDialog("New Watchlist",
          "Create a new watchlist", "", -1);
      tmpResult.ifPresent(name -> {
        String tmpCleanName = name.trim();
        if (!tmpCleanName.isEmpty()) {
          if (validateWatchlistName(tmpCleanName, -1)) {
            Watchlist tmpNw =
                watchlistRepository.insert(new Watchlist(tmpCleanName));
            if (tmpNw.getId() != null) {
              watchlistRepository.addMovieToWatchlist(tmpNw.getId(),
                  movie.getId());
              refreshWatchlistStatus();
              showStatus("Created watchlist and added movie.");
            }
          }
        }
      });
    });
    menuWatchlists.getItems().add(tmpCreateNew);
  }

  private Optional<String> showWatchlistDialog(String title, String header,
      String defaultText, int currentId) {
    Dialog<String> tmpDialog = new Dialog<>();
    tmpDialog.setTitle(title);
    tmpDialog.setHeaderText(header);

    tmpDialog.getDialogPane().setPrefSize(480, 280);
    tmpDialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage tmpStage =
        (javafx.stage.Stage) tmpDialog.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    ButtonType tmpOkButtonType =
        new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    tmpDialog.getDialogPane().getButtonTypes().addAll(tmpOkButtonType,
        ButtonType.CANCEL);

    VBox tmpVbox = new VBox(5);
    Label tmpErrorLabel = new Label();
    tmpErrorLabel.setWrapText(true);
    tmpErrorLabel.setPrefWidth(430);
    tmpErrorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    tmpErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    tmpErrorLabel.setVisible(false);

    TextField tmpTextField = new TextField(defaultText);
    tmpTextField.setPromptText("Watchlist Name");

    tmpVbox.getChildren().addAll(tmpErrorLabel, tmpTextField);
    tmpDialog.getDialogPane().setContent(tmpVbox);

    javafx.scene.Node tmpOkButton =
        tmpDialog.getDialogPane().lookupButton(tmpOkButtonType);
    tmpOkButton
        .setDisable(defaultText.trim().isEmpty() || defaultText.length() > 30);

    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.length() > 30) {
            tmpTextField.setText(oldValue);
            return;
          }
          // Enforce ASCII-only characters in watchlist names
          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpErrorLabel.setText(
                "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else {
            boolean tmpDuplicate = false;
            List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
            for (Watchlist tmpW : tmpWatchlists) {
              if (tmpW.getName().equalsIgnoreCase(newValue.trim())) {
                if (tmpW.getId() != null && tmpW.getId() != currentId) {
                  tmpDuplicate = true;
                  break;
                }
              }
            }

            if (tmpDuplicate) {
              tmpErrorLabel.setText(
                  "A watchlist with this name already exists. Please choose a different name.");
              tmpErrorLabel.setVisible(true);
              tmpOkButton.setDisable(true);
            } else {
              tmpErrorLabel.setVisible(false);
              tmpOkButton.setDisable(newValue.trim().isEmpty());
            }
          }
        });

    tmpDialog.setResultConverter(dialogButton -> {
      if (dialogButton == tmpOkButtonType) {
        return tmpTextField.getText();
      }
      return null;
    });

    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpDialog);
    return tmpDialog.showAndWait();
  }

  private boolean validateWatchlistName(String cleanName, int currentId) {
    List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
    for (Watchlist tmpW : tmpWatchlists) {
      if (tmpW.getName().equalsIgnoreCase(cleanName)) {
        if (tmpW.getId() != null && tmpW.getId() != currentId) {
          Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
          javafx.stage.Stage tmpStage = (javafx.stage.Stage) tmpAlert
              .getDialogPane().getScene().getWindow();
          tmpStage.getIcons()
              .add(new javafx.scene.image.Image(getClass().getResourceAsStream(
                  "/io/github/kullik01/kinonode/images/logo.png")));
          tmpAlert.setTitle("Validation Error");
          tmpAlert.setHeaderText("Duplicate Watchlist");
          tmpAlert.setContentText(
              "A watchlist with this name already exists. Please choose a different name.");

          javafx.scene.image.ImageView tmpCustomIcon =
              new javafx.scene.image.ImageView(
                  new javafx.scene.image.Image(getClass().getResourceAsStream(
                      "/io/github/kullik01/kinonode/images/error_icon.png")));
          tmpCustomIcon.setFitHeight(48);
          tmpCustomIcon.setFitWidth(48);
          tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

          tmpAlert.getDialogPane()
              .setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
          io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpAlert);
          tmpAlert.showAndWait();
          return false;
        }
      }
    }
    return true;
  }

  @FXML
  private void changePoster() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Choose Poster Image");
    tmpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
        "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

    File tmpSelected =
        tmpChooser.showOpenDialog(imgPoster.getScene().getWindow());
    if (tmpSelected != null) {
      try {
        Path tmpDbPath =
            AppContext.getInstance().getDatabaseService().getDbPath();
        Path tmpCoversDir = tmpDbPath.getParent().resolve("covers");
        // Ensure covers folder directory is present beside the database file
        if (!Files.exists(tmpCoversDir)) {
          Files.createDirectories(tmpCoversDir);
        }

        String tmpExtension = PathUtils.getFileExtension(tmpSelected);
        if (tmpExtension.isEmpty())
          tmpExtension = "jpg";

        // Generate unique filename with timestamp to bypass cover art browser
        // caching issues
        String tmpFilename = "cover_" + movie.getId() + "_"
            + System.currentTimeMillis() + "." + tmpExtension;
        Path tmpDestPath = tmpCoversDir.resolve(tmpFilename);

        Files.copy(tmpSelected.toPath(), tmpDestPath,
            StandardCopyOption.REPLACE_EXISTING);

        String tmpNewPath =
            tmpDestPath.toAbsolutePath().toString().replace("\\", "/");
        movie.setCoverImagePath(tmpNewPath);

        // Reload image directly in details screen
        imgPoster
            .setImage(new Image(tmpDestPath.toUri().toURL().toExternalForm(),
                240, 360, false, true, true));
        showStatus("Poster image updated.");
      } catch (IOException tmpException) {
        LOGGER.log(Level.SEVERE, "Failed to copy cover poster file",
            tmpException);
        showStatus("Error saving poster: " + tmpException.getMessage());
      }
    }
  }

  @FXML
  private void playMovie() {
    try {
      // Trigger PlaybackService which handles configuring and executing
      // external media player
      // process
      AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
      showStatus("Opening external player...");
    } catch (Exception tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to launch external media player",
          tmpException);
      Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
      javafx.stage.Stage tmpStage =
          (javafx.stage.Stage) tmpAlert.getDialogPane().getScene().getWindow();
      tmpStage.getIcons()
          .add(new javafx.scene.image.Image(getClass().getResourceAsStream(
              "/io/github/kullik01/kinonode/images/logo.png")));
      tmpAlert.setTitle("Playback Failed");
      tmpAlert.setHeaderText("Could not launch external media player");
      tmpAlert.setContentText(tmpException.getMessage());

      javafx.scene.image.ImageView tmpCustomIcon =
          new javafx.scene.image.ImageView(
              new javafx.scene.image.Image(getClass().getResourceAsStream(
                  "/io/github/kullik01/kinonode/images/error_icon.png")));
      tmpCustomIcon.setFitHeight(48);
      tmpCustomIcon.setFitWidth(48);
      tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

      io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpAlert);
      tmpAlert.showAndWait();
    }
  }

  @FXML
  private void watchTrailer() {
    String tmpUrl = txtTrailerUrl.getText().trim();
    if (tmpUrl.isEmpty())
      return;

    if (!tmpUrl.startsWith("http://") && !tmpUrl.startsWith("https://")) {
      tmpUrl = "https://" + tmpUrl;
    }

    try {
      // Open default web browser pointing to trailer link
      if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop()
          .isSupported(java.awt.Desktop.Action.BROWSE)) {
        java.awt.Desktop.getDesktop().browse(new java.net.URI(tmpUrl));
        showStatus("Opening trailer link in browser...");
      } else {
        showStatus("System web browser action not supported on this platform.");
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING, "Failed to open browser trailer link",
          tmpException);
      showStatus("Failed to open browser.");
    }
  }

  @FXML
  private void openImdbPage() {
    if (movie == null) {
      return;
    }

    String tmpUrl = null;
    String tmpCover = movie.getCoverImagePath();
    if (tmpCover != null) {
      java.util.regex.Pattern tmpPattern =
          java.util.regex.Pattern.compile("tt\\d+");
      java.util.regex.Matcher tmpMatcher = tmpPattern.matcher(tmpCover);
      if (tmpMatcher.find()) {
        tmpUrl = "https://www.imdb.com/title/" + tmpMatcher.group() + "/";
      }
    }

    if (tmpUrl == null) {
      // Fallback to IMDb search by title
      String tmpTitle = movie.getTitle();
      if (tmpTitle != null && !tmpTitle.isBlank()) {
        try {
          tmpUrl = "https://www.imdb.com/find/?q=" + java.net.URLEncoder
              .encode(tmpTitle.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception tmpEx) {
          LOGGER.log(Level.WARNING, "Failed to encode title for IMDb search",
              tmpEx);
        }
      }
    }

    if (tmpUrl == null) {
      tmpUrl = "https://www.imdb.com/";
    }

    MainController tmpMain = AppContext.getInstance().getMainController();
    if (tmpMain != null) {
      tmpMain.showImdbWithUrl(tmpUrl);
      showStatus("Switching to IMDb tab...");
    } else {
      showStatus("Could not switch to IMDb tab.");
    }
  }

  private enum InputConstraint {
    ANY, NUMBERS_ONLY, NO_NUMBERS
  }

  private void showEditDialog(String title, String fieldName,
      TextInputControl control, InputConstraint constraint,
      java.util.function.Consumer<String> updater) {
    Dialog<String> tmpDialog = new Dialog<>();
    tmpDialog.setTitle("Edit " + title);
    tmpDialog.setHeaderText("Edit " + fieldName);

    tmpDialog.getDialogPane().setPrefSize(480, 280);
    tmpDialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage tmpStage =
        (javafx.stage.Stage) tmpDialog.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    javafx.scene.image.ImageView tmpCustomIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(getClass().getResourceAsStream(
                "/io/github/kullik01/kinonode/images/question_mark.png")));
    tmpCustomIcon.setFitHeight(48);
    tmpCustomIcon.setFitWidth(48);
    tmpDialog.getDialogPane().setGraphic(tmpCustomIcon);

    ButtonType tmpOkButtonType =
        new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    tmpDialog.getDialogPane().getButtonTypes().addAll(tmpOkButtonType,
        ButtonType.CANCEL);

    VBox tmpVbox = new VBox(5);
    Label tmpErrorLabel = new Label();
    tmpErrorLabel.setWrapText(true);
    tmpErrorLabel.setPrefWidth(430);
    tmpErrorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    tmpErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String tmpDefaultText =
        control.getText() != null ? control.getText().trim() : "";

    if (!tmpDefaultText.isEmpty()) {
      tmpErrorLabel.setText("This is the current " + fieldName
          + " and must be updated by a new one.");
      tmpErrorLabel.setVisible(true);
    } else {
      tmpErrorLabel.setVisible(false);
    }

    TextInputControl tmpInputField;
    if (control instanceof TextArea) {
      TextArea tmpTa = new TextArea(tmpDefaultText);
      tmpTa.setWrapText(true);
      tmpTa.setPrefHeight(100);
      tmpInputField = tmpTa;
    } else {
      tmpInputField = new TextField(tmpDefaultText);
    }

    // Set formatting constraints on user input
    if (constraint == InputConstraint.NUMBERS_ONLY) {
      tmpInputField.setTextFormatter(new TextFormatter<>(change -> {
        if (change.getText().matches("\\d*")) {
          return change;
        }
        return null;
      }));
    } else if (constraint == InputConstraint.NO_NUMBERS) {
      tmpInputField.setTextFormatter(new TextFormatter<>(change -> {
        if (change.getText().matches("[^0-9]*")) {
          return change;
        }
        return null;
      }));
    }

    tmpVbox.getChildren().addAll(new Label("Enter the new " + fieldName + ":"),
        tmpErrorLabel, tmpInputField);
    tmpDialog.getDialogPane().setContent(tmpVbox);

    javafx.scene.Node tmpOkButton =
        tmpDialog.getDialogPane().lookupButton(tmpOkButtonType);
    tmpOkButton.setDisable(true);

    tmpInputField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          String tmpVal = newValue.trim();
          boolean tmpIsUnchanged = tmpVal.equals(tmpDefaultText);

          // Require input content to be ASCII only
          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpErrorLabel.setText(
                "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else if (tmpIsUnchanged) {
            if (!tmpDefaultText.isEmpty()) {
              tmpErrorLabel.setText("This is the current " + fieldName
                  + " and must be updated by a new one.");
              tmpErrorLabel.setVisible(true);
            }
            tmpOkButton.setDisable(true);
          } else {
            tmpErrorLabel.setVisible(false);
            tmpOkButton.setDisable(false);
          }
        });

    tmpDialog.setResultConverter(dialogButton -> {
      if (dialogButton == tmpOkButtonType) {
        return tmpInputField.getText();
      }
      return null;
    });

    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpDialog);
    java.util.Optional<String> tmpResult = tmpDialog.showAndWait();
    tmpResult.ifPresent(val -> {
      String newVal = val.trim();
      control.setText(newVal);
      updater.accept(newVal);
      movieRepository.update(movie);
      showStatus(title + " updated!");
    });
  }

  @FXML
  private void editTitle() {
    showEditDialog("Title", "Movie Title", txtTitle, InputConstraint.ANY,
        val -> movie.setTitle(val.isEmpty() ? null : val));
  }

  @FXML
  private void editYear() {
    showEditDialog("Year", "Release Year", txtYear,
        InputConstraint.NUMBERS_ONLY, val -> {
          try {
            movie.setYear(val.isEmpty() ? null : Integer.parseInt(val));
          } catch (NumberFormatException tmpException) {
            showStatus("Year must be a number!");
          }
        });
  }

  @FXML
  private void editDuration() {
    showEditDialog("Duration", "Duration in minutes", txtDuration,
        InputConstraint.NUMBERS_ONLY, val -> {
          try {
            if (val.isEmpty()) {
              movie.setDuration(null);
              txtDuration.setText("Not specified (Click 📝 to set duration)");
              txtDuration.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
            } else {
              movie.setDuration(Integer.parseInt(val));
              txtDuration.setText(movie.getFormattedDuration());
              txtDuration.setStyle("-fx-text-fill: -color-fg-default;");
            }
          } catch (NumberFormatException tmpException) {
            showStatus("Duration must be a number!");
          }
        });
  }

  @FXML
  private void editGenre() {
    showEditDialog("Genre", "Genre", txtGenre, InputConstraint.NO_NUMBERS,
        val -> movie.setGenre(val.isEmpty() ? null : val));
  }

  @FXML
  private void editDirector() {
    showEditDialog("Director", "Director(s)", txtDirector,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setDirector(val.isEmpty() ? null : val);
          setupMultiField(movie.getDirector(), txtDirector, comboDirector);
        });
  }

  @FXML
  private void editWriter() {
    showEditDialog("Writer", "Writer(s)", txtWriter, InputConstraint.NO_NUMBERS,
        val -> {
          movie.setWriter(val.isEmpty() ? null : val);
          setupMultiField(movie.getWriter(), txtWriter, comboWriter);
        });
  }

  @FXML
  private void editProducer() {
    showEditDialog("Producer", "Producer(s)", txtProducer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setProducer(val.isEmpty() ? null : val);
          setupMultiField(movie.getProducer(), txtProducer, comboProducer);
        });
  }

  @FXML
  private void editCinematographer() {
    showEditDialog("Cinematographer", "Cinematographer(s)", txtCinematographer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setCinematographer(val.isEmpty() ? null : val);
          setupMultiField(movie.getCinematographer(), txtCinematographer,
              comboCinematographer);
        });
  }

  @FXML
  private void editComposer() {
    showEditDialog("Composer", "Composer(s)", txtComposer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setComposer(val.isEmpty() ? null : val);
          setupMultiField(movie.getComposer(), txtComposer, comboComposer);
        });
  }

  @FXML
  private void editActors() {
    showEditDialog("Actor(s)", "Actor(s)", txtActors,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setActors(val.isEmpty() ? null : val);
          setupMultiField(movie.getActors(), txtActors, comboActors);
        });
  }

  @FXML
  private void editDescription() {
    showEditDialog("Description", "Description", txtDescription,
        InputConstraint.NO_NUMBERS,
        val -> movie.setDescription(val.isEmpty() ? null : val));
  }

  @FXML
  private void editTrailerUrl() {
    Dialog<String> tmpDialog = new Dialog<>();
    tmpDialog.setTitle("Edit Trailer Link");
    tmpDialog.setHeaderText("YouTube Trailer URL");

    tmpDialog.getDialogPane().setPrefSize(480, 280);
    tmpDialog.getDialogPane().setMinSize(480, 280);

    javafx.stage.Stage tmpStage =
        (javafx.stage.Stage) tmpDialog.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    javafx.scene.image.ImageView tmpCustomIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(getClass().getResourceAsStream(
                "/io/github/kullik01/kinonode/images/question_mark.png")));
    tmpCustomIcon.setFitHeight(48);
    tmpCustomIcon.setFitWidth(48);
    tmpDialog.getDialogPane().setGraphic(tmpCustomIcon);

    ButtonType tmpOkButtonType =
        new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    tmpDialog.getDialogPane().getButtonTypes().addAll(tmpOkButtonType,
        ButtonType.CANCEL);

    VBox tmpVbox = new VBox(5);
    Label tmpErrorLabel = new Label();
    tmpErrorLabel.setWrapText(true);
    tmpErrorLabel.setPrefWidth(430);
    tmpErrorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    tmpErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String tmpDefaultText = txtTrailerUrl.getText().trim();

    if (!tmpDefaultText.isEmpty()) {
      tmpErrorLabel.setText(
          "This is the current YouTube link and must be updated by a new one.");
      tmpErrorLabel.setVisible(true);
    } else {
      tmpErrorLabel.setVisible(false);
    }

    TextField tmpTextField = new TextField(tmpDefaultText);
    tmpTextField.setPromptText("https://www.youtube.com/watch?v=...");

    tmpVbox.getChildren().addAll(
        new Label("Enter the new YouTube Trailer Link:"), tmpErrorLabel,
        tmpTextField);
    tmpDialog.getDialogPane().setContent(tmpVbox);

    javafx.scene.Node tmpOkButton =
        tmpDialog.getDialogPane().lookupButton(tmpOkButtonType);
    tmpOkButton.setDisable(true);

    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          String tmpUrl = newValue.trim();
          boolean tmpIsUnchanged = tmpUrl.equalsIgnoreCase(tmpDefaultText);

          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpErrorLabel.setText(
                "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else if (tmpIsUnchanged && !tmpDefaultText.isEmpty()) {
            tmpErrorLabel.setText(
                "This is the current YouTube link and must be updated by a new one.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else if (!tmpUrl.isEmpty() && !tmpUrl.contains("youtube.com")
              && !tmpUrl.contains("youtu.be")) {
            tmpErrorLabel.setText("Trailer Link must be a valid YouTube URL.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else {
            tmpErrorLabel.setVisible(false);
            tmpOkButton.setDisable(tmpUrl.isEmpty());
          }
        });

    tmpDialog.setResultConverter(dialogButton -> {
      if (dialogButton == tmpOkButtonType) {
        return tmpTextField.getText();
      }
      return null;
    });

    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpDialog);
    java.util.Optional<String> tmpResult = tmpDialog.showAndWait();
    tmpResult.ifPresent(url -> {
      String newUrl = url.trim();
      txtTrailerUrl.setText(newUrl);
      movie.setTrailerUrl(newUrl.isEmpty() ? null : newUrl);
      movieRepository.update(movie);
      showStatus("Trailer Link updated!");
    });
  }

  /**
   * Evaluates if any of the UI input fields have unsaved modifications compared
   * to the database.
   *
   * @return {@code true} if unsaved changes exist; {@code false} otherwise
   */
  public boolean hasUnsavedChanges() {
    if (movie == null)
      return false;
    if (!java.util.Objects.equals(movie.getTitle(), txtTitle.getText().trim()))
      return true;

    String tmpMovieYear =
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "";
    if (!java.util.Objects.equals(tmpMovieYear, txtYear.getText().trim()))
      return true;

    String tmpMovieGenre = movie.getGenre() != null ? movie.getGenre() : "";
    if (!java.util.Objects.equals(tmpMovieGenre, txtGenre.getText().trim()))
      return true;

    String tmpMovieDirector =
        movie.getDirector() != null ? movie.getDirector() : "";
    if (!java.util.Objects.equals(tmpMovieDirector,
        txtDirector.getText().trim()))
      return true;

    String tmpMovieWriter = movie.getWriter() != null ? movie.getWriter() : "";
    if (!java.util.Objects.equals(tmpMovieWriter, txtWriter.getText().trim()))
      return true;

    String tmpMovieProducer =
        movie.getProducer() != null ? movie.getProducer() : "";
    if (!java.util.Objects.equals(tmpMovieProducer,
        txtProducer.getText().trim()))
      return true;

    String tmpMovieCinematographer =
        movie.getCinematographer() != null ? movie.getCinematographer() : "";
    if (!java.util.Objects.equals(tmpMovieCinematographer,
        txtCinematographer.getText().trim()))
      return true;

    String tmpMovieComposer =
        movie.getComposer() != null ? movie.getComposer() : "";
    if (!java.util.Objects.equals(tmpMovieComposer,
        txtComposer.getText().trim()))
      return true;

    String tmpMovieActors = movie.getActors() != null ? movie.getActors() : "";
    if (!java.util.Objects.equals(tmpMovieActors, txtActors.getText().trim()))
      return true;

    String tmpMovieDesc =
        movie.getDescription() != null ? movie.getDescription() : "";
    if (!java.util.Objects.equals(tmpMovieDesc,
        txtDescription.getText().trim()))
      return true;

    String tmpMovieTrailer =
        movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "";
    if (!java.util.Objects.equals(tmpMovieTrailer,
        txtTrailerUrl.getText().trim()))
      return true;

    if (!java.util.Objects.equals(movie.getRating(), currentRating))
      return true;

    return false;
  }

  /**
   * Performs validation and persists all modified fields to the database.
   * Prints feedback status to the status label.
   *
   * @return {@code true} if save is completed successfully; {@code false} if
   *         validation failed
   */
  public boolean performSave() {
    String tmpTitle = txtTitle.getText().trim();
    if (tmpTitle.isEmpty()) {
      showStatus("Title cannot be empty.");
      return false;
    }

    Integer tmpYear = null;
    if (!txtYear.getText().trim().isEmpty()) {
      try {
        tmpYear = Integer.parseInt(txtYear.getText().trim());
      } catch (NumberFormatException tmpE) {
        showStatus("Release year must be a valid integer number.");
        return false;
      }
    }

    String tmpTrailer = txtTrailerUrl.getText().trim();
    if (!tmpTrailer.isEmpty()) {
      if (!tmpTrailer.contains("youtube.com")
          && !tmpTrailer.contains("youtu.be")) {
        showStatus("Trailer Link must be a valid YouTube URL.");
        return false;
      }
    }

    movie.setTitle(tmpTitle);
    movie.setYear(tmpYear);
    movie.setGenre(
        txtGenre.getText().trim().isEmpty() ? null : txtGenre.getText().trim());
    movie.setDirector(txtDirector.getText().trim().isEmpty() ? null
        : txtDirector.getText().trim());
    movie.setWriter(txtWriter.getText().trim().isEmpty() ? null
        : txtWriter.getText().trim());
    movie.setProducer(txtProducer.getText().trim().isEmpty() ? null
        : txtProducer.getText().trim());
    movie
        .setCinematographer(txtCinematographer.getText().trim().isEmpty() ? null
            : txtCinematographer.getText().trim());
    movie.setComposer(txtComposer.getText().trim().isEmpty() ? null
        : txtComposer.getText().trim());
    movie.setActors(txtActors.getText().trim().isEmpty() ? null
        : txtActors.getText().trim());
    movie.setDescription(txtDescription.getText().trim().isEmpty() ? null
        : txtDescription.getText().trim());
    movie.setTrailerUrl(tmpTrailer.isEmpty() ? null : tmpTrailer);
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
        URL tmpUrl = getClass()
            .getResource("/io/github/kullik01/kinonode/fxml/LibraryView.fxml");
        FXMLLoader tmpLoader = new FXMLLoader(tmpUrl);
        Parent tmpLibraryView = tmpLoader.load();

        StackPane tmpContentArea =
            (StackPane) lblHeaderTitle.getScene().lookup("#contentArea");
        if (tmpContentArea != null) {
          tmpContentArea.getChildren().setAll(tmpLibraryView);
        }
      } catch (IOException tmpE) {
        LOGGER.log(Level.SEVERE, "Failed to load LibraryView FXML", tmpE);
      }
    }
  }

  @FXML
  private void searchDirectorOnImdb() {
    searchPersonOnImdb(comboDirector, txtDirector);
  }

  @FXML
  private void searchWriterOnImdb() {
    searchPersonOnImdb(comboWriter, txtWriter);
  }

  @FXML
  private void searchProducerOnImdb() {
    searchPersonOnImdb(comboProducer, txtProducer);
  }

  @FXML
  private void searchCinematographerOnImdb() {
    searchPersonOnImdb(comboCinematographer, txtCinematographer);
  }

  @FXML
  private void searchComposerOnImdb() {
    searchPersonOnImdb(comboComposer, txtComposer);
  }

  @FXML
  private void searchActorsOnImdb() {
    searchPersonOnImdb(comboActors, txtActors);
  }

  private void searchPersonOnImdb(ComboBox<String> combo, TextField txt) {
    String name = null;
    if (combo != null && combo.isVisible()) {
      name = combo.getSelectionModel().getSelectedItem();
    } else if (txt != null) {
      name = txt.getText();
    }

    if (name == null || name.trim().isEmpty()) {
      return;
    }

    final String finalName = name.trim();
    showStatus("Searching IMDb ID for " + finalName + "...");

    // Get movie IMDb ID if available
    String tempMovieImdbId = null;
    if (movie != null) {
      String cover = movie.getCoverImagePath();
      if (cover != null) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("tt\\d+");
        java.util.regex.Matcher m = p.matcher(cover);
        if (m.find()) {
          tempMovieImdbId = m.group();
        }
      }
    }
    final String movieImdbId = tempMovieImdbId;

    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
      String imdbId = null;

      // Step 1: Try to query person IMDb ID associated with this movie
      if (movieImdbId != null) {
        try {
          String sparql = "SELECT ?imdbId WHERE { " + "  ?movie wdt:P345 \""
              + movieImdbId + "\". " + "  { ?movie wdt:P57 ?person. } UNION "
              + "  { ?movie wdt:P58 ?person. } UNION "
              + "  { ?movie wdt:P162 ?person. } UNION "
              + "  { ?movie wdt:P344 ?person. } UNION "
              + "  { ?movie wdt:P86 ?person. } UNION "
              + "  { ?movie wdt:P161 ?person. } " + "  ?person rdfs:label \""
              + finalName.replace("\"", "\\\"") + "\"@en. "
              + "  ?person wdt:P345 ?imdbId. " + "} LIMIT 1";

          imdbId = executeSparql(sparql);
        } catch (Exception e) {
          LOGGER.log(Level.FINE,
              "Failed to find movie-associated person IMDb ID for: "
                  + finalName,
              e);
        }
      }

      // Step 2: Fallback to general search by name
      if (imdbId == null) {
        try {
          String sparql = "SELECT ?imdbId WHERE { " + "  ?person rdfs:label \""
              + finalName.replace("\"", "\\\"") + "\"@en. "
              + "  ?person wdt:P345 ?imdbId. " + "} LIMIT 1";

          imdbId = executeSparql(sparql);
        } catch (Exception e) {
          LOGGER.log(Level.FINE,
              "Failed to find general person IMDb ID for: " + finalName, e);
        }
      }

      return imdbId;
    }).thenAccept(imdbId -> {
      javafx.application.Platform.runLater(() -> {
        String targetUrl;
        if (imdbId != null && !imdbId.trim().isEmpty()) {
          targetUrl = "https://www.imdb.com/name/" + imdbId.trim() + "/";
          showStatus("Found " + finalName + " on IMDb!");
        } else {
          // Fallback to name search if no IMDb ID was found on Wikidata
          try {
            String encoded = java.net.URLEncoder.encode(finalName,
                java.nio.charset.StandardCharsets.UTF_8);
            targetUrl = "https://www.imdb.com/find/?q=" + encoded + "&s=nm";
            showStatus("Opened IMDb search for " + finalName);
          } catch (Exception e) {
            targetUrl = "https://www.imdb.com/";
          }
        }

        MainController tmpMain = AppContext.getInstance().getMainController();
        if (tmpMain != null) {
          tmpMain.showImdbWithUrl(targetUrl);
        }
      });
    });
  }

  private String executeSparql(String sparql) throws Exception {
    String urlStr =
        "https://query.wikidata.org/sparql?query=" + java.net.URLEncoder
            .encode(sparql, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20") + "&format=json";

    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest request =
        java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(urlStr))
            .header("User-Agent", "Mozilla/5.0 KinoNode/1.0")
            .header("Accept", "application/json").GET().build();

    java.net.http.HttpResponse<String> response = client.send(request,
        java.net.http.HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      com.google.gson.JsonObject json = com.google.gson.JsonParser
          .parseString(response.body()).getAsJsonObject();
      com.google.gson.JsonArray bindings =
          json.getAsJsonObject("results").getAsJsonArray("bindings");
      if (bindings.size() > 0) {
        return bindings.get(0).getAsJsonObject().getAsJsonObject("imdbId")
            .get("value").getAsString();
      }
    }
    return null;
  }

  private void setupMultiField(String value, TextField txt,
      ComboBox<String> combo) {
    java.util.List<String> names = parseNames(value);
    txt.setText(value != null ? value : "");
    if (names.size() <= 1) {
      txt.setVisible(true);
      txt.setManaged(true);
      if (combo != null) {
        combo.setVisible(false);
        combo.setManaged(false);
      }
    } else {
      txt.setVisible(false);
      txt.setManaged(false);
      if (combo != null) {
        combo.setVisible(true);
        combo.setManaged(true);
        combo.getItems().setAll(names);
        combo.getSelectionModel().selectFirst();
      }
    }
  }

  private java.util.List<String> parseNames(String value) {
    java.util.List<String> list = new java.util.ArrayList<>();
    if (value != null && !value.isBlank()) {
      String clean = value.trim();
      if (clean.endsWith(", ...")) {
        clean = clean.substring(0, clean.length() - 5).trim();
      }
      for (String part : clean.split(",")) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          list.add(trimmed);
        }
      }
    }
    return list;
  }
}
