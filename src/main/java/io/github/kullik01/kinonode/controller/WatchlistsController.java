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
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller for the Watchlists View of the application. Manages the
 * presentation, creation, deletion, renaming, and contents of user watchlists.
 * Features a split layout with watchlists on the left and movie lists (with
 * poster thumbnails and visual rating filmstrips) on the right.
 */
public class WatchlistsController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(WatchlistsController.class.getName());
  private static Image PLACEHOLDER_IMAGE;

  @FXML
  private ListView<Watchlist> lstWatchlists;
  @FXML
  private VBox paneDetails;
  @FXML
  private Label lblWatchlistName;
  @FXML
  private Label lblMovieCount;
  @FXML
  private ListView<Movie> lstMovies;
  @FXML
  private Button btnRenameWatchlist;
  @FXML
  private Button btnDeleteWatchlist;
  @FXML
  private Button btnRemoveMovie;

  private WatchlistRepository watchlistRepository;
  private Watchlist selectedWatchlist;

  /**
   * Initializes the watchlists layout. Installs custom cell factories for
   * watchlist titles and movie row cells, establishes selection event handlers,
   * and binds double-click controls to trigger media playback.
   */
  @FXML
  public void initialize() {
    watchlistRepository = AppContext.getInstance().getWatchlistRepository();

    // Custom Cell Factory for Watchlists
    lstWatchlists.setCellFactory(tmpListView -> new ListCell<>() {
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
    lstWatchlists.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          selectedWatchlist = newValue;
          loadWatchlistMovies();
          updateWatchlistButtonStates(newValue != null);
        });

    updateWatchlistButtonStates(false);

    // Custom Cell Factory for Movies inside Watchlist (displays cover thumbnail
    // and star rating
    // row)
    lstMovies.setCellFactory(tmpListView -> new ListCell<>() {
      private final HBox content = new HBox(15);
      private final ImageView thumbnail = new ImageView();
      private final Label titleLabel = new Label();
      private final HBox ratingBox = new HBox(2);
      private final Label ratingTextLabel = new Label();

      {
        content.setAlignment(Pos.CENTER_LEFT);
        thumbnail.setFitWidth(30);
        thumbnail.setFitHeight(45);
        titleLabel.setStyle(
            "-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        ratingTextLabel.setPrefWidth(50);
        ratingTextLabel.setMinWidth(50);
        ratingTextLabel.setMaxWidth(50);
        ratingTextLabel.setAlignment(Pos.CENTER_RIGHT);
        ratingTextLabel.setStyle(
            "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 5px;");
        ratingBox.setAlignment(Pos.CENTER_RIGHT);

        // Allow title to expand, keeping rating aligned to right
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        ratingBox.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE,
            javafx.scene.layout.Region.USE_PREF_SIZE);
        ratingTextLabel.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE,
            javafx.scene.layout.Region.USE_PREF_SIZE);

        content.maxWidthProperty()
            .bind(tmpListView.widthProperty().subtract(40));

        content.getChildren().addAll(thumbnail, titleLabel, ratingBox,
            ratingTextLabel);
      }

      @Override
      protected void updateItem(Movie item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
        } else {
          String tmpText = item.getTitle()
              + (item.getYear() != null ? " (" + item.getYear() + ")" : "");
          if (item.getCinematographer() != null
              && !item.getCinematographer().isBlank()) {
            tmpText += " - " + item.getCinematographer();
          }
          titleLabel.setText(tmpText);

          // Render movie rating film reel icons dynamically in the cell
          ratingBox.getChildren().clear();
          for (int i = 1; i <= 10; i++) {
            Label tmpReel = new Label("🎥");
            tmpReel.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE,
                javafx.scene.control.Label.USE_PREF_SIZE);
            if (item.getRating() != null && i <= item.getRating()) {
              tmpReel.setStyle(
                  "-fx-font-size: 13px; -fx-text-fill: #ED8B00; -fx-opacity: 1.0;");
            } else {
              tmpReel.setStyle(
                  "-fx-font-size: 13px; -fx-text-fill: #444444; -fx-opacity: 0.3;");
            }
            ratingBox.getChildren().add(tmpReel);
          }
          if (item.getRating() != null) {
            ratingTextLabel.setText(item.getRating() + "/10");
            ratingTextLabel.setStyle(
                "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 5px;");
          } else {
            ratingTextLabel.setText("");
          }

          // Retrieve and load cover art image in the ListView cells
          Image tmpImg = null;
          if (item.getCoverImagePath() != null) {
            File tmpFile = new File(item.getCoverImagePath());
            if (tmpFile.exists()) {
              try {
                tmpImg = new Image(tmpFile.toURI().toURL().toExternalForm(), 30,
                    45, false, true, true);
              } catch (MalformedURLException tmpIgnoredException) {
              }
            }
          }
          if (tmpImg == null) {
            if (PLACEHOLDER_IMAGE == null) {
              try {
                var tmpResource = getClass().getResource(
                    "/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
                if (tmpResource != null) {
                  PLACEHOLDER_IMAGE = new Image(tmpResource.toExternalForm(),
                      30, 45, false, true, true);
                }
              } catch (Exception tmpIgnoredException) {
              }
            }
            tmpImg = PLACEHOLDER_IMAGE;
          }
          if (tmpImg != null) {
            thumbnail.setImage(tmpImg);
          }
          setGraphic(content);
        }
      }
    });

    // Double-click action to play selected movie in external player
    lstMovies.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        Movie tmpSelectedMovie =
            lstMovies.getSelectionModel().getSelectedItem();
        if (tmpSelectedMovie != null) {
          playMovie(tmpSelectedMovie);
        }
      }
    });

    // Update button states on selection changes
    lstMovies.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          updateButtonStates();
        });

    updateButtonStates();
    refresh();
  }

  /**
   * Refreshes watchlists from repository. Retains the user's active watchlist
   * selection index if it remains within bounds.
   */
  @Override
  public void refresh() {
    int tmpSelectedIdx = lstWatchlists.getSelectionModel().getSelectedIndex();
    List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
    lstWatchlists.getItems().setAll(tmpWatchlists);

    // Keep active selection focus when resetting/updating lists
    if (tmpSelectedIdx >= 0 && tmpSelectedIdx < tmpWatchlists.size()) {
      lstWatchlists.getSelectionModel().select(tmpSelectedIdx);
      selectedWatchlist = lstWatchlists.getSelectionModel().getSelectedItem();
      loadWatchlistMovies();
    } else if (!tmpWatchlists.isEmpty()) {
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
      Watchlist tmpRefreshed =
          watchlistRepository.findById(selectedWatchlist.getId());
      if (tmpRefreshed != null) {
        selectedWatchlist = tmpRefreshed;
      }

      lblWatchlistName.setText(selectedWatchlist.getName());
      List<Movie> tmpMovies = selectedWatchlist.getMovies();
      lblMovieCount.setText(
          tmpMovies.size() + (tmpMovies.size() == 1 ? " movie" : " movies"));
      lstMovies.getItems().setAll(tmpMovies);
      paneDetails.setDisable(false);
    }
    updateButtonStates();
  }

  private void updateButtonStates() {
    boolean selected = lstMovies.getSelectionModel().getSelectedItem() != null;
    btnRemoveMovie.setDisable(!selected);
  }

  private void updateWatchlistButtonStates(boolean hasSelection) {
    if (btnRenameWatchlist != null)
      btnRenameWatchlist.setDisable(!hasSelection);
    if (btnDeleteWatchlist != null)
      btnDeleteWatchlist.setDisable(!hasSelection);
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
    tmpOkButton.setDisable(true); // Always disable initially; user must type or
                                  // change the name

    if (currentId != -1) {
      tmpErrorLabel.setText(
          "A watchlist with this name already exists. Please choose a different name.");
      tmpErrorLabel.setVisible(true);
    }

    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.length() > 30) {
            tmpTextField.setText(oldValue);
            return;
          }
          // Restrict input watchlist names to standard ASCII chars only
          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpErrorLabel.setText(
                "The text you entered is not ASCII code. Only standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space) can be used.");
            tmpErrorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else {
            boolean tmpDuplicate = false;
            List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
            for (Watchlist tmpWatchlist : tmpWatchlists) {
              if (tmpWatchlist.getName().equalsIgnoreCase(newValue.trim())) {
                if (tmpWatchlist.getId() != null
                    && tmpWatchlist.getId() != currentId) {
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
              boolean tmpIsUnchanged = currentId != -1
                  && newValue.trim().equalsIgnoreCase(defaultText.trim());
              if (tmpIsUnchanged) {
                tmpErrorLabel.setText(
                    "A watchlist with this name already exists. Please choose a different name.");
                tmpErrorLabel.setVisible(true);
                tmpOkButton.setDisable(true);
              } else {
                tmpErrorLabel.setVisible(false);
                tmpOkButton.setDisable(newValue.trim().isEmpty());
              }
            }
          }
        });

    tmpDialog.setResultConverter(dialogButton -> {
      if (dialogButton == tmpOkButtonType) {
        return tmpTextField.getText();
      }
      return null;
    });

    return tmpDialog.showAndWait();
  }

  @FXML
  private void createWatchlist() {
    Optional<String> tmpResult =
        showWatchlistDialog("New Watchlist", "Create a new watchlist", "", -1);

    tmpResult.ifPresent(name -> {
      String tmpCleanName = name.trim();
      if (!tmpCleanName.isEmpty()) {
        if (validateWatchlistName(tmpCleanName, -1)) {
          watchlistRepository.insert(new Watchlist(tmpCleanName));
          refresh();
        }
      }
    });
  }

  @FXML
  private void renameWatchlist() {
    if (selectedWatchlist == null)
      return;

    Optional<String> tmpResult = showWatchlistDialog("Rename Watchlist",
        "Rename watchlist: " + selectedWatchlist.getName(),
        selectedWatchlist.getName(),
        selectedWatchlist.getId() == null ? -1 : selectedWatchlist.getId());

    tmpResult.ifPresent(name -> {
      String tmpCleanName = name.trim();
      if (!tmpCleanName.isEmpty()
          && !tmpCleanName.equals(selectedWatchlist.getName())) {
        if (validateWatchlistName(tmpCleanName,
            selectedWatchlist.getId() == null ? -1
                : selectedWatchlist.getId())) {
          selectedWatchlist.setName(tmpCleanName);
          watchlistRepository.update(selectedWatchlist);
          refresh();
        }
      }
    });
  }

  private boolean validateWatchlistName(String cleanName, int currentId) {
    List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
    for (Watchlist tmpWatchlist : tmpWatchlists) {
      if (tmpWatchlist.getName().equalsIgnoreCase(cleanName)) {
        if (tmpWatchlist.getId() != null && tmpWatchlist.getId() != currentId) {
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
          tmpAlert.showAndWait();
          return false;
        }
      }
    }
    return true;
  }

  @FXML
  private void deleteWatchlist() {
    if (selectedWatchlist == null)
      return;

    Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
    javafx.stage.Stage tmpStage =
        (javafx.stage.Stage) tmpAlert.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
    tmpAlert.setTitle("Delete Watchlist");
    tmpAlert.setHeaderText("Delete watchlist: " + selectedWatchlist.getName());
    tmpAlert.setContentText(
        "Are you sure you want to delete this watchlist? Movies will not be deleted from your disk or library.");

    javafx.scene.image.ImageView tmpCustomIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(getClass().getResourceAsStream(
                "/io/github/kullik01/kinonode/images/removal_icon.png")));
    tmpCustomIcon.setFitHeight(48);
    tmpCustomIcon.setFitWidth(48);
    tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

    tmpAlert.getDialogPane()
        .setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

    Optional<ButtonType> tmpResult = tmpAlert.showAndWait();
    if (tmpResult.isPresent() && tmpResult.get() == ButtonType.OK) {
      watchlistRepository.delete(selectedWatchlist.getId());
      refresh();
    }
  }

  @FXML
  private void removeMovie() {
    Movie tmpSelectedMovie = lstMovies.getSelectionModel().getSelectedItem();
    if (tmpSelectedMovie == null || selectedWatchlist == null)
      return;

    Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
    javafx.stage.Stage tmpStage =
        (javafx.stage.Stage) tmpAlert.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));
    tmpAlert.setTitle("Remove Movie");
    tmpAlert.setHeaderText("Remove from Watchlist");
    tmpAlert.setContentText("Are you sure you want to remove this movie from "
        + selectedWatchlist.getName() + "?");

    javafx.scene.image.ImageView tmpCustomIcon =
        new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(getClass().getResourceAsStream(
                "/io/github/kullik01/kinonode/images/removal_icon.png")));
    tmpCustomIcon.setFitHeight(48);
    tmpCustomIcon.setFitWidth(48);
    tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

    tmpAlert.getDialogPane()
        .setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    Optional<ButtonType> tmpResult = tmpAlert.showAndWait();

    if (tmpResult.isPresent() && tmpResult.get() == ButtonType.OK) {
      watchlistRepository.removeMovieFromWatchlist(selectedWatchlist.getId(),
          tmpSelectedMovie.getId());
      refresh();
    }
  }

  private void playMovie(Movie movie) {
    try {
      AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
    } catch (Exception tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to play movie from watchlist",
          tmpException);
      Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
      javafx.stage.Stage tmpStage =
          (javafx.stage.Stage) tmpAlert.getDialogPane().getScene().getWindow();
      tmpStage.getIcons()
          .add(new javafx.scene.image.Image(getClass().getResourceAsStream(
              "/io/github/kullik01/kinonode/images/logo.png")));
      tmpAlert.setTitle("Playback Failed");
      tmpAlert.setHeaderText("Cannot open media player");
      tmpAlert.setContentText(tmpException.getMessage());

      javafx.scene.image.ImageView tmpCustomIcon =
          new javafx.scene.image.ImageView(
              new javafx.scene.image.Image(getClass().getResourceAsStream(
                  "/io/github/kullik01/kinonode/images/error_icon.png")));
      tmpCustomIcon.setFitHeight(48);
      tmpCustomIcon.setFitWidth(48);
      tmpAlert.getDialogPane().setGraphic(tmpCustomIcon);

      tmpAlert.getDialogPane()
          .setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
      tmpAlert.showAndWait();
    }
  }
}
