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
import static dev.deimoslabs.easysignmarkers.Constants.LINE_UNDER_DEPTH_TEST;

/**
 * Creates, updates and removes BlueMap LineMarker objects based on persisted sign-line data.
 */
public class LineMarkerManager {

    private final FeatureProvider featureProvider;
    private final LineStore lineStore;
    private final int lineWidth;

    public LineMarkerManager(FeatureProvider featureProvider, LineStore lineStore, int lineWidth) {
        this.featureProvider = featureProvider;
        this.lineStore = lineStore;
        this.lineWidth = lineWidth;
    }

    public LineRenderResult upsertPoint(World world, String lineId, int order, Location location, boolean under, String colorText) {
        lineStore.put(world, lineId, order, location, under, colorText);
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
            .map(location -> new Vector3d(
                location.getX() + 0.5D,
                location.getY() + 0.5D,
                location.getZ() + 0.5D
            ))
            .collect(Collectors.toList());

        boolean underMode = lineStore.isUnderMode(world, lineId);
        boolean depthTest = underMode ? LINE_UNDER_DEPTH_TEST : LINE_DEPTH_TEST;
        float defaultAlpha = LINE_COLOR_ALPHA;
        String colorText = lineStore.getPrimaryColor(world, lineId);
        ParsedLineColor parsedLineColor = parseLineColor(colorText, defaultAlpha);

        Line line = Line.builder().addPoints(points.toArray(new Vector3d[0])).build();
        LineMarker marker = LineMarker.builder()
                .label(LINE_MARKER_LABEL_PREFIX + lineId)
                .line(line)
                .centerPosition()
                .depthTestEnabled(depthTest)
                .lineWidth(lineWidth)
                .lineColor(new Color(parsedLineColor.red(), parsedLineColor.green(), parsedLineColor.blue(), parsedLineColor.alpha()))
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

    private ParsedLineColor parseLineColor(String colorText, float defaultAlpha) {
        if (colorText == null || colorText.isBlank()) {
            return new ParsedLineColor(LINE_COLOR_RED, LINE_COLOR_GREEN, LINE_COLOR_BLUE, defaultAlpha);
        }

        String raw = colorText.trim();
        if (raw.startsWith("#")) raw = raw.substring(1);

        try {
            if (raw.length() == 6) {
                int red = Integer.parseInt(raw.substring(0, 2), 16);
                int green = Integer.parseInt(raw.substring(2, 4), 16);
                int blue = Integer.parseInt(raw.substring(4, 6), 16);
                return new ParsedLineColor(red, green, blue, defaultAlpha);
            }

            if (raw.length() == 8) {
                int red = Integer.parseInt(raw.substring(0, 2), 16);
                int green = Integer.parseInt(raw.substring(2, 4), 16);
                int blue = Integer.parseInt(raw.substring(4, 6), 16);
                int alphaInt = Integer.parseInt(raw.substring(6, 8), 16);
                return new ParsedLineColor(red, green, blue, alphaInt / 255.0f);
            }
        } catch (NumberFormatException ignored) {
        }

        return new ParsedLineColor(LINE_COLOR_RED, LINE_COLOR_GREEN, LINE_COLOR_BLUE, defaultAlpha);
    }

    private record ParsedLineColor(int red, int green, int blue, float alpha) {
    }
}
