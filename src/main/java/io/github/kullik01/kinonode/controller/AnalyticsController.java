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
import io.github.kullik01.kinonode.service.AnalyticsService;
import io.github.kullik01.kinonode.service.AppContext;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for the Analytics & Portability dashboard view. Computes live
 * statistics for the collection and offers catalog export and database backup
 * actions.
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
  private VBox topDirectorsBox;
  @FXML
  private VBox topActorsBox;

  @FXML
  public void initialize() {
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

    Map<String, Integer> tmpDecades = new TreeMap<>();
    Map<String, Integer> tmpRatings = new LinkedHashMap<>();
    for (int i = 0; i <= 10; i++) {
      tmpRatings.put(i + "/10", 0);
    }

    Map<String, Integer> tmpDirectors = new HashMap<>();
    Map<String, Integer> tmpActors = new HashMap<>();
    Map<String, Integer> tmpGenres = new HashMap<>();

    for (Movie tmpMovie : tmpMovies) {
      if (tmpMovie.getRating() != null) {
        tmpTotalRating += tmpMovie.getRating();
        tmpRatedCount++;
        String tmpRatingKey = tmpMovie.getRating() + "/10";
        tmpRatings.put(tmpRatingKey,
            tmpRatings.getOrDefault(tmpRatingKey, 0) + 1);
      }

      if (tmpMovie.getYear() != null) {
        tmpMinYear = Math.min(tmpMinYear, tmpMovie.getYear());
        tmpMaxYear = Math.max(tmpMaxYear, tmpMovie.getYear());
        int tmpDecadeStart = (tmpMovie.getYear() / 10) * 10;
        String tmpDecadeKey = tmpDecadeStart + "s";
        tmpDecades.put(tmpDecadeKey,
            tmpDecades.getOrDefault(tmpDecadeKey, 0) + 1);
      }

      if (tmpMovie.getGenre() != null && !tmpMovie.getGenre().isBlank()) {
        for (String tmpGenre : tmpMovie.getGenre().split(",")) {
          String tmpClean = tmpGenre.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpGenres.put(tmpClean, tmpGenres.getOrDefault(tmpClean, 0) + 1);
          }
        }
      }

      if (tmpMovie.getDirector() != null && !tmpMovie.getDirector().isBlank()) {
        for (String tmpDir : tmpMovie.getDirector().split(",")) {
          String tmpClean = tmpDir.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpDirectors.put(tmpClean,
                tmpDirectors.getOrDefault(tmpClean, 0) + 1);
          }
        }
      }

      if (tmpMovie.getActors() != null && !tmpMovie.getActors().isBlank()) {
        for (String tmpAct : tmpMovie.getActors().split(",")) {
          String tmpClean = tmpAct.trim();
          if (!tmpClean.isEmpty() && !tmpClean.equalsIgnoreCase("Unknown")) {
            tmpActors.put(tmpClean, tmpActors.getOrDefault(tmpClean, 0) + 1);
          }
        }
      }
    }

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

    // Populate Top Lists
    populateRankingList(topDirectorsBox, tmpDirectors, 5);
    populateRankingList(topActorsBox, tmpActors, 5);
  }

  private void populateDecadeChart(Map<String, Integer> decades) {
    if (decadeChart == null) {
      return;
    }
    decadeChart.getData().clear();
    XYChart.Series<String, Number> tmpSeries = new XYChart.Series<>();
    tmpSeries.setName("Movies per Decade");
    for (Map.Entry<String, Integer> tmpEntry : decades.entrySet()) {
      tmpSeries.getData()
          .add(new XYChart.Data<>(tmpEntry.getKey(), tmpEntry.getValue()));
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
      tmpSeries.getData()
          .add(new XYChart.Data<>(tmpEntry.getKey(), tmpEntry.getValue()));
    }
    ratingChart.getData().add(tmpSeries);
  }

  private void populateRankingList(VBox container,
      Map<String, Integer> frequencies, int limit) {
    if (container == null) {
      return;
    }
    container.getChildren().clear();
    frequencies.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(limit).forEach(entry -> {
          HBox tmpRow = new HBox(10);
          tmpRow.setStyle(
              "-fx-padding: 8 12; -fx-background-color: -color-bg-default; -fx-background-radius: 6; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-border-width: 1;");
          Label tmpName = new Label(entry.getKey());
          tmpName.setStyle(
              "-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
          Label tmpCount = new Label(entry.getValue() + " title(s)");
          tmpCount.setStyle("-fx-text-fill: -color-fg-muted;");
          javafx.scene.layout.Region tmpSpacer =
              new javafx.scene.layout.Region();
          HBox.setHgrow(tmpSpacer, javafx.scene.layout.Priority.ALWAYS);
          tmpRow.getChildren().addAll(tmpName, tmpSpacer, tmpCount);
          container.getChildren().add(tmpRow);
        });
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
    String tmpTimestamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
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
    tmpAlert.showAndWait();
  }

  private void showErrorAlert(String title, String message) {
    Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
    tmpAlert.setTitle(title);
    tmpAlert.setHeaderText(null);
    tmpAlert.setContentText(message);
    tmpAlert.showAndWait();
  }
}
