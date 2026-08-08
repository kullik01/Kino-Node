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
package io.github.kullik01.kinonode.view.pages;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.service.ImdbService;
import io.github.kullik01.kinonode.service.LibraryScanService;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modal dialog presenting a complete overview of movies processed after syncing
 * the library. Displays each scanned file alongside options to alter matching
 * via Interactive Match Selection or Manual Search via IMDb ID directly next to
 * the filename. Guarantees responsive resizing layout with Ok button always in
 * the bottom-right corner and disables UI interactions during re-indexing tasks
 * to prevent accidental double actions.
 */
public class SyncOverviewView {
  private static final Logger LOGGER =
      Logger.getLogger(SyncOverviewView.class.getName());

  private final Stage ownerStage;
  private final ResourceBundle resources;
  private final LibraryScanService.ScanResult scanResult;
  private final ImdbService imdbService;
  private final MovieRepository movieRepository;

  private Stage dialogStage;
  private VBox movieListContainer;

  /**
   * Constructs a new SyncOverviewView dialog.
   *
   * @param ownerStage the owner stage for modality
   * @param resources the resource bundle for i18n
   * @param scanResult the scan result containing processed movies
   * @param imdbService the IMDb service for re-indexing
   * @param movieRepository the movie repository for persistence
   */
  public SyncOverviewView(Stage ownerStage, ResourceBundle resources,
      LibraryScanService.ScanResult scanResult, ImdbService imdbService,
      MovieRepository movieRepository) {
    this.ownerStage = ownerStage;
    this.resources = resources;
    this.scanResult = scanResult;
    this.imdbService = imdbService;
    this.movieRepository = movieRepository;
  }

  /**
   * Displays the sync overview dialog and blocks until closed.
   */
  public void showAndWait() {
    dialogStage = new Stage();
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    dialogStage.initOwner(ownerStage);
    dialogStage.setTitle("Library Sync Overview");
    dialogStage.setMinWidth(850);
    dialogStage.setMinHeight(650);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setPadding(new Insets(20));
    tmpRoot.setStyle("-fx-background-color: -color-bg-default;");

    // Header Summary Box
    VBox tmpHeaderBox = new VBox(8);
    tmpHeaderBox.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: -color-border-default; -fx-border-radius: 10;");

    Label tmpTitleLbl = new Label("Library Sync Completed");
    tmpTitleLbl.setStyle(
        "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2178dd;");

    String tmpSummaryText = String.format(
        "Total files scanned: %d  •  New movies added: %d  •  Unresolved items: %d",
        scanResult.totalScanned(), scanResult.newAdded(),
        scanResult.unresolvedMovies() != null
            ? scanResult.unresolvedMovies().size()
            : 0);
    Label tmpSummaryLbl = new Label(tmpSummaryText);
    tmpSummaryLbl
        .setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");

    tmpHeaderBox.getChildren().addAll(tmpTitleLbl, tmpSummaryLbl);

    Label tmpListTitle = new Label("Processed Movies Overview");
    tmpListTitle.setStyle(
        "-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-fg-default;");

    VBox tmpTopBox = new VBox(10, tmpHeaderBox, tmpListTitle);
    tmpRoot.setTop(tmpTopBox);

    // Scrollable movie rows container
    movieListContainer = new VBox(12);
    movieListContainer.setPadding(new Insets(5));
    ScrollPane tmpScroll = new ScrollPane(movieListContainer);
    tmpScroll.setFitToWidth(true);
    tmpScroll.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(tmpScroll, Priority.ALWAYS);
    BorderPane.setMargin(tmpScroll, new Insets(12, 0, 12, 0));

    populateMovieRows();
    tmpRoot.setCenter(tmpScroll);

    // Bottom Ok Button - ALWAYS pinned at bottom right corner
    Button btnDone = new Button("OK");
    btnDone.getStyleClass().add("button-primary");
    btnDone.setStyle(
        "-fx-padding: 8px 28px; -fx-font-size: 14px; -fx-font-weight: bold;");
    btnDone.setOnAction(e -> dialogStage.close());

    HBox tmpBottomBox = new HBox(btnDone);
    tmpBottomBox.setAlignment(Pos.BOTTOM_RIGHT);
    tmpRoot.setBottom(tmpBottomBox);

    Scene tmpScene = new Scene(tmpRoot, 900, 700);
    if (ownerStage.getScene() != null) {
      tmpScene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
    }
    dialogStage.setScene(tmpScene);
    dialogStage.showAndWait();
  }

  private void populateMovieRows() {
    movieListContainer.getChildren().clear();

    List<Movie> tmpMovies = scanResult.scannedMovies();
    if (tmpMovies == null || tmpMovies.isEmpty()) {
      Label tmpEmptyLbl = new Label("No movies processed during this scan.");
      tmpEmptyLbl
          .setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");
      movieListContainer.getChildren().add(tmpEmptyLbl);
      return;
    }

    for (Movie tmpMovie : tmpMovies) {
      movieListContainer.getChildren().add(createMovieRow(tmpMovie));
    }
  }

  private HBox createMovieRow(Movie movie) {
    HBox tmpRow = new HBox(15);
    tmpRow.setAlignment(Pos.CENTER_LEFT);
    tmpRow.setStyle(
        "-fx-background-color: -color-bg-subtle; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: -color-border-default; -fx-border-radius: 8;");

    // Poster preview
    ImageView tmpPosterView = new ImageView();
    tmpPosterView.setFitWidth(60);
    tmpPosterView.setFitHeight(90);
    tmpPosterView.setPreserveRatio(true);

    if (movie.getCoverImagePath() != null
        && !movie.getCoverImagePath().isBlank()) {
      File tmpImgFile = new File(movie.getCoverImagePath());
      if (tmpImgFile.exists()) {
        try {
          tmpPosterView
              .setImage(new Image(tmpImgFile.toURI().toURL().toExternalForm(),
                  60, 90, true, true, true));
        } catch (MalformedURLException tmpException) {
          LOGGER.log(Level.WARNING, "Failed to load poster image",
              tmpException);
        }
      }
    }

    // Movie details and filename information
    VBox tmpInfoBox = new VBox(4);
    HBox.setHgrow(tmpInfoBox, Priority.ALWAYS);

    File tmpFile = new File(movie.getFilePath());
    Label tmpFileLbl = new Label("Filename: " + tmpFile.getName());
    tmpFileLbl.setStyle(
        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2178dd;");

    Label tmpTitleLbl = new Label(
        "Title: " + (movie.getTitle() != null ? movie.getTitle() : "—"));
    tmpTitleLbl.setStyle(
        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

    String tmpYearStr =
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "—";
    String tmpMatchStr =
        movie.getMatchStatus() != null ? movie.getMatchStatus() : "MATCHED";
    Label tmpSubLbl =
        new Label("Year: " + tmpYearStr + "  •  Status: " + tmpMatchStr);
    tmpSubLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");

    tmpInfoBox.getChildren().addAll(tmpFileLbl, tmpTitleLbl, tmpSubLbl);

    if (movie.getDirector() != null && !movie.getDirector().isBlank()) {
      Label tmpDirLbl = new Label("Director: " + movie.getDirector());
      tmpDirLbl
          .setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-subtle;");
      tmpInfoBox.getChildren().add(tmpDirLbl);
    }

    // Action buttons next to filename and details
    VBox tmpActionsBox = new VBox(8);
    tmpActionsBox.setAlignment(Pos.CENTER_RIGHT);

    Button buttonInteractiveMatch = new Button("Interactive Match Selection");
    buttonInteractiveMatch.getStyleClass().add("button-primary");
    buttonInteractiveMatch
        .setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px;");
    buttonInteractiveMatch
        .setOnAction(e -> openInteractiveMatchDialog(movie, tmpRow));

    Button buttonManualImdb = new Button("Manual Search via IMDb ID");
    buttonManualImdb.getStyleClass().add("button-secondary");
    buttonManualImdb.setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px;");
    buttonManualImdb.setOnAction(e -> openManualImdbDialog(movie, tmpRow));

    tmpActionsBox.getChildren().addAll(buttonInteractiveMatch,
        buttonManualImdb);

    tmpRow.getChildren().addAll(tmpPosterView, tmpInfoBox, tmpActionsBox);
    return tmpRow;
  }

  private void openInteractiveMatchDialog(Movie movie, HBox rowNode) {
    Stage tmpMatchStage = new Stage();
    tmpMatchStage.initModality(Modality.APPLICATION_MODAL);
    tmpMatchStage.initOwner(dialogStage);
    tmpMatchStage.setTitle("Interactive Match Selection — " + movie.getTitle());
    tmpMatchStage.setMinWidth(750);
    tmpMatchStage.setMinHeight(600);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setPadding(new Insets(20));
    tmpRoot.setStyle("-fx-background-color: -color-bg-default;");

    Label tmpHeaderLabel = new Label("Interactive Match Selection");
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");

    File tmpFile = new File(movie.getFilePath());
    Label tmpFileLabel = new Label("File: " + tmpFile.getName());
    tmpFileLabel
        .setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");

    TextField tmpSearchInput = new TextField(movie.getTitle());
    tmpSearchInput.setPromptText("Enter movie title to search candidates...");
    Button buttonSearchCandidates = new Button("Search Candidates");
    buttonSearchCandidates.getStyleClass().add("button-primary");
    HBox tmpSearchBox = new HBox(10, tmpSearchInput, buttonSearchCandidates);
    HBox.setHgrow(tmpSearchInput, Priority.ALWAYS);

    VBox tmpTopBox = new VBox(8, tmpHeaderLabel, tmpFileLabel, tmpSearchBox);
    tmpRoot.setTop(tmpTopBox);

    VBox tmpCandidateContainer = new VBox(10);
    ScrollPane tmpScroll = new ScrollPane(tmpCandidateContainer);
    tmpScroll.setFitToWidth(true);
    tmpScroll.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(tmpScroll, Priority.ALWAYS);
    BorderPane.setMargin(tmpScroll, new Insets(12, 0, 12, 0));
    tmpRoot.setCenter(tmpScroll);

    // OK Button ALWAYS pinned at bottom right corner
    Button btnCancel = new Button("OK");
    btnCancel.getStyleClass().add("button-secondary");
    btnCancel.setOnAction(e -> tmpMatchStage.close());
    HBox tmpBottom = new HBox(btnCancel);
    tmpBottom.setAlignment(Pos.BOTTOM_RIGHT);
    tmpRoot.setBottom(tmpBottom);

    Scene tmpScene = new Scene(tmpRoot, 800, 650);
    if (dialogStage.getScene() != null) {
      tmpScene.getStylesheets().addAll(dialogStage.getScene().getStylesheets());
    }

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
              .add(new Label("No candidate matches found."));
        } else {
          for (Movie tmpCand : tmpResults) {
            HBox tmpCandRow = new HBox(15);
            tmpCandRow.setAlignment(Pos.CENTER_LEFT);
            tmpCandRow.setStyle(
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

            Button buttonSelectMatch = new Button("Select & Re-index");
            buttonSelectMatch.getStyleClass().add("button-primary");
            buttonSelectMatch.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 16px;");
            buttonSelectMatch.setOnAction(e -> {
              String tmpTargetId = tmpCand.getImdbId();
              if (tmpTargetId == null || tmpTargetId.isBlank())
                return;

              // Prevent user from clicking anywhere during re-indexing
              tmpRoot.setDisable(true);
              tmpScene.setCursor(Cursor.WAIT);

              Task<Void> tmpReindexTask = new Task<>() {
                @Override
                protected Void call() {
                  imdbService.enrichMovieDataById(tmpTargetId, movie);
                  movie.setMatchStatus("MANUAL");
                  movieRepository.update(movie);
                  return null;
                }
              };
              tmpReindexTask.setOnSucceeded(e2 -> {
                tmpMatchStage.close();
                refreshRowInOverview(movie, rowNode);
              });
              tmpReindexTask.setOnFailed(e2 -> {
                tmpRoot.setDisable(false);
                tmpScene.setCursor(Cursor.DEFAULT);
              });
              Thread t = new Thread(tmpReindexTask);
              t.setDaemon(true);
              t.start();
            });

            tmpCandRow.getChildren().addAll(tmpPoster, tmpInfo, tmpSp,
                buttonSelectMatch);
            tmpCandidateContainer.getChildren().add(tmpCandRow);
          }
        }
      });

      Thread t = new Thread(tmpTask);
      t.setDaemon(true);
      t.start();
    };

    buttonSearchCandidates.setOnAction(e -> tmpPerformSearch.run());
    tmpSearchInput.setOnAction(e -> tmpPerformSearch.run());

    tmpMatchStage.setScene(tmpScene);
    tmpPerformSearch.run();
    tmpMatchStage.showAndWait();
  }

  private void openManualImdbDialog(Movie movie, HBox rowNode) {
    Stage tmpImdbStage = new Stage();
    tmpImdbStage.initModality(Modality.APPLICATION_MODAL);
    tmpImdbStage.initOwner(dialogStage);
    tmpImdbStage.setTitle("Manual Search via IMDb ID — " + movie.getTitle());
    tmpImdbStage.setMinWidth(520);
    tmpImdbStage.setMinHeight(240);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setPadding(new Insets(20));
    tmpRoot.setStyle("-fx-background-color: -color-bg-default;");

    Label tmpHeaderLabel = new Label("Manual Search via IMDb ID");
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");

    File tmpFile = new File(movie.getFilePath());
    Label tmpFileLabel = new Label("File: " + tmpFile.getName());
    tmpFileLabel
        .setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");

    VBox tmpTopBox = new VBox(8, tmpHeaderLabel, tmpFileLabel);
    tmpRoot.setTop(tmpTopBox);

    TextField tmpInput = new TextField();
    tmpInput.setPromptText("Enter IMDb ID (e.g. tt0111161)");

    Button buttonImport = new Button("Instant Import / Re-index");
    buttonImport.getStyleClass().add("button-success");

    HBox tmpBox = new HBox(10, tmpInput, buttonImport);
    HBox.setHgrow(tmpInput, Priority.ALWAYS);
    BorderPane.setMargin(tmpBox, new Insets(15, 0, 15, 0));
    tmpRoot.setCenter(tmpBox);

    // OK Button ALWAYS pinned at bottom right corner
    Button btnCancel = new Button("OK");
    btnCancel.getStyleClass().add("button-secondary");
    btnCancel.setOnAction(e -> tmpImdbStage.close());

    HBox tmpBottom = new HBox(btnCancel);
    tmpBottom.setAlignment(Pos.BOTTOM_RIGHT);
    tmpRoot.setBottom(tmpBottom);

    Scene tmpScene = new Scene(tmpRoot, 520, 240);
    if (dialogStage.getScene() != null) {
      tmpScene.getStylesheets().addAll(dialogStage.getScene().getStylesheets());
    }

    buttonImport.setOnAction(e -> {
      String tmpId = tmpInput.getText().trim();
      if (!tmpId.matches("^tt\\d{7,}$")) {
        tmpInput.setStyle("-fx-border-color: red;");
        return;
      }
      tmpInput.setStyle("");

      // Prevent any interactions during import
      tmpRoot.setDisable(true);
      tmpScene.setCursor(Cursor.WAIT);

      Task<Void> tmpTask = new Task<>() {
        @Override
        protected Void call() {
          imdbService.enrichMovieDataById(tmpId, movie);
          movie.setMatchStatus("MANUAL");
          movieRepository.update(movie);
          return null;
        }
      };
      tmpTask.setOnSucceeded(evt -> {
        tmpImdbStage.close();
        refreshRowInOverview(movie, rowNode);
      });
      tmpTask.setOnFailed(evt -> {
        tmpRoot.setDisable(false);
        tmpScene.setCursor(Cursor.DEFAULT);
      });
      Thread t = new Thread(tmpTask);
      t.setDaemon(true);
      t.start();
    });

    tmpImdbStage.setScene(tmpScene);
    tmpImdbStage.showAndWait();
  }

  private void refreshRowInOverview(Movie movie, HBox rowNode) {
    int index = movieListContainer.getChildren().indexOf(rowNode);
    if (index >= 0) {
      movieListContainer.getChildren().set(index, createMovieRow(movie));
    }
  }
}
