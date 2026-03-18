package dev.deimoslabs.easysignmarkers.helpers;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.deimoslabs.easysignmarkers.FeatureProvider;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.deimoslabs.easysignmarkers.Constants.BM_LINE_KEYWORD;
import static dev.deimoslabs.easysignmarkers.Constants.BM_LINE_UNDER_KEYWORD;
import static dev.deimoslabs.easysignmarkers.Constants.MARKER_ID_PREFIX;

/**
 * Controls player-specific visibility of BlueMap marker signs.
 */
public class MarkerVisibilityService {

    private static final Pattern POI_ID_PATTERN = Pattern.compile("^marker-(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)$");

    private final FeatureProvider featureProvider;
    private final LineStore lineStore;
    private final String lineTag;
    private final String lineUnderTag;
    private final Set<UUID> editModePlayers = ConcurrentHashMap.newKeySet();

    public MarkerVisibilityService(FeatureProvider featureProvider, LineStore lineStore, String startPrefix, String endPrefix) {
        this.featureProvider = featureProvider;
        this.lineStore = lineStore;
        this.lineTag = startPrefix + BM_LINE_KEYWORD + endPrefix;
        this.lineUnderTag = startPrefix + BM_LINE_UNDER_KEYWORD + endPrefix;
    }

    public boolean setEditMode(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        boolean changed;
        if (enabled) {
            changed = editModePlayers.add(uuid);
        } else {
            changed = editModePlayers.remove(uuid);
        }

        applyVisibility(player);
        return changed;
    }

    public boolean toggleEditMode(Player player) {
        boolean target = !isEditMode(player);
        setEditMode(player, target);
        return target;
    }

    public boolean isEditMode(Player player) {
        return editModePlayers.contains(player.getUniqueId());
    }

    public void clearEditMode(Player player) {
        editModePlayers.remove(player.getUniqueId());
    }

    public void applyVisibility(Player player) {
        World world = player.getWorld();
        boolean visible = isEditMode(player);

        for (Location location : collectMarkerSignLocations(world)) {
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
            Block block = world.getBlockAt(location);
            if (visible) {
                showRealBlockForPlayer(player, location, block);
            } else if (isMarkerSignBlock(block)) {
                sendHiddenMarkerBlock(player, location);
            }
        }
    }

    public void applyVisibilityForLocation(World world, Location location) {
        for (Player player : world.getPlayers()) {
            boolean visible = isEditMode(player);
            Block block = world.getBlockAt(location);
            if (visible) {
                showRealBlockForPlayer(player, location, block);
            } else if (isMarkerSignBlock(block)) {
                sendHiddenMarkerBlock(player, location);
            } else {
                player.sendBlockChange(location, block.getBlockData());
            }
        }
    }

    public boolean isMarkerSignLocation(Location location) {
        Block block = location.getWorld().getBlockAt(location);
        return isMarkerSignBlock(block);
    }

    private boolean isMarkerSignBlock(Block block) {
        if (!(block.getState() instanceof Sign sign)) return false;

        String line0 = sign.getSide(Side.FRONT).getLine(0);
        if (line0 != null && (line0.equalsIgnoreCase(lineTag) || line0.equalsIgnoreCase(lineUnderTag))) {
            return true;
        }

        return hasPoiMarkerAtLocation(block);
    }

    private boolean hasPoiMarkerAtLocation(Block block) {
        MarkerSet markerSet = featureProvider.getMarkerSet().get(block.getWorld());
        if (markerSet == null) return false;

        String markerId = MARKER_ID_PREFIX + (double) block.getX() + "-" + (double) block.getY() + "-" + (double) block.getZ();
        return markerSet.get(markerId) != null;
    }

    private Set<Location> collectMarkerSignLocations(World world) {
        Set<Location> locations = new HashSet<>();

        Map<String, TreeMap<Integer, Location>> worldLineMap = lineStore.getLineMap(world);
        for (TreeMap<Integer, Location> ordered : worldLineMap.values()) {
            for (Location location : ordered.values()) {
                if (location.getWorld() != null && location.getWorld().equals(world)) {
                    locations.add(new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                }
            }
        }

        MarkerSet markerSet = featureProvider.getMarkerSet().get(world);
        if (markerSet == null) return locations;

        for (String markerId : markerSet.getMarkers().keySet()) {
            if (!markerId.startsWith(MARKER_ID_PREFIX)) continue;
            Location location = parsePoiMarkerLocation(world, markerId);
            if (location != null) locations.add(location);
        }

        return locations;
    }

    private Location parsePoiMarkerLocation(World world, String markerId) {
        Matcher matcher = POI_ID_PATTERN.matcher(markerId);
        if (!matcher.matches()) return null;

        try {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3));
            return new Location(world, x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void applyVisibilityToAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyVisibility(player);
        }
    }

    /**
     * Reasserts hidden marker-sign state for a specific player.
     * This counters client-side prediction that can temporarily show ghost signs.
     */
    public void reassertHiddenMarkerSign(Player player, Location markerLocation) {
        if (isEditMode(player) || markerLocation.getWorld() == null) return;

        Location blockLocation = new Location(
                markerLocation.getWorld(),
                markerLocation.getBlockX(),
                markerLocation.getBlockY(),
                markerLocation.getBlockZ()
        );

        sendHiddenMarkerBlock(player, blockLocation);

        featureProvider.getPlugin().getServer().getScheduler().runTask(featureProvider.getPlugin(),
                () -> sendHiddenMarkerBlock(player, blockLocation));

        featureProvider.getPlugin().getServer().getScheduler().runTaskLater(featureProvider.getPlugin(),
                () -> sendHiddenMarkerBlock(player, blockLocation), 2L);
    }

    public void applyVisibilityForChunk(Chunk chunk) {
        World world = chunk.getWorld();
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Sign)) continue;

            Location location = state.getLocation();
            if (!isMarkerSignLocation(location)) continue;

            for (Player player : world.getPlayers()) {
                if (isEditMode(player)) {
                    showRealBlockForPlayer(player, location, world.getBlockAt(location));
                } else {
                    sendHiddenMarkerBlock(player, location);
                }
            }
        }
    }

    private void showRealBlockForPlayer(Player player, Location location, Block block) {
        player.sendBlockChange(location, block.getBlockData());

        if (!(block.getState() instanceof Sign sign)) return;

        String[] lines = new String[] {
                sign.getSide(Side.FRONT).getLine(0),
                sign.getSide(Side.FRONT).getLine(1),
                sign.getSide(Side.FRONT).getLine(2),
                sign.getSide(Side.FRONT).getLine(3)
        };
        player.sendSignChange(location, lines);
    }

    private void sendHiddenMarkerBlock(Player player, Location location) {
        player.sendBlockChange(location, Material.AIR.createBlockData());
    }
}
