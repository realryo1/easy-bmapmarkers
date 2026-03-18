package dev.deimoslabs.easysignmarkers;

/**
 * Common constants used across the plugin.
 */
public class Constants {
    /** Modrinth plugin slug used for update checks. */
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
     * Prefix used when creating per-world marker id.
     */
    public final static String MARKER_ID_PREFIX = "marker-";

    /** Prefix used when creating line marker ids. */
    public final static String LINE_MARKER_ID_PREFIX = "line-";

    /** Prefix used when creating per-world line data filenames. */
    public final static String LINE_DATA_PREFIX = "line-data-";

    /** Suffix for line data files. */
    public final static String LINE_DATA_FILENAME = ".yml";

    /** Label prefix displayed in BlueMap for line markers. */
    public final static String LINE_MARKER_LABEL_PREFIX = "Line: ";

    /** Config keys and defaults for sign-tag wrappers. */
    public final static String CONFIG_TAG_START_PREFIX = "startprefix";
    public final static String CONFIG_TAG_END_PREFIX = "endprefix";
    public final static String DEFAULT_TAG_START_PREFIX = "=";
    public final static String DEFAULT_TAG_END_PREFIX = "=";

    /** Sign tag keywords for line markers (wrapped by configured prefix/suffix). */
    public final static String BM_LINE_KEYWORD = "BMLine";
    public final static String BM_LINE_UNDER_KEYWORD = "BMLineUnder";

    /**
     * Label prefix displayed in BlueMap for sign markers.
     */
    public final static String SIGN_MARKERS_PREFIX = "Sign Markers For ";
    /**
        * Suffix for marker-set data files.
     */
        public final static String MARKER_DATA_FILENAME = ".yml";

        /** Suffix for legacy marker-set JSON files (migration source). */
    public final static String JSON_FILENAME = ".json";

        /** Legacy plugin data folder used as migration source for marker JSON files. */
        public final static String LEGACY_PLUGIN_DATA_FOLDER = "EasyBMSignMarkers";
    /**
     * Template message shown when a marker is removed. Format args: x, y, z (integers).
     */
    public final static String REMOVED_TEMPLATE = "Marker successfully removed at %d %d %d";

    /**
     * Template message shown when a marker is added. Format args: s (string), x, y, z (integers).
     */
    public final static String ADDED_TEMPLATE = "Marker <%s> successfully added at %d %d %d";

    /** Template shown when a line point is created or updated. */
    public final static String LINE_POINT_TEMPLATE = "Line <%s> point #%d registered at %d %d %d";

    /** Template shown when line marker cannot be rendered yet. */
    public final static String LINE_WAITING_TEMPLATE = "Line <%s> has %d point(s). Add at least 2 points to render.";

    /** Template shown when one or more line points are removed by block break. */
    public final static String LINE_REMOVED_TEMPLATE = "Line point(s) removed. Affected line(s): %d";

    /** Validation error shown when BMLine input is invalid. */
    public final static String LINE_INPUT_ERROR_TEMPLATE = "Invalid BMLine sign. Use line 2 for ID and line 3 for numeric order.";
    public final static String MARKER_BLOCKED_PLACE_TEMPLATE = "その場所はBlueMap用のマーカーが置かれているため置けません";

    /** Default line style used for all BMLine markers. */
    public final static boolean LINE_DEPTH_TEST = true;
    public final static int LINE_WIDTH = 3;
    public final static int LINE_COLOR_RED = 255;
    public final static int LINE_COLOR_GREEN = 80;
    public final static int LINE_COLOR_BLUE = 80;
    public final static float LINE_COLOR_ALPHA = 1.0f;
    public final static int LINE_MAX_DISTANCE = 100000;

    /** Style used when the first (lowest order) point is BMLineUnder. */
    public final static boolean LINE_UNDER_DEPTH_TEST = false;

    /** Command and permission constants for marker edit mode. */
    public final static String EDIT_MODE_COMMAND = "bmedit";
    public final static String EDIT_MODE_PERMISSION = "easybmsignmarkers.edit";

    /**
     * MiniMessage template used for player messages. Argument: marker text.
     */
    public final static String MSG_PREFIX = "<green>[EasyBMSignMarkers] %s</green>";
    /**
     * HTML template used as the marker detail content. The first three string placeholders
     * are used for the three label lines. Subsequent placeholders are used for X, Y, Z,
     * timestamp and author (in that order).
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
