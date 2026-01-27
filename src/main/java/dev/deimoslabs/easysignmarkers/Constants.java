package dev.deimoslabs.easysignmarkers;

/**
 * Common constants used across the plugin.
 */
public class Constants {
    public static final String MODRINTH_SLUG = "easy-bluemap-sign-markers";
    /**
     * Relative path inside BlueMap webroot where marker images are stored.
     */
    public static final String IMAGE_PATH = "markers/";
    /**
     * Resource folder inside the plugin JAR where marker images are located.
     */
    public static final String RES_FOLDER = IMAGE_PATH + "dynmap";
    /**
     * Prefix used when creating per-world marker set filenames.
     */
    public final static String MARKER_SET_PREFIX = "marker-set-";
    /**
     * Label prefix displayed in BlueMap for sign markers.
     */
    public final static String SIGN_MARKERS_PREFIX = "Sign Markers For ";
    /**
     * Suffix for the JSON files that store marker sets.
     */
    public final static String JSON_FILENAME = ".json";
    /**
     * Template message shown when a marker is removed. Format args: x, y, z (integers).
     */
    public final static String REMOVED_TEMPLATE = "Marker successfully removed at %d %d %d";
    /**
     * MiniMessage template used to announce that a marker was added. Argument: marker text.
     */
    public final static String ADDED_TEMPLATE = "<green>[EasyBMSignMarkers] %s</green>";
    /**
     * HTML template used as the marker detail content. Three string placeholders for lines.
     */
    public final static String HTML_TEMPLATE = """
            <div style='
                padding: 10px;
                text-align: center;
                line-height: 1.4;
                font-family: sans-serif;
                min-width: 170px;
            '>
            <div style='border-style: dashed; border-width: 2px; border-color: #ABABAB; padding: 5px; margin-bottom: 10px'>
                            <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
                            <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
                            <div style='color: #FFFFFF; font-size: 1.1em;'>%s</div>
                            </div>
                <div style='color: #ABABAB; font-size: 0.7em;'>position (x y z)</div>
                <div style='color: #FFFFFF; font-size: 0.8em;'>%d %d %d</div>
                <div style='color: #ABABAB; font-size: 0.7em;'>created %s</div>
                <div style='color: #FFFFFF; font-size: 0.8em;'>by %s</div>
            </div>
            """;
}
