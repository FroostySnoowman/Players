package xyz.nexusservices.players.commands;

import org.bukkit.OfflinePlayer;
import xyz.nexusservices.players.Players;
import xyz.nexusservices.players.utils.Database;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Seen implements CommandExecutor {
    private final Players plugin;
    private final Database db;

    public Seen(Players plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.usage")));
            return false;
        }

        String playerName = args[0];
        UUID playerUUID = getPlayerUUID(playerName);

        if (playerUUID == null) {
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.player_not_found")));
            return true;
        }

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE UUID = ?")) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.player_not_found")));
                        return true;
                    }

                    // Player exists, fetch data
                    String name = rs.getString("Name");
                    String joined = rs.getString("Joined");
                    String lastLogon = rs.getString("LastLogon");

                    // Fetch islands owned
                    List<String> islandsOwned = new ArrayList<>();
                    try (PreparedStatement psIslandsOwned = conn.prepareStatement("SELECT IslandNum, Claimed, War FROM islands WHERE IslandNum IN (SELECT IslandNum FROM islandmembers WHERE Player = ? AND Owner = 1)")) {
                        psIslandsOwned.setString(1, playerUUID.toString());
                        try (ResultSet rsIslandsOwned = psIslandsOwned.executeQuery()) {
                            while (rsIslandsOwned.next()) {
                                String islandNum = rsIslandsOwned.getString("IslandNum");
                                String claimed = rsIslandsOwned.getString("Claimed");
                                boolean warStatus = rsIslandsOwned.getBoolean("War");
                                String warStatusString = warStatus ? "Yes" : "No";
                                islandsOwned.add(islandNum + " | claimed " + claimed + " | at war? " + warStatusString);
                            }
                        }
                    }

                    // Fetch islands the player is a member of
                    List<String> islandsMemberOf = new ArrayList<>();
                    try (PreparedStatement psIslandsMemberOf = conn.prepareStatement("SELECT IslandNum, Claimed, War FROM islands WHERE IslandNum IN (SELECT IslandNum FROM islandmembers WHERE Player = ? AND Owner = 0)")) {
                        psIslandsMemberOf.setString(1, playerUUID.toString());
                        try (ResultSet rsIslandsMemberOf = psIslandsMemberOf.executeQuery()) {
                            while (rsIslandsMemberOf.next()) {
                                String islandNum = rsIslandsMemberOf.getString("IslandNum");
                                String claimed = rsIslandsMemberOf.getString("Claimed");
                                boolean warStatus = rsIslandsMemberOf.getBoolean("War");
                                String warStatusString = warStatus ? "Yes" : "No";
                                islandsMemberOf.add(islandNum + " | claimed " + claimed + " | at war? " + warStatusString);
                            }
                        }
                    }

                    // Set "none" if no islands found
                    if (islandsOwned.isEmpty()) {
                        islandsOwned.add("none");
                    }
                    if (islandsMemberOf.isEmpty()) {
                        islandsMemberOf.add("none");
                    }

                    // Construct the message
                    String islandsOwnedString = String.join("\n", islandsOwned);
                    String islandsMemberOfString = String.join("\n", islandsMemberOf);

                    String playerInfoMessage = translateColorCodes(this.plugin.getConfig().getString("Messages.player_info"))
                            .replace("%name%", name)
                            .replace("%joined%", joined)
                            .replace("%lastlogon%", lastLogon)
                            .replace("%islands_owned%", islandsOwnedString)
                            .replace("%islands_member_of%", islandsMemberOfString);

                    sender.sendMessage(playerInfoMessage);

                }
            }
        } catch (SQLException e) {
            sender.sendMessage(translateColorCodes("&cAn error occurred while querying the database."));
            e.printStackTrace();
        }

        return true;
    }

    private String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private UUID getPlayerUUID(String playerName) {
        // Check if the player is online
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }

        // If the player is not online, fetch the OfflinePlayer object
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }

        // Return null if the player has never joined the server
        return null;
    }
}