package xyz.nexusservices.players.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.nexusservices.players.Players;
import xyz.nexusservices.players.utils.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Events implements Listener {
    private final Players plugin;
    private Database db;

    public Events(Players plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        createOrUpdatePlayer(player);
    }

    private void createOrUpdatePlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        Date currentDate = new Date(System.currentTimeMillis());

        try (Connection conn = db.getConnection()) {
            // Check if the player already exists
            try (PreparedStatement psCheck = conn.prepareStatement("SELECT Name FROM players WHERE UUID = ?")) {
                psCheck.setString(1, uuid);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        String storedName = rs.getString("Name");
                        // Check if the stored name matches the current player's name
                        if (!storedName.equals(name)) {
                            // Update the name if mismatched
                            try (PreparedStatement psUpdateName = conn.prepareStatement("UPDATE players SET Name = ?, LastLogon = ? WHERE UUID = ?")) {
                                psUpdateName.setString(1, name);
                                psUpdateName.setDate(2, currentDate);
                                psUpdateName.setString(3, uuid);
                                psUpdateName.executeUpdate();
                            }
                        } else {
                            // Just update the LastLogon if name matches
                            try (PreparedStatement psUpdateLogon = conn.prepareStatement("UPDATE players SET LastLogon = ? WHERE UUID = ?")) {
                                psUpdateLogon.setDate(1, currentDate);
                                psUpdateLogon.setString(2, uuid);
                                psUpdateLogon.executeUpdate();
                            }
                        }
                    } else {
                        // Player does not exist, insert new record
                        try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO players (UUID, Name, Joined, LastLogon, MVP) VALUES (?, ?, ?, ?, ?)")) {
                            psInsert.setString(1, uuid);
                            psInsert.setString(2, name);
                            psInsert.setDate(3, currentDate);
                            psInsert.setDate(4, currentDate);
                            psInsert.setBoolean(5, false);
                            psInsert.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            player.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.error_updating_data")));
        }
    }

    private String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}