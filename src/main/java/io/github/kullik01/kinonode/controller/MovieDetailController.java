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

import io.github.kullik01.kinonode.util.DialogBuilder;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
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
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.util.PathUtils;
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

  private Label lblHeaderTitle;
  private ImageView imgPoster;
  private Button btnPlay;
  private Button btnTrailer;
  private Button btnImdbPage;

  private TextField txtTitle;
  private TextField txtYear;
  private TextField txtDuration;
  private TextField txtGenre;
  private TextField txtDirector;
  private TextField txtWriter;
  private TextField txtProducer;
  private TextField txtCinematographer;
  private TextField txtComposer;
  private TextField txtActors;
  private ComboBox<String> comboDirector;
  private ComboBox<String> comboWriter;
  private ComboBox<String> comboProducer;
  private ComboBox<String> comboCinematographer;
  private ComboBox<String> comboComposer;
  private ComboBox<String> comboActors;
  private TextArea txtDescription;
  private TextField txtTrailerUrl;

  private FlowPane ratingContainer;
  private Label lblWatchlistsList;
  private Button btnWatchlists;
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
    statusTimer = new javafx.animation.PauseTransition(Duration.seconds(5));
    statusTimer.setOnFinished(e -> lblStatus.setText(""));
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

    Button btnBack = new Button("⬅");
    btnBack.getStyleClass().add("button-secondary");
    btnBack.setMinWidth(Region.USE_PREF_SIZE);
    btnBack.setOnAction(e -> goBack());
    Tooltip ttBack = new Tooltip("Back to Library");
    ttBack.setShowDelay(Duration.millis(250));
    btnBack.setTooltip(ttBack);

    lblHeaderTitle = new Label("Movie Details");
    lblHeaderTitle.getStyleClass().add("detail-title");

    headerBox.getChildren().addAll(btnBack, lblHeaderTitle);

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

    Button btnChangePoster = new Button();
    btnChangePoster.getStyleClass().addAll("button-secondary", "button-icon");
    btnChangePoster.setMinSize(36.0, 36.0);
    btnChangePoster.setMaxSize(36.0, 36.0);
    btnChangePoster.setOnAction(e -> changePoster());
    SVGPath svgImageChangePoster = new SVGPath();
    svgImageChangePoster.setContent(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-1 16H6c-.55 0-1-.45-1-1V6c0-.55.45-1 1-1h12c.55 0 1 .45 1 1v12c0 .55-.45 1-1 1zm-4.44-6.23L11.2 15.4l-2.45-3.07c-.2-.25-.59-.25-.79 0l-2.71 3.39c-.26.33-.02.81.39.81h12.7c.41 0 .65-.48.39-.81l-3.84-4.8c-.2-.26-.6-.26-.8 0z");
    svgImageChangePoster.setFill(Color.WHITE);
    svgImageChangePoster.setScaleX(0.75);
    svgImageChangePoster.setScaleY(0.75);
    btnChangePoster.setGraphic(svgImageChangePoster);
    Tooltip ttChangePoster = new Tooltip("Change Poster");
    ttChangePoster.setShowDelay(Duration.millis(250));
    btnChangePoster.setTooltip(ttChangePoster);

    VBox actionBox = new VBox(15.0);
    actionBox.setAlignment(Pos.CENTER);
    actionBox.setStyle("-fx-padding: 10px 0 0 0;");

    btnPlay = new Button();
    btnPlay.getStyleClass().addAll("button-play", "button-icon");
    btnPlay.setMinSize(36.0, 36.0);
    btnPlay.setMaxSize(36.0, 36.0);
    btnPlay.setOnAction(e -> playMovie());
    SVGPath svgImagePlay = new SVGPath();
    svgImagePlay.setContent("M8 5v14l11-7z");
    svgImagePlay.setFill(Color.WHITE);
    svgImagePlay.setScaleX(0.75);
    svgImagePlay.setScaleY(0.75);
    btnPlay.setGraphic(svgImagePlay);
    Tooltip ttPlay = new Tooltip("Play Movie");
    ttPlay.setShowDelay(Duration.millis(250));
    btnPlay.setTooltip(ttPlay);

    btnTrailer = new Button();
    btnTrailer.getStyleClass().addAll("button-secondary", "button-icon");
    btnTrailer.setMinSize(36.0, 36.0);
    btnTrailer.setMaxSize(36.0, 36.0);
    btnTrailer.setOnAction(e -> watchTrailer());
    SVGPath svgImageTrailer = new SVGPath();
    svgImageTrailer.setContent(
        "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12zm-5-6l-7 4V7z");
    svgImageTrailer.setFill(Color.WHITE);
    svgImageTrailer.setScaleX(0.75);
    svgImageTrailer.setScaleY(0.75);
    btnTrailer.setGraphic(svgImageTrailer);
    Tooltip ttTrailer = new Tooltip("Watch Trailer");
    ttTrailer.setShowDelay(Duration.millis(250));
    btnTrailer.setTooltip(ttTrailer);

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

    actionBox.getChildren().addAll(btnPlay, btnTrailer, btnImdbPage);
    leftBox.getChildren().addAll(posterContainer, btnChangePoster, actionBox);

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
    txtTitle = createReadOnlyField();
    addGridRow(grid, row++, "Title:", txtTitle, e -> editTitle());

    // Year
    txtYear = createReadOnlyField();
    addGridRow(grid, row++, "Release Year:", txtYear, e -> editYear());

    // Duration
    txtDuration = createReadOnlyField();
    addGridRow(grid, row++, "Duration:", txtDuration, e -> editDuration());

    // Genre
    txtGenre = createReadOnlyField();
    addGridRow(grid, row++, "Genre:", txtGenre, e -> editGenre());

    // Director
    txtDirector = createReadOnlyField();
    comboDirector = new ComboBox<>();
    addMultiGridRow(grid, row++, "Director(s):", txtDirector, comboDirector,
        e -> editDirector(), e -> searchDirectorOnImdb());

    // Writer
    txtWriter = createReadOnlyField();
    comboWriter = new ComboBox<>();
    addMultiGridRow(grid, row++, "Writer(s):", txtWriter, comboWriter,
        e -> editWriter(), e -> searchWriterOnImdb());

    // Producer
    txtProducer = createReadOnlyField();
    comboProducer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Producer(s):", txtProducer, comboProducer,
        e -> editProducer(), e -> searchProducerOnImdb());

    // Cinematographer
    txtCinematographer = createReadOnlyField();
    comboCinematographer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Cinematographer(s):", txtCinematographer,
        comboCinematographer, e -> editCinematographer(),
        e -> searchCinematographerOnImdb());

    // Composer
    txtComposer = createReadOnlyField();
    comboComposer = new ComboBox<>();
    addMultiGridRow(grid, row++, "Composer(s):", txtComposer, comboComposer,
        e -> editComposer(), e -> searchComposerOnImdb());

    // Actors
    txtActors = createReadOnlyField();
    comboActors = new ComboBox<>();
    addMultiGridRow(grid, row++, "Actor(s):", txtActors, comboActors,
        e -> editActors(), e -> searchActorsOnImdb());

    // Trailer URL
    Label lblTrailer = new Label("Trailer Link:");
    lblTrailer.setStyle("-fx-font-weight: bold;");
    grid.add(lblTrailer, 0, row);

    HBox trailerBox = new HBox(5.0);
    txtTrailerUrl = createReadOnlyField();
    txtTrailerUrl.setPromptText("https://www.youtube.com/watch?v=...");
    HBox.setHgrow(txtTrailerUrl, Priority.ALWAYS);
    Button btnEditTrailer = new Button("📝");
    btnEditTrailer.getStyleClass().add("button-secondary");
    btnEditTrailer.setMinWidth(Region.USE_PREF_SIZE);
    btnEditTrailer.setOnAction(e -> editTrailerUrl());
    Tooltip ttEditTrailer = new Tooltip("Edit Trailer Link");
    ttEditTrailer.setShowDelay(Duration.millis(250));
    btnEditTrailer.setTooltip(ttEditTrailer);
    trailerBox.getChildren().addAll(txtTrailerUrl, btnEditTrailer);
    grid.add(trailerBox, 1, row++);

    // Rating
    Label lblRating = new Label("IMDb Rating:");
    lblRating.setStyle("-fx-font-weight: bold;");
    grid.add(lblRating, 0, row);

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
    txtDescription = new TextArea();
    txtDescription.setStyle("-fx-text-fill: -color-fg-default;");
    txtDescription.setEditable(false);
    txtDescription.setMouseTransparent(true);
    txtDescription.setFocusTraversable(false);
    txtDescription.setPrefHeight(100.0);
    txtDescription.setWrapText(true);
    HBox.setHgrow(txtDescription, Priority.ALWAYS);

    Button btnEditDesc = new Button("📝");
    btnEditDesc.getStyleClass().add("button-secondary");
    btnEditDesc.setMinWidth(Region.USE_PREF_SIZE);
    btnEditDesc.setOnAction(e -> editDescription());
    Tooltip ttEditDesc = new Tooltip("Edit Description");
    ttEditDesc.setShowDelay(Duration.millis(250));
    btnEditDesc.setTooltip(ttEditDesc);
    descriptionBox.getChildren().addAll(txtDescription, btnEditDesc);
    grid.add(descriptionBox, 1, row);

    lblStatus = new Label("");
    lblStatus.setStyle("-fx-text-fill: -color-success-fg;");

    rightBox.getChildren().addAll(grid, lblStatus);
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
          io.github.kullik01.kinonode.service.ImdbService
              .fetchImdbDurationDirectly(movie);
        }
        if (movie.getDuration() == null || movie.getDuration() <= 0
            || (movie.getRating() == null && movie.getRawRating() == null)) {
          io.github.kullik01.kinonode.service.ImdbService
              .enrichMovieData(movie);
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
            AppContext.getInstance().notifyLibraryChanged();
          }
        });
      });
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

    // Auto-save when changing tabs (which removes the view from the scene)
    lblHeaderTitle.sceneProperty().addListener((obs, oldVal, newVal) -> {
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
              "/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
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
      AppContext.getInstance().getPlaybackService().play(movie.getFilePath());
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
    String url = txtTrailerUrl.getText().trim();
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

    MainController tmpMain = AppContext.getInstance().getMainController();
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
            "/io/github/kullik01/kinonode/images/question_mark.png");

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
    showEditDialog("Title", "Movie Title", txtTitle, InputConstraint.ANY,
        val -> movie.setTitle(val.isEmpty() ? null : val));
  }

  private void editYear() {
    showEditDialog("Year", "Release Year", txtYear,
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
    showEditDialog("Genre", "Genre", txtGenre, InputConstraint.NO_NUMBERS,
        val -> movie.setGenre(val.isEmpty() ? null : val));
  }

  private void editDirector() {
    showEditDialog("Director", "Director(s)", txtDirector,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setDirector(val.isEmpty() ? null : val);
          setupMultiField(movie.getDirector(), txtDirector, comboDirector);
        });
  }

  private void editWriter() {
    showEditDialog("Writer", "Writer(s)", txtWriter, InputConstraint.NO_NUMBERS,
        val -> {
          movie.setWriter(val.isEmpty() ? null : val);
          setupMultiField(movie.getWriter(), txtWriter, comboWriter);
        });
  }

  private void editProducer() {
    showEditDialog("Producer", "Producer(s)", txtProducer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setProducer(val.isEmpty() ? null : val);
          setupMultiField(movie.getProducer(), txtProducer, comboProducer);
        });
  }

  private void editCinematographer() {
    showEditDialog("Cinematographer", "Cinematographer(s)", txtCinematographer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setCinematographer(val.isEmpty() ? null : val);
          setupMultiField(movie.getCinematographer(), txtCinematographer,
              comboCinematographer);
        });
  }

  private void editComposer() {
    showEditDialog("Composer", "Composer(s)", txtComposer,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setComposer(val.isEmpty() ? null : val);
          setupMultiField(movie.getComposer(), txtComposer, comboComposer);
        });
  }

  private void editActors() {
    showEditDialog("Actor(s)", "Actor(s)", txtActors,
        InputConstraint.NO_NUMBERS, val -> {
          movie.setActors(val.isEmpty() ? null : val);
          setupMultiField(movie.getActors(), txtActors, comboActors);
        });
  }

  private void editDescription() {
    showEditDialog("Description", "Description", txtDescription,
        InputConstraint.NO_NUMBERS,
        val -> movie.setDescription(val.isEmpty() ? null : val));
  }

  private void editTrailerUrl() {
    Dialog<String> dialog =
        DialogBuilder.createDialog("Edit Trailer Link", "YouTube Trailer URL",
            "/io/github/kullik01/kinonode/images/question_mark.png");



    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
        ButtonType.CANCEL);

    VBox vBox = new VBox(5);
    Label errorLabel = new Label();
    errorLabel.setWrapText(true);
    errorLabel.setPrefWidth(430);
    errorLabel.setMinHeight(Region.USE_PREF_SIZE);
    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

    String defaultText = txtTrailerUrl.getText().trim();

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
      } catch (NumberFormatException exception) {
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
    AppContext.getInstance().notifyLibraryChanged();
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
        LibraryController controller = new LibraryController();
        Parent libraryView = controller.buildView();
        controller.initialize();

        StackPane contentArea =
            (StackPane) lblHeaderTitle.getScene().lookup("#contentArea");
        if (contentArea != null) {
          contentArea.getChildren().setAll(libraryView);
        }
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create LibraryView view", e);
      }
    }
  }

  private void searchDirectorOnImdb() {
    searchPersonOnImdb(comboDirector, txtDirector);
  }

  private void searchWriterOnImdb() {
    searchPersonOnImdb(comboWriter, txtWriter);
  }

  private void searchProducerOnImdb() {
    searchPersonOnImdb(comboProducer, txtProducer);
  }

  private void searchCinematographerOnImdb() {
    searchPersonOnImdb(comboCinematographer, txtCinematographer);
  }

  private void searchComposerOnImdb() {
    searchPersonOnImdb(comboComposer, txtComposer);
  }

  private void searchActorsOnImdb() {
    searchPersonOnImdb(comboActors, txtActors);
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

        MainController tmpMain = AppContext.getInstance().getMainController();
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
}
