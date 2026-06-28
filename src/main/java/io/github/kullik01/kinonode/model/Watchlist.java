/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Watchlist entity in the Kino-Node application. A watchlist has a name, a creation
 * timestamp, and contains a list of {@link Movie} objects that are part of the watchlist.
 */
public class Watchlist {
  private Integer id;
  private String name;
  private Instant createdAt;
  private List<Movie> movies = new ArrayList<>();

  /** Constructs a new Watchlist with the current timestamp as the creation time. */
  public Watchlist() {
    // Initialize createdAt to the current instant in time
    this.createdAt = Instant.now();
  }

  /**
   * Constructs a new Watchlist with a specified name and sets the creation time to the current
   * timestamp.
   *
   * @param name the name of the watchlist
   */
  public Watchlist(String name) {
    this.name = name;
    // Initialize createdAt to the current instant in time
    this.createdAt = Instant.now();
  }

  /**
   * Gets the unique identifier of the watchlist.
   *
   * @return the unique database ID, or null if the watchlist has not been persisted
   */
  public Integer getId() {
    return id;
  }

  /**
   * Sets the unique identifier of the watchlist.
   *
   * @param id the unique database ID to set
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Gets the name of the watchlist.
   *
   * @return the watchlist name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the watchlist.
   *
   * @param name the watchlist name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the creation timestamp of the watchlist.
   *
   * @return the creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the creation timestamp of the watchlist.
   *
   * @param createdAt the creation timestamp to set
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Gets the list of movies associated with this watchlist.
   *
   * @return the list of {@link Movie} objects in this watchlist
   */
  public List<Movie> getMovies() {
    return movies;
  }

  /**
   * Sets the list of movies associated with this watchlist.
   *
   * @param movies the list of {@link Movie} objects to associate with this watchlist
   */
  public void setMovies(List<Movie> movies) {
    this.movies = movies;
  }

  /**
   * Returns a string representation of the Watchlist, including its ID and name.
   *
   * @return a string representing this Watchlist
   */
  @Override
  public String toString() {
    return "Watchlist{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}
