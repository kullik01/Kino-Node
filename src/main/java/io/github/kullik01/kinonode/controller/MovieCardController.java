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
