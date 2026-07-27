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
import javafx.scene.layout.StackPane;
import javafx.util.Duration;


import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.AppContext;
import io.github.kullik01.kinonode.util.CustomAlert;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
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

  @FXML
  private Label lblTotalMovies;
  @FXML
  private Label lblAvgRating;
  @FXML
  private Label lblYearRange;
  @FXML
  private Label lblTotalWatchlists;
  @FXML
  private Label lblTopGenre;
  @FXML
  private Label lblTotalDirectors;

  @FXML
  private BarChart<String, Number> decadeChart;
  @FXML
  private BarChart<String, Number> ratingChart;
  @FXML
  private BarChart<String, Number> genreChart;

  @FXML
  private ComboBox<String> comboRoleFilter;
  @FXML
  private VBox topPeopleBox;

  private final Map<String, Map<String, Integer>> roleFrequencies =
      new HashMap<>();
  private final Map<String, List<String>> decadeMovies = new TreeMap<>();
  private final Map<String, List<String>> ratingMovies = new LinkedHashMap<>();
  private final Map<String, List<String>> genreMovies = new HashMap<>();
  private final Map<String, Map<String, List<String>>> rolePersonMovies =
      new HashMap<>();

  /**
   * Initializes the analytics view by disabling unwanted chart animations
   * and populating the role filter dropdown with default values.
   */
  @FXML
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

    for (Movie tmpMovie : tmpMovies) {
      String title =
          (tmpMovie.getTitle() != null && !tmpMovie.getTitle().isBlank())
              ? tmpMovie.getTitle()
              : "Untitled";

      if (tmpMovie.getRawRating() != null) {
        tmpTotalRating += tmpMovie.getRawRating();
        tmpRatedCount++;
      } else if (tmpMovie.getRating() != null) {
        tmpTotalRating += tmpMovie.getRating();
        tmpRatedCount++;
      }

      if (tmpMovie.getRating() != null) {
        String tmpRatingKey = tmpMovie.getRating() + "/10";
        tmpRatings.put(tmpRatingKey,
            tmpRatings.getOrDefault(tmpRatingKey, 0) + 1);
        ratingMovies.computeIfAbsent(tmpRatingKey, k -> new ArrayList<>())
            .add(title);
      }

      if (tmpMovie.getYear() != null) {
        tmpMinYear = Math.min(tmpMinYear, tmpMovie.getYear());
        tmpMaxYear = Math.max(tmpMaxYear, tmpMovie.getYear());
        int tmpDecadeStart = (tmpMovie.getYear() / 10) * 10;
        String tmpDecadeKey = tmpDecadeStart + "s";
        tmpDecades.put(tmpDecadeKey,
            tmpDecades.getOrDefault(tmpDecadeKey, 0) + 1);
        decadeMovies.computeIfAbsent(tmpDecadeKey, k -> new ArrayList<>())
            .add(title);
      }

      if (tmpMovie.getGenre() != null && !tmpMovie.getGenre().isBlank()) {
        for (String tmpGenre : tmpMovie.getGenre().split(",")) {
          String tmpClean = tmpGenre.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpGenres.put(tmpClean, tmpGenres.getOrDefault(tmpClean, 0) + 1);
            genreMovies.computeIfAbsent(tmpClean, k -> new ArrayList<>())
                .add(title);
          }
        }
      }

      parseAndCountWithTitle(tmpMovie.getDirector(), tmpDirectors,
          tmpDirectorMovies, title);
      parseAndCountWithTitle(tmpMovie.getWriter(), tmpWriters, tmpWriterMovies,
          title);
      parseAndCountWithTitle(tmpMovie.getProducer(), tmpProducers,
          tmpProducerMovies, title);
      parseAndCountWithTitle(tmpMovie.getCinematographer(), tmpCinematographers,
          tmpCinematographerMovies, title);
      parseAndCountWithTitle(tmpMovie.getComposer(), tmpComposers,
          tmpComposerMovies, title);
      parseAndCountWithTitle(tmpMovie.getActors(), tmpActors, tmpActorMovies,
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
   * Parses comma-separated names from a movie field and updates frequency/movie tracking maps.
   *
   * @param rawField       The raw string from the database (e.g. "Director1, Director2").
   * @param targetMap      The map to increment frequencies in.
   * @param targetMovieMap The map tracking which movies a person is associated with.
   * @param movieTitle     The title of the movie being processed.
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
   * Constructs a formatted, newline-separated bullet list of movie titles
   * to be displayed inside tooltips.
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
  @FXML
  private void handleRoleFilterChange() {
    updateTopPeople();
  }

  /**
   * Updates the Top People ranking list based on the currently selected role filter.
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
   * Attaches an interactive tooltip and hover styling to a specific chart data node.
   *
   * @param data        The XYChart data point.
   * @param tooltipText The text to display in the tooltip.
   */
  private void installChartTooltip(XYChart.Data<String, Number> data,
      String tooltipText) {
    Runnable attacher = () -> {
      Node node = data.getNode();
      if (node != null) {
        javafx.scene.control.Tooltip tmpTooltip =
            new javafx.scene.control.Tooltip(tooltipText);
        tmpTooltip.setShowDelay(Duration.millis(150));
        tmpTooltip.setShowDuration(Duration.seconds(15));
        tmpTooltip.getStyleClass().add("chart-popup-tooltip");
        javafx.scene.control.Tooltip.install(node, tmpTooltip);

        node.setCursor(
            io.github.kullik01.kinonode.util.CursorUtils.getCameraCursor());
        // Use a style class toggle instead of setOpacity() to avoid
        // conflicting with CSS :hover rules which causes flickering.
        // hoverProperty() fires even when the mouse is stationary.
        node.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
          if (isHovered) {
            if (!node.getStyleClass().contains("chart-bar-hovered")) {
              node.getStyleClass().add("chart-bar-hovered");
            }
          } else {
            node.getStyleClass().remove("chart-bar-hovered");
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
   * Populates a VBox container with visually rich ranking rows showing
   * the top individuals for a selected role.
   *
   * @param container   The VBox layout to populate.
   * @param frequencies A map of person names to their occurrence counts.
   * @param limit       The maximum number of top results to display.
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
      VBox tmpCard = new VBox(6);
      tmpCard.getStyleClass().add("ranking-row");

      HBox tmpHeader = new HBox(10);
      tmpHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

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
      HBox.setHgrow(tmpSpacer, javafx.scene.layout.Priority.ALWAYS);

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
      StackPane.setAlignment(tmpFill, javafx.geometry.Pos.CENTER_LEFT);

      tmpCard.getChildren().addAll(tmpHeader, tmpTrack);

      List<String> titles =
          personMovies.getOrDefault(entry.getKey(), Collections.emptyList());
      javafx.scene.control.Tooltip tmpTooltip =
          new javafx.scene.control.Tooltip(buildMovieListText(titles));
      tmpTooltip.setShowDelay(Duration.millis(150));
      tmpTooltip.setShowDuration(Duration.seconds(15));
      tmpTooltip.getStyleClass().add("chart-popup-tooltip");
      javafx.scene.control.Tooltip.install(tmpCard, tmpTooltip);

      container.getChildren().add(tmpCard);
      rank++;
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog as a JSON file.
   */
  @FXML
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
      } catch (IOException tmpException) {
        LOGGER.log(Level.SEVERE, "Failed to export JSON", tmpException);
        showErrorAlert("Export Error",
            "Could not write JSON file: " + tmpException.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog as a CSV file.
   */
  @FXML
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
      } catch (IOException tmpException) {
        LOGGER.log(Level.SEVERE, "Failed to export CSV", tmpException);
        showErrorAlert("Export Error",
            "Could not write CSV file: " + tmpException.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and exports the entire movie catalog as a styled HTML report.
   */
  @FXML
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
      } catch (IOException tmpException) {
        LOGGER.log(Level.SEVERE, "Failed to export HTML", tmpException);
        showErrorAlert("Export Error",
            "Could not write HTML file: " + tmpException.getMessage());
      }
    }
  }

  /**
   * Prompts the user for a save location and creates a snapshot backup of the underlying SQLite database.
   */
  @FXML
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
      } catch (IOException tmpException) {
        LOGGER.log(Level.SEVERE, "Failed to create backup", tmpException);
        showErrorAlert("Backup Error",
            "Could not backup database: " + tmpException.getMessage());
      }
    }
  }

  private void showSuccessAlert(String title, String message) {
    io.github.kullik01.kinonode.util.CustomAlert.showInformation(
        lblTotalMovies.getScene().getWindow(), title, title, message);
  }

  private void showErrorAlert(String title, String message) {
    io.github.kullik01.kinonode.util.CustomAlert.showError(
        lblTotalMovies.getScene().getWindow(), title, title, message);
  }
}
