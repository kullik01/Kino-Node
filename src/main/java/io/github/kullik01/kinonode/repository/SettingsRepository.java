package io.github.kullik01.kinonode.repository;

import io.github.kullik01.kinonode.service.DatabaseService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsRepository {
    private static final Logger LOGGER = Logger.getLogger(SettingsRepository.class.getName());
    private final DatabaseService databaseService;

    public SettingsRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public String get(String key, String defaultValue) {
        String query = "SELECT value FROM settings WHERE key = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(query)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    return val != null ? val : defaultValue;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load setting: " + key, e);
        }
        return defaultValue;
    }

    public void set(String key, String value) {
        String upsert = """
            INSERT INTO settings (key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value;
            """;
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(upsert)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save setting: " + key, e);
        }
    }
}
