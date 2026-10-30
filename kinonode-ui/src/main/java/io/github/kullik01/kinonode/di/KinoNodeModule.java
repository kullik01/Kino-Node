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
import io.github.kullik01.kinonode.service.MovieFileCopyService;

public class KinoNodeModule extends AbstractModule {
  @Override
  protected void configure() {
    // Services
    bind(DatabaseService.class).in(Singleton.class);
    bind(AnalyticsService.class).in(Singleton.class);
    bind(LibraryScanService.class).in(Singleton.class);
    bind(PlaybackService.class).in(Singleton.class);
    bind(MovieFileCopyService.class).in(Singleton.class);

    // Repositories
    bind(MovieRepository.class).in(Singleton.class);
    bind(SettingsRepository.class).in(Singleton.class);
    bind(WatchlistRepository.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  AppContext provideAppContext() {
    return AppContext.getInstance();
  }

  @Provides
  @Singleton
  ResourceBundle provideResourceBundle() {
    return ResourceBundle
        .getBundle("io.github.kullik01.kinonode.i18n.messages");
  }
}
