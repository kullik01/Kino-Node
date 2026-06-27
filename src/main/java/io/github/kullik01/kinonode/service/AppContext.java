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
