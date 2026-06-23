package io.github.kullik01.kinonode.service;

import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;

/**
 * Registry/Context containing all singletons and repositories.
 * Simplifies dependency sharing between MVC controllers.
 */
public class AppContext {
    private static AppContext instance;

    private final DatabaseService databaseService;
    private final MovieRepository movieRepository;
    private final WatchlistRepository watchlistRepository;
    private final SettingsRepository settingsRepository;
    private final LibraryScanService libraryScanService;
    private final PlaybackService playbackService;

    private AppContext() {
        this.databaseService = new DatabaseService();
        this.movieRepository = new MovieRepository(databaseService);
        this.watchlistRepository = new WatchlistRepository(databaseService, movieRepository);
        this.settingsRepository = new SettingsRepository(databaseService);
        this.libraryScanService = new LibraryScanService(movieRepository, settingsRepository);
        this.playbackService = new PlaybackService(settingsRepository);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public MovieRepository getMovieRepository() {
        return movieRepository;
    }

    public WatchlistRepository getWatchlistRepository() {
        return watchlistRepository;
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public LibraryScanService getLibraryScanService() {
        return libraryScanService;
    }

    public PlaybackService getPlaybackService() {
        return playbackService;
    }

    public void close() {
        if (databaseService != null) {
            databaseService.close();
        }
    }
}
