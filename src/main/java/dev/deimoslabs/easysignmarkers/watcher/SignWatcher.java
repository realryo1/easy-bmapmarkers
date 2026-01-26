package dev.deimoslabs.easysignmarkers.watcher;


import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import dev.deimoslabs.easysignmarkers.SignMarkers;
import dev.deimoslabs.easysignmarkers.helpers.MarkerIcon;
import net.kyori.adventure.text.Component;
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
import java.nio.file.Path;
import java.util.logging.Logger;

public class SignWatcher implements Listener {


    private final static String IMG_PATH = "markers/";
    private final static String REMOVED_TEMPLATE = "Marker successfully removed at %d %d %d";
    private final static String ADDED_TEMPLATE = "<green>[EasyBMSignMarkers] %s</green>";
    private final static String HTML_TEMPLATE = """
            <div style='
                padding: 10px;\s
                text-align: center;\s
                line-height: 1.4;\s
                font-family: sans-serif;
                min-width: 150px;
            '>
                <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
                <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
                <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
            </div>
            """;
    private final Logger logger;
    private final Path webRoot;

    public SignWatcher(Logger logger, Path webRoot) {
        this.logger = logger;
        this.webRoot = webRoot;
    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event) {

        String iconLabel;
        MarkerIcon markerIcon;

        // ### Mapping sign's line 0 to specific marker type (translates to icon)
        final Component header = event.line(0);
        if (header != Component.empty() && header != null) {
            String line0 = LegacyComponentSerializer.legacySection().serialize(header);
            if (line0.startsWith("[") && line0.endsWith("]")) {
                markerIcon = MarkerIcon.match(line0);
                if (markerIcon == null) {
                    markerIcon = MarkerIcon.map;
                }
                iconLabel = markerIcon.name();
            } else {
                return;
            }
        } else {
            return;
        }

        // ### Getting the actual image file for a marker
        String icon = IMG_PATH + iconLabel + ".png";
        File iconFile = new File(webRoot + "/" + icon);
        if (!iconFile.exists()) return;

        Vector2i anchor;
        try {
            BufferedImage image = ImageIO.read(iconFile);
            int width = image.getWidth();
            int height = image.getHeight();
            anchor = new Vector2i(height / 2, width / 2);
        } catch (IOException e) {
            logger.warning(String.format("Something wrong with image %s, details: %s", iconFile.getPath(), e.getMessage()));
            return;
        }


        // ### Building label for the sign, from lines 1-3
        String label1 = "";
        String label2 = "";
        String label3 = "";

        final Component clabel1 = event.line(1);
        if (clabel1 != null) {
            label1 = LegacyComponentSerializer.legacySection().serialize(clabel1);
        }

        final Component clabel2 = event.line(2);
        if (clabel2 != null) {
            label2 = LegacyComponentSerializer.legacySection().serialize(clabel2);
        }

        final Component clabel3 = event.line(3);
        if (clabel3 != null) {
            label3 = LegacyComponentSerializer.legacySection().serialize(clabel3);
        }

        String fullLabel = label1 + " " + label2 + " " + label3;

        // ### No description - no marker
        if (fullLabel.isBlank()) return;

        // ### Getting sign's block XYZ position. This will be marker's id
        Block block = event.getBlock();
        Vector3d pos = new Vector3d(block.getX(), block.getY(), block.getZ());

        String id = "marker-" + pos.getX() + "-" + pos.getY() + "-" + pos.getZ();
        POIMarker marker = POIMarker.builder()
                .position(pos)
                .label(fullLabel)
                .icon(icon, anchor)
                .maxDistance(100000)
                .detail(buildMarkerContent(label1, label2, label3))
                .build();
        SignMarkers.markerSet.get(block.getWorld()).put(id, marker);

        // ### Replace first line, with prefix, e.g. [map], to <marker> indicator
        event.line(0, Component.text("> marker <"));
        event.getPlayer().sendMessage(formatMessage("Marker <" + markerIcon.name() + "> successfully added at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
    }

    @EventHandler
    public void onSignDestroy(BlockBreakEvent event) {

        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) return;

        MarkerSet set = SignMarkers.markerSet.get(block.getWorld());
        if (set == null) return;

        Vector3d pos = new Vector3d(block.getX(), block.getY(), block.getZ());
        String id = "marker-" + pos.getX() + "-" + pos.getY() + "-" + pos.getZ();

        Marker marker = set.get(id);
        if (marker == null) return;
        set.remove(id);

        event.getPlayer().sendMessage(formatMessage(String.format(REMOVED_TEMPLATE,
                (int) pos.getX(),
                (int) pos.getY(),
                (int) pos.getZ())));
    }

    private String buildMarkerContent(String label1, String label2, String label3) {
        return String.format(HTML_TEMPLATE, label1, label2, label3);
    }

    private Component formatMessage(String message) {
        return MiniMessage.miniMessage().deserialize(String.format(ADDED_TEMPLATE, message));
    }
}