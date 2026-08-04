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
import java.io.File;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modal dialog view for resolving ambiguous movie matches during library sync.
 * Presents candidate movies from IMDb search results and allows the user to
 * select the correct match, enter an IMDb ID manually, or skip resolution.
 */
public class DisambiguationView {
  private static final Logger LOGGER =
      Logger.getLogger(DisambiguationView.class.getName());

  private final ResourceBundle resources;
  private final List<Movie> unresolvedMovies;
  private final ImdbService imdbService;
  private final MovieRepository movieRepository;
  private final Stage ownerStage;

  private Stage dialogStage;
  private int currentIndex;
  private Label lblProgress;
  private Label lblFileName;
  private Label lblExtractedTitle;
  private VBox candidateList;
  private TextField txtImdbId;
  private VBox rootLayout;

  /**
   * Constructs a new DisambiguationView dialog.
   *
   * @param ownerStage the owner stage for modality
   * @param resources the resource bundle for i18n strings
   * @param unresolvedMovies the list of movies needing disambiguation
   * @param imdbService the IMDb service for lookups
   * @param movieRepository the movie repository for persistence
   */
  public DisambiguationView(Stage ownerStage, ResourceBundle resources,
      List<Movie> unresolvedMovies, ImdbService imdbService,
      MovieRepository movieRepository) {
    this.ownerStage = ownerStage;
    this.resources = resources;
    this.unresolvedMovies = unresolvedMovies;
    this.imdbService = imdbService;
    this.movieRepository = movieRepository;
    this.currentIndex = 0;
  }

  /**
   * Shows the disambiguation dialog and blocks until closed.
   */
  public void showAndWait() {
    if (unresolvedMovies == null || unresolvedMovies.isEmpty()) {
      return;
    }

    dialogStage = new Stage();
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    dialogStage.initOwner(ownerStage);
    dialogStage.setTitle(resources.getString("library.disambiguation.title"));
    dialogStage.setMinWidth(650);
    dialogStage.setMinHeight(550);

    rootLayout = new VBox(15);
    rootLayout.setPadding(new Insets(20));
    rootLayout.setStyle("-fx-background-color: -color-bg-default;");

    // Header info
    lblProgress = new Label();
    lblProgress.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

    lblFileName = new Label();
    lblFileName.setWrapText(true);
    lblExtractedTitle = new Label();
    lblExtractedTitle.setStyle("-fx-text-fill: -color-accent-emphasis;");

    VBox tmpHeaderBox =
        new VBox(5, lblProgress, lblFileName, lblExtractedTitle);
    tmpHeaderBox.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 15; -fx-background-radius: 8;");

    // Middle section: Scrollable candidate list
    Label tmpSelectLabel =
        new Label(resources.getString("library.disambiguation.select"));
    candidateList = new VBox(10);
    ScrollPane tmpScroll = new ScrollPane(candidateList);
    tmpScroll.setFitToWidth(true);
    tmpScroll.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(tmpScroll, Priority.ALWAYS);

    // Manual IMDb ID section
    Label tmpManualLabel =
        new Label(resources.getString("library.disambiguation.imdb.prompt"));
    txtImdbId = new TextField();
    txtImdbId.setPromptText("tt0133093");
    Button btnLookup =
        new Button(resources.getString("library.disambiguation.imdb.lookup"));
    btnLookup.getStyleClass().add("button-secondary");
    btnLookup.setOnAction(e -> handleManualLookup());
    HBox tmpManualBox = new HBox(10, tmpManualLabel, txtImdbId, btnLookup);
    tmpManualBox.setAlignment(Pos.CENTER_LEFT);

    // Bottom action buttons
    Button btnSkip =
        new Button(resources.getString("library.disambiguation.skip"));
    btnSkip.setOnAction(e -> handleSkip());
    Button btnSkipAll =
        new Button(resources.getString("library.disambiguation.skip.all"));
    btnSkipAll.setOnAction(e -> handleSkipAll());
    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);
    HBox tmpActionBox = new HBox(10, btnSkipAll, tmpSpacer, btnSkip);

    rootLayout.getChildren().addAll(tmpHeaderBox, tmpSelectLabel, tmpScroll,
        tmpManualBox, tmpActionBox);

    Scene tmpScene = new Scene(rootLayout, 700, 600);
    // Add theme stylesheets
    tmpScene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());

    dialogStage.setScene(tmpScene);

    loadCurrentMovie();

    dialogStage.showAndWait();
  }

  private void loadCurrentMovie() {
    if (currentIndex >= unresolvedMovies.size()) {
      dialogStage.close();
      return;
    }

    Movie tmpCurrentMovie = unresolvedMovies.get(currentIndex);

    lblProgress.setText(MessageFormat.format(
        resources.getString("library.disambiguation.progress"),
        (currentIndex + 1), unresolvedMovies.size()));

    File tmpFile = new File(tmpCurrentMovie.getFilePath());
    lblFileName.setText(resources.getString("library.disambiguation.file")
        + ": " + tmpFile.getName());
    lblExtractedTitle
        .setText(resources.getString("library.disambiguation.extracted") + ": "
            + tmpCurrentMovie.getTitle());

    txtImdbId.clear();
    candidateList.getChildren().clear();

    // Start background task to fetch suggestions
    Task<List<Movie>> tmpFetchTask = new Task<>() {
      @Override
      protected List<Movie> call() {
        return imdbService.searchSuggestions(tmpCurrentMovie.getTitle());
      }
    };

    tmpFetchTask.setOnSucceeded(event -> {
      List<Movie> tmpSuggestions = tmpFetchTask.getValue();
      if (tmpSuggestions.isEmpty()) {
        Label tmpNoResults =
            new Label(resources.getString("library.disambiguation.no.results"));
        candidateList.getChildren().add(tmpNoResults);
      } else {
        for (Movie tmpCandidate : tmpSuggestions) {
          candidateList.getChildren().add(createCandidateRow(tmpCandidate));
        }
      }
    });

    Thread tmpThread = new Thread(tmpFetchTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  private HBox createCandidateRow(Movie candidate) {
    HBox tmpRow = new HBox(15);
    tmpRow.setAlignment(Pos.CENTER_LEFT);
    tmpRow.setStyle(
        "-fx-background-color: -color-bg-subtle; -fx-padding: 10; -fx-background-radius: 5;");

    ImageView tmpPosterView = new ImageView();
    tmpPosterView.setFitWidth(60);
    tmpPosterView.setFitHeight(90);
    tmpPosterView.setPreserveRatio(true);

    if (candidate.getCoverImagePath() != null) {
      File tmpImgFile = new File(candidate.getCoverImagePath());
      if (tmpImgFile.exists()) {
        try {
          Image tmpImage =
              new Image(tmpImgFile.toURI().toURL().toExternalForm(), 60, 90,
                  true, true, true);
          tmpPosterView.setImage(tmpImage);
        } catch (MalformedURLException tmpException) {
          LOGGER.log(Level.WARNING, "Failed to load poster URL", tmpException);
        }
      }
    }

    VBox tmpInfoBox = new VBox(5);
    Label tmpTitleLbl = new Label(candidate.getTitle());
    tmpTitleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    Label tmpYearLbl = new Label(
        candidate.getYear() != null ? String.valueOf(candidate.getYear()) : "");
    tmpInfoBox.getChildren().addAll(tmpTitleLbl, tmpYearLbl);

    Region tmpSpacer = new Region();
    HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

    Button btnSelect =
        new Button(resources.getString("library.disambiguation.select.button"));
    btnSelect.getStyleClass().add("button-primary");
    btnSelect.setOnAction(e -> handleSelectCandidate(candidate));

    tmpRow.getChildren().addAll(tmpPosterView, tmpInfoBox, tmpSpacer,
        btnSelect);
    return tmpRow;
  }

  private void handleSelectCandidate(Movie candidate) {
    Movie tmpCurrentMovie = unresolvedMovies.get(currentIndex);

    // Extract ID from coverImagePath or use dummy for now if missing
    String tmpId = extractImdbId(candidate.getCoverImagePath());
    if (tmpId == null) {
      // fallback if no cover image
      tmpId = "tt0000000";
    }

    final String tmpFinalId = tmpId;

    Task<Void> tmpEnrichTask = new Task<>() {
      @Override
      protected Void call() {
        imdbService.enrichMovieDataById(tmpFinalId, tmpCurrentMovie);
        tmpCurrentMovie.setMatchStatus("MANUAL");
        movieRepository.update(tmpCurrentMovie);
        return null;
      }
    };

    tmpEnrichTask.setOnSucceeded(e -> {
      currentIndex++;
      loadCurrentMovie();
    });

    Thread tmpThread = new Thread(tmpEnrichTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  private String extractImdbId(String imagePath) {
    if (imagePath == null)
      return null;
    File tmpFile = new File(imagePath);
    String tmpName = tmpFile.getName();
    if (tmpName.contains(".")) {
      tmpName = tmpName.substring(0, tmpName.lastIndexOf('.'));
    }
    return tmpName;
  }

  private void handleManualLookup() {
    String tmpImdbId = txtImdbId.getText().trim();
    if (!tmpImdbId.matches("^tt\\d{7,}$")) {
      txtImdbId.setStyle("-fx-border-color: red;");
      return;
    }
    txtImdbId.setStyle("");

    Movie tmpCurrentMovie = unresolvedMovies.get(currentIndex);

    Task<Void> tmpEnrichTask = new Task<>() {
      @Override
      protected Void call() {
        imdbService.enrichMovieDataById(tmpImdbId, tmpCurrentMovie);
        tmpCurrentMovie.setMatchStatus("MANUAL");
        movieRepository.update(tmpCurrentMovie);
        return null;
      }
    };

    tmpEnrichTask.setOnSucceeded(e -> {
      currentIndex++;
      loadCurrentMovie();
    });

    Thread tmpThread = new Thread(tmpEnrichTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  private void handleSkip() {
    Movie tmpCurrentMovie = unresolvedMovies.get(currentIndex);
    tmpCurrentMovie.setMatchStatus("UNMATCHED");
    // Since it's already in DB from scan, update the status
    Task<Void> tmpUpdateTask = new Task<>() {
      @Override
      protected Void call() {
        movieRepository.update(tmpCurrentMovie);
        return null;
      }
    };
    tmpUpdateTask.setOnSucceeded(e -> {
      currentIndex++;
      loadCurrentMovie();
    });
    Thread tmpThread = new Thread(tmpUpdateTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  private void handleSkipAll() {
    Task<Void> tmpUpdateTask = new Task<>() {
      @Override
      protected Void call() {
        for (int i = currentIndex; i < unresolvedMovies.size(); i++) {
          Movie tmpMovie = unresolvedMovies.get(i);
          tmpMovie.setMatchStatus("UNMATCHED");
          movieRepository.update(tmpMovie);
        }
        return null;
      }
    };
    tmpUpdateTask.setOnSucceeded(e -> dialogStage.close());
    Thread tmpThread = new Thread(tmpUpdateTask);
    tmpThread.setDaemon(true);
    tmpThread.start();
  }
}
