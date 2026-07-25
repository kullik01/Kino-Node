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
import java.io.File;
import java.net.MalformedURLException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

/**
 * Controller representing an individual movie card in the library grid view.
 * Handles binding a single {@link Movie} model's details to the card layout and
 * lazy-loading its poster art from file system/classpath resources.
 */
public class MovieCardController {
  private static final Logger LOGGER =
      Logger.getLogger(MovieCardController.class.getName());
  private static Image PLACEHOLDER_IMAGE;

  @FXML
  private ImageView imgPoster;
  @FXML
  private Label lblTitle;
  @FXML
  private Label lblYear;
  @FXML
  private Label lblDirector;

  private Movie movie;
  private Consumer<Movie> clickHandler;

  /**
   * Configures the card's data and click action. Updates text labels and
   * triggers the background poster loading logic.
   *
   * @param movie the {@link Movie} representation to bind to this card
   * @param clickHandler the callback action triggered when the card is selected
   *        by user
   */
  public void setMovie(Movie movie, Consumer<Movie> clickHandler) {
    this.movie = movie;
    this.clickHandler = clickHandler;

    lblTitle.setText(movie.getTitle());
    lblYear.setText(
        movie.getYear() != null ? String.valueOf(movie.getYear()) : "—");

    String tmpDirector = movie.getDirector();
    // Expose or collapse the director label depending on whether info exists
    if (tmpDirector != null && !tmpDirector.isBlank()) {
      lblDirector.setText(tmpDirector);
      lblDirector.setManaged(true);
      lblDirector.setVisible(true);
    } else {
      lblDirector.setText("");
      lblDirector.setManaged(false);
      lblDirector.setVisible(false);
    }

    loadPoster();
  }

  private void loadPoster() {
    String tmpPath = movie.getCoverImagePath();
    Image tmpImage = null;
    if (tmpPath != null && !tmpPath.isBlank()) {
      File tmpFile = new File(tmpPath);
      if (tmpFile.exists()) {
        try {
          // Construct URL and load image with backgroundLoading set to true
          // (6th argument)
          // to avoid freezing the JavaFX application UI thread.
          tmpImage = new Image(tmpFile.toURI().toURL().toExternalForm(), 220,
              320, false, true, true);
        } catch (MalformedURLException tmpException) {
          LOGGER.log(Level.WARNING, "Failed to load poster image: " + tmpPath,
              tmpException);
        }
      }
    }

    // Lazily load and cache the default placeholder poster image if no valid
    // cover is present
    if (tmpImage == null) {
      if (PLACEHOLDER_IMAGE == null) {
        try {
          var tmpResource = getClass().getResource(
              "/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
          if (tmpResource != null) {
            PLACEHOLDER_IMAGE = new Image(tmpResource.toExternalForm(), 220,
                320, false, true, true);
          }
        } catch (Exception tmpException) {
          LOGGER.log(Level.WARNING, "Failed to load placeholder cover resource",
              tmpException);
        }
      }
      tmpImage = PLACEHOLDER_IMAGE;
    }

    if (tmpImage != null) {
      imgPoster.setImage(tmpImage);
    }
  }

  @FXML
  private void onCardClicked(MouseEvent event) {
    if (clickHandler != null) {
      clickHandler.accept(movie);
    }
  }
}
