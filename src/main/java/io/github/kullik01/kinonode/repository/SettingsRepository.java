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
package io.github.kullik01.kinonode.repository;

import io.github.kullik01.kinonode.service.DatabaseService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access repository for managing system/application settings in the database. Provides
 * key-value pair persistence mechanisms with upsert support.
 */
public class SettingsRepository {
  private static final Logger LOGGER = Logger.getLogger(SettingsRepository.class.getName());
  private final DatabaseService databaseService;

  /**
   * Constructs a SettingsRepository with the specified database service.
   *
   * @param databaseService the service providing the active database connection
   */
  public SettingsRepository(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  /**
   * Retrieves the configuration value associated with the specified key. If the key does not exist
   * or its value is null, the default value is returned.
   *
   * @param key the unique configuration key
   * @param defaultValue the default value to return if the key is not found or has a null value
   * @return the configuration value, or defaultValue if key is missing or database error occurs
   */
  public String get(String key, String defaultValue) {
    String tmpQuery = "SELECT value FROM settings WHERE key = ?";
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpQuery)) {
      tmpStmt.setString(1, key);
      try (ResultSet tmpRs = tmpStmt.executeQuery()) {
        // Check if a record exists for the given key
        if (tmpRs.next()) {
          String tmpVal = tmpRs.getString("value");
          // Fallback to default if the retrieved column value is null
          return tmpVal != null ? tmpVal : defaultValue;
        }
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to load setting: " + key, tmpE);
    }
    return defaultValue;
  }

  /**
   * Saves or updates a configuration setting key-value pair. Uses SQL upsert (ON CONFLICT)
   * semantics to handle existing keys.
   *
   * @param key the configuration key to set
   * @param value the configuration value to store
   */
  public void set(String key, String value) {
    // SQL query that attempts an INSERT and updates the value on key conflict
    String tmpUpsert =
        """
            INSERT INTO settings (key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value;
            """;
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpUpsert)) {
      tmpStmt.setString(1, key);
      tmpStmt.setString(2, value);
      tmpStmt.executeUpdate();
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to save setting: " + key, tmpE);
    }
  }
}
