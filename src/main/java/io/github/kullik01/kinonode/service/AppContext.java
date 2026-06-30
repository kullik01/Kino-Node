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

import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;

/**
 * Registry/Context containing all singletons and repositories. Simplifies
 * dependency sharing between MVC controllers. This class provides a centralized
 * point of access to various services and repositories used in the application.
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
    this.watchlistRepository =
        new WatchlistRepository(databaseService, movieRepository);
    this.settingsRepository = new SettingsRepository(databaseService);
    this.libraryScanService =
        new LibraryScanService(movieRepository, settingsRepository);
    this.playbackService = new PlaybackService(settingsRepository);
  }

  /**
   * Retrieves the single instance of {@link AppContext}. If the instance does
   * not exist yet, it is created in a thread-safe manner.
   *
   * @return the singleton instance of {@code AppContext}
   */
  public static synchronized AppContext getInstance() {
    // Double check/lazy initialization of the singleton instance
    if (instance == null) {
      instance = new AppContext();
    }
    return instance;
  }

  /**
   * Gets the database service instance.
   *
   * @return the {@link DatabaseService} instance
   */
  public DatabaseService getDatabaseService() {
    return databaseService;
  }

  /**
   * Gets the movie repository instance.
   *
   * @return the {@link MovieRepository} instance
   */
  public MovieRepository getMovieRepository() {
    return movieRepository;
  }

  /**
   * Gets the watchlist repository instance.
   *
   * @return the {@link WatchlistRepository} instance
   */
  public WatchlistRepository getWatchlistRepository() {
    return watchlistRepository;
  }

  /**
   * Gets the settings repository instance.
   *
   * @return the {@link SettingsRepository} instance
   */
  public SettingsRepository getSettingsRepository() {
    return settingsRepository;
  }

  /**
   * Gets the library scan service instance.
   *
   * @return the {@link LibraryScanService} instance
   */
  public LibraryScanService getLibraryScanService() {
    return libraryScanService;
  }

  /**
   * Gets the playback service instance.
   *
   * @return the {@link PlaybackService} instance
   */
  public PlaybackService getPlaybackService() {
    return playbackService;
  }

  private io.github.kullik01.kinonode.controller.MainController mainController;

  /**
   * Gets the main controller instance.
   *
   * @return the {@link io.github.kullik01.kinonode.controller.MainController} instance
   */
  public io.github.kullik01.kinonode.controller.MainController getMainController() {
    return mainController;
  }

  /**
   * Sets the main controller instance.
   *
   * @param mainController the {@link io.github.kullik01.kinonode.controller.MainController} to set
   */
  public void setMainController(io.github.kullik01.kinonode.controller.MainController mainController) {
    this.mainController = mainController;
  }

  /**
   * Closes the context and cleans up resources, specifically closing the
   * underlying database service.
   */
  public void close() {
    // Safely close the database connection if the service has been initialized
    if (databaseService != null) {
      databaseService.close();
    }
  }
}
