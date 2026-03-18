package dev.deimoslabs.easysignmarkers.helpers;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import dev.deimoslabs.easysignmarkers.FeatureProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.deimoslabs.easysignmarkers.Constants.LINE_COLOR_ALPHA;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_COLOR_BLUE;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_COLOR_GREEN;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_COLOR_RED;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_DEPTH_TEST;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_MARKER_ID_PREFIX;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_MARKER_LABEL_PREFIX;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_MAX_DISTANCE;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_WIDTH;

/**
 * Creates, updates and removes BlueMap LineMarker objects based on persisted sign-line data.
 */
public class LineMarkerManager {

    private final FeatureProvider featureProvider;
    private final LineStore lineStore;

    public LineMarkerManager(FeatureProvider featureProvider, LineStore lineStore) {
        this.featureProvider = featureProvider;
        this.lineStore = lineStore;
    }

    public LineRenderResult upsertPoint(World world, String lineId, int order, Location location) {
        lineStore.put(world, lineId, order, location);
        lineStore.saveWorld(world);
        return redrawLine(world, lineId);
    }

    public Set<String> removePointsAtLocation(World world, Location location) {
        Set<String> affectedLineIds = lineStore.removePointsAtLocation(world, location);
        if (affectedLineIds.isEmpty()) return affectedLineIds;

        for (String lineId : affectedLineIds) {
            redrawLine(world, lineId);
        }
        lineStore.saveWorld(world);
        return affectedLineIds;
    }

    public void rebuildAllLines() {
        for (World world : Bukkit.getWorlds()) {
            for (String lineId : lineStore.getLineIds(world)) {
                redrawLine(world, lineId);
            }
        }
    }

    public LineRenderResult redrawLine(World world, String lineId) {
        List<Location> orderedLocations = lineStore.getOrderedLocations(world, lineId);
        MarkerSet markerSet = featureProvider.getMarkerSet().get(world);

        if (markerSet == null) {
            return new LineRenderResult(orderedLocations.size(), false);
        }

        String markerId = buildLineMarkerId(lineId);
        if (orderedLocations.size() < 2) {
            markerSet.remove(markerId);
            return new LineRenderResult(orderedLocations.size(), false);
        }

        List<Vector3d> points = orderedLocations.stream()
                .map(location -> new Vector3d(location.getX(), location.getY(), location.getZ()))
                .collect(Collectors.toList());

        Line line = Line.builder().addPoints(points.toArray(new Vector3d[0])).build();
        LineMarker marker = LineMarker.builder()
                .label(LINE_MARKER_LABEL_PREFIX + lineId)
                .line(line)
                .centerPosition()
                .depthTestEnabled(LINE_DEPTH_TEST)
                .lineWidth(LINE_WIDTH)
                .lineColor(new Color(LINE_COLOR_RED, LINE_COLOR_GREEN, LINE_COLOR_BLUE, LINE_COLOR_ALPHA))
                .maxDistance(LINE_MAX_DISTANCE)
                .detail("lineId: " + lineId + "<br>points: " + orderedLocations.size())
                .build();

        markerSet.put(markerId, marker);
        return new LineRenderResult(orderedLocations.size(), true);
    }

    private String buildLineMarkerId(String lineId) {
        return LINE_MARKER_ID_PREFIX + lineId
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
    }

    public record LineRenderResult(int pointCount, boolean rendered) {
    }
}
