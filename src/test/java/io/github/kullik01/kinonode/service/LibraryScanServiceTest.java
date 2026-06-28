/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2026, Hannah Kullik
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
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

import static org.junit.jupiter.api.Assertions.*;

import io.github.kullik01.kinonode.model.Movie;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LibraryScanServiceTest {

  private LibraryScanService libraryScanService;

  @BeforeEach
  public void setUp() {
    libraryScanService = new LibraryScanService(null, null);
  }

  @Test
  public void testParseMovieWithStandardYear() {
    File tmpFile = new File("C:/movies/Inception (2010).mp4");
    Movie tmpMovie = libraryScanService.parseMovieFromFile(tmpFile);
    assertEquals("Inception", tmpMovie.getTitle());
    assertEquals(2010, tmpMovie.getYear());
  }

  @Test
  public void testParseMovieWithBrackets() {
    File tmpFile = new File("C:/movies/The Matrix [1999].mkv");
    Movie tmpMovie = libraryScanService.parseMovieFromFile(tmpFile);
    assertEquals("The Matrix", tmpMovie.getTitle());
    assertEquals(1999, tmpMovie.getYear());
  }

  @Test
  public void testParseMovieWithGarbageTags() {
    File tmpFile = new File("C:/movies/Gladiator.2000.1080p.BluRay.x264.mkv");
    Movie tmpMovie = libraryScanService.parseMovieFromFile(tmpFile);
    assertEquals("Gladiator", tmpMovie.getTitle());
    assertEquals(2000, tmpMovie.getYear());
  }

  @Test
  public void testParseMovieWithNoYear() {
    File tmpFile = new File("C:/movies/Interstellar.mp4");
    Movie tmpMovie = libraryScanService.parseMovieFromFile(tmpFile);
    assertEquals("Interstellar", tmpMovie.getTitle());
    assertNull(tmpMovie.getYear());
  }

  @Test
  public void testParseMovieWithUnderscores() {
    File tmpFile = new File("C:/movies/The_Dark_Knight_2008.mkv");
    Movie tmpMovie = libraryScanService.parseMovieFromFile(tmpFile);
    assertEquals("The Dark Knight", tmpMovie.getTitle());
    assertEquals(2008, tmpMovie.getYear());
  }
}
