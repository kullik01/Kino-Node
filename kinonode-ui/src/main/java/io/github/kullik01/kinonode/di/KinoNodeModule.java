package io.github.kullik01.kinonode.di;

import java.util.ResourceBundle;
import com.google.inject.Provides;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.github.kullik01.kinonode.service.DatabaseService;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.repository.WatchlistRepository;
import io.github.kullik01.kinonode.service.AnalyticsService;
import io.github.kullik01.kinonode.service.LibraryScanService;
import io.github.kullik01.kinonode.service.PlaybackService;

public class KinoNodeModule extends AbstractModule {
  @Override
  protected void configure() {
    // Services
    bind(DatabaseService.class).in(Singleton.class);
    bind(AnalyticsService.class).in(Singleton.class);
    bind(LibraryScanService.class).in(Singleton.class);
    bind(PlaybackService.class).in(Singleton.class);

    // Repositories
    bind(MovieRepository.class).in(Singleton.class);
    bind(SettingsRepository.class).in(Singleton.class);
    bind(WatchlistRepository.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  ResourceBundle provideResourceBundle() {
    return ResourceBundle.getBundle("io.github.kullik01.kinonode.i18n.messages");
  }
}
