package xyz.nexusservices.players.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.nexusservices.players.utils.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Events implements Listener {

    private Database db;

    public Events(Database db) {
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
            try (PreparedStatement psCheck = conn.prepareStatement("SELECT * FROM players WHERE UUID = ?")) {
                psCheck.setString(1, uuid);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        // Player exists, update LastLogon
                        try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE players SET LastLogon = ? WHERE UUID = ?")) {
                            psUpdate.setDate(1, currentDate);
                            psUpdate.setString(2, uuid);
                            psUpdate.executeUpdate();
                        }
                    } else {
                        // Player does not exist, insert new record
                        try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO players (UUID, Name, Joined, LastLogon) VALUES (?, ?, ?, ?)")) {
                            psInsert.setString(1, uuid);
                            psInsert.setString(2, name);
                            psInsert.setDate(3, currentDate);
                            psInsert.setDate(4, currentDate);
                            psInsert.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            player.sendMessage(translateColorCodes("&cAn error occurred while updating your player data. If this persists, please contact an admin."));
        }
    }

    private String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}