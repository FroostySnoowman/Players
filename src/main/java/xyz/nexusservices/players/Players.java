package xyz.nexusservices.players;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.nexusservices.players.commands.Reload;
import xyz.nexusservices.players.commands.Seen;
import xyz.nexusservices.players.events.Events;
import xyz.nexusservices.players.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Players extends JavaPlugin {

    private static Players instance;
    private Database db;
    public @NotNull FileConfiguration config;

    @Override
    public void onEnable() {
        // Set the instance
        if (instance == null) instance = this;

        // Initialize config
        reloadConfig();

        // Initialize Database
        initializeDatabase();

        // Commands
        this.getCommand("seen").setExecutor(new Seen(this, db));
        this.getCommand("players").setExecutor(new Reload(this));

        // Events
        getServer().getPluginManager().registerEvents(new Events(db), this);
    }

    @Override
    public void onDisable() {
        if (db != null) {
            db.close();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        saveDefaultConfig();
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
    }

    public void reloadPlugin() {
        // Reload the configuration
        reloadConfig();

        // Reinitialize the database
        if (db != null) {
            db.close();
        }
        initializeDatabase();

        // Re-register commands and events if necessary
        getCommand("seen").setExecutor(new Seen(this, db));
        getServer().getPluginManager().registerEvents(new Events(db), this);

        getLogger().info("Plugin and configuration reloaded successfully.");
    }

    private void initializeDatabase() {
        // Initialize the database
        this.db = new Database(this);
    }

    public Database getDatabase() {
        return db;
    }

    private void initDatabase() {
        try (Connection conn = db.getConnection()) {
            String sqlTest = "CREATE TABLE IF NOT EXISTS test (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY)";
            try (PreparedStatement ps = conn.prepareStatement(sqlTest)) {
                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create test table", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }
}