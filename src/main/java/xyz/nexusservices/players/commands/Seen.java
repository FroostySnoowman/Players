package xyz.nexusservices.players.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.nexusservices.players.Players;
import xyz.nexusservices.players.utils.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Seen implements CommandExecutor, TabCompleter {
    private final Players plugin;
    private final Database db;
    private final List<String> cachedPlayerNames = new CopyOnWriteArrayList<>();

    public Seen(Players plugin, Database db) {
        this.plugin = plugin;
        this.db = db;

        startCacheUpdateTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.seen_usage")));
            return true;
        }

        String playerName = args[0];

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE Name = ?")) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.player_not_found")));
                        return true;
                    }

                    // Player exists, fetch data
                    String name = rs.getString("Name");
                    String joined = rs.getString("Joined");
                    String lastLogon = rs.getString("LastLogon");
                    String playerUUID = rs.getString("UUID");

                    // Fetch islands owned
                    List<String> islandsOwned = new ArrayList<>();
                    try (PreparedStatement psIslandsOwned = conn.prepareStatement("SELECT IslandNum, Claimed, War FROM islands WHERE IslandNum IN (SELECT IslandNum FROM islandmembers WHERE Player = ? AND Owner = 1)")) {
                        psIslandsOwned.setString(1, playerUUID);
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
                        psIslandsMemberOf.setString(1, playerUUID);
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
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String searchTerm = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String playerName : cachedPlayerNames) {
                if (playerName.toLowerCase().startsWith(searchTerm)) {
                    suggestions.add(playerName);
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private void startCacheUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerNameCache();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1200L);
    }

    private void updatePlayerNameCache() {
        List<String> newCache = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT Name FROM players")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        newCache.add(rs.getString("Name"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update player name cache: " + e.getMessage());
        }

        cachedPlayerNames.clear();
        cachedPlayerNames.addAll(newCache);
    }

    private String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}