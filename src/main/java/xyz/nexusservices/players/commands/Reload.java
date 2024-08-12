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
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        // Check if the correct argument is provided
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            // Reload the plugin
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "The plugin has been successfully reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /players reload");
        }
        return true;
    }
}