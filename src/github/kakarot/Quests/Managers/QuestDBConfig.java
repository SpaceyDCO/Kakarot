package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class QuestDBConfig {
    private final Main plugin;
    private static Connection connection;
    @Getter
    private final String databasePath;
    public QuestDBConfig(Main plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "quests.db";
    }
    public boolean initialize() {
        try {
            if(!plugin.getDataFolder().exists()) {
                if(plugin.getDataFolder().mkdirs()) plugin.getLogger().info("Created data folder: " + plugin.getDataFolder().getAbsolutePath());
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            plugin.getLogger().info("Database connection established " + databasePath);
            createTables();
            plugin.getLogger().info("Database initialized successfully");
            return true;
        }catch(ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite driver not found", e);
            return false;
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    public Connection getConnection() {
        try {
            // Check if connection is still valid (timeout after 2 seconds)
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection was closed, reconnecting...");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            }
            return connection;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get database connection", e);
            return null;
        }
    }
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error closing database connection!", e);
            }
        }
    }
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            //Stores main quest progress for each player
            String createPlayerProgress =
                    "CREATE TABLE IF NOT EXISTS player_progress (" +
                    "    player_uuid TEXT NOT NULL," +
                    "    quest_id INTEGER NOT NULL," +
                    "    quest_status TEXT NOT NULL DEFAULT 'NOT_PICKED_UP'," +
                    "    picked_up_at INTEGER DEFAULT NULL," +
                    "    completed_at INTEGER DEFAULT NULL," +
                    "    last_completed INTEGER DEFAULT NULL," +
                    "    next_available INTEGER DEFAULT 0," +
                    "    PRIMARY KEY (player_uuid, quest_id)" +
                    ");";
            stmt.execute(createPlayerProgress);
            plugin.getLogger().info("Table 'player_progress' created/verified.");
            //Stores individual objective progress (normalized array)
            String createObjectiveProgress =
                    "CREATE TABLE IF NOT EXISTS player_objective_progress (" +
                    "    player_uuid TEXT NOT NULL," +
                    "    quest_id INTEGER NOT NULL," +
                    "    objective_index INTEGER NOT NULL," +
                    "    objective_progress INTEGER NOT NULL DEFAULT 0," +
                    "    PRIMARY KEY (player_uuid, quest_id, objective_index)," +
                    "    FOREIGN KEY (player_uuid, quest_id)" +
                    "        REFERENCES player_progress(player_uuid, quest_id)" +
                    "        ON DELETE CASCADE" +
                    ");";

            stmt.execute(createObjectiveProgress);
            plugin.getLogger().info("Table 'player_objective_progress' created/verified.");
            String createPlayerSettings =
                    "CREATE TABLE IF NOT EXISTS player_settings (" +
                    "    player_uuid TEXT PRIMARY KEY NOT NULL," +
                    "    language TEXT NOT NULL DEFAULT 'es'," + //Player language preference, spanish by default
                    "    created_at INTEGER NOT NULL," + //First time the player logged in
                    "    last_login INTEGER NOT NULL" + //Last player login timestamp
                    ");";
            //Indexes for faster query
            createIndexes(stmt);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
            throw e;
        }
    }
    private void createIndexes(Statement stmt) throws SQLException {
        //Index on player_uuid for fast "get all quests for player" queries
        stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_player_uuid " +
                        "ON player_progress(player_uuid);"
        );
        //Index on quest_status for fast "get all IN_PROGRESS quests" queries
        stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_quest_status " +
                        "ON player_progress(quest_status);"
        );
        plugin.getLogger().info("Database indexes created/verified.");
    }
}
