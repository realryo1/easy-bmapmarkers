package dev.deimoslabs.easysignmarkers.command;

import dev.deimoslabs.easysignmarkers.helpers.MarkerVisibilityService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static dev.deimoslabs.easysignmarkers.Constants.EDIT_MODE_PERMISSION;

/**
 * Command for toggling player-specific marker-sign edit mode.
 */
public class EditModeCommand implements CommandExecutor, TabCompleter {

    private final MarkerVisibilityService visibilityService;

    public EditModeCommand(MarkerVisibilityService visibilityService) {
        this.visibilityService = visibilityService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (!player.hasPermission(EDIT_MODE_PERMISSION)) {
            player.sendMessage(formatMessage("You don't have permission to use edit mode."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean enabled = visibilityService.toggleEditMode(player);
            player.sendMessage(formatMessage(enabled
                    ? "Edit mode enabled. Marker signs are now visible."
                    : "Edit mode disabled. Marker signs are now hidden."
            ));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            visibilityService.setEditMode(player, true);
            player.sendMessage(formatMessage("Edit mode enabled. Marker signs are now visible."));
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            visibilityService.setEditMode(player, false);
            player.sendMessage(formatMessage("Edit mode disabled. Marker signs are now hidden."));
            return true;
        }

        player.sendMessage(formatMessage("Usage: /" + label + " [on|off|toggle]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();

        List<String> options = new ArrayList<>();
        options.add("on");
        options.add("off");
        options.add("toggle");

        String prefix = args[0].toLowerCase();
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }

    private String formatMessage(String message) {
        return LegacyComponentSerializer.legacySection()
                .serialize(MiniMessage.miniMessage().deserialize("<green>[EasyBMSignMarkers] " + message + "</green>"));
    }
}
