/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2026, Hannah Kullik
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.controller;

import io.github.kullik01.kinonode.model.Movie;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieCardController {
    private static final Logger LOGGER = Logger.getLogger(MovieCardController.class.getName());
    private static Image PLACEHOLDER_IMAGE;

    @FXML private ImageView imgPoster;
    @FXML private Label lblTitle;
    @FXML private Label lblYear;
    @FXML private Label lblCinematographer;

    private Movie movie;
    private Consumer<Movie> clickHandler;

    public void setMovie(Movie movie, Consumer<Movie> clickHandler) {
        this.movie = movie;
        this.clickHandler = clickHandler;

        lblTitle.setText(movie.getTitle());
        lblYear.setText(movie.getYear() != null ? String.valueOf(movie.getYear()) : "—");
        
        String cinematographer = movie.getCinematographer();
        if (cinematographer != null && !cinematographer.isBlank()) {
            lblCinematographer.setText(cinematographer);
            lblCinematographer.setManaged(true);
            lblCinematographer.setVisible(true);
        } else {
            lblCinematographer.setText("");
            lblCinematographer.setManaged(false);
            lblCinematographer.setVisible(false);
        }

        loadPoster();
    }

    private void loadPoster() {
        String path = movie.getCoverImagePath();
        Image image = null;
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    // 6th argument is backgroundLoading=true
                    image = new Image(f.toURI().toURL().toExternalForm(), 130, 190, false, true, true);
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "Failed to load poster image: " + path, e);
                }
            }
        }

        if (image == null) {
            if (PLACEHOLDER_IMAGE == null) {
                try {
                    var resource = getClass().getResource("/io/github/kullik01/kinonode/images/placeholder_cover.jpg");
                    if (resource != null) {
                        PLACEHOLDER_IMAGE = new Image(resource.toExternalForm(), 130, 190, false, true, true);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load placeholder cover resource", e);
                }
            }
            image = PLACEHOLDER_IMAGE;
        }
        
        if (image != null) {
            imgPoster.setImage(image);
        }
    }

    @FXML
    private void onCardClicked(MouseEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(movie);
        }
    }
}
