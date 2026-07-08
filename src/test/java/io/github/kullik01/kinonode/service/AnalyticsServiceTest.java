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
package io.github.kullik01.kinonode.service;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kullik01.kinonode.model.Movie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyticsServiceTest {
  @TempDir
  Path tempDir;

  private AnalyticsService analyticsService;
  private List<Movie> sampleMovies;

  @BeforeEach
  void setUp() {
    analyticsService = new AnalyticsService();
    Movie tmpMovie1 = new Movie("/movies/inception.mp4", "Inception");
    tmpMovie1.setYear(2010);
    tmpMovie1.setRating(9);
    tmpMovie1.setGenre("Sci-Fi");
    tmpMovie1.setDirector("Christopher Nolan");

    Movie tmpMovie2 = new Movie("/movies/matrix.mp4", "The Matrix");
    tmpMovie2.setYear(1999);
    tmpMovie2.setRating(10);
    tmpMovie2.setGenre("Action, Sci-Fi");
    tmpMovie2.setDirector("The Wachowskis");

    sampleMovies = List.of(tmpMovie1, tmpMovie2);
  }

  @Test
  void testExportToJson() throws IOException {
    Path tmpTargetFile = tempDir.resolve("catalog.json");
    analyticsService.exportToJson(tmpTargetFile, sampleMovies);

    assertTrue(Files.exists(tmpTargetFile));
    String tmpContent = Files.readString(tmpTargetFile);
    assertTrue(tmpContent.contains("\"title\": \"Inception\""));
    assertTrue(tmpContent.contains("\"year\": 1999"));
  }

  @Test
  void testExportToCsv() throws IOException {
    Path tmpTargetFile = tempDir.resolve("catalog.csv");
    analyticsService.exportToCsv(tmpTargetFile, sampleMovies);

    assertTrue(Files.exists(tmpTargetFile));
    String tmpContent = Files.readString(tmpTargetFile);
    assertTrue(tmpContent
        .contains("ID,Title,Year,Rating,Genre,Director,Actors,FilePath"));
    assertTrue(tmpContent.contains("Inception"));
    assertTrue(tmpContent.contains("The Matrix"));
  }

  @Test
  void testExportToHtml() throws IOException {
    Path tmpTargetFile = tempDir.resolve("catalog.html");
    analyticsService.exportToHtml(tmpTargetFile, sampleMovies);

    assertTrue(Files.exists(tmpTargetFile));
    String tmpContent = Files.readString(tmpTargetFile);
    assertTrue(tmpContent.contains("<!DOCTYPE html>"));
    assertTrue(tmpContent.contains("KinoNode Library Report"));
    assertTrue(tmpContent.contains("Inception"));
    assertTrue(tmpContent.contains("Christopher Nolan"));
  }
}
