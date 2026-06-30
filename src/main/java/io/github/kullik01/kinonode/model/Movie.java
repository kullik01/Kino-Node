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
package io.github.kullik01.kinonode.model;

import java.time.Instant;

/**
 * Represents a Movie entity in the Kino-Node application. This class stores
 * metadata about a movie, including its file path, title, production details
 * (director, writer, producer, etc.), release year, genre, description, rating,
 * and resources like cover image path and trailer URL.
 */
public class Movie {
  private Integer id;
  private String filePath;
  private String title;
  private Integer year;
  private String genre;
  private String description;
  private Integer rating; // 0-10 or null
  private String coverImagePath;
  private String trailerUrl;
  private String director;
  private String writer;
  private String producer;
  private String cinematographer;
  private String composer;
  private String actors;
  private Instant dateAdded;

  /** Constructs a new Movie with the current timestamp as the date added. */
  public Movie() {
    // Initialize dateAdded to the current instant in time
    this.dateAdded = Instant.now();
  }

  /**
   * Constructs a new Movie with a specified file path and title, and sets the
   * date added to the current timestamp.
   *
   * @param filePath the file system path to the movie file
   * @param title the title of the movie
   */
  public Movie(String filePath, String title) {
    this.filePath = filePath;
    this.title = title;
    // Initialize dateAdded to the current instant in time
    this.dateAdded = Instant.now();
  }

  /**
   * Gets the unique identifier of the movie.
   *
   * @return the unique database ID, or null if the movie has not been persisted
   */
  public Integer getId() {
    return id;
  }

  /**
   * Sets the unique identifier of the movie.
   *
   * @param id the unique database ID to set
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Gets the file system path of the movie.
   *
   * @return the file path where the movie is stored
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Sets the file system path of the movie.
   *
   * @param filePath the file path to set
   */
  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  /**
   * Gets the title of the movie.
   *
   * @return the title of the movie
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the movie.
   *
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the release year of the movie.
   *
   * @return the year the movie was released, or null if unknown
   */
  public Integer getYear() {
    return year;
  }

  /**
   * Sets the release year of the movie.
   *
   * @param year the release year to set
   */
  public void setYear(Integer year) {
    this.year = year;
  }

  /**
   * Gets the genre(s) of the movie.
   *
   * @return the movie genre(s)
   */
  public String getGenre() {
    return genre;
  }

  /**
   * Sets the genre(s) of the movie.
   *
   * @param genre the movie genre(s) to set
   */
  public void setGenre(String genre) {
    this.genre = genre;
  }

  /**
   * Gets the description/synopsis of the movie.
   *
   * @return the movie description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description/synopsis of the movie.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the user rating of the movie (typically 0-10).
   *
   * @return the rating, or null if not rated
   */
  public Integer getRating() {
    return rating;
  }

  /**
   * Sets the user rating of the movie (typically 0-10).
   *
   * @param rating the rating to set
   */
  public void setRating(Integer rating) {
    this.rating = rating;
  }

  /**
   * Gets the file path to the cover image of the movie.
   *
   * @return the cover image path
   */
  public String getCoverImagePath() {
    return coverImagePath;
  }

  /**
   * Sets the file path to the cover image of the movie.
   *
   * @param coverImagePath the cover image path to set
   */
  public void setCoverImagePath(String coverImagePath) {
    this.coverImagePath = coverImagePath;
  }

  /**
   * Gets the trailer URL of the movie.
   *
   * @return the trailer URL
   */
  public String getTrailerUrl() {
    return trailerUrl;
  }

  /**
   * Sets the trailer URL of the movie.
   *
   * @param trailerUrl the trailer URL to set
   */
  public void setTrailerUrl(String trailerUrl) {
    this.trailerUrl = trailerUrl;
  }

  /**
   * Gets the date/time when this movie was added to the repository.
   *
   * @return the timestamp when added
   */
  public Instant getDateAdded() {
    return dateAdded;
  }

  /**
   * Sets the date/time when this movie was added to the repository.
   *
   * @param dateAdded the timestamp to set
   */
  public void setDateAdded(Instant dateAdded) {
    this.dateAdded = dateAdded;
  }

  /**
   * Gets the director of the movie.
   *
   * @return the director name
   */
  public String getDirector() {
    return director;
  }

  /**
   * Sets the director of the movie.
   *
   * @param director the director name to set
   */
  public void setDirector(String director) {
    this.director = director;
  }

  /**
   * Gets the writer of the movie.
   *
   * @return the writer name
   */
  public String getWriter() {
    return writer;
  }

  /**
   * Sets the writer of the movie.
   *
   * @param writer the writer name to set
   */
  public void setWriter(String writer) {
    this.writer = writer;
  }

  /**
   * Gets the producer of the movie.
   *
   * @return the producer name
   */
  public String getProducer() {
    return producer;
  }

  /**
   * Sets the producer of the movie.
   *
   * @param producer the producer name to set
   */
  public void setProducer(String producer) {
    this.producer = producer;
  }

  /**
   * Gets the cinematographer of the movie.
   *
   * @return the cinematographer name
   */
  public String getCinematographer() {
    return cinematographer;
  }

  /**
   * Sets the cinematographer of the movie.
   *
   * @param cinematographer the cinematographer name to set
   */
  public void setCinematographer(String cinematographer) {
    this.cinematographer = cinematographer;
  }

  /**
   * Gets the composer of the movie.
   *
   * @return the composer name
   */
  public String getComposer() {
    return composer;
  }

  /**
   * Sets the composer of the movie.
   *
   * @param composer the composer name to set
   */
  public void setComposer(String composer) {
    this.composer = composer;
  }

  /**
   * Gets the actors of the movie.
   *
   * @return the actors names, typically comma-separated
   */
  public String getActors() {
    if (actors != null && !actors.isBlank()) {
      String trimmed = actors.trim();
      if (!trimmed.endsWith(", ...")) {
        return trimmed + ", ...";
      }
    }
    return actors;
  }

  /**
   * Sets the actors of the movie.
   *
   * @param actors the actors names to set
   */
  public void setActors(String actors) {
    if (actors != null) {
      String trimmed = actors.trim();
      if (trimmed.endsWith(", ...")) {
        trimmed = trimmed.substring(0, trimmed.length() - 5).trim();
      }
      this.actors = trimmed.isEmpty() ? null : trimmed;
    } else {
      this.actors = null;
    }
  }

  /**
   * Returns a string representation of the Movie object, including its ID,
   * title, and file path.
   *
   * @return a string representing this Movie
   */
  @Override
  public String toString() {
    return "Movie{" + "id=" + id + ", title='" + title + '\'' + ", filePath='"
        + filePath + '\'' + '}';
  }
}
