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
