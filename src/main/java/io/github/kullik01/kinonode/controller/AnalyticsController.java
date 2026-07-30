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

import io.github.kullik01.kinonode.util.CursorUtils;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.util.CustomAlert;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for the Analytics dashboard view. Computes live statistics for the
 * collection (movies per decade, rating distribution, genre distribution, and
 * top cast & crew rankings) and offers catalog export capabilities.
 */
public class AnalyticsController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(AnalyticsController.class.getName());

  private Label lblTotalMovies;
  private Label lblAvgRating;
  private Label lblYearRange;
  private Label lblTotalWatchlists;
  private Label lblTopGenre;
  private Label lblTotalDirectors;

  private BarChart<String, Number> decadeChart;
  private BarChart<String, Number> ratingChart;
  private BarChart<String, Number> genreChart;

  private ComboBox<String> comboRoleFilter;
  private VBox topPeopleBox;

  private final Map<String, Map<String, Integer>> roleFrequencies =
      new HashMap<>();
  private final Map<String, List<String>> decadeMovies = new TreeMap<>();
  private final Map<String, List<String>> ratingMovies = new LinkedHashMap<>();
  private final Map<String, List<String>> genreMovies = new HashMap<>();
  private final Map<String, Map<String, List<String>>> rolePersonMovies =
      new HashMap<>();

  public Parent buildView() {
    ScrollPane rootPane = new ScrollPane();
    rootPane.setFitToWidth(true);
    rootPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    VBox mainContent = new VBox(20.0);
    mainContent.getStyleClass().add("detail-pane");
    mainContent.setStyle("-fx-padding: 24px;");

    // Page Header & Action Buttons
    VBox headerCard = new VBox(16.0);
    headerCard.getStyleClass().add("analytics-header-card");

    VBox headerTitles = new VBox(4.0);
    Label titleLabel = new Label("Collection Insights & Analytics");
    titleLabel.getStyleClass().add("analytics-title");
    Label subtitleLabel = new Label(
        "Real-time catalog metrics, visual charts, and distribution reports.");
    subtitleLabel.getStyleClass().add("analytics-subtitle");
    headerTitles.getChildren().addAll(titleLabel, subtitleLabel);

    FlowPane btnFlow = new FlowPane();
    btnFlow.setHgap(12.0);
    btnFlow.setVgap(12.0);

    javafx.scene.control.Button btnJson =
        new javafx.scene.control.Button("Export JSON");
    btnJson.getStyleClass().add("analytics-action-button");
    btnJson.setOnAction(e -> handleExportJson());
    SVGPath svgJson = new SVGPath();
    svgJson.getStyleClass().add("analytics-btn-svg");
    svgJson.setScaleX(0.75);
    svgJson.setScaleY(0.75);
    svgJson.setContent("M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z");
    btnJson.setGraphic(svgJson);
    Tooltip ttJson = new Tooltip("Export entire movie catalog to JSON format");
    ttJson.setShowDelay(Duration.millis(100));
    btnJson.setTooltip(ttJson);

    javafx.scene.control.Button btnCsv =
        new javafx.scene.control.Button("Export CSV");
    btnCsv.getStyleClass().add("analytics-action-button");
    btnCsv.setOnAction(e -> handleExportCsv());
    SVGPath svgCsv = new SVGPath();
    svgCsv.getStyleClass().add("analytics-btn-svg");
    svgCsv.setScaleX(0.75);
    svgCsv.setScaleY(0.75);
    svgCsv.setContent(
        "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6zm4-8h4v2h-4zm0 4h4v2h-4z");
    btnCsv.setGraphic(svgCsv);
    Tooltip ttCsv =
        new Tooltip("Export movie catalog spreadsheet to CSV format");
    ttCsv.setShowDelay(Duration.millis(100));
    btnCsv.setTooltip(ttCsv);

    javafx.scene.control.Button btnHtml =
        new javafx.scene.control.Button("HTML Visual Report");
    btnHtml.getStyleClass().add("analytics-action-button");
    btnHtml.setOnAction(e -> handleExportHtml());
    SVGPath svgHtml = new SVGPath();
    svgHtml.getStyleClass().add("analytics-btn-svg");
    svgHtml.setScaleX(0.75);
    svgHtml.setScaleY(0.75);
    svgHtml.setContent(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z");
    btnHtml.setGraphic(svgHtml);
    Tooltip ttHtml =
        new Tooltip("Generate a styled HTML report of your collection");
    ttHtml.setShowDelay(Duration.millis(100));
    btnHtml.setTooltip(ttHtml);

    btnFlow.getChildren().addAll(btnJson, btnCsv, btnHtml);
    headerCard.getChildren().addAll(headerTitles, btnFlow);

    // KPI Summary Cards
    GridPane kpiGrid = new GridPane();
    kpiGrid.setHgap(16.0);
    kpiGrid.setVgap(16.0);
    ColumnConstraints columnConstraint1 = new ColumnConstraints();
    columnConstraint1.setPercentWidth(33.3333);
    ColumnConstraints columnConstraint2 = new ColumnConstraints();
    columnConstraint2.setPercentWidth(33.3333);
    ColumnConstraints cc3 = new ColumnConstraints();
    cc3.setPercentWidth(33.3334);
    kpiGrid.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
        cc3);

    lblTotalMovies = new Label("0");
    VBox kpiTotalMovies = createKpiCard("TOTAL MOVIES", lblTotalMovies,
        "titles in catalog",
        "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z",
        "kpi-card-blue");
    kpiGrid.add(kpiTotalMovies, 0, 0);

    lblAvgRating = new Label("—");
    VBox kpiAvgRating = createKpiCard("AVERAGE IMDb RATING", lblAvgRating,
        "out of 10.0 scale",
        "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z",
        "kpi-card-orange");
    kpiGrid.add(kpiAvgRating, 1, 0);

    lblYearRange = new Label("—");
    VBox kpiYearRange = createKpiCard("RELEASE ERA", lblYearRange,
        "year range span",
        "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z",
        "kpi-card-blue");
    kpiGrid.add(kpiYearRange, 2, 0);

    lblTotalWatchlists = new Label("0");
    VBox kpiWatchlists = createKpiCard("WATCHLISTS", lblTotalWatchlists,
        "active collections",
        "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 9H10V9h8v2zm-4 4h-4v-2h4v2zm4-8h-8V5h8v2z",
        "kpi-card-orange");
    kpiGrid.add(kpiWatchlists, 0, 1);

    lblTopGenre = new Label("—");
    VBox kpiTopGenre = createKpiCard("TOP GENRE", lblTopGenre,
        "most frequent genre",
        "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z",
        "kpi-card-blue");
    kpiGrid.add(kpiTopGenre, 1, 1);

    lblTotalDirectors = new Label("0");
    VBox kpiDirectors = createKpiCard("DIRECTORS", lblTotalDirectors,
        "unique filmmakers",
        "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z",
        "kpi-card-orange");
    kpiGrid.add(kpiDirectors, 2, 1);

    // Charts & Top Lists
    GridPane chartGrid = new GridPane();
    chartGrid.setHgap(16.0);
    chartGrid.setVgap(16.0);
    VBox.setVgrow(chartGrid, Priority.ALWAYS);
    ColumnConstraints ch1 = new ColumnConstraints();
    ch1.setPercentWidth(50.0);
    ColumnConstraints ch2 = new ColumnConstraints();
    ch2.setPercentWidth(50.0);
    chartGrid.getColumnConstraints().addAll(ch1, ch2);

    VBox decBox = new VBox(16.0);
    decBox.getStyleClass().add("analytics-section-card");
    decBox.setMaxWidth(Double.MAX_VALUE);
    Label decLabel = new Label("Movies per Decade");
    decLabel.getStyleClass().add("analytics-section-header");
    javafx.scene.chart.CategoryAxis decX =
        new javafx.scene.chart.CategoryAxis();
    decX.setLabel("Decade");
    javafx.scene.chart.NumberAxis decY = new javafx.scene.chart.NumberAxis();
    decY.setLabel("Count");
    decadeChart = new BarChart<>(decX, decY);
    decadeChart.setLegendVisible(false);
    decadeChart.setAnimated(false);
    decadeChart.setPrefHeight(260.0);
    decBox.getChildren().addAll(decLabel, decadeChart);
    chartGrid.add(decBox, 0, 0);

    VBox ratBox = new VBox(16.0);
    ratBox.getStyleClass().add("analytics-section-card");
    ratBox.setMaxWidth(Double.MAX_VALUE);
    Label ratLabel = new Label("IMDb Rating Breakdown (0-10)");
    ratLabel.getStyleClass().add("analytics-section-header");
    javafx.scene.chart.CategoryAxis ratX =
        new javafx.scene.chart.CategoryAxis();
    ratX.setLabel("IMDb Rating");
    javafx.scene.chart.NumberAxis ratY = new javafx.scene.chart.NumberAxis();
    ratY.setLabel("Count");
    ratingChart = new BarChart<>(ratX, ratY);
    ratingChart.setLegendVisible(false);
    ratingChart.setAnimated(false);
    ratingChart.setPrefHeight(260.0);
    ratBox.getChildren().addAll(ratLabel, ratingChart);
    chartGrid.add(ratBox, 1, 0);

    VBox genBox = new VBox(16.0);
    genBox.getStyleClass().add("analytics-section-card");
    genBox.setMaxWidth(Double.MAX_VALUE);
    Label genLabel = new Label("Movie Genres & Appearance");
    genLabel.getStyleClass().add("analytics-section-header");
    javafx.scene.chart.CategoryAxis genX =
        new javafx.scene.chart.CategoryAxis();
    genX.setLabel("Genre");
    javafx.scene.chart.NumberAxis genY = new javafx.scene.chart.NumberAxis();
    genY.setLabel("Count");
    genreChart = new BarChart<>(genX, genY);
    genreChart.setLegendVisible(false);
    genreChart.setAnimated(false);
    genreChart.setPrefHeight(260.0);
    genBox.getChildren().addAll(genLabel, genreChart);
    chartGrid.add(genBox, 0, 1);

    VBox pplBox = new VBox(16.0);
    pplBox.getStyleClass().add("analytics-section-card");
    pplBox.setMaxWidth(Double.MAX_VALUE);
    HBox pplHeaderBox = new HBox(10.0);
    pplHeaderBox.setAlignment(Pos.CENTER_LEFT);
    Label pplLabel = new Label("Top People");
    pplLabel.getStyleClass().add("analytics-section-header");
    Region pplSpacer = new Region();
    HBox.setHgrow(pplSpacer, Priority.ALWAYS);
    comboRoleFilter = new ComboBox<>();
    comboRoleFilter.setOnAction(e -> handleRoleFilterChange());
    pplHeaderBox.getChildren().addAll(pplLabel, pplSpacer, comboRoleFilter);
    topPeopleBox = new VBox(10.0);
    topPeopleBox.setStyle("-fx-padding: 6px 0 0 0;");
    pplBox.getChildren().addAll(pplHeaderBox, topPeopleBox);
    chartGrid.add(pplBox, 1, 1);

    mainContent.getChildren().addAll(headerCard, kpiGrid, chartGrid);
    rootPane.setContent(mainContent);

    return rootPane;
  }

  private VBox createKpiCard(String title, Label valLabel, String subtitle,
      String svgContent, String extraClass) {
    VBox v = new VBox();
    v.setMaxWidth(Double.MAX_VALUE);
    v.getStyleClass().addAll("analytics-kpi-card", extraClass);

    HBox h = new HBox(8.0);
    h.setAlignment(Pos.CENTER_LEFT);
    SVGPath svg = new SVGPath();
    svg.getStyleClass().add("analytics-kpi-svg");
    svg.setScaleX(0.85);
    svg.setScaleY(0.85);
    svg.setContent(svgContent);
    Label tLabel = new Label(title);
    tLabel.getStyleClass().add("analytics-kpi-title");
    h.getChildren().addAll(svg, tLabel);

    valLabel.getStyleClass().add("analytics-kpi-value");
    Label subLabel = new Label(subtitle);
    subLabel.getStyleClass().add("analytics-kpi-subtitle");

    v.getChildren().addAll(h, valLabel, subLabel);
    return v;
  }

  /**
   * Initializes the analytics view by disabling unwanted chart animations and
   * populating the role filter dropdown with default values.
   */
  public void initialize() {
    if (decadeChart != null) {
      decadeChart.setAnimated(false);
    }
    if (ratingChart != null) {
      ratingChart.setAnimated(false);
    }
    if (genreChart != null) {
      genreChart.setAnimated(false);
    }
    if (comboRoleFilter != null && comboRoleFilter.getItems().isEmpty()) {
      comboRoleFilter.getItems().addAll("Director(s)", "Writer(s)",
          "Producer(s)", "Cinematographer(s)", "Composer(s)", "Actor(s)");
      comboRoleFilter.setValue("Director(s)");
    }
    refresh();
  }

  @Override
  public void refresh() {
    List<Movie> tmpMovies =
        AppContext.getInstance().getMovieRepository().findAll();
    List<Watchlist> tmpWatchlists =
        AppContext.getInstance().getWatchlistRepository().findAll();

    // KPI Summary
    lblTotalMovies.setText(String.valueOf(tmpMovies.size()));
    lblTotalWatchlists.setText(String.valueOf(tmpWatchlists.size()));

    double tmpTotalRating = 0;
    int tmpRatedCount = 0;
    int tmpMinYear = Integer.MAX_VALUE;
    int tmpMaxYear = Integer.MIN_VALUE;

    decadeMovies.clear();
    ratingMovies.clear();
    genreMovies.clear();
    rolePersonMovies.clear();

    Map<String, Integer> tmpDecades = new TreeMap<>();
    Map<String, Integer> tmpRatings = new LinkedHashMap<>();
    for (int i = 0; i <= 10; i++) {
      String tmpKey = i + "/10";
      tmpRatings.put(tmpKey, 0);
      ratingMovies.put(tmpKey, new ArrayList<>());
    }

    Map<String, Integer> tmpDirectors = new HashMap<>();
    Map<String, Integer> tmpWriters = new HashMap<>();
    Map<String, Integer> tmpProducers = new HashMap<>();
    Map<String, Integer> tmpCinematographers = new HashMap<>();
    Map<String, Integer> tmpComposers = new HashMap<>();
    Map<String, Integer> tmpActors = new HashMap<>();
    Map<String, Integer> tmpGenres = new HashMap<>();

    Map<String, List<String>> tmpDirectorMovies = new HashMap<>();
    Map<String, List<String>> tmpWriterMovies = new HashMap<>();
    Map<String, List<String>> tmpProducerMovies = new HashMap<>();
    Map<String, List<String>> tmpCinematographerMovies = new HashMap<>();
    Map<String, List<String>> tmpComposerMovies = new HashMap<>();
    Map<String, List<String>> tmpActorMovies = new HashMap<>();

    for (Movie movieItem : tmpMovies) {
      String title =
          (movieItem.getTitle() != null && !movieItem.getTitle().isBlank())
              ? movieItem.getTitle()
              : "Untitled";

      if (movieItem.getRawRating() != null) {
        tmpTotalRating += movieItem.getRawRating();
        tmpRatedCount++;
      } else if (movieItem.getRating() != null) {
        tmpTotalRating += movieItem.getRating();
        tmpRatedCount++;
      }

      if (movieItem.getRating() != null) {
        String tmpRatingKey = movieItem.getRating() + "/10";
        tmpRatings.put(tmpRatingKey,
            tmpRatings.getOrDefault(tmpRatingKey, 0) + 1);
        ratingMovies.computeIfAbsent(tmpRatingKey, k -> new ArrayList<>())
            .add(title);
      }

      if (movieItem.getYear() != null) {
        tmpMinYear = Math.min(tmpMinYear, movieItem.getYear());
        tmpMaxYear = Math.max(tmpMaxYear, movieItem.getYear());
        int tmpDecadeStart = (movieItem.getYear() / 10) * 10;
        String tmpDecadeKey = tmpDecadeStart + "s";
        tmpDecades.put(tmpDecadeKey,
            tmpDecades.getOrDefault(tmpDecadeKey, 0) + 1);
        decadeMovies.computeIfAbsent(tmpDecadeKey, k -> new ArrayList<>())
            .add(title);
      }

      if (movieItem.getGenre() != null && !movieItem.getGenre().isBlank()) {
        for (String tmpGenre : movieItem.getGenre().split(",")) {
          String tmpClean = tmpGenre.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpGenres.put(tmpClean, tmpGenres.getOrDefault(tmpClean, 0) + 1);
            genreMovies.computeIfAbsent(tmpClean, k -> new ArrayList<>())
                .add(title);
          }
        }
      }

      parseAndCountWithTitle(movieItem.getDirector(), tmpDirectors,
          tmpDirectorMovies, title);
      parseAndCountWithTitle(movieItem.getWriter(), tmpWriters, tmpWriterMovies,
          title);
      parseAndCountWithTitle(movieItem.getProducer(), tmpProducers,
          tmpProducerMovies, title);
      parseAndCountWithTitle(movieItem.getCinematographer(),
          tmpCinematographers, tmpCinematographerMovies, title);
      parseAndCountWithTitle(movieItem.getComposer(), tmpComposers,
          tmpComposerMovies, title);
      parseAndCountWithTitle(movieItem.getActors(), tmpActors, tmpActorMovies,
          title);
    }

    roleFrequencies.put("Director(s)", tmpDirectors);
    roleFrequencies.put("Writer(s)", tmpWriters);
    roleFrequencies.put("Producer(s)", tmpProducers);
    roleFrequencies.put("Cinematographer(s)", tmpCinematographers);
    roleFrequencies.put("Composer(s)", tmpComposers);
    roleFrequencies.put("Actor(s)", tmpActors);

    rolePersonMovies.put("Director(s)", tmpDirectorMovies);
    rolePersonMovies.put("Writer(s)", tmpWriterMovies);
    rolePersonMovies.put("Producer(s)", tmpProducerMovies);
    rolePersonMovies.put("Cinematographer(s)", tmpCinematographerMovies);
    rolePersonMovies.put("Composer(s)", tmpComposerMovies);
    rolePersonMovies.put("Actor(s)", tmpActorMovies);

    if (tmpRatedCount > 0) {
      lblAvgRating
          .setText(String.format("%.1f / 10", tmpTotalRating / tmpRatedCount));
    } else {
      lblAvgRating.setText("—");
    }

    if (tmpMinYear != Integer.MAX_VALUE && tmpMaxYear != Integer.MIN_VALUE) {
      lblYearRange.setText(tmpMinYear + " – " + tmpMaxYear);
    } else {
      lblYearRange.setText("—");
    }

    if (lblTopGenre != null) {
      String tmpTopGenre = tmpGenres.entrySet().stream()
          .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");
      lblTopGenre.setText(tmpTopGenre);
    }

    if (lblTotalDirectors != null) {
      lblTotalDirectors.setText(String.valueOf(tmpDirectors.size()));
    }

    // Populate charts
    populateDecadeChart(tmpDecades);
    populateRatingChart(tmpRatings);
    populateGenreChart(tmpGenres);

    // Populate Top People
    updateTopPeople();
  }

  /**
   * Parses comma-separated names from a movie field and updates frequency/movie
   * tracking maps.
   *
   * @param rawField The raw string from the database (e.g. "Director1,
   *        Director2").
   * @param targetMap The map to increment frequencies in.
   * @param targetMovieMap The map tracking which movies a person is associated
   *        with.
   * @param movieTitle The title of the movie being processed.
   */
  private void parseAndCountWithTitle(String rawField,
      Map<String, Integer> targetMap, Map<String, List<String>> targetMovieMap,
      String movieTitle) {
    if (rawField != null && !rawField.isBlank()) {
      for (String token : rawField.split(",")) {
        String tmpClean = token.trim();
        if (!tmpClean.isEmpty() && !tmpClean.equals("...")
            && !tmpClean.equalsIgnoreCase("Unknown")) {
          targetMap.put(tmpClean, targetMap.getOrDefault(tmpClean, 0) + 1);
          targetMovieMap.computeIfAbsent(tmpClean, k -> new ArrayList<>())
              .add(movieTitle);
        }
      }
    }
  }

  /**
   * Constructs a formatted, newline-separated bullet list of movie titles to be
   * displayed inside tooltips.
   *
   * @param rawMovies List of raw movie titles.
   * @return A formatted string of titles.
   */
  private String buildMovieListText(List<String> rawMovies) {
    if (rawMovies == null || rawMovies.isEmpty()) {
      return "No movies found";
    }
    List<String> movies = rawMovies.stream().distinct().toList();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < movies.size(); i++) {
      if (i > 0) {
        sb.append("\n");
      }
      sb.append("• ").append(movies.get(i));
    }
    return sb.toString();
  }

  /**
   * Event handler triggered when the user changes the selected role filter
   * (e.g., from Directors to Actors). Re-populates the Top People ranking.
   */
  private void handleRoleFilterChange() {
    updateTopPeople();
  }

  /**
   * Updates the Top People ranking list based on the currently selected role
   * filter.
   */
  private void updateTopPeople() {
    if (comboRoleFilter == null || topPeopleBox == null) {
      return;
    }
    String selectedRole = comboRoleFilter.getValue();
    if (selectedRole == null) {
      selectedRole = "Director(s)";
    }
    Map<String, Integer> frequencies =
        roleFrequencies.getOrDefault(selectedRole, Collections.emptyMap());
    populateRankingList(topPeopleBox, frequencies, 5);
  }

  /**
   * Attaches an interactive tooltip and hover styling to a specific chart data
   * node.
   *
   * @param data The XYChart data point.
   * @param tooltipText The text to display in the tooltip.
   */
  private void installChartTooltip(XYChart.Data<String, Number> data,
      String tooltipText) {
    Runnable attacher = () -> {
      Node tmpNode = data.getNode();
      if (tmpNode != null) {
        Tooltip tmpTooltip = new Tooltip(tooltipText);
        tmpTooltip.setShowDelay(Duration.millis(150));
        tmpTooltip.setShowDuration(Duration.seconds(15));
        tmpTooltip.getStyleClass().add("chart-popup-tooltip");
        Tooltip.install(tmpNode, tmpTooltip);

        CursorUtils.applyCursor(tmpNode);
        // Use a style class toggle instead of setOpacity() to avoid
        // conflicting with CSS :hover rules which causes flickering.
        // hoverProperty() fires even when the mouse is stationary.
        tmpNode.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
          if (isHovered) {
            if (!tmpNode.getStyleClass().contains("chart-bar-hovered")) {
              tmpNode.getStyleClass().add("chart-bar-hovered");
            }
          } else {
            tmpNode.getStyleClass().remove("chart-bar-hovered");
          }
        });
      }
    };

    if (data.getNode() != null) {
      attacher.run();
    } else {
      data.nodeProperty().addListener((obs, oldNode, newNode) -> {
        if (newNode != null) {
          attacher.run();
        }
      });
    }
  }

  /**
   * Populates the "Movies per Decade" bar chart.
   *
   * @param decades A map of decade labels to their corresponding movie counts.
   */
  private void populateDecadeChart(Map<String, Integer> decades) {
    if (decadeChart == null) {
      return;
    }
    decadeChart.getData().clear();
    XYChart.Series<String, Number> tmpSeries = new XYChart.Series<>();
    tmpSeries.setName("Movies per Decade");

    for (Map.Entry<String, Integer> tmpEntry : decades.entrySet()) {
      XYChart.Data<String, Number> tmpData =
          new XYChart.Data<>(tmpEntry.getKey(), tmpEntry.getValue());
      tmpSeries.getData().add(tmpData);

      List<String> titles =
          decadeMovies.getOrDefault(tmpEntry.getKey(), Collections.emptyList());
      installChartTooltip(tmpData, buildMovieListText(titles));
    }
    decadeChart.getData().add(tmpSeries);
  }

  /**
   * Populates the "Rating Distribution" bar chart.
   *
   * @param ratings A map of rating labels to their corresponding movie counts.
   */
  private void populateRatingChart(Map<String, Integer> ratings) {
    if (ratingChart == null) {
      return;
    }
    ratingChart.getData().clear();
    XYChart.Series<String, Number> tmpSeries = new XYChart.Series<>();
    tmpSeries.setName("Rating Distribution");

    for (Map.Entry<String, Integer> tmpEntry : ratings.entrySet()) {
      XYChart.Data<String, Number> tmpData =
          new XYChart.Data<>(tmpEntry.getKey(), tmpEntry.getValue());
      tmpSeries.getData().add(tmpData);

      List<String> titles =
          ratingMovies.getOrDefault(tmpEntry.getKey(), Collections.emptyList());
      installChartTooltip(tmpData, buildMovieListText(titles));
    }
    ratingChart.getData().add(tmpSeries);
  }

  /**
   * Populates the "Movies per Genre" bar chart, displaying the top 10 genres.
   *
   * @param genres A map of genre labels to their corresponding movie counts.
   */
  private void populateGenreChart(Map<String, Integer> genres) {
    if (genreChart == null) {
      return;
    }
    genreChart.getData().clear();
    XYChart.Series<String, Number> tmpSeries = new XYChart.Series<>();
    tmpSeries.setName("Movies per Genre");

    genres.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10).forEach(entry -> {
          XYChart.Data<String, Number> tmpData =
              new XYChart.Data<>(entry.getKey(), entry.getValue());
          tmpSeries.getData().add(tmpData);

          List<String> titles =
              genreMovies.getOrDefault(entry.getKey(), Collections.emptyList());
          installChartTooltip(tmpData, buildMovieListText(titles));
        });
    genreChart.getData().add(tmpSeries);
  }

  /**
   * Populates a VBox container with visually rich ranking rows showing the top
   * individuals for a selected role.
   *
   * @param container The VBox layout to populate.
   * @param frequencies A map of person names to their occurrence counts.
   * @param limit The maximum number of top results to display.
   */
  private void populateRankingList(VBox container,
      Map<String, Integer> frequencies, int limit) {
    if (container == null) {
      return;
    }
    container.getChildren().clear();

    if (frequencies == null || frequencies.isEmpty()) {
      Label tmpEmpty = new Label("No statistics available for this role");
      tmpEmpty.getStyleClass().add("analytics-subtitle");
      container.getChildren().add(tmpEmpty);
      return;
    }

    String selectedRole =
        comboRoleFilter != null ? comboRoleFilter.getValue() : "Director(s)";
    Map<String, List<String>> personMovies =
        rolePersonMovies.getOrDefault(selectedRole, Collections.emptyMap());

    List<Map.Entry<String, Integer>> tmpSorted = frequencies.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(limit).toList();

    int tmpMaxCount =
        tmpSorted.stream().mapToInt(Map.Entry::getValue).max().orElse(1);

    int rank = 1;
    for (Map.Entry<String, Integer> entry : tmpSorted) {
      VBox card = new VBox(6);
      card.getStyleClass().add("ranking-row");

      HBox tmpHeader = new HBox(10);
      tmpHeader.setAlignment(Pos.CENTER_LEFT);

      Label tmpBadge = new Label("#" + rank);
      tmpBadge.getStyleClass().add("rank-badge");
      if (rank == 1) {
        tmpBadge.getStyleClass().add("rank-badge-top1");
      } else if (rank == 2) {
        tmpBadge.getStyleClass().add("rank-badge-top2");
      } else if (rank == 3) {
        tmpBadge.getStyleClass().add("rank-badge-top3");
      }

      Label tmpName = new Label(entry.getKey());
      tmpName
          .setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

      Region tmpSpacer = new Region();
      HBox.setHgrow(tmpSpacer, Priority.ALWAYS);

      int count = entry.getValue();
      Label tmpCount = new Label(count + (count == 1 ? " title" : " titles"));
      tmpCount.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");

      tmpHeader.getChildren().addAll(tmpBadge, tmpName, tmpSpacer, tmpCount);

      // Relative visual bar track & fill
      StackPane tmpTrack = new StackPane();
      tmpTrack.getStyleClass().add("ranking-bar-track");
      tmpTrack.setMaxWidth(Double.MAX_VALUE);

      double tmpRatio = (double) count / tmpMaxCount;
      Region tmpFill = new Region();
      tmpFill.getStyleClass().add("ranking-bar-fill");
      if (rank == 1) {
        tmpFill.getStyleClass().add("ranking-bar-fill-top1");
      } else if (rank == 2) {
        tmpFill.getStyleClass().add("ranking-bar-fill-top2");
      } else if (rank == 3) {
        tmpFill.getStyleClass().add("ranking-bar-fill-top3");
      }
      tmpFill.prefWidthProperty()
          .bind(tmpTrack.widthProperty().multiply(tmpRatio));

      tmpTrack.getChildren().add(tmpFill);
      StackPane.setAlignment(tmpFill, Pos.CENTER_LEFT);

      card.getChildren().addAll(tmpHeader, tmpTrack);

      List<String> titles =
          personMovies.getOrDefault(entry.getKey(), Collections.emptyList());
      Tooltip tmpTooltip = new Tooltip(buildMovieListText(titles));
      tmpTooltip.setShowDelay(Duration.millis(150));
      tmpTooltip.setShowDuration(Duration.seconds(15));
      tmpTooltip.getStyleClass().add("chart-popup-tooltip");
      Tooltip.install(card, tmpTooltip);

      container.getChildren().add(card);
      rank++;
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog
   * as a JSON file.
   */
  private void handleExportJson() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Export Catalog as JSON");
    tmpChooser.setInitialFileName("kinonode_catalog.json");
    tmpChooser.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
    File tmpFile =
        tmpChooser.showSaveDialog(lblTotalMovies.getScene().getWindow());
    if (tmpFile != null) {
      try {
        List<Movie> tmpMovies =
            AppContext.getInstance().getMovieRepository().findAll();
        AppContext.getInstance().getAnalyticsService()
            .exportToJson(tmpFile.toPath(), tmpMovies);
        showSuccessAlert("Export Complete",
            "Catalog exported successfully to JSON.");
      } catch (IOException exception) {
        LOGGER.log(Level.SEVERE, "Failed to export JSON", exception);
        showErrorAlert("Export Error",
            "Could not write JSON file: " + exception.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog
   * as a CSV file.
   */
  private void handleExportCsv() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Export Catalog as CSV");
    tmpChooser.setInitialFileName("kinonode_catalog.csv");
    tmpChooser.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
    File tmpFile =
        tmpChooser.showSaveDialog(lblTotalMovies.getScene().getWindow());
    if (tmpFile != null) {
      try {
        List<Movie> tmpMovies =
            AppContext.getInstance().getMovieRepository().findAll();
        AppContext.getInstance().getAnalyticsService()
            .exportToCsv(tmpFile.toPath(), tmpMovies);
        showSuccessAlert("Export Complete",
            "Catalog exported successfully to CSV.");
      } catch (IOException exception) {
        LOGGER.log(Level.SEVERE, "Failed to export CSV", exception);
        showErrorAlert("Export Error",
            "Could not write CSV file: " + exception.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog
   * as a styled HTML report.
   */
  private void handleExportHtml() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Export Visual Report (HTML)");
    tmpChooser.setInitialFileName("kinonode_report.html");
    tmpChooser.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("HTML Files (*.html)", "*.html"));
    File tmpFile =
        tmpChooser.showSaveDialog(lblTotalMovies.getScene().getWindow());
    if (tmpFile != null) {
      try {
        List<Movie> tmpMovies =
            AppContext.getInstance().getMovieRepository().findAll();
        AppContext.getInstance().getAnalyticsService()
            .exportToHtml(tmpFile.toPath(), tmpMovies);
        showSuccessAlert("Export Complete",
            "HTML Catalog report created successfully.");
      } catch (IOException exception) {
        LOGGER.log(Level.SEVERE, "Failed to export HTML", exception);
        showErrorAlert("Export Error",
            "Could not write HTML file: " + exception.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and creates a snapshot backup of the
   * underlying SQLite database.
   */
  private void handleBackupDatabase() {
    FileChooser tmpChooser = new FileChooser();
    tmpChooser.setTitle("Backup SQLite Database");
    String tmpTimestamp = java.time.LocalDateTime.now().format(
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    tmpChooser.setInitialFileName("kinonode_backup_" + tmpTimestamp + ".db");
    tmpChooser.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));
    File tmpFile =
        tmpChooser.showSaveDialog(lblTotalMovies.getScene().getWindow());
    if (tmpFile != null) {
      try {
        AppContext.getInstance().getAnalyticsService()
            .backupDatabase(tmpFile.toPath());
        showSuccessAlert("Backup Created",
            "Database snapshot saved successfully to:\n"
                + tmpFile.getAbsolutePath());
      } catch (IOException exception) {
        LOGGER.log(Level.SEVERE, "Failed to create backup", exception);
        showErrorAlert("Backup Error",
            "Could not backup database: " + exception.getMessage());
      }
    }
  }

  private void showSuccessAlert(String title, String message) {
    CustomAlert.showInformation(lblTotalMovies.getScene().getWindow(), title,
        title, message);
  }

  private void showErrorAlert(String title, String message) {
    CustomAlert.showError(lblTotalMovies.getScene().getWindow(), title, title,
        message);
  }
}
