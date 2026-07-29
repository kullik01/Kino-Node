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

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import javafx.application.Platform;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.model.SmartRule;
import com.google.gson.Gson;
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
import javafx.scene.shape.SVGPath;

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
  private Button btnCreateWatchlist;
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

    // Subscribe to reactive library updates to automatically refresh smart
    // watchlists
    AppContext.getInstance().addLibraryChangeListener(() -> {
      Platform.runLater(this::refresh);
    });

    // Custom Cell Factory for Watchlists
    lstWatchlists.setCellFactory(tmpListView -> new ListCell<>() {
      @Override
      protected void updateItem(Watchlist item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item.isSmart() ? "✨ " + item.getName() : item.getName());
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
        ratingBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        ratingTextLabel.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

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
          for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
            Label tmpReel = new Label();
            SVGPath tmpIcon = new SVGPath();
            tmpIcon.setContent(
                "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z");
            tmpIcon.setScaleX(0.65);
            tmpIcon.setScaleY(0.65);
            tmpReel.setGraphic(tmpIcon);
            tmpReel.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE,
                javafx.scene.control.Label.USE_PREF_SIZE);
            if (item.getRating() != null && tmpIndex <= item.getRating()) {
              tmpIcon.setStyle("-fx-fill: #ED8B00; -fx-opacity: 1.0;");
            } else {
              tmpIcon.setStyle("-fx-fill: #666666; -fx-opacity: 0.35;");
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

  /**
   * Loads the movies associated with the currently selected watchlist and updates
   * the UI. If no watchlist is selected, it clears the movie list and disables the details pane.
   */
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

  /**
   * Updates the enabled/disabled state of the movie management buttons
   * (e.g., Remove Movie) based on the current selection and whether the active
   * watchlist is a Smart Watchlist (which cannot be manually edited).
   */
  private void updateButtonStates() {
    boolean selected = lstMovies.getSelectionModel().getSelectedItem() != null;
    boolean tmpIsSmart =
        selectedWatchlist != null && selectedWatchlist.isSmart();
    btnRemoveMovie.setDisable(!selected || tmpIsSmart);
  }

  /**
   * Updates the enabled/disabled state of the watchlist management buttons
   * (Rename, Delete) based on whether a watchlist is currently selected.
   *
   * @param hasSelection True if a watchlist is selected, false otherwise.
   */
  private void updateWatchlistButtonStates(boolean hasSelection) {
    if (btnRenameWatchlist != null)
      btnRenameWatchlist.setDisable(!hasSelection);
    if (btnDeleteWatchlist != null)
      btnDeleteWatchlist.setDisable(!hasSelection);
  }

  /**
   * Displays a customized dialog for creating or renaming a standard watchlist.
   * Features real-time input validation to enforce character limits and ASCII-only
   * names, and prevents duplicate names.
   *
   * @param title       The dialog window title.
   * @param header      The dialog header text.
   * @param defaultText The initial text to populate in the input field.
   * @param currentId   The database ID of the current watchlist being renamed (-1 if creating new).
   * @return An Optional containing the validated watchlist name input, or empty if cancelled.
   */
  private Optional<String> showWatchlistDialog(String title, String header,
      String defaultText, int currentId) {
    Dialog<String> tmpDialog = new Dialog<>();
    tmpDialog.setTitle(title);
    tmpDialog.setHeaderText(header);

    tmpDialog.getDialogPane().setPrefSize(480, Region.USE_COMPUTED_SIZE);
    tmpDialog.getDialogPane().setMinSize(480, Region.USE_COMPUTED_SIZE);

    Stage tmpStage = (Stage) tmpDialog.getDialogPane().getScene().getWindow();
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
    tmpErrorLabel.setMinHeight(Region.USE_PREF_SIZE);
    tmpErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    tmpErrorLabel.setVisible(false);

    TextField tmpTextField = new TextField(defaultText);
    tmpTextField.setPromptText("Watchlist Name");
    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue != null && (newValue.length() > 33
              || !newValue.matches("\\A\\p{ASCII}*\\z"))) {
            tmpTextField.setText(oldValue);
          }
        });

    Label tmpAsciiInfo = new Label(
        "Only ASCII characters allowed: standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space).");
    tmpAsciiInfo.setStyle(
        "-fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");
    tmpAsciiInfo.setWrapText(true);
    tmpAsciiInfo.setPrefWidth(430);
    tmpAsciiInfo.setMinHeight(Region.USE_PREF_SIZE);

    tmpVbox.getChildren().addAll(tmpErrorLabel, tmpTextField, tmpAsciiInfo);
    tmpDialog.getDialogPane().setContent(tmpVbox);

    Node tmpOkButton = tmpDialog.getDialogPane().lookupButton(tmpOkButtonType);
    if (tmpOkButton != null) {
      tmpOkButton.getStyleClass().add("button-primary");
      tmpOkButton.setDisable(true); // Always disable initially; user must type
                                    // or change the name
    }

    Node tmpCancelButton =
        tmpDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    if (tmpCancelButton != null) {
      tmpCancelButton.getStyleClass().add("button-primary");
    }

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
          if (tmpDialog.getDialogPane().getScene() != null
              && tmpDialog.getDialogPane().getScene().getWindow() != null) {
            tmpDialog.getDialogPane().getScene().getWindow().sizeToScene();
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

  /**
   * Displays a context menu to let the user choose between creating a standard
   * watchlist or a smart watchlist.
   *
   * @param event The action event that triggered the menu, used to position the popup.
   */
  @FXML
  private void showCreateMenu(javafx.event.ActionEvent event) {
    javafx.scene.control.ContextMenu tmpContextMenu =
        new javafx.scene.control.ContextMenu();
    javafx.scene.control.MenuItem tmpItemStandard =
        new javafx.scene.control.MenuItem("New Watchlist");
    tmpItemStandard.setOnAction(e -> createWatchlist());
    javafx.scene.control.MenuItem tmpItemSmart =
        new javafx.scene.control.MenuItem("New Smart Watchlist");
    tmpItemSmart.setOnAction(e -> createSmartWatchlist());
    tmpContextMenu.getItems().addAll(tmpItemStandard, tmpItemSmart);
    javafx.scene.Node tmpSource = (javafx.scene.Node) event.getSource();
    tmpContextMenu.show(tmpSource, javafx.geometry.Side.TOP, 0, 0);
  }

  /**
   * Triggers the creation flow for a new standard watchlist.
   * Shows the input dialog, validates the result, and inserts it into the repository.
   */
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

  /**
   * Triggers the renaming flow for the currently selected watchlist.
   * Shows the input dialog, validates the new name, and updates the repository.
   */
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

  /**
   * Validates that a newly proposed watchlist name does not conflict with an existing one.
   * Shows an error alert if a duplicate is detected.
   *
   * @param cleanName The proposed watchlist name to check.
   * @param currentId The ID of the watchlist being checked (to ignore itself), or -1 if new.
   * @return True if the name is valid (unique), false if it's a duplicate.
   */
  private boolean validateWatchlistName(String cleanName, int currentId) {
    List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
    for (Watchlist tmpWatchlist : tmpWatchlists) {
      if (tmpWatchlist.getName().equalsIgnoreCase(cleanName)) {
        if (tmpWatchlist.getId() != null && tmpWatchlist.getId() != currentId) {
          io.github.kullik01.kinonode.util.CustomAlert.showError(null,
              "Validation Error", "Duplicate Watchlist",
              "A watchlist with this name already exists. Please choose a different name.",
              "/io/github/kullik01/kinonode/images/error_icon.png");
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Deletes the currently selected watchlist after seeking confirmation from the user.
   */
  @FXML
  private void deleteWatchlist() {
    if (selectedWatchlist == null)
      return;

    boolean tmpConfirm = io.github.kullik01.kinonode.util.CustomAlert
        .showConfirmation(lstMovies.getScene().getWindow(), "Delete Watchlist",
            "Delete watchlist: " + selectedWatchlist.getName(),
            "Are you sure you want to delete this watchlist? Movies will not be deleted from your disk or library.",
            "/io/github/kullik01/kinonode/images/removal_icon.png");

    if (tmpConfirm) {
      watchlistRepository.delete(selectedWatchlist.getId());
      refresh();
    }
  }

  /**
   * Removes the currently selected movie from the active watchlist after seeking confirmation.
   * This action is disabled for Smart Watchlists.
   */
  @FXML
  private void removeMovie() {
    Movie tmpSelectedMovie = lstMovies.getSelectionModel().getSelectedItem();
    if (tmpSelectedMovie == null || selectedWatchlist == null
        || selectedWatchlist.isSmart())
      return;

    boolean tmpConfirm = io.github.kullik01.kinonode.util.CustomAlert
        .showConfirmation(lstMovies.getScene().getWindow(), "Remove Movie",
            "Remove from Watchlist",
            "Are you sure you want to remove this movie from "
                + selectedWatchlist.getName() + "?",
            "/io/github/kullik01/kinonode/images/removal_icon.png");

    if (tmpConfirm) {
      watchlistRepository.removeMovieFromWatchlist(selectedWatchlist.getId(),
          tmpSelectedMovie.getId());
      refresh();
    }
  }

  /**
   * Opens the specified movie in the system's default media player.
   * Shows an error alert if playback fails.
   *
   * @param movie The movie to play.
   */
  private void playMovie(Movie movie) {
    try {
      AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
    } catch (Exception tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to play movie from watchlist",
          tmpException);
      io.github.kullik01.kinonode.util.CustomAlert.showError(
          lstMovies.getScene().getWindow(), "Playback Failed",
          "Cannot open media player", tmpException.getMessage(),
          "/io/github/kullik01/kinonode/images/error_icon.png");
    }
  }

  /**
   * Triggers the creation flow for a new Smart Watchlist.
   * Displays a complex dialog allowing the user to name the watchlist and define a
   * dynamic filtering rule (e.g., "IMDb Rating GREATER_THAN 8.0").
   */
  @FXML
  private void createSmartWatchlist() {
    Dialog<Watchlist> tmpDialog = new Dialog<>();
    tmpDialog.setTitle("New Smart Watchlist");
    tmpDialog.setHeaderText("Create a dynamic watchlist based on rules");

    tmpDialog.getDialogPane().setPrefSize(700, Region.USE_COMPUTED_SIZE);
    tmpDialog.getDialogPane().setMinSize(700, Region.USE_COMPUTED_SIZE);

    Stage tmpStage = (Stage) tmpDialog.getDialogPane().getScene().getWindow();
    tmpStage.getIcons().add(new javafx.scene.image.Image(getClass()
        .getResourceAsStream("/io/github/kullik01/kinonode/images/logo.png")));

    ButtonType tmpOkButtonType =
        new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
    tmpDialog.getDialogPane().getButtonTypes().addAll(tmpOkButtonType,
        ButtonType.CANCEL);

    VBox tmpVbox = new VBox(10);

    Label tmpErrorLabel = new Label();
    tmpErrorLabel.setWrapText(true);
    tmpErrorLabel.setPrefWidth(600);
    tmpErrorLabel.setMinHeight(Region.USE_PREF_SIZE);
    tmpErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    tmpErrorLabel.setVisible(false);

    TextField tmpNameField = new TextField();
    tmpNameField.setPromptText("Smart Watchlist Name");
    HBox tmpRuleBox = new HBox(5);
    tmpRuleBox.setDisable(true);
    ComboBox<String> tmpFieldComboBox = new ComboBox<>();
    tmpFieldComboBox.getItems().addAll("Actors", "Cinematographer", "Composer",
        "Director", "Duration", "Genre", "IMDb Rating", "Producer", "Writer",
        "Year");
    tmpFieldComboBox.getSelectionModel().selectFirst();
    tmpFieldComboBox.setPrefWidth(200);
    tmpFieldComboBox.setMinWidth(Region.USE_PREF_SIZE);

    TextField tmpValueField = new TextField();
    tmpValueField.setPrefWidth(250);
    tmpValueField.setMinWidth(Region.USE_PREF_SIZE);
    ComboBox<String> tmpOperatorComboBox = new ComboBox<>();
    tmpOperatorComboBox.setPrefWidth(200);
    tmpOperatorComboBox.setMinWidth(Region.USE_PREF_SIZE);
    Runnable tmpUpdateOperators = () -> {
      String tmpSelectedField = tmpFieldComboBox.getValue();
      String tmpCurrentOperator = tmpOperatorComboBox.getValue();
      boolean tmpIsNumeric = "IMDb Rating".equals(tmpSelectedField)
          || "Year".equals(tmpSelectedField)
          || "Duration".equals(tmpSelectedField);

      if (tmpIsNumeric) {
        tmpValueField.setPromptText("Value");
      } else {
        tmpValueField.setPromptText("Text");
      }

      List<String> tmpAllowedOperators;
      if (tmpIsNumeric) {
        tmpAllowedOperators =
            List.of("EQUALS", "NOT_EQUALS", "GREATER_THAN", "LESS_THAN");
      } else {
        tmpAllowedOperators = List.of("EQUALS", "NOT_EQUALS", "CONTAINS");
      }

      if (!tmpOperatorComboBox.getItems().equals(tmpAllowedOperators)) {
        tmpOperatorComboBox.getItems().setAll(tmpAllowedOperators);
        if (tmpCurrentOperator != null
            && tmpAllowedOperators.contains(tmpCurrentOperator)) {
          tmpOperatorComboBox.getSelectionModel().select(tmpCurrentOperator);
        } else {
          tmpOperatorComboBox.getSelectionModel().selectFirst();
        }
      }
    };
    tmpUpdateOperators.run();

    // Proactive input restriction for Name field
    tmpNameField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue != null && (newValue.length() > 33
              || !newValue.matches("\\A\\p{ASCII}*\\z"))) {
            tmpNameField.setText(oldValue);
          }
        });

    // Proactive input restriction for Value field based on active filter field
    tmpValueField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue == null || newValue.isEmpty()) {
            return;
          }
          if (newValue.length() > 33) {
            tmpValueField.setText(oldValue);
            return;
          }
          String tmpField = tmpFieldComboBox.getValue();
          if ("Year".equals(tmpField)) {
            if (!newValue.matches("\\d{0,4}")) {
              tmpValueField.setText(oldValue);
            }
          } else if ("Duration".equals(tmpField)) {
            if (!newValue.matches("\\d*")) {
              tmpValueField.setText(oldValue);
            }
          } else if ("IMDb Rating".equals(tmpField)) {
            if (!newValue.matches("^\\d{0,2}([.,]\\d{0,1})?$")) {
              tmpValueField.setText(oldValue);
            } else {
              try {
                double tmpVal = Double.parseDouble(newValue.replace(",", "."));
                if (tmpVal < 0 || tmpVal > 10) {
                  tmpValueField.setText(oldValue);
                }
              } catch (NumberFormatException tmpException) {
                // Allow intermediate typing like "8." or "8,"
              }
            }
          } else {
            // Text fields: Genre, Director, Writer, Producer, Cinematographer,
            // Composer, Actors
            if (newValue.matches(".*\\d.*")
                || !newValue.matches("\\A\\p{ASCII}*\\z")) {
              tmpValueField.setText(oldValue);
            }
          }
        });

    tmpRuleBox.getChildren().addAll(tmpFieldComboBox, tmpOperatorComboBox,
        tmpValueField);

    Label tmpAsciiInfo = new Label(
        "Only ASCII characters allowed: standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space).");
    tmpAsciiInfo.setStyle(
        "-fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");
    tmpAsciiInfo.setWrapText(true);
    tmpAsciiInfo.setPrefWidth(600);
    tmpAsciiInfo.setMinHeight(Region.USE_PREF_SIZE);

    tmpVbox.getChildren().addAll(tmpErrorLabel, new Label("Name:"),
        tmpNameField, new Label("Rule:"), tmpRuleBox, tmpAsciiInfo);
    tmpDialog.getDialogPane().setContent(tmpVbox);

    Node tmpOkButton = tmpDialog.getDialogPane().lookupButton(tmpOkButtonType);
    if (tmpOkButton != null) {
      tmpOkButton.getStyleClass().add("button-primary");
      tmpOkButton.setDisable(true);
    }

    Node tmpCancelButton =
        tmpDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    if (tmpCancelButton != null) {
      tmpCancelButton.getStyleClass().add("button-primary");
    }

    javafx.beans.value.ChangeListener<String> tmpValidationListener =
        (observable, oldValue, newValue) -> {
          boolean tmpIsValid = true;
          boolean tmpIsNameValid = true;
          String tmpName = tmpNameField.getText().trim();
          String tmpValue = tmpValueField.getText().trim();

          if (tmpName.isEmpty()) {
            tmpIsValid = false;
            tmpIsNameValid = false;
          } else if (!tmpName.matches("\\A\\p{ASCII}*\\z")) {
            tmpErrorLabel.setText(
                "The text you entered is not ASCII code. Only standard English letters, numbers, and basic symbols can be used.");
            tmpErrorLabel.setVisible(true);
            tmpIsValid = false;
            tmpIsNameValid = false;
          } else {
            boolean tmpDuplicate = false;
            List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
            for (Watchlist tmpWatchlist : tmpWatchlists) {
              if (tmpWatchlist.getName().equalsIgnoreCase(tmpName)) {
                tmpDuplicate = true;
                break;
              }
            }
            if (tmpDuplicate) {
              tmpErrorLabel.setText(
                  "A watchlist with this name already exists. Please choose a different name.");
              tmpErrorLabel.setVisible(true);
              tmpIsValid = false;
              tmpIsNameValid = false;
            } else {
              String tmpField = tmpFieldComboBox.getValue();
              if (!tmpValue.isEmpty()) {
                if ("Year".equals(tmpField) && !tmpValue.matches("\\d{4}")) {
                  tmpErrorLabel.setText("Year must be exactly 4 digits.");
                  tmpErrorLabel.setVisible(true);
                  tmpIsValid = false;
                } else if ("IMDb Rating".equals(tmpField)) {
                  try {
                    double tmpRating =
                        Double.parseDouble(tmpValue.replace(",", "."));
                    if (tmpRating < 0 || tmpRating > 10) {
                      tmpErrorLabel
                          .setText("IMDb Rating must be between 0 and 10.");
                      tmpErrorLabel.setVisible(true);
                      tmpIsValid = false;
                    }
                  } catch (NumberFormatException tmpException) {
                    tmpErrorLabel
                        .setText("IMDb Rating must be a valid number.");
                    tmpErrorLabel.setVisible(true);
                    tmpIsValid = false;
                  }
                } else if ("Duration".equals(tmpField)) {
                  if (!tmpValue.matches("\\d+")) {
                    tmpErrorLabel
                        .setText("Duration must be a valid number of minutes.");
                    tmpErrorLabel.setVisible(true);
                    tmpIsValid = false;
                  }
                } else if (!tmpValue.matches("\\A\\p{ASCII}*\\z")) {
                  tmpErrorLabel
                      .setText("The text you entered is not ASCII code.");
                  tmpErrorLabel.setVisible(true);
                  tmpIsValid = false;
                } else if (("Genre".equals(tmpField)
                    || "Director".equals(tmpField) || "Writer".equals(tmpField)
                    || "Producer".equals(tmpField)
                    || "Cinematographer".equals(tmpField)
                    || "Composer".equals(tmpField) || "Actors".equals(tmpField))
                    && tmpValue.matches(".*\\d.*")) {
                  tmpErrorLabel.setText(
                      tmpField + " must be text and cannot contain numbers.");
                  tmpErrorLabel.setVisible(true);
                  tmpIsValid = false;
                }
              }
            }
          }

          tmpRuleBox.setDisable(!tmpIsNameValid);

          if (tmpIsValid) {
            tmpErrorLabel.setVisible(false);
          }

          if (tmpOkButton != null) {
            tmpOkButton.setDisable(
                !tmpIsValid || tmpName.isEmpty() || tmpValue.isEmpty());
          }

          if (tmpDialog.getDialogPane().getScene() != null
              && tmpDialog.getDialogPane().getScene().getWindow() != null) {
            tmpDialog.getDialogPane().getScene().getWindow().sizeToScene();
          }
        };

    tmpNameField.textProperty().addListener(tmpValidationListener);
    tmpValueField.textProperty().addListener(tmpValidationListener);
    tmpFieldComboBox.valueProperty()
        .addListener((observable, oldValue, newValue) -> {
          tmpValueField.clear();
          tmpUpdateOperators.run();
          tmpValidationListener.changed(null, null, null);
        });
    tmpOperatorComboBox.valueProperty().addListener((observable, oldValue,
        newValue) -> tmpValidationListener.changed(null, null, null));

    tmpDialog.setResultConverter(dialogButton -> {
      if (dialogButton == tmpOkButtonType) {
        Watchlist tmpWatchlist = new Watchlist(tmpNameField.getText().trim());
        tmpWatchlist.setSmart(true);
        SmartRule tmpRule = new SmartRule(tmpFieldComboBox.getValue(),
            tmpOperatorComboBox.getValue(), tmpValueField.getText().trim());
        tmpWatchlist.setSmartRulesJson(new Gson().toJson(List.of(tmpRule)));
        return tmpWatchlist;
      }
      return null;
    });

    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpDialog);
    Optional<Watchlist> tmpResult = tmpDialog.showAndWait();
    tmpResult.ifPresent(watchlist -> {
      if (validateWatchlistName(watchlist.getName(), -1)) {
        watchlistRepository.insert(watchlist);
        refresh();
        javafx.application.Platform.runLater(() -> {
          io.github.kullik01.kinonode.util.CustomAlert.showInformation(null,
              "Smart Watchlist Created",
              "Sync the library to fill the created smart watchlist.",
              "Smart watchlists are filled dynamically based on the movies in your library. Please run a Library Sync if your watchlist appears empty.");
        });
      }
    });
  }
}
