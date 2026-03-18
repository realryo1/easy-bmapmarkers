package dev.deimoslabs.easysignmarkers.watcher;


import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import dev.deimoslabs.easysignmarkers.FeatureProvider;
import dev.deimoslabs.easysignmarkers.helpers.LineMarkerManager;
import dev.deimoslabs.easysignmarkers.helpers.MarkerIcon;
import dev.deimoslabs.easysignmarkers.helpers.MarkerVisibilityService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static dev.deimoslabs.easysignmarkers.Constants.*;

/**
 * Listener that watches for sign creation and destruction events and manages
 * corresponding BlueMap markers based on sign contents.
 * Behavior summary:
 * - When a player writes a sign with a recognized icon tag on the first line (e.g. "[map]")
 * the listener will attempt to create a POI marker on BlueMap using the chosen icon and
 * the text from lines 1-3 as the marker label/details.
 * - When a sign is broken, the listener will remove the marker with the corresponding
 * coordinate-based id from the marker set for that world.
 */
public class SignWatcher implements Listener {

    private final String startPrefix;
    private final String endPrefix;
    private final String lineTag;
    private final String lineUnderTag;

    /**
     * FeatureProvider used to access plugin-level facilities:
     * - BlueMap webroot path (for icons)
     * - plugin {@link java.util.logging.Logger}
     * - the central map of {@link org.bukkit.World} -> {@link de.bluecolored.bluemap.api.markers.MarkerSet}
     */
    private final FeatureProvider featureProvider;
    /** Manager for BMLine marker lifecycle. */
    private final LineMarkerManager lineMarkerManager;
    /** Visibility controller for marker signs. */
    private final MarkerVisibilityService markerVisibilityService;

    /**
     * Constructs a SignWatcher using the provided FeatureProvider to access plugin features.
     *
     * @param featureProvider provider exposing BlueMap webroot, logger and marker map
     */
    public SignWatcher(FeatureProvider featureProvider, LineMarkerManager lineMarkerManager, MarkerVisibilityService markerVisibilityService, String startPrefix, String endPrefix) {
        this.featureProvider = featureProvider;
        this.lineMarkerManager = lineMarkerManager;
        this.markerVisibilityService = markerVisibilityService;
        this.startPrefix = startPrefix;
        this.endPrefix = endPrefix;
        this.lineTag = startPrefix + BM_LINE_KEYWORD + endPrefix;
        this.lineUnderTag = startPrefix + BM_LINE_UNDER_KEYWORD + endPrefix;
    }

    /**
     * Handles sign change events (player writes a sign).
     * <p>
     * Expected workflow:
     * - Reads the first line and attempts to match an icon token (e.g. "[map]").
     * - If an icon is found and its image exists in BlueMap's webroot, constructs a POI marker
     * using the sign's position and lines 1-3 as label/detail.
     * - Stores the marker in the marker map under an id built from the block coordinates.
     * - Replaces the first line of the sign with a marker indicator and notifies the player.
     * If image reading or file access fails, the error is logged and the marker is not created.
     *
     * @param event the SignChangeEvent triggered when a player finalizes sign text
     */
    @EventHandler
    public void onSignWrite(SignChangeEvent event) {

        final String line0 = event.getLine(0);
        if (line0 != null && (line0.equalsIgnoreCase(lineTag) || line0.equalsIgnoreCase(lineUnderTag))) {
            handleLineSign(event, line0.equalsIgnoreCase(lineUnderTag));
            return;
        }

        String iconLabel;
        MarkerIcon markerIcon;

        // ### Mapping sign's line 0 to specific marker type (translates to icon)
        if (line0 != null && isCompactWrappedToken(line0)) {
            markerIcon = MarkerIcon.matchToken(extractWrappedToken(line0));
            iconLabel = markerIcon.name();
        } else {
            return;
        }


        // ### Getting the actual image file for a marker
        String icon = IMAGE_PATH + iconLabel + ".png";
        File iconFile = new File(featureProvider.getWebRoot() + "/" + icon);
        if (!iconFile.exists()) return;

        Vector2i anchor;
        try {
            BufferedImage image = ImageIO.read(iconFile);
            int width = image.getWidth();
            int height = image.getHeight();
            anchor = new Vector2i(height / 2, width / 2);
        } catch (IOException e) {
            featureProvider.getLogger().warning(String.format("Something wrong with image %s, details: %s", iconFile.getPath(), e.getMessage()));
            return;
        }


        // ### Building label for the sign, from lines 1-3
        String label1 = event.getLine(1);
        String label2 = event.getLine(2);
        String label3 = event.getLine(3);

        String fullLabel = label1 + " " + label2 + " " + label3;

        // ### No description - no marker
        if (fullLabel.isBlank()) return;

        // ### Getting sign's block XYZ position. This will be marker's id
        Block block = event.getBlock();
        Vector3d pos = new Vector3d(block.getX(), block.getY(), block.getZ());

        String id = MARKER_ID_PREFIX + pos.getX() + "-" + pos.getY() + "-" + pos.getZ();
        MarkerContent markerContent = MarkerContent.builder()
                .label1(label1)
                .label2(label2)
                .label3(label3)
                .position(pos)
                .author(event.getPlayer().getName())
                .build();
        String markerDetails = buildMarkerContent(markerContent);
        POIMarker marker = POIMarker.builder().position(pos).label(fullLabel).icon(icon, anchor).maxDistance(100000).detail(markerDetails).build();
        featureProvider.getMarkerSet().get(block.getWorld()).put(id, marker);

        markerVisibilityService.applyVisibilityForLocation(block.getWorld(), block.getLocation());
        event.getPlayer().sendMessage(formatMessage(String.format(ADDED_TEMPLATE, markerIcon.name(), pos.getFloorX(), pos.getFloorY(), pos.getFloorZ())));
    }

    /**
     * Handles block break events to remove markers when signs are destroyed.
     * If the broken block is a sign and a marker with the corresponding coordinate-based id
     * exists in the marker set, the marker will be removed and the player will be notified.
     *
     * @param event the BlockBreakEvent representing the block destruction
     */
    @EventHandler
    public void onSignDestroy(BlockBreakEvent event) {

        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) return;

        int affectedLineCount = lineMarkerManager.removePointsAtLocation(block.getWorld(), block.getLocation()).size();
        if (affectedLineCount > 0) {
            event.getPlayer().sendMessage(formatMessage(String.format(LINE_REMOVED_TEMPLATE, affectedLineCount)));
        }

        MarkerSet set = featureProvider.getMarkerSet().get(block.getWorld());
        if (set == null) return;

        Vector3d pos = new Vector3d(block.getX(), block.getY(), block.getZ());
        String id = MARKER_ID_PREFIX + pos.getX() + "-" + pos.getY() + "-" + pos.getZ();

        Marker marker = set.get(id);
        if (marker == null) return;
        set.remove(id);
        markerVisibilityService.applyVisibilityForLocation(block.getWorld(), block.getLocation());

        event.getPlayer().sendMessage(formatMessage(String.format(REMOVED_TEMPLATE, (int) pos.getX(), (int) pos.getY(), (int) pos.getZ())));
    }

    /**
     * Handles BMLine sign input:
     * - line 2: line id
     * - line 3: numeric order
     */
    private void handleLineSign(SignChangeEvent event, boolean underMode) {
        String lineId = event.getLine(1);
        String orderRaw = event.getLine(2);
        String colorRaw = event.getLine(3);

        if (lineId == null || lineId.isBlank() || orderRaw == null || orderRaw.isBlank()) {
            event.getPlayer().sendMessage(formatMessage(LINE_INPUT_ERROR_TEMPLATE));
            return;
        }

        int order;
        try {
            order = Integer.parseInt(orderRaw.trim());
        } catch (NumberFormatException ex) {
            event.getPlayer().sendMessage(formatMessage(LINE_INPUT_ERROR_TEMPLATE));
            return;
        }

        Block block = event.getBlock();
        Vector3d pos = new Vector3d(block.getX(), block.getY(), block.getZ());
        LineMarkerManager.LineRenderResult result = lineMarkerManager.upsertPoint(
                block.getWorld(),
                lineId.trim(),
                order,
                block.getLocation(),
                underMode,
                colorRaw == null ? "" : colorRaw
        );

        markerVisibilityService.applyVisibilityForLocation(block.getWorld(), block.getLocation());
        event.getPlayer().sendMessage(formatMessage(String.format(
                LINE_POINT_TEMPLATE,
                lineId.trim(),
                order,
                pos.getFloorX(),
                pos.getFloorY(),
                pos.getFloorZ()
        )));

        if (!result.rendered()) {
            event.getPlayer().sendMessage(formatMessage(String.format(LINE_WAITING_TEMPLATE, lineId.trim(), result.pointCount())));
        }
    }

    /**
     * Builds the HTML content for a marker's detail popup using the provided information.
     *
     * @param content {@link MarkerContent} object containing label lines, position, timestamp and author
     * @return HTML string suitable for use as the marker detail content (ready to be passed to BlueMap)
     */
    private String buildMarkerContent(MarkerContent content) {
        return String.format(HTML_TEMPLATE,
                content.getLabel1(),
                content.getLabel2(),
                content.getLabel3(),
                content.getX(),
                content.getY(),
                content.getZ(),
                content.getTimestamp(),
                content.getAuthor());
    }

    /**
     * Formats a message using MiniMessage/Legacy templates for player feedback.
     *
     * @param message the message text to format
     * @return an Adventure {@link String} ready to send to a player
     */
    private String formatMessage(String message) {
        return LegacyComponentSerializer.legacySection()
                .serialize(MiniMessage.miniMessage().deserialize(String.format(MSG_PREFIX, message)));
    }

    private boolean isCompactWrappedToken(String raw) {
        if (!raw.startsWith(startPrefix) || !raw.endsWith(endPrefix)) return false;
        String token = extractWrappedToken(raw);
        if (token.isBlank()) return false;

        for (int i = 0; i < token.length(); i++) {
            if (Character.isWhitespace(token.charAt(i))) return false;
        }
        return true;
    }

    private String extractWrappedToken(String raw) {
        int begin = startPrefix.length();
        int end = raw.length() - endPrefix.length();
        if (end < begin) return "";
        return raw.substring(begin, end);
    }
}