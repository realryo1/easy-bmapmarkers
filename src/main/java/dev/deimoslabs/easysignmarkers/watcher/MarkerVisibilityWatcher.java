package dev.deimoslabs.easysignmarkers.watcher;

import dev.deimoslabs.easysignmarkers.helpers.MarkerVisibilityService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import static dev.deimoslabs.easysignmarkers.Constants.MARKER_BLOCKED_PLACE_TEMPLATE;

/**
 * Applies marker-sign visibility per player and protects hidden marker-sign blocks.
 */
public class MarkerVisibilityWatcher implements Listener {

    private final MarkerVisibilityService visibilityService;

    public MarkerVisibilityWatcher(MarkerVisibilityService visibilityService) {
        this.visibilityService = visibilityService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        visibilityService.applyVisibility(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        visibilityService.applyVisibility(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        visibilityService.clearEditMode(event.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        visibilityService.applyVisibilityForChunk(event.getChunk());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (visibilityService.isEditMode(event.getPlayer())) return;
        boolean placedOnMarkerSign = visibilityService.isMarkerSignLocation(event.getBlock().getLocation());
        boolean placedAgainstMarkerSign = visibilityService.isMarkerSignLocation(event.getBlockAgainst().getLocation());
        if (!placedOnMarkerSign && !placedAgainstMarkerSign) return;

        event.setCancelled(true);
        if (placedOnMarkerSign) {
            visibilityService.reassertHiddenMarkerSign(event.getPlayer(), event.getBlock().getLocation());
        }
        if (placedAgainstMarkerSign) {
            visibilityService.reassertHiddenMarkerSign(event.getPlayer(), event.getBlockAgainst().getLocation());
        }
        event.getPlayer().sendMessage(formatMessage(MARKER_BLOCKED_PLACE_TEMPLATE));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (visibilityService.isEditMode(event.getPlayer())) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            // Case 1: player directly clicked a marker-sign block.
            if (visibilityService.isMarkerSignLocation(event.getClickedBlock().getLocation())) {
                denyInteract(event);
                visibilityService.reassertHiddenMarkerSign(event.getPlayer(), event.getClickedBlock().getLocation());
                event.getPlayer().sendMessage(formatMessage(MARKER_BLOCKED_PLACE_TEMPLATE));
                return;
            }

            // Case 2: player clicked an adjacent block face and the placement target is a marker-sign location.
            if (event.getItem() != null && event.getItem().getType().isBlock()) {
                var targetBlock = event.getClickedBlock().getRelative(event.getBlockFace());
                if (visibilityService.isMarkerSignLocation(targetBlock.getLocation())) {
                    denyInteract(event);
                    visibilityService.reassertHiddenMarkerSign(event.getPlayer(), targetBlock.getLocation());
                    event.getPlayer().sendMessage(formatMessage(MARKER_BLOCKED_PLACE_TEMPLATE));
                    return;
                }
            }
        }
    }

    private void denyInteract(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
    }

    private String formatMessage(String message) {
        return LegacyComponentSerializer.legacySection()
                .serialize(MiniMessage.miniMessage().deserialize("<green>[EasyBMSignMarkers] " + message + "</green>"));
    }
}
