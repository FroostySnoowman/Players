package xyz.nexusservices.players.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.nexusservices.players.Players;

public class Reload implements CommandExecutor {

    private final Players plugin;

    public Reload(Players plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("players.reload")) {
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.no_permission_message")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.plugin_reloaded_message")));
        } else {
            sender.sendMessage(translateColorCodes(plugin.getConfig().getString("Messages.reload_usage")));
        }
        return true;
    }

    private String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}