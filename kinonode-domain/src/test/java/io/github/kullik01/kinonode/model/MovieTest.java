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
package io.github.kullik01.kinonode.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test suite verifying properties and behavior of the {@link Movie} domain
 * model.
 */
public class MovieTest {

  @Test
  @DisplayName("Constructs a new Movie with valid attributes and verifies getter methods")
  public void testMovieGettersAndSetters() {
    Movie tmpMovie = new Movie();
    tmpMovie.setId(1);
    tmpMovie.setTitle("Inception");
    tmpMovie.setYear(2010);
    tmpMovie.setDirector("Christopher Nolan");
    tmpMovie.setRating(9);

    assertEquals(Integer.valueOf(1), tmpMovie.getId());
    assertEquals("Inception", tmpMovie.getTitle());
    assertEquals(Integer.valueOf(2010), tmpMovie.getYear());
    assertEquals("Christopher Nolan", tmpMovie.getDirector());
    assertEquals(Integer.valueOf(9), tmpMovie.getRating());
  }

  @Test
  @DisplayName("Verifies default values of a newly instantiated Movie object")
  public void testMovieDefaults() {
    Movie tmpMovie = new Movie();

    assertNull(tmpMovie.getId());
    assertNull(tmpMovie.getTitle());
    assertNull(tmpMovie.getYear());
    assertNull(tmpMovie.getDirector());
  }
}
