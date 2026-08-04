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
package io.github.kullik01.kinonode.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.kullik01.kinonode.controller.MainController;
import io.github.kullik01.kinonode.di.KinoNodeModule;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.DatabaseService;
import io.github.kullik01.kinonode.service.LibraryScanService;
import io.github.kullik01.kinonode.service.PlaybackService;
import io.github.kullik01.kinonode.service.AnalyticsService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.application.Platform;

public class AppContext {
  private static AppContext instance;
  private final Injector injector;

  private final List<Runnable> libraryChangeListeners =
      new CopyOnWriteArrayList<>();

  public void addLibraryChangeListener(Runnable listener) {
    libraryChangeListeners.add(listener);
  }

  public void notifyLibraryChanged() {
    for (Runnable listener : libraryChangeListeners) {
      Platform.runLater(listener);
    }
  }

  private AppContext() {
    this.injector = Guice.createInjector(new KinoNodeModule());
  }

  public static synchronized AppContext getInstance() {
    if (instance == null) {
      instance = new AppContext();
    }
    return instance;
  }

  public Injector getInjector() {
    return injector;
  }

  public DatabaseService getDatabaseService() {
    return injector.getInstance(DatabaseService.class);
  }

  public MovieRepository getMovieRepository() {
    return injector.getInstance(MovieRepository.class);
  }

  public WatchlistRepository getWatchlistRepository() {
    return injector.getInstance(WatchlistRepository.class);
  }

  public SettingsRepository getSettingsRepository() {
    return injector.getInstance(SettingsRepository.class);
  }

  public LibraryScanService getLibraryScanService() {
    return injector.getInstance(LibraryScanService.class);
  }

  public PlaybackService getPlaybackService() {
    return injector.getInstance(PlaybackService.class);
  }

  public AnalyticsService getAnalyticsService() {
    return injector.getInstance(AnalyticsService.class);
  }

  private MainController mainController;

  public MainController getMainController() {
    return mainController;
  }

  public void setMainController(MainController mainController) {
    this.mainController = mainController;
  }

  public void close() {
    DatabaseService ds = getDatabaseService();
    if (ds != null) {
      ds.close();
    }
  }
}
