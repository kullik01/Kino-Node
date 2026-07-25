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
import io.github.kullik01.kinonode.service.AppContext;
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
      String title = (tmpMovie.getTitle() != null && !tmpMovie.getTitle().isBlank())
          ? tmpMovie.getTitle() : "Untitled";

      if (tmpMovie.getRating() != null) {
        tmpTotalRating += tmpMovie.getRating();
        tmpRatedCount++;
        String tmpRatingKey = tmpMovie.getRating() + "/10";
        tmpRatings.put(tmpRatingKey,
            tmpRatings.getOrDefault(tmpRatingKey, 0) + 1);
        ratingMovies.computeIfAbsent(tmpRatingKey, k -> new ArrayList<>()).add(title);
      }

      if (tmpMovie.getYear() != null) {
        tmpMinYear = Math.min(tmpMinYear, tmpMovie.getYear());
        tmpMaxYear = Math.max(tmpMaxYear, tmpMovie.getYear());
        int tmpDecadeStart = (tmpMovie.getYear() / 10) * 10;
        String tmpDecadeKey = tmpDecadeStart + "s";
        tmpDecades.put(tmpDecadeKey,
            tmpDecades.getOrDefault(tmpDecadeKey, 0) + 1);
        decadeMovies.computeIfAbsent(tmpDecadeKey, k -> new ArrayList<>()).add(title);
      }

      if (tmpMovie.getGenre() != null && !tmpMovie.getGenre().isBlank()) {
        for (String tmpGenre : tmpMovie.getGenre().split(",")) {
          String tmpClean = tmpGenre.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpGenres.put(tmpClean, tmpGenres.getOrDefault(tmpClean, 0) + 1);
            genreMovies.computeIfAbsent(tmpClean, k -> new ArrayList<>()).add(title);
          }
        }
      }

      parseAndCountWithTitle(tmpMovie.getDirector(), tmpDirectors, tmpDirectorMovies, title);
      parseAndCountWithTitle(tmpMovie.getWriter(), tmpWriters, tmpWriterMovies, title);
      parseAndCountWithTitle(tmpMovie.getProducer(), tmpProducers, tmpProducerMovies, title);
      parseAndCountWithTitle(tmpMovie.getCinematographer(), tmpCinematographers, tmpCinematographerMovies, title);
      parseAndCountWithTitle(tmpMovie.getComposer(), tmpComposers, tmpComposerMovies, title);
      parseAndCountWithTitle(tmpMovie.getActors(), tmpActors, tmpActorMovies, title);
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

  private void parseAndCountWithTitle(String rawField, Map<String, Integer> targetMap,
      Map<String, List<String>> targetMovieMap, String movieTitle) {
    if (rawField != null && !rawField.isBlank()) {
      for (String token : rawField.split(",")) {
        String tmpClean = token.trim();
        if (!tmpClean.isEmpty() && !tmpClean.equals("...")
            && !tmpClean.equalsIgnoreCase("Unknown")) {
          targetMap.put(tmpClean, targetMap.getOrDefault(tmpClean, 0) + 1);
          targetMovieMap.computeIfAbsent(tmpClean, k -> new ArrayList<>()).add(movieTitle);
        }
      }
    }
  }

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

  @FXML
  private void handleRoleFilterChange() {
    updateTopPeople();
  }

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

  private void installChartTooltip(
      XYChart.Data<String, Number> data, String tooltipText) {
    Runnable attacher = () -> {
      javafx.scene.Node node = data.getNode();
      if (node != null) {
        javafx.scene.control.Tooltip tmpTooltip =
            new javafx.scene.control.Tooltip(tooltipText);
        tmpTooltip.setShowDelay(javafx.util.Duration.millis(150));
        tmpTooltip.setShowDuration(javafx.util.Duration.seconds(15));
        tmpTooltip.getStyleClass().add("chart-popup-tooltip");
        javafx.scene.control.Tooltip.install(node, tmpTooltip);

        node.setCursor(
            io.github.kullik01.kinonode.util.CursorUtils.getCameraCursor());
        node.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
          if (isHovered) {
            node.setOpacity(0.80);
          } else {
            node.setOpacity(1.0);
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

      List<String> titles = decadeMovies.getOrDefault(tmpEntry.getKey(), Collections.emptyList());
      installChartTooltip(tmpData, buildMovieListText(titles));
    }
    decadeChart.getData().add(tmpSeries);
  }

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

      List<String> titles = ratingMovies.getOrDefault(tmpEntry.getKey(), Collections.emptyList());
      installChartTooltip(tmpData, buildMovieListText(titles));
    }
    ratingChart.getData().add(tmpSeries);
  }

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

          List<String> titles = genreMovies.getOrDefault(entry.getKey(), Collections.emptyList());
          installChartTooltip(tmpData, buildMovieListText(titles));
        });
    genreChart.getData().add(tmpSeries);
  }

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

    String selectedRole = comboRoleFilter != null ? comboRoleFilter.getValue() : "Director(s)";
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

      javafx.scene.layout.Region tmpSpacer = new javafx.scene.layout.Region();
      HBox.setHgrow(tmpSpacer, javafx.scene.layout.Priority.ALWAYS);

      int count = entry.getValue();
      Label tmpCount = new Label(count + (count == 1 ? " title" : " titles"));
      tmpCount.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");

      tmpHeader.getChildren().addAll(tmpBadge, tmpName, tmpSpacer, tmpCount);

      // Relative visual bar track & fill
      javafx.scene.layout.StackPane tmpTrack =
          new javafx.scene.layout.StackPane();
      tmpTrack.getStyleClass().add("ranking-bar-track");
      tmpTrack.setMaxWidth(Double.MAX_VALUE);

      double tmpRatio = (double) count / tmpMaxCount;
      javafx.scene.layout.Region tmpFill = new javafx.scene.layout.Region();
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
      javafx.scene.layout.StackPane.setAlignment(tmpFill,
          javafx.geometry.Pos.CENTER_LEFT);

      tmpCard.getChildren().addAll(tmpHeader, tmpTrack);

      List<String> titles = personMovies.getOrDefault(entry.getKey(), Collections.emptyList());
      javafx.scene.control.Tooltip tmpTooltip =
          new javafx.scene.control.Tooltip(buildMovieListText(titles));
      tmpTooltip.setShowDelay(javafx.util.Duration.millis(150));
      tmpTooltip.setShowDuration(javafx.util.Duration.seconds(15));
      tmpTooltip.getStyleClass().add("chart-popup-tooltip");
      javafx.scene.control.Tooltip.install(tmpCard, tmpTooltip);

      container.getChildren().add(tmpCard);
      rank++;
    }
  }

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
    Alert tmpAlert = new Alert(Alert.AlertType.INFORMATION);
    tmpAlert.setTitle(title);
    tmpAlert.setHeaderText(null);
    tmpAlert.setContentText(message);
    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpAlert);
    tmpAlert.showAndWait();
  }

  private void showErrorAlert(String title, String message) {
    Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
    tmpAlert.setTitle(title);
    tmpAlert.setHeaderText(null);
    tmpAlert.setContentText(message);
    io.github.kullik01.kinonode.util.CursorUtils.applyCursor(tmpAlert);
    tmpAlert.showAndWait();
  }
}
