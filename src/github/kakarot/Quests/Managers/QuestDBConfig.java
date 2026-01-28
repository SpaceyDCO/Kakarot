package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class QuestDBConfig {
    private static final String DB_PATH = "plugins/Kakarot/quests.db";
    private static Connection connection;
    public static void initialize(Main plugin) {
        try {
            File dataFolder = plugin.getDataFolder();
            if(!dataFolder.exists()) if(!dataFolder.mkdirs()) System.err.println("Could not create folder \"Kakarot\" inside plugins");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            Bukkit.getLogger().info("[Kakarot Quests] Connected to SQLite database at " + DB_PATH);
            createTables();
            Bukkit.getLogger().info("[Kakarot Quests] Database initialized successfully");
        }catch(SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Failed to initialize database!", e);
        }
    }
    public static Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed()) connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        return connection;
    }
    public static void closeConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
                Bukkit.getLogger().info("[Kakarot Quests] Database connection closed");
            }
        }catch(SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Could not close database connection properly", e);
        }
    }
    private static void createTables() throws SQLException {
        try(Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_quests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT,player_uuid TEXT NOT NULL,quest_id INTEGER NOT NULL,objective_index INTEGER NOT NULL," +
                    "progress INTEGER DEFAULT 0,max_progress INTEGER NOT NULL,UNIQUE(player_uuid, quest_id, objective_index)" + ")"
            );
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS tracked_quests (" +
                        "player_uuid TEXT PRIMARY KEY, quest_id INTEGER)"
            );
            Bukkit.getLogger().info("[Kakarot Quests] All database tables created");
        }
    }
}
