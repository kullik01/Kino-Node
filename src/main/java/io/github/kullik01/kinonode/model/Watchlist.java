package io.github.kullik01.kinonode.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Watchlist {
    private Integer id;
    private String name;
    private Instant createdAt;
    private List<Movie> movies = new ArrayList<>();

    public Watchlist() {
        this.createdAt = Instant.now();
    }

    public Watchlist(String name) {
        this.name = name;
        this.createdAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    @Override
    public String toString() {
        return "Watchlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
