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

import io.github.kullik01.kinonode.uiutil.DialogBuilder;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.di.AppContext;
import io.github.kullik01.kinonode.util.PathUtils;
import io.github.kullik01.kinonode.service.ImdbService;
import io.github.kullik01.kinonode.service.PlaybackService;
import io.github.kullik01.kinonode.service.DatabaseService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Control;
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
public class MovieDetailController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(MovieDetailController.class.getName());
  private static Image PLACEHOLDER_IMAGE;

  private Label labelHeaderTitle;
  private ImageView imgPoster;
  private Button buttonPlay;
  private Button buttonTrailer;
  private Button btnImdbPage;

  private TextField textFieldTitle;
  private TextField textFieldYear;
  private TextField txtDuration;
  private TextField textFieldGenre;
  private TextField textFieldDirector;
  private TextField textFieldWriter;
  private TextField textFieldProducer;
  private TextField textFieldCinematographer;
  private TextField textFieldComposer;
  private TextField textFieldActors;
  private ComboBox<String> comboBoxDirector;
  private ComboBox<String> comboBoxWriter;
  private ComboBox<String> comboBoxProducer;
  private ComboBox<String> comboBoxCinematographer;
  private ComboBox<String> comboBoxComposer;
  private ComboBox<String> comboBoxActors;
  private TextArea textFieldDescription;
  private TextField textFieldTrailerUrl;

  private FlowPane ratingContainer;
  private Label lblWatchlistsList;
  private Button btnWatchlists;
  private Label labelStatus;

  private Movie movie;
  private Integer currentRating; // Nullable

  private final MovieRepository movieRepository;
  private final WatchlistRepository watchlistRepository;
  private final AppContext appContext;
  private final ImdbService imdbService;
  private final PlaybackService playbackService;
  private final DatabaseService databaseService;
  private final Provider<LibraryController> libraryControllerProvider;

  @Inject
  public MovieDetailController(MovieRepository movieRepository,
      WatchlistRepository watchlistRepository, AppContext appContext,
      ImdbService imdbService, PlaybackService playbackService,
      DatabaseService databaseService,
      Provider<LibraryController> libraryControllerProvider) {
    this.movieRepository = movieRepository;
    this.watchlistRepository = watchlistRepository;
    this.appContext = appContext;
    this.imdbService = imdbService;
    this.playbackService = playbackService;
    this.databaseService = databaseService;
    this.libraryControllerProvider = libraryControllerProvider;
  }

  private javafx.animation.PauseTransition statusTimer;

  private void showStatus(String message) {
    labelStatus.setText(message);
    if (statusTimer != null) {
      statusTimer.stop();
    }
    // Automatically clear status labels after 5 seconds
    statusTimer = new javafx.animation.PauseTransition(Duration.seconds(5));
    statusTimer.setOnFinished(e -> labelStatus.setText(""));
    statusTimer.play();
  }

  public Parent buildView() {
    VBox rootPane = new VBox(20.0);
    rootPane.getStyleClass().add("detail-pane");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());
    rootPane.setPadding(new Insets(25.0));

    // Header / Back button
    HBox headerBox = new HBox(15.0);
    headerBox.setAlignment(Pos.CENTER_LEFT);

    Button buttonBack = new Button("⬅");
    buttonBack.getStyleClass().add("button-secondary");
    buttonBack.setMinWidth(Region.USE_PREF_SIZE);
    buttonBack.setOnAction(e -> goBack());
    Tooltip ttBack = new Tooltip("Back to Library");
    ttBack.setShowDelay(Duration.millis(250));
    buttonBack.setTooltip(ttBack);

    labelHeaderTitle = new Label("Movie Details");
    labelHeaderTitle.getStyleClass().add("detail-title");

    headerBox.getChildren().addAll(buttonBack, labelHeaderTitle);

    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setFitToWidth(true);
    scrollPane.getStyleClass().add("edge-to-edge");
    scrollPane.setStyle(
        "-fx-background-color: transparent; -fx-control-inner-background: transparent;");
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    HBox contentHBox = new HBox(30.0);
    contentHBox.setStyle("-fx-padding: 0 10px 0 0;");

    // Left Side: Poster and Actions
    VBox leftBox = new VBox(15.0);
    leftBox.setAlignment(Pos.TOP_CENTER);
    leftBox.setPrefWidth(360.0);

    VBox posterContainer = new VBox();
    posterContainer.setStyle(
        "-fx-pref-width: 350px; -fx-pref-height: 525px; -fx-background-radius: 12px;");
    posterContainer.getStyleClass().add("movie-poster-container");
    imgPoster = new ImageView();
    imgPoster.setFitHeight(525.0);
    imgPoster.setFitWidth(350.0);
    imgPoster.getStyleClass().add("detail-cover");
    posterContainer.getChildren().add(imgPoster);

    Button buttonChangePoster = new Button();
    buttonChangePoster.getStyleClass().addAll("button-secondary",
        "button-icon");
    buttonChangePoster.setMinSize(36.0, 36.0);
    buttonChangePoster.setMaxSize(36.0, 36.0);
    buttonChangePoster.setOnAction(e -> changePoster());
    SVGPath svgImageChangePoster = new SVGPath();
    svgImageChangePoster.setContent(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-1 16H6c-.55 0-1-.45-1-1V6c0-.55.45-1 1-1h12c.55 0 1 .45 1 1v12c0 .55-.45 1-1 1zm-4.44-6.23L11.2 15.4l-2.45-3.07c-.2-.25-.59-.25-.79 0l-2.71 3.39c-.26.33-.02.81.39.81h12.7c.41 0 .65-.48.39-.81l-3.84-4.8c-.2-.26-.6-.26-.8 0z");
    svgImageChangePoster.setFill(Color.WHITE);
    svgImageChangePoster.setScaleX(0.75);
    svgImageChangePoster.setScaleY(0.75);
    buttonChangePoster.setGraphic(svgImageChangePoster);
    Tooltip ttChangePoster = new Tooltip("Change Poster");
    ttChangePoster.setShowDelay(Duration.millis(250));
    buttonChangePoster.setTooltip(ttChangePoster);

    VBox actionBox = new VBox(15.0);
    actionBox.setAlignment(Pos.CENTER);
    actionBox.setStyle("-fx-padding: 10px 0 0 0;");

    buttonPlay = new Button();
    buttonPlay.getStyleClass().addAll("button-play", "button-icon");
    buttonPlay.setMinSize(36.0, 36.0);
    buttonPlay.setMaxSize(36.0, 36.0);
    buttonPlay.setOnAction(e -> playMovie());
    SVGPath svgImagePlay = new SVGPath();
    svgImagePlay.setContent("M8 5v14l11-7z");
    svgImagePlay.setFill(Color.WHITE);
    svgImagePlay.setScaleX(0.75);
    svgImagePlay.setScaleY(0.75);
    buttonPlay.setGraphic(svgImagePlay);
    Tooltip ttPlay = new Tooltip("Play Movie");
    ttPlay.setShowDelay(Duration.millis(250));
    buttonPlay.setTooltip(ttPlay);

    buttonTrailer = new Button();
    buttonTrailer.getStyleClass().addAll("button-secondary", "button-icon");
    buttonTrailer.setMinSize(36.0, 36.0);
    buttonTrailer.setMaxSize(36.0, 36.0);
    buttonTrailer.setOnAction(e -> watchTrailer());
    SVGPath svgImageTrailer = new SVGPath();
    svgImageTrailer.setContent(
        "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12zm-5-6l-7 4V7z");
    svgImageTrailer.setFill(Color.WHITE);
    svgImageTrailer.setScaleX(0.75);
    svgImageTrailer.setScaleY(0.75);
    buttonTrailer.setGraphic(svgImageTrailer);
    Tooltip ttTrailer = new Tooltip("Watch Trailer");
    ttTrailer.setShowDelay(Duration.millis(250));
    buttonTrailer.setTooltip(ttTrailer);

    btnImdbPage = new Button();
    btnImdbPage.getStyleClass().addAll("button-imdb", "button-icon");
    btnImdbPage.setMinSize(36.0, 36.0);
    btnImdbPage.setMaxSize(36.0, 36.0);
    btnImdbPage.setOnAction(e -> openImdbPage());
    SVGPath svgImdbPage = new SVGPath();
    svgImdbPage.setContent(
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z");
    svgImdbPage.setFill(Color.WHITE);
    svgImdbPage.setScaleX(0.75);
    svgImdbPage.setScaleY(0.75);
    btnImdbPage.setGraphic(svgImdbPage);
    Tooltip ttImdbPage = new Tooltip("Open in IMDb");
    ttImdbPage.setShowDelay(Duration.millis(250));
    btnImdbPage.setTooltip(ttImdbPage);

    Button buttonFixMatch = new Button();
    buttonFixMatch.getStyleClass().addAll("button-secondary", "button-icon");
    buttonFixMatch.setMinSize(36.0, 36.0);
    buttonFixMatch.setMaxSize(36.0, 36.0);
    buttonFixMatch.setOnAction(e -> showFixMatchDialog());
    SVGPath svgFixMatch = new SVGPath();
    svgFixMatch.setContent(
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
    svgFixMatch.setFill(Color.WHITE);
    svgFixMatch.setScaleX(0.75);
    svgFixMatch.setScaleY(0.75);
    buttonFixMatch.setGraphic(svgFixMatch);
    Tooltip ttFixMatch = new Tooltip("Match & Re-index Movie");
    ttFixMatch.setShowDelay(Duration.millis(250));
    buttonFixMatch.setTooltip(ttFixMatch);

    actionBox.getChildren().addAll(buttonPlay, buttonTrailer, btnImdbPage,
        buttonFixMatch);
    leftBox.getChildren().addAll(posterContainer, buttonChangePoster,
        actionBox);

    // Right Side: Details Grid
    VBox rightBox = new VBox(15.0);
    HBox.setHgrow(rightBox, Priority.ALWAYS);

    GridPane grid = new GridPane();
    grid.setHgap(15.0);
    grid.setVgap(15.0);
    VBox.setVgrow(grid, Priority.ALWAYS);

    ColumnConstraints columnConstraint1 = new ColumnConstraints();
    columnConstraint1.setMinWidth(160.0);
    columnConstraint1.setPrefWidth(180.0);
    ColumnConstraints columnConstraint2 = new ColumnConstraints();
    columnConstraint2.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().addAll(columnConstraint1, columnConstraint2);

    for (int i = 0; i < 13; i++) {
      RowConstraints rowConstraint = new RowConstraints();
      rowConstraint.setMinHeight(30.0);
      grid.getRowConstraints().add(rowConstraint);
    }
    RowConstraints rowConstraintDescription = new RowConstraints();
    rowConstraintDescription.setMinHeight(100.0);
    grid.getRowConstraints().add(rowConstraintDescription);

    // Helpers for grid rows
    int row = 0;

    // Title
    textFieldTitle = createReadOnlyField();
    addGridRow(grid, row++, "Title:", textFieldTitle, e -> editTitle());

    // Year
    textFieldYear = createReadOnlyField();
    addGridRow(grid, row++, "Release Year:", textFieldYear, e -> editYear());

    // Duration
    txtDuration = createReadOnlyField();
    addGridRow(grid, row++, "Duration:", txtDuration, e -> editDuration());

    // Genre
    textFieldGenre = createReadOnlyField();
    addGridRow(grid, row++, "Genre:", textFieldGenre, e -> editGenre());

    // Director
    textFieldDirector = createReadOnlyField();
    comboBoxDirector = new ComboBox<>();
    addMultiGridRow(grid, row++, "Director(s):", textFieldDirector,
        comboBoxDirector, e -> editDirector(), e -> searchDirectorOnImdb());

    // Writer
    textFieldWriter = createReadOnlyField();
    comboBoxWriter = new ComboBox<>();
    addMultiGridRow(grid, row++, "Writer(s):", textFieldWriter, comboBoxWriter,
        e -> editWriter(), e -> searchWriterOnImdb());

    // Producer
    textFieldProducer = createReadOnlyField();
    comboBoxProducer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Producer(s):", textFieldProducer,
        comboBoxProducer, e -> editProducer(), e -> searchProducerOnImdb());

    // Cinematographer
    textFieldCinematographer = createReadOnlyField();
    comboBoxCinematographer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Cinematographer(s):",
        textFieldCinematographer, comboBoxCinematographer,
        e -> editCinematographer(), e -> searchCinematographerOnImdb());

    // Composer
    textFieldComposer = createReadOnlyField();
    comboBoxComposer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Composer(s):", textFieldComposer,
        comboBoxComposer, e -> editComposer(), e -> searchComposerOnImdb());

    // Actors
    textFieldActors = createReadOnlyField();
    comboBoxActors = new ComboBox<>();
    addMultiGridRow(grid, row++, "Actor(s):", textFieldActors, comboBoxActors,
        e -> editActors(), e -> searchActorsOnImdb());

    // Trailer URL
    Label lblTrailer = new Label("Trailer Link:");
    lblTrailer.setStyle("-fx-font-weight: bold;");
    grid.add(lblTrailer, 0, row);

    HBox trailerBox = new HBox(5.0);
    textFieldTrailerUrl = createReadOnlyField();
    textFieldTrailerUrl.setPromptText("https://www.youtube.com/watch?v=...");
    HBox.setHgrow(textFieldTrailerUrl, Priority.ALWAYS);
    Button btnEditTrailer = new Button("📝");
    btnEditTrailer.getStyleClass().add("button-secondary");
    btnEditTrailer.setMinWidth(Region.USE_PREF_SIZE);
    btnEditTrailer.setOnAction(e -> editTrailerUrl());
    Tooltip ttEditTrailer = new Tooltip("Edit Trailer Link");
    ttEditTrailer.setShowDelay(Duration.millis(250));
    btnEditTrailer.setTooltip(ttEditTrailer);
    trailerBox.getChildren().addAll(textFieldTrailerUrl, btnEditTrailer);
    grid.add(trailerBox, 1, row++);

    // Rating
    Label labelRating = new Label("IMDb Rating:");
    labelRating.setStyle("-fx-font-weight: bold;");
    grid.add(labelRating, 0, row);

    ratingContainer = new FlowPane();
    ratingContainer.setAlignment(Pos.CENTER_LEFT);
    ratingContainer.setHgap(2.0);
    ratingContainer.setVgap(4.0);
    grid.add(ratingContainer, 1, row++);

    // Watchlists
    Label lblWatchlists = new Label("Watchlists:");
    lblWatchlists.setStyle("-fx-font-weight: bold;");
    grid.add(lblWatchlists, 0, row);

    HBox watchlistBox = new HBox(10.0);
    watchlistBox.setAlignment(Pos.CENTER_LEFT);
    lblWatchlistsList = new Label("None");
    lblWatchlistsList.setStyle("-fx-text-fill: -color-fg-muted;");
    HBox.setHgrow(lblWatchlistsList, Priority.ALWAYS);

    btnWatchlists = new Button();
    btnWatchlists.getStyleClass().addAll("button-secondary", "button-icon");
    btnWatchlists.setMinSize(36.0, 36.0);
    btnWatchlists.setMaxSize(36.0, 36.0);
    btnWatchlists.setOnAction(e -> manageMovieWatchlists());
    SVGPath svgImageWatchlist = new SVGPath();
    svgImageWatchlist.setContent(
        "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 9H10V9h8v2zm-4 4h-4v-2h4v2zm4-8h-8V5h8v2z");
    svgImageWatchlist.setFill(Color.WHITE);
    svgImageWatchlist.setScaleX(0.75);
    svgImageWatchlist.setScaleY(0.75);
    btnWatchlists.setGraphic(svgImageWatchlist);
    Tooltip ttWatchlist = new Tooltip("Manage Watchlists");
    ttWatchlist.setShowDelay(Duration.millis(250));
    btnWatchlists.setTooltip(ttWatchlist);
    watchlistBox.getChildren().addAll(lblWatchlistsList, btnWatchlists);
    grid.add(watchlistBox, 1, row++);

    // Description
    Label lblDesc = new Label("Description:");
    lblDesc.setStyle("-fx-font-weight: bold;");
    grid.add(lblDesc, 0, row);

    HBox descriptionBox = new HBox(5.0);
    descriptionBox.setAlignment(Pos.CENTER_LEFT);
    textFieldDescription = new TextArea();
    textFieldDescription.setStyle("-fx-text-fill: -color-fg-default;");
    textFieldDescription.setEditable(false);
    textFieldDescription.setMouseTransparent(true);
    textFieldDescription.setFocusTraversable(false);
    textFieldDescription.setPrefHeight(100.0);
    textFieldDescription.setWrapText(true);
    HBox.setHgrow(textFieldDescription, Priority.ALWAYS);

    Button btnEditDesc = new Button("📝");
    btnEditDesc.getStyleClass().add("button-secondary");
    btnEditDesc.setMinWidth(Region.USE_PREF_SIZE);
    btnEditDesc.setOnAction(e -> editDescription());
    Tooltip ttEditDesc = new Tooltip("Edit Description");
    ttEditDesc.setShowDelay(Duration.millis(250));
    btnEditDesc.setTooltip(ttEditDesc);
    descriptionBox.getChildren().addAll(textFieldDescription, btnEditDesc);
    grid.add(descriptionBox, 1, row);

    labelStatus = new Label("");
    labelStatus.setStyle("-fx-text-fill: -color-success-fg;");

    rightBox.getChildren().addAll(grid, labelStatus);
    contentHBox.getChildren().addAll(leftBox, rightBox);
    scrollPane.setContent(contentHBox);

    rootPane.getChildren().addAll(headerBox, scrollPane);

    return rootPane;
  }

  private TextField createReadOnlyField() {
    TextField textField = new TextField();
    textField.setStyle("-fx-text-fill: -color-fg-default;");
    textField.setEditable(false);
    textField.setMouseTransparent(true);
    textField.setFocusTraversable(false);
    return textField;
  }

  private void addGridRow(GridPane grid, int row, String label,
      TextField textField, EventHandler<ActionEvent> editAction) {
    Label rowLabel = new Label(label);
    rowLabel.setStyle("-fx-font-weight: bold;");
    grid.add(rowLabel, 0, row);

    HBox horizontalBox = new HBox(5.0);
    horizontalBox.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(textField, Priority.ALWAYS);

    Button buttonEdit = new Button("📝");
    buttonEdit.getStyleClass().add("button-secondary");
    buttonEdit.setMinWidth(Region.USE_PREF_SIZE);
    buttonEdit.setOnAction(editAction);
    Tooltip tooltip =
        new Tooltip("Edit " + label.replace(":", "").replace("(s)", ""));
    tooltip.setShowDelay(Duration.millis(250));
    buttonEdit.setTooltip(tooltip);

    horizontalBox.getChildren().addAll(textField, buttonEdit);
    grid.add(horizontalBox, 1, row);
  }

  private void addMultiGridRow(GridPane grid, int row, String label,
      TextField textField, ComboBox<String> comboBox,
      EventHandler<ActionEvent> editAction,
      EventHandler<ActionEvent> imdbAction) {
    Label rowLabel = new Label(label);
    rowLabel.setStyle("-fx-font-weight: bold;");
    grid.add(rowLabel, 0, row);

    HBox horizontalBox = new HBox(5.0);
    horizontalBox.setAlignment(Pos.CENTER_LEFT);

    StackPane stackPane = new StackPane();
    HBox.setHgrow(stackPane, Priority.ALWAYS);
    textField.setVisible(true);
    textField.setManaged(true);
    comboBox.setMaxWidth(Double.MAX_VALUE);
    comboBox.setVisible(false);
    comboBox.setManaged(false);
    stackPane.getChildren().addAll(textField, comboBox);

    Button buttonImdb = new Button();
    buttonImdb.getStyleClass().add("button-secondary");
    buttonImdb.setMinWidth(Region.USE_PREF_SIZE);
    buttonImdb.setOnAction(imdbAction);
    SVGPath svgImageImdb = new SVGPath();
    svgImageImdb.setContent(
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z");
    svgImageImdb.setFill(Color.WHITE);
    svgImageImdb.setScaleX(0.75);
    svgImageImdb.setScaleY(0.75);
    buttonImdb.setGraphic(svgImageImdb);
    Tooltip ttImdb = new Tooltip(
        "Open " + label.replace(":", "").replace("(s)", "") + " in IMDb");
    ttImdb.setShowDelay(Duration.millis(250));
    buttonImdb.setTooltip(ttImdb);

    Button buttonEdit = new Button("📝");
    buttonEdit.getStyleClass().add("button-secondary");
    buttonEdit.setMinWidth(Region.USE_PREF_SIZE);
    buttonEdit.setOnAction(editAction);
    Tooltip ttEdit =
        new Tooltip("Edit " + label.replace(":", "").replace("(s)", ""));
    ttEdit.setShowDelay(Duration.millis(250));
    buttonEdit.setTooltip(ttEdit);

    horizontalBox.getChildren().addAll(stackPane, buttonImdb, buttonEdit);
    grid.add(horizontalBox, 1, row);
  }

  /** Initializes the controller. Sets up search/trailer binding listeners. */
  public void initialize() {

    // Listeners for trailer URL to dynamically disable/enable Watch Trailer
    // button
    textFieldTrailerUrl.textProperty().addListener((obs, oldVal, newVal) -> {
      buttonTrailer.setDisable(newVal == null || newVal.trim().isEmpty());
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

    labelHeaderTitle.setText(movie.getTitle());
    textFieldTitle.setText(movie.getTitle());
    textFieldYear.setText(
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "");
    loadPoster();
    buildRatingWidget();
    refreshWatchlistStatus();

    boolean missingDuration =
        movie.getDuration() == null || movie.getDuration() <= 0;
    boolean missingRating =
        movie.getRating() == null && movie.getRawRating() == null;

    if (missingDuration) {
      txtDuration.setText("Fetching IMDb duration (imdb.com)...");
      txtDuration
          .setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
    } else {
      txtDuration.setText(movie.getFormattedDuration());
      txtDuration.setStyle("-fx-text-fill: -color-fg-default;");
    }

    if (missingDuration || missingRating) {
      CompletableFuture.runAsync(() -> {
        if (missingDuration) {
          imdbService.fetchImdbDurationDirectly(movie);
        }
        if (movie.getDuration() == null || movie.getDuration() <= 0
            || (movie.getRating() == null && movie.getRawRating() == null)) {
          imdbService.enrichMovieData(movie);
        }
        Platform.runLater(() -> {
          if (movie.getDuration() != null && movie.getDuration() > 0) {
            txtDuration.setText(movie.getFormattedDuration());
            txtDuration.setStyle("-fx-text-fill: -color-fg-default;");
          } else {
            txtDuration.setText("Not specified (Click 📝 to set duration)");
            txtDuration.setStyle(
                "-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
          }
          if (movie.getRating() != null || movie.getRawRating() != null) {
            this.currentRating = movie.getRating();
            updateRatingUI();
          }
          movieRepository.update(movie);
          if ((movie.getDuration() != null && movie.getDuration() > 0)
              || movie.getRating() != null || movie.getRawRating() != null) {
            showStatus("IMDb Metadata loaded!");
            appContext.notifyLibraryChanged();
            refresh();
          }
        });
      });
    }
    textFieldGenre.setText(movie.getGenre() != null ? movie.getGenre() : "");
    setupMultiField(movie.getDirector(), textFieldDirector, comboBoxDirector);
    setupMultiField(movie.getWriter(), textFieldWriter, comboBoxWriter);
    setupMultiField(movie.getProducer(), textFieldProducer, comboBoxProducer);
    setupMultiField(movie.getCinematographer(), textFieldCinematographer,
        comboBoxCinematographer);
    setupMultiField(movie.getComposer(), textFieldComposer, comboBoxComposer);
    setupMultiField(movie.getActors(), textFieldActors, comboBoxActors);
    textFieldDescription
        .setText(movie.getDescription() != null ? movie.getDescription() : "");
    textFieldTrailerUrl
        .setText(movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "");

    buttonTrailer.setDisable(textFieldTrailerUrl.getText().trim().isEmpty());

    String tmpFilePath = movie.getFilePath();
    buttonPlay.setDisable(tmpFilePath == null || tmpFilePath.trim().isEmpty()
        || !new File(tmpFilePath).exists());

    // Auto-save when changing tabs (which removes the view from the scene)
    labelHeaderTitle.sceneProperty().addListener((obs, oldVal, newVal) -> {
      if (oldVal != null && newVal == null) {
        if (hasUnsavedChanges()) {
          performSave();
        }
      }
    });
  }

  /**
   * Loads the poster image for the current movie. If the file is not found or
   * path is empty, falls back to a default placeholder image.
   */
  private void loadPoster() {
    String tmpPath = movie.getCoverImagePath();
    Image tmpImage = null;
    if (tmpPath != null && !tmpPath.isBlank()) {
      File tmpFile = new File(tmpPath);
      if (tmpFile.exists()) {
        try {
          tmpImage = new Image(tmpFile.toURI().toURL().toExternalForm(), 240,
              360, false, true, true);
        } catch (MalformedURLException exception) {
          LOGGER.log(Level.WARNING,
              "Failed to load poster in details: " + tmpPath, exception);
        }
      }
    }

    if (tmpImage == null) {
      if (PLACEHOLDER_IMAGE == null) {
        try {
          var tmpResource = getClass().getResource(
              "/io/github/kullik01/kinonode/images/placeholders/placeholder_cover.jpg");
          if (tmpResource != null) {
            PLACEHOLDER_IMAGE = new Image(tmpResource.toExternalForm(), 240,
                360, false, true, true);
          }
        } catch (Exception exception) {
          LOGGER.log(Level.WARNING, "Failed to load details placeholder cover",
              exception);
        }
      }
      tmpImage = PLACEHOLDER_IMAGE;
    }

    if (tmpImage != null) {
      imgPoster.setImage(tmpImage);
    }
  }

  /**
   * Creates an SVGPath instance representing a camera icon used in the rating
   * widget.
   *
   * @return A styled SVGPath of a camera.
   */
  private SVGPath createCameraSvg() {
    SVGPath tmpIcon = new SVGPath();
    tmpIcon.setContent(
        "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z");
    tmpIcon.setScaleX(0.75);
    tmpIcon.setScaleY(0.75);
    tmpIcon.setPickOnBounds(true);
    tmpIcon.setMouseTransparent(true);
    return tmpIcon;
  }

  /**
   * Sets the visual fill state of a single camera rating icon. Uses gradient
   * filling to support fractional ratings (e.g., 8.5/10).
   *
   * @param reelLabel The label containing the SVG icon.
   * @param fillAmount The fill fraction between 0.0 and 1.0.
   */
  private void setReelState(Label reelLabel, double fillAmount) {
    if (fillAmount >= 1.0) {
      SVGPath tmpIcon = createCameraSvg();
      tmpIcon.setStyle(
          "-fx-fill: #ED8B00; -fx-opacity: 1.0; -fx-effect: dropshadow(three-pass-box, rgba(237, 139, 0, 0.4), 6, 0, 0, 0);");
      reelLabel.setGraphic(tmpIcon);
    } else if (fillAmount <= 0.0) {
      SVGPath tmpIcon = createCameraSvg();
      tmpIcon
          .setStyle("-fx-fill: #666666; -fx-opacity: 0.35; -fx-effect: none;");
      reelLabel.setGraphic(tmpIcon);
    } else {
      // Create stacked layered icon: background unfilled camera + half-filled
      // foreground camera
      SVGPath bgIcon = createCameraSvg();
      bgIcon
          .setStyle("-fx-fill: #666666; -fx-opacity: 0.35; -fx-effect: none;");

      SVGPath fillIcon = createCameraSvg();
      // Use a linear gradient to perfectly fill the exact percentage of the
      // icon's bounds
      double pct = fillAmount * 100.0;
      String gradient = String.format(Locale.US,
          "linear-gradient(to right, #ED8B00 0%%, #ED8B00 %.1f%%, transparent %.1f%%, transparent 100%%)",
          pct, pct);
      fillIcon.setStyle("-fx-fill: " + gradient
          + "; -fx-opacity: 1.0; -fx-effect: dropshadow(three-pass-box, rgba(237, 139, 0, 0.4), 6, 0, 0, 0);");

      StackPane comboPane = new StackPane(bgIcon, fillIcon);
      comboPane.setPrefSize(28.0, 28.0);
      comboPane.setMinSize(28.0, 28.0);
      comboPane.setMaxSize(28.0, 28.0);
      reelLabel.setGraphic(comboPane);
    }
  }

  /**
   * Detects the numerical score associated with a mouse hover event over the
   * rating widget.
   *
   * @param e The mouse event.
   * @return The rating score (1-10) corresponding to the hovered position.
   */
  private int detectHoveredScore(MouseEvent e) {
    if (ratingContainer == null || ratingContainer.getChildren().isEmpty()) {
      return 1;
    }
    for (int tmpIndex = 0; tmpIndex < 10
        && tmpIndex < ratingContainer.getChildren().size(); tmpIndex++) {
      Node tmpChild = ratingContainer.getChildren().get(tmpIndex);
      Point2D localPt = tmpChild.sceneToLocal(e.getSceneX(), e.getSceneY());
      if (localPt.getX() >= 0 && localPt.getX() <= 28.0 && localPt.getY() >= 0
          && localPt.getY() <= 28.0) {
        return tmpIndex + 1;
      }
    }
    Point2D containerPt =
        ratingContainer.sceneToLocal(e.getSceneX(), e.getSceneY());
    int index = (int) Math.ceil(containerPt.getX() / 28.0);
    return Math.max(1, Math.min(10, index));
  }

  /**
   * Constructs the interactive 10-camera rating widget. Wires up hover effects,
   * click actions, and dynamic fractional filling.
   */
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
      tmpReel.setAlignment(Pos.CENTER);
      tmpReel.getStyleClass().add("rating-reel");
      tmpReel.setStyle("-fx-padding: 0;");
      Tooltip tmpReelTooltip = new Tooltip("Rate " + tmpScore + "/10");
      tmpReelTooltip.setShowDelay(Duration.millis(250));
      tmpReel.setTooltip(tmpReelTooltip);

      Runnable applyRating = () -> {
        if (currentRating != null && currentRating == tmpScore) {
          currentRating = null; // Toggle off to unrated
          if (movie != null) {
            movie.setRawRating(null);
          }
        } else {
          currentRating = tmpScore;
          if (movie != null) {
            movie.setRawRating((double) tmpScore);
          }
        }
        if (movie != null) {
          movie.setRating(currentRating);
          movieRepository.update(movie);
        }
        updateRatingUI();
      };

      tmpReel.setOnMouseClicked(e -> applyRating.run());
      tmpReel.setOnMouseEntered(e -> previewRatingHover(tmpScore));
      tmpReel.setOnMouseMoved(e -> previewRatingHover(tmpScore));
      tmpReels.add(tmpReel);
      ratingContainer.getChildren().add(tmpReel);
    }

    ratingContainer.setOnMouseExited(e -> updateRatingUI());

    // Numerical display label positioned at the end of the rating container
    Label tmpLblRatingVal = new Label();
    tmpLblRatingVal.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    tmpLblRatingVal.setStyle(
        "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    ratingContainer.getChildren().add(tmpLblRatingVal);

    updateRatingUI();
  }

  /**
   * Temporarily updates the rating UI to show the projected score when hovering
   * over a rating icon.
   *
   * @param hoverScore The score being previewed.
   */
  private void previewRatingHover(int hoverScore) {
    Label tmpLblRatingVal = (Label) ratingContainer.getChildren()
        .get(ratingContainer.getChildren().size() - 1);
    for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
      Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
      setReelState(tmpReel, tmpIndex <= hoverScore ? 1.0 : 0.0);
    }
    tmpLblRatingVal.setText(hoverScore + "/10");
    tmpLblRatingVal.setStyle(
        "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
  }

  /**
   * Re-applies the actual saved rating score to the rating UI, clearing any
   * hover preview. Shows fractional values appropriately using gradient SVGs.
   */
  private void updateRatingUI() {
    Label tmpLblRatingVal = (Label) ratingContainer.getChildren()
        .get(ratingContainer.getChildren().size() - 1);

    Double displayRating =
        (movie != null && movie.getRawRating() != null) ? movie.getRawRating()
            : (currentRating != null ? currentRating.doubleValue() : null);

    if (displayRating == null) {
      for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
        Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
        setReelState(tmpReel, 0.0);
      }
      tmpLblRatingVal.setText("Unrated");
      tmpLblRatingVal.setStyle(
          "-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-padding: 0 0 0 10px;");
    } else {
      for (int tmpIndex = 1; tmpIndex <= 10; tmpIndex++) {
        Label tmpReel = (Label) ratingContainer.getChildren().get(tmpIndex - 1);
        if (displayRating >= tmpIndex) {
          setReelState(tmpReel, 1.0);
        } else if (displayRating > tmpIndex - 1) {
          double fraction = displayRating - (tmpIndex - 1);
          setReelState(tmpReel, fraction);
        } else {
          setReelState(tmpReel, 0.0);
        }
      }
      String formattedRating =
          (displayRating % 1.0 == 0) ? String.format("%.0f", displayRating)
              : String.format(Locale.US, "%.1f", displayRating);
      tmpLblRatingVal.setText(formattedRating + "/10");
      tmpLblRatingVal.setStyle(
          "-fx-text-fill: #ED8B00; -fx-font-weight: bold; -fx-padding: 0 0 0 10px;");
    }
  }

  @Override
  public void refresh() {
    refreshWatchlistStatus();
  }

  /**
   * Scans the repository to identify which watchlists contain the current movie
   * and updates the displayed watchlist summary label.
   */
  public void refreshWatchlistStatus() {
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
  }

  /**
   * Displays a multi-select dialog allowing the user to add or remove the
   * current movie from various watchlists simultaneously.
   */
  private void manageMovieWatchlists() {
    Dialog<Void> dialog = DialogBuilder.createDialog("Manage Watchlists",
        "Add or Remove \"" + movie.getTitle() + "\" from Watchlists", null);

    // Dialog size matching standard dialog width

    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
        ButtonType.CANCEL);

    // Style both OK and Cancel buttons as blue primary buttons
    Node tmpOkBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
    if (tmpOkBtn != null) {
      tmpOkBtn.getStyleClass().add("button-primary");
    }
    Node tmpCancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    if (tmpCancelBtn != null) {
      tmpCancelBtn.getStyleClass().add("button-primary");
    }

    VBox tmpContentBox = new VBox(15);
    tmpContentBox.setPadding(new Insets(10));

    // ScrollPane for watchlists list
    ScrollPane tmpScrollPane = new ScrollPane();
    tmpScrollPane.setFitToWidth(true);
    tmpScrollPane.setPrefHeight(180);
    tmpScrollPane.setStyle(
        "-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 4px;");

    VBox tmpWatchlistsListVBox = new VBox(10);
    tmpWatchlistsListVBox.setPadding(new Insets(10));
    tmpScrollPane.setContent(tmpWatchlistsListVBox);

    // Keep track of which watchlists the movie should be in
    Map<Integer, Boolean> pendingChanges = new HashMap<>();

    // Populate method
    Runnable populateWatchlists = () -> {
      tmpWatchlistsListVBox.getChildren().clear();
      List<Watchlist> allW = watchlistRepository.findAll().stream()
          .filter(w -> !w.isSmart()).toList();
      if (allW.isEmpty()) {
        Label emptyLbl = new Label("No watchlists available.");
        emptyLbl.setStyle(
            "-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
        tmpWatchlistsListVBox.getChildren().add(emptyLbl);
      } else {
        for (Watchlist w : allW) {
          CheckBox comboBox = new CheckBox(w.getName());
          comboBox.setStyle("-fx-font-size: 14px; -fx-padding: 4px 0;");
          boolean isMovieIn = w.getMovies().stream()
              .anyMatch(m -> m.getId().equals(movie.getId()));

          boolean isSelected = pendingChanges.containsKey(w.getId())
              ? pendingChanges.get(w.getId())
              : isMovieIn;
          comboBox.setSelected(isSelected);

          comboBox.selectedProperty()
              .addListener((obs, wasSelected, nowSelected) -> {
                pendingChanges.put(w.getId(), nowSelected);
              });
          tmpWatchlistsListVBox.getChildren().add(comboBox);
        }
      }
    };

    populateWatchlists.run();

    // Create New Watchlist button
    Button tmpCreateBtn = new Button("+ Create New Watchlist");
    tmpCreateBtn.getStyleClass().add("button-primary");
    tmpCreateBtn.setOnAction(e -> {
      Optional<String> result = showWatchlistDialog("New Watchlist",
          "Create a new watchlist", "", -1);
      if (result.isPresent() && !result.get().trim().isEmpty()) {
        String cleanName = result.get().trim();
        Watchlist nw = watchlistRepository.insert(new Watchlist(cleanName));
        if (nw != null && nw.getId() != null) {
          // Add to pending changes as selected
          pendingChanges.put(nw.getId(), true);
          populateWatchlists.run();
          showStatus("Created watchlist (will add movie upon saving).");
        }
      }
    });

    HBox tmpCreateBox = new HBox(tmpCreateBtn);
    tmpCreateBox.setAlignment(Pos.CENTER_LEFT);

    tmpContentBox.getChildren().addAll(tmpScrollPane, tmpCreateBox);
    dialog.getDialogPane().setContent(tmpContentBox);

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        for (Entry<Integer, Boolean> entry : pendingChanges.entrySet()) {
          int wId = entry.getKey();
          boolean wantIn = entry.getValue();
          Watchlist w = watchlistRepository.findById(wId);
          if (w != null) {
            boolean isMovieIn = w.getMovies().stream()
                .anyMatch(m -> m.getId().equals(movie.getId()));
            if (wantIn && !isMovieIn) {
              watchlistRepository.addMovieToWatchlist(wId, movie.getId());
            } else if (!wantIn && isMovieIn) {
              watchlistRepository.removeMovieFromWatchlist(wId, movie.getId());
            }
          }
        }
        refreshWatchlistStatus();
        showStatus("Watchlists updated successfully.");
      }
      return null;
    });

    dialog.showAndWait();
  }

  private Optional<String> showWatchlistDialog(String title, String header,
      String defaultText, int currentId) {
    Dialog<String> dialog = DialogBuilder.createDialog(title, header, null);



    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
        ButtonType.CANCEL);

    VBox vBox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    errorLabel.setVisible(false);

    TextField tmpTextField = new TextField(defaultText);
    tmpTextField.setPromptText("Watchlist Name");

    Label tmpAsciiInfo = new Label(
        "Only ASCII characters allowed: standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space).");
    tmpAsciiInfo.setStyle(
        "-fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");
    tmpAsciiInfo.setWrapText(true);
    tmpAsciiInfo.setPrefWidth(430);
    tmpAsciiInfo.setMinHeight(Region.USE_PREF_SIZE);

    vBox.getChildren().addAll(errorLabel, tmpTextField, tmpAsciiInfo);
    dialog.getDialogPane().setContent(vBox);

    Node tmpOkButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
    if (tmpOkButton != null) {
      tmpOkButton.getStyleClass().add("button-primary");
      tmpOkButton.setDisable(
          defaultText.trim().isEmpty() || defaultText.length() > 30);
    }

    Node tmpCancelButton =
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    if (tmpCancelButton != null) {
      tmpCancelButton.getStyleClass().add("button-primary");
    }

    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.length() > 30) {
            tmpTextField.setText(oldValue);
            return;
          }
          // Enforce ASCII-only characters in watchlist names
          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpTextField.setText(oldValue);
            return;
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
              errorLabel.setText(
                  "A watchlist with this name already exists. Please choose a different name.");
              errorLabel.setVisible(true);
              tmpOkButton.setDisable(true);
            } else {
              errorLabel.setVisible(false);
              tmpOkButton.setDisable(newValue.trim().isEmpty());
            }
          }
          if (dialog.getDialogPane().getScene() != null
              && dialog.getDialogPane().getScene().getWindow() != null) {
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
          }
        });

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        return tmpTextField.getText();
      }
      return null;
    });

    dialog.getDialogPane().setContent(vBox);
    return dialog.showAndWait();
  }

  private boolean validateWatchlistName(String cleanName, int currentId) {
    List<Watchlist> tmpWatchlists = watchlistRepository.findAll();
    for (Watchlist tmpWatchlist : tmpWatchlists) {
      if (tmpWatchlist.getName().equalsIgnoreCase(cleanName)) {
        if (tmpWatchlist.getId() != null && tmpWatchlist.getId() != currentId) {
          DialogBuilder.showError("Validation Error", "Duplicate Watchlist",
              "A watchlist with this name already exists. Please choose a different name.");
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Opens a file chooser to let the user select a custom poster image. Copies
   * the chosen file to the app's covers directory and updates the movie.
   */
  private void changePoster() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Choose Poster Image");
    tmpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
        "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

    File tmpSelected =
        tmpChooser.showOpenDialog(imgPoster.getScene().getWindow());
    if (tmpSelected != null) {
      try {
        Path tmpDbPath = databaseService.getDbPath();
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
      } catch (IOException exception) {
        LOGGER.log(Level.SEVERE, "Failed to copy cover poster file", exception);
        showStatus("Error saving poster: " + exception.getMessage());
      }
    }
  }

  /**
   * Invokes the PlaybackService to open the movie's media file in the
   * configured external media player.
   */
  private void playMovie() {
    try {
      // Trigger PlaybackService which handles configuring and executing
      // external media player
      // process
      playbackService.play(movie.getFilePath());
      showStatus("Opening external player...");
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "Failed to launch external media player",
          exception);
      DialogBuilder.showError("Playback Failed",
          "Could not launch external media player", exception.getMessage());
    }
  }

  /**
   * Opens the configured YouTube trailer URL in the user's default web browser.
   */
  private void watchTrailer() {
    String url = textFieldTrailerUrl.getText().trim();
    if (url.isEmpty())
      return;

    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "https://" + url;
    }

    try {
      // Open default web browser pointing to trailer link
      if (Desktop.isDesktopSupported()
          && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
        showStatus("Opening trailer link in browser...");
      } else {
        showStatus("System web browser action not supported on this platform.");
      }
    } catch (Exception exception) {
      LOGGER.log(Level.WARNING, "Failed to open browser trailer link",
          exception);
      showStatus("Failed to open browser.");
    }
  }

  /**
   * Attempts to determine the IMDb URL for the current movie and opens it in
   * the internal IMDb web browser tab. Uses the cover filename's 'tooltip' id
   * if possible.
   */
  private void openImdbPage() {
    if (movie == null) {
      return;
    }

    String url = null;
    String tmpCover = movie.getCoverImagePath();
    if (tmpCover != null) {
      Pattern tmpPattern = Pattern.compile("tooltip\\d+");
      Matcher tmpMatcher = tmpPattern.matcher(tmpCover);
      if (tmpMatcher.find()) {
        url = "https://www.imdb.com/title/" + tmpMatcher.group() + "/";
      }
    }

    if (url == null) {
      // Fallback to IMDb search by title
      String tmpTitle = movie.getTitle();
      if (tmpTitle != null && !tmpTitle.isBlank()) {
        try {
          url = "https://www.imdb.com/find/?q="
              + URLEncoder.encode(tmpTitle.trim(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
          LOGGER.log(Level.WARNING, "Failed to encode title for IMDb search",
              exception);
        }
      }
    }

    if (url == null) {
      url = "https://www.imdb.com/";
    }

    MainController tmpMain = appContext.getMainController();
    if (tmpMain != null) {
      tmpMain.showImdbWithUrl(url);
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
    Dialog<String> dialog =
        DialogBuilder.createDialog("Edit " + title, "Edit " + fieldName,
            "/io/github/kullik01/kinonode/images/icons/question_mark.png");

    // Removed fixed sizing to let the dialog size to its content

    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
        ButtonType.CANCEL);

    VBox vBox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String defaultText =
        control.getText() != null ? control.getText().trim() : "";

    if (!defaultText.isEmpty()) {
      errorLabel.setText("This is the current " + fieldName
          + " and must be updated by a new one.");
      errorLabel.setVisible(true);
    } else {
      errorLabel.setVisible(false);
    }

    TextInputControl inputField;
    if (control instanceof TextArea) {
      TextArea textArea = new TextArea(defaultText);
      textArea.setWrapText(true);
      textArea.setPrefHeight(100);
      inputField = textArea;
    } else {
      inputField = new TextField(defaultText);
    }

    // Set formatting constraints on user input
    if (constraint == InputConstraint.NUMBERS_ONLY) {
      inputField.setTextFormatter(new TextFormatter<>(change -> {
        if (change.getText().matches("\\d*")) {
          return change;
        }
        return null;
      }));
    } else if (constraint == InputConstraint.NO_NUMBERS) {
      inputField.setTextFormatter(new TextFormatter<>(change -> {
        if (change.getText().matches("[^0-9]*")) {
          return change;
        }
        return null;
      }));
    }

    Label tmpAsciiInfo = new Label(
        "Only ASCII characters allowed: standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space).");
    tmpAsciiInfo.setStyle(
        "-fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");
    tmpAsciiInfo.setWrapText(true);
    tmpAsciiInfo.setPrefWidth(430);
    tmpAsciiInfo.setMinHeight(Region.USE_PREF_SIZE);

    vBox.getChildren().addAll(new Label("Enter the new " + fieldName + ":"),
        errorLabel, inputField, tmpAsciiInfo);
    dialog.getDialogPane().setContent(vBox);

    Node tmpOkButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
    tmpOkButton.setDisable(true);

    inputField.textProperty().addListener((observable, oldValue, newValue) -> {
      String tmpValue = newValue.trim();
      boolean tmpIsUnchanged = tmpValue.equals(defaultText);

      // Require input content to be ASCII only
      if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
        inputField.setText(oldValue);
        return;
      } else if (tmpIsUnchanged) {
        if (!defaultText.isEmpty()) {
          errorLabel.setText("This is the current " + fieldName
              + " and must be updated by a new one.");
          errorLabel.setVisible(true);
        }
        tmpOkButton.setDisable(true);
      } else {
        errorLabel.setVisible(false);
        tmpOkButton.setDisable(false);
      }
    });

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        return inputField.getText();
      }
      return null;
    });


    Optional<String> tmpResult = dialog.showAndWait();
    tmpResult.ifPresent(val -> {
      String newVal = val.trim();
      control.setText(newVal);
      updater.accept(newVal);
      movieRepository.update(movie);
      showStatus(title + " updated!");
    });
  }

  private void editTitle() {
    showEditDialog("Title", "Movie Title", textFieldTitle, InputConstraint.ANY,
        val -> movie.setTitle(val.isEmpty() ? null : val));
  }

  private void editYear() {
    showEditDialog("Year", "Release Year", textFieldYear,
        InputConstraint.NUMBERS_ONLY, val -> {
          try {
            movie.setYear(val.isEmpty() ? null : Integer.parseInt(val));
          } catch (NumberFormatException exception) {
            showStatus("Year must be a number!");
          }
        });
  }

  private void editDuration() {
    showEditDialog("Duration", "Duration in minutes", txtDuration,
        InputConstraint.NUMBERS_ONLY, val -> {
          try {
            if (val.isEmpty()) {
              movie.setDuration(null);
              txtDuration.setText("Not specified (Click 📝 to set duration)");
              txtDuration.setStyle(
                  "-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
            } else {
              movie.setDuration(Integer.parseInt(val));
              txtDuration.setText(movie.getFormattedDuration());
              txtDuration.setStyle("-fx-text-fill: -color-fg-default;");
            }
          } catch (NumberFormatException exception) {
            showStatus("Duration must be a number!");
          }
        });
  }

  private void editGenre() {
    showEditDialog("Genre", "Genre", textFieldGenre, InputConstraint.NO_NUMBERS,
        val -> movie.setGenre(val.isEmpty() ? null : val));
  }

  private void editDirector() {
    showEditDialog("Director", "Director(s)", textFieldDirector,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setDirector(val.isEmpty() ? null : val);
          setupMultiField(movie.getDirector(), textFieldDirector,
              comboBoxDirector);
        });
  }

  private void editWriter() {
    showEditDialog("Writer", "Writer(s)", textFieldWriter,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setWriter(val.isEmpty() ? null : val);
          setupMultiField(movie.getWriter(), textFieldWriter, comboBoxWriter);
        });
  }

  private void editProducer() {
    showEditDialog("Producer", "Producer(s)", textFieldProducer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setProducer(val.isEmpty() ? null : val);
          setupMultiField(movie.getProducer(), textFieldProducer,
              comboBoxProducer);
        });
  }

  private void editCinematographer() {
    showEditDialog("Cinematographer", "Cinematographer(s)",
        textFieldCinematographer, InputConstraint.NO_NUMBERS, val -> {
          movie.setCinematographer(val.isEmpty() ? null : val);
          setupMultiField(movie.getCinematographer(), textFieldCinematographer,
              comboBoxCinematographer);
        });
  }

  private void editComposer() {
    showEditDialog("Composer", "Composer(s)", textFieldComposer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setComposer(val.isEmpty() ? null : val);
          setupMultiField(movie.getComposer(), textFieldComposer,
              comboBoxComposer);
        });
  }

  private void editActors() {
    showEditDialog("Actor(s)", "Actor(s)", textFieldActors,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setActors(val.isEmpty() ? null : val);
          setupMultiField(movie.getActors(), textFieldActors, comboBoxActors);
        });
  }

  private void editDescription() {
    showEditDialog("Description", "Description", textFieldDescription,
        InputConstraint.NO_NUMBERS,
        val -> movie.setDescription(val.isEmpty() ? null : val));
  }

  private void editTrailerUrl() {
    Dialog<String> dialog =
        DialogBuilder.createDialog("Edit Trailer Link", "YouTube Trailer URL",
            "/io/github/kullik01/kinonode/images/icons/question_mark.png");



    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
        ButtonType.CANCEL);

    VBox vBox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String defaultText = textFieldTrailerUrl.getText().trim();

    if (!defaultText.isEmpty()) {
      errorLabel.setText(
          "This is the current YouTube link and must be updated by a new one.");
      errorLabel.setVisible(true);
    } else {
      errorLabel.setVisible(false);
    }

    TextField tmpTextField = new TextField(defaultText);
    tmpTextField.setPromptText("https://www.youtube.com/watch?v=...");

    Label tmpAsciiInfo = new Label(
        "Only ASCII characters allowed: standard English letters (a-z, A-Z), numbers (0-9), and basic symbols (! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ space).");
    tmpAsciiInfo.setStyle(
        "-fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");
    tmpAsciiInfo.setWrapText(true);
    tmpAsciiInfo.setPrefWidth(430);
    tmpAsciiInfo.setMinHeight(Region.USE_PREF_SIZE);

    vBox.getChildren().addAll(new Label("Enter the new YouTube Trailer Link:"),
        errorLabel, tmpTextField, tmpAsciiInfo);
    dialog.getDialogPane().setContent(vBox);

    Node tmpOkButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
    tmpOkButton.setDisable(true);

    tmpTextField.textProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (!newValue.matches("\\A\\p{ASCII}*\\z")) {
            tmpTextField.setText(oldValue);
            return;
          }
          String url = newValue.trim();
          boolean tmpIsUnchanged = url.equalsIgnoreCase(defaultText);

          if (tmpIsUnchanged && !defaultText.isEmpty()) {
            errorLabel.setText(
                "This is the current YouTube link and must be updated by a new one.");
            errorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else if (!url.isEmpty() && !url.contains("youtube.com")
              && !url.contains("youtu.be")) {
            errorLabel.setText("Trailer Link must be a valid YouTube URL.");
            errorLabel.setVisible(true);
            tmpOkButton.setDisable(true);
          } else {
            errorLabel.setVisible(false);
            tmpOkButton.setDisable(url.isEmpty());
          }
        });

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        return tmpTextField.getText();
      }
      return null;
    });


    Optional<String> tmpResult = dialog.showAndWait();
    tmpResult.ifPresent(url -> {
      String newUrl = url.trim();
      textFieldTrailerUrl.setText(newUrl);
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
    if (!java.util.Objects.equals(movie.getTitle(),
        textFieldTitle.getText().trim()))
      return true;

    String tmpMovieYear =
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "";
    if (!java.util.Objects.equals(tmpMovieYear, textFieldYear.getText().trim()))
      return true;

    String tmpMovieGenre = movie.getGenre() != null ? movie.getGenre() : "";
    if (!java.util.Objects.equals(tmpMovieGenre,
        textFieldGenre.getText().trim()))
      return true;

    String tmpMovieDirector =
        movie.getDirector() != null ? movie.getDirector() : "";
    if (!java.util.Objects.equals(tmpMovieDirector,
        textFieldDirector.getText().trim()))
      return true;

    String tmpMovieWriter = movie.getWriter() != null ? movie.getWriter() : "";
    if (!java.util.Objects.equals(tmpMovieWriter,
        textFieldWriter.getText().trim()))
      return true;

    String tmpMovieProducer =
        movie.getProducer() != null ? movie.getProducer() : "";
    if (!java.util.Objects.equals(tmpMovieProducer,
        textFieldProducer.getText().trim()))
      return true;

    String tmpMovieCinematographer =
        movie.getCinematographer() != null ? movie.getCinematographer() : "";
    if (!java.util.Objects.equals(tmpMovieCinematographer,
        textFieldCinematographer.getText().trim()))
      return true;

    String tmpMovieComposer =
        movie.getComposer() != null ? movie.getComposer() : "";
    if (!java.util.Objects.equals(tmpMovieComposer,
        textFieldComposer.getText().trim()))
      return true;

    String tmpMovieActors = movie.getActors() != null ? movie.getActors() : "";
    if (!java.util.Objects.equals(tmpMovieActors,
        textFieldActors.getText().trim()))
      return true;

    String tmpMovieDesc =
        movie.getDescription() != null ? movie.getDescription() : "";
    if (!java.util.Objects.equals(tmpMovieDesc,
        textFieldDescription.getText().trim()))
      return true;

    String tmpMovieTrailer =
        movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "";
    if (!java.util.Objects.equals(tmpMovieTrailer,
        textFieldTrailerUrl.getText().trim()))
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
    String tmpTitle = textFieldTitle.getText().trim();
    if (tmpTitle.isEmpty()) {
      showStatus("Title cannot be empty.");
      return false;
    }

    Integer tmpYear = null;
    if (!textFieldYear.getText().trim().isEmpty()) {
      try {
        tmpYear = Integer.parseInt(textFieldYear.getText().trim());
      } catch (NumberFormatException exception) {
        showStatus("Release year must be a valid integer number.");
        return false;
      }
    }

    String tmpTrailer = textFieldTrailerUrl.getText().trim();
    if (!tmpTrailer.isEmpty()) {
      if (!tmpTrailer.contains("youtube.com")
          && !tmpTrailer.contains("youtu.be")) {
        showStatus("Trailer Link must be a valid YouTube URL.");
        return false;
      }
    }

    movie.setTitle(tmpTitle);
    movie.setYear(tmpYear);
    movie.setGenre(textFieldGenre.getText().trim().isEmpty() ? null
        : textFieldGenre.getText().trim());
    movie.setDirector(textFieldDirector.getText().trim().isEmpty() ? null
        : textFieldDirector.getText().trim());
    movie.setWriter(textFieldWriter.getText().trim().isEmpty() ? null
        : textFieldWriter.getText().trim());
    movie.setProducer(textFieldProducer.getText().trim().isEmpty() ? null
        : textFieldProducer.getText().trim());
    movie.setCinematographer(
        textFieldCinematographer.getText().trim().isEmpty() ? null
            : textFieldCinematographer.getText().trim());
    movie.setComposer(textFieldComposer.getText().trim().isEmpty() ? null
        : textFieldComposer.getText().trim());
    movie.setActors(textFieldActors.getText().trim().isEmpty() ? null
        : textFieldActors.getText().trim());
    movie.setDescription(textFieldDescription.getText().trim().isEmpty() ? null
        : textFieldDescription.getText().trim());
    movie.setTrailerUrl(tmpTrailer.isEmpty() ? null : tmpTrailer);
    movie.setRating(currentRating);

    movieRepository.update(movie);
    showStatus("Movie details saved successfully.");
    appContext.notifyLibraryChanged();
    return true;
  }

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
        LibraryController controller = libraryControllerProvider.get();
        Parent libraryView = controller.buildView();
        controller.initialize();

        StackPane contentArea =
            (StackPane) labelHeaderTitle.getScene().lookup("#contentArea");
        if (contentArea != null) {
          contentArea.getChildren().setAll(libraryView);
        }
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create LibraryView view", e);
      }
    }
  }

  private void searchDirectorOnImdb() {
    searchPersonOnImdb(comboBoxDirector, textFieldDirector);
  }

  private void searchWriterOnImdb() {
    searchPersonOnImdb(comboBoxWriter, textFieldWriter);
  }

  private void searchProducerOnImdb() {
    searchPersonOnImdb(comboBoxProducer, textFieldProducer);
  }

  private void searchCinematographerOnImdb() {
    searchPersonOnImdb(comboBoxCinematographer, textFieldCinematographer);
  }

  private void searchComposerOnImdb() {
    searchPersonOnImdb(comboBoxComposer, textFieldComposer);
  }

  private void searchActorsOnImdb() {
    searchPersonOnImdb(comboBoxActors, textFieldActors);
  }

  /**
   * Orchestrates an intelligent IMDb search for a cast/crew member by name.
   * Attempts to resolve the exact IMDb person ID via a Wikidata SPARQL query,
   * before falling back to a general IMDb text search.
   *
   * @param combo The ComboBox containing multiple parsed names.
   * @param txt The TextField containing a single name.
   */
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
    String tmpMovieImdbId = null;
    if (movie != null) {
      String cover = movie.getCoverImagePath();
      if (cover != null) {
        Pattern p = Pattern.compile("tooltip\\d+");
        Matcher m = p.matcher(cover);
        if (m.find()) {
          tmpMovieImdbId = m.group();
        }
      }
    }
    final String movieImdbId = tmpMovieImdbId;

    CompletableFuture.supplyAsync(() -> {
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
      Platform.runLater(() -> {
        String targetUrl;
        if (imdbId != null && !imdbId.trim().isEmpty()) {
          targetUrl = "https://www.imdb.com/name/" + imdbId.trim() + "/";
          showStatus("Found " + finalName + " on IMDb!");
        } else {
          // Fallback to name search if no IMDb ID was found on Wikidata
          try {
            String encoded =
                URLEncoder.encode(finalName, StandardCharsets.UTF_8);
            targetUrl = "https://www.imdb.com/find/?q=" + encoded + "&s=nm";
            showStatus("Opened IMDb search for " + finalName);
          } catch (Exception e) {
            targetUrl = "https://www.imdb.com/";
          }
        }

        MainController tmpMain = appContext.getMainController();
        if (tmpMain != null) {
          tmpMain.showImdbWithUrl(targetUrl);
        }
      });
    });
  }

  /**
   * Executes a SPARQL query against the public Wikidata endpoint to retrieve
   * linked data. Used to resolve human-readable names to IMDb identifiers.
   *
   * @param sparql The query string.
   * @return The first extracted IMDb ID, or null if not found.
   * @throws Exception If network or parsing errors occur.
   */
  private String executeSparql(String sparql) throws Exception {
    String urlStr = "https://query.wikidata.org/sparql?query="
        + URLEncoder.encode(sparql, StandardCharsets.UTF_8).replace("+", "%20")
        + "&format=json";

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlStr))
        .header("User-Agent", "Mozilla/5.0 KinoNode/1.0")
        .header("Accept", "application/json").GET().build();

    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString());
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

  /**
   * Configures the layout for fields that may contain multiple values (e.g.
   * Actors). Switches to a ComboBox view if multiple comma-separated names are
   * detected.
   *
   * @param value The raw string value from the database.
   * @param txt The TextField representation for a single name.
   * @param combo The ComboBox representation for multiple names.
   */
  private void setupMultiField(String value, TextField txt,
      ComboBox<String> combo) {
    List<String> names = parseNames(value);
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

  private List<String> parseNames(String value) {
    List<String> list = new ArrayList<>();
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

  /**
   * Displays an interactive match management dialog for re-indexing the current
   * movie, providing interactive candidate selection with metadata preview and
   * manual search via IMDb ID.
   */
  private void showFixMatchDialog() {
    if (movie == null)
      return;

    Stage tmpStage = new Stage();
    tmpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
    if (imgPoster != null && imgPoster.getScene() != null) {
      tmpStage.initOwner(imgPoster.getScene().getWindow());
    }
    tmpStage.setTitle("Match & Re-index Movie — " + movie.getTitle());
    tmpStage.setMinWidth(780);
    tmpStage.setMinHeight(620);

    BorderPane tmpRoot = new BorderPane();
    tmpRoot.setPadding(new Insets(20));
    tmpRoot.setStyle("-fx-background-color: -color-bg-default;");

    Label tmpHeaderLabel =
        new Label("Enhanced Movie Import & Match Management");
    tmpHeaderLabel.setStyle(
        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-accent-emphasis;");
    tmpRoot.setTop(tmpHeaderLabel);

    // Interactive Title Search & Match Options List
    VBox tmpSection1 = new VBox(10);
    tmpSection1.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 15; -fx-background-radius: 8;");

    Label tmpSec1Title = new Label("Interactive Match Selection");
    tmpSec1Title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    TextField tmpSearchInput = new TextField(movie.getTitle());
    tmpSearchInput.setPromptText("Enter movie title to search candidates...");
    Button btnSearchCandidates = new Button("Search Candidates");
    btnSearchCandidates.getStyleClass().add("button-primary");
    HBox tmpSearchBox = new HBox(10, tmpSearchInput, btnSearchCandidates);
    HBox.setHgrow(tmpSearchInput, Priority.ALWAYS);

    VBox tmpCandidateContainer = new VBox(10);
    ScrollPane tmpScroll = new ScrollPane(tmpCandidateContainer);
    tmpScroll.setFitToWidth(true);
    tmpScroll.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(tmpScroll, Priority.ALWAYS);

    tmpSection1.getChildren().addAll(tmpSec1Title, tmpSearchBox, tmpScroll);
    VBox.setVgrow(tmpSection1, Priority.ALWAYS);

    // Manual Search via IMDb ID
    VBox tmpSection2 = new VBox(10);
    tmpSection2.setStyle(
        "-fx-background-color: -color-bg-overlay; -fx-padding: 15; -fx-background-radius: 8;");

    Label tmpSec2Title = new Label("Manual Search via IMDb ID");
    tmpSec2Title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    TextField tmpImdbInput = new TextField();
    tmpImdbInput.setPromptText("e.g. tt0111161");
    Button btnImdbImport = new Button("Instant Import / Re-index");
    btnImdbImport.getStyleClass().add("button-success");
    HBox tmpImdbBox = new HBox(10, tmpImdbInput, btnImdbImport);
    HBox.setHgrow(tmpImdbInput, Priority.ALWAYS);

    tmpSection2.getChildren().addAll(tmpSec2Title, tmpImdbBox);

    VBox tmpCenter = new VBox(15, tmpSection1, tmpSection2);
    VBox.setVgrow(tmpCenter, Priority.ALWAYS);
    BorderPane.setMargin(tmpCenter, new Insets(12, 0, 12, 0));
    tmpRoot.setCenter(tmpCenter);

    // OK Button MUST always occur in the right bottom corner
    Button btnClose = new Button("OK");
    btnClose.getStyleClass().add("button-secondary");
    btnClose.setStyle("-fx-font-weight: bold; -fx-padding: 8px 20px;");
    btnClose.setOnAction(e -> tmpStage.close());

    HBox tmpBottomBox = new HBox(btnClose);
    tmpBottomBox.setAlignment(Pos.BOTTOM_RIGHT);
    tmpRoot.setBottom(tmpBottomBox);

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
              .add(new Label("No candidate movie matches found."));
        } else {
          for (Movie tmpCand : tmpResults) {
            HBox tmpRow = new HBox(15);
            tmpRow.setAlignment(Pos.CENTER_LEFT);
            tmpRow.setStyle(
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

            Button btnSelectMatch = new Button("Select & Re-index");
            btnSelectMatch.getStyleClass().add("button-primary");
            btnSelectMatch.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 16px;");
            btnSelectMatch.setOnAction(e -> {
              String tmpTargetId = tmpCand.getImdbId();
              if (tmpTargetId == null || tmpTargetId.isBlank())
                return;

              tmpRoot.setDisable(true);
              if (tmpStage.getScene() != null) {
                tmpStage.getScene().setCursor(Cursor.WAIT);
              }

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
                tmpStage.close();
                setMovie(movie);
                showStatus("Movie re-indexed successfully!");
              });
              tmpReindexTask.setOnFailed(e2 -> {
                tmpRoot.setDisable(false);
                if (tmpStage.getScene() != null) {
                  tmpStage.getScene().setCursor(Cursor.DEFAULT);
                }
              });
              Thread t = new Thread(tmpReindexTask);
              t.setDaemon(true);
              t.start();
            });

            tmpRow.getChildren().addAll(tmpPoster, tmpInfo, tmpSp,
                btnSelectMatch);
            tmpCandidateContainer.getChildren().add(tmpRow);
          }
        }
      });

      Thread t = new Thread(tmpTask);
      t.setDaemon(true);
      t.start();
    };

    btnSearchCandidates.setOnAction(e -> tmpPerformSearch.run());
    tmpSearchInput.setOnAction(e -> tmpPerformSearch.run());

    btnImdbImport.setOnAction(e -> {
      String tmpImdbId = tmpImdbInput.getText().trim();
      if (!tmpImdbId.matches("^tt\\d{7,}$")) {
        tmpImdbInput.setStyle("-fx-border-color: red;");
        return;
      }
      tmpImdbInput.setStyle("");

      tmpRoot.setDisable(true);
      if (tmpStage.getScene() != null) {
        tmpStage.getScene().setCursor(Cursor.WAIT);
      }

      Task<Void> tmpImdbTask = new Task<>() {
        @Override
        protected Void call() {
          imdbService.enrichMovieDataById(tmpImdbId, movie);
          movie.setMatchStatus("MANUAL");
          movieRepository.update(movie);
          return null;
        }
      };

      tmpImdbTask.setOnSucceeded(evt -> {
        tmpStage.close();
        setMovie(movie);
        showStatus("Movie re-indexed via IMDb ID: " + tmpImdbId);
      });
      tmpImdbTask.setOnFailed(evt -> {
        tmpRoot.setDisable(false);
        if (tmpStage.getScene() != null) {
          tmpStage.getScene().setCursor(Cursor.DEFAULT);
        }
      });

      Thread t = new Thread(tmpImdbTask);
      t.setDaemon(true);
      t.start();
    });

    Scene tmpScene = new Scene(tmpRoot, 780, 620);
    if (imgPoster != null && imgPoster.getScene() != null) {
      tmpScene.getStylesheets().addAll(imgPoster.getScene().getStylesheets());
    }
    tmpStage.setScene(tmpScene);

    tmpPerformSearch.run();
    tmpStage.showAndWait();
  }
}
