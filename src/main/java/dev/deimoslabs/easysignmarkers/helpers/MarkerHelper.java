package dev.deimoslabs.easysignmarkers.helpers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.deimoslabs.easysignmarkers.FeatureProvider;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.deimoslabs.easysignmarkers.Constants.*;

/**
 * Helper responsible for creating, loading, saving and registering BlueMap marker sets
 * for each Bukkit world. This class centralizes file I/O and BlueMap integration so the
 * main plugin class remains small.
 */
public class MarkerHelper {

    /** Logger obtained from FeatureProvider. */
    private final Logger logger;
    /** Plugin instance obtained from FeatureProvider. */
    private final Plugin plugin;
    /** The feature provider used to access webroot, marker map and logger. */
    private final FeatureProvider featureProvider;

    /**
     * Creates a new MarkerHelper and immediately initializes marker files and registrations.
     *
     * @param featureProvider provider exposing plugin features and marker storage
     */
    public MarkerHelper(FeatureProvider featureProvider) {
        this.featureProvider = featureProvider;
        this.logger = featureProvider.getLogger();
        this.plugin = featureProvider.getPlugin();
        init();
    }

    /**
     * Performs initialization: creates files, loads marker sets for all worlds and registers them with BlueMap.
     */
    private void init() {
        createFiles();
        final List<World> worlds = Bukkit.getWorlds();
        for (World world : worlds) {
            loadWorldMarkerSet(world);
        }
        registerWorlds(worlds);
    }

    /**
     * Called when the plugin is disabled to persist marker sets for all loaded worlds.
     */
    public void onPluginDisable() {
        final List<World> worlds = Bukkit.getWorlds();
        for (World world : worlds) {
            saveWorldMarkerSet(world);
        }
    }

    /**
     * Creates plugin data files (one YAML file per world) and the plugin data folder if it does not exist.
     * Files are named using the {@code MARKER_SET_PREFIX} and {@code MARKER_DATA_FILENAME} constants.
     * This method is safe to call multiple times.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createFiles() {
        for (World world : Bukkit.getWorlds()) {
            File file = worldMarkerDataFile(world);
            try {
                File folder = plugin.getDataFolder();
                if (!folder.exists()) folder.mkdirs();
                if (!file.exists()) file.createNewFile();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Problem while creating plugin files!", ex);
            }
        }
    }

    /**
     * Registers BlueMap marker sets for a list of worlds once BlueMap API becomes available.
     *
     * @param worlds list of Bukkit worlds to register
     */
    private void registerWorlds(List<World> worlds) {
        BlueMapAPI.onEnable(api -> {
            for (World world : worlds) {
                api.getWorld(world).ifPresent(blueWorld -> putMarkers(world, blueWorld));
            }
        });
    }

    /**
     * Attaches or creates a MarkerSet for every map in the provided BlueMap world and stores it
     * in the central marker map from FeatureProvider.
     *
     * @param world the Bukkit world
     * @param blueWorld the BlueMap wrapper for this world
     */
    private void putMarkers(World world, BlueMapWorld blueWorld) {
        for (BlueMapMap map : blueWorld.getMaps()) {
            String label = SIGN_MARKERS_PREFIX + world.getName();
            MarkerSet set = featureProvider.getMarkerSet().get(world);
            if (set == null) set = MarkerSet.builder().label(label).build();
            map.getMarkerSets().put(label, set);
            featureProvider.getMarkerSet().put(world, set);
        }
    }

    /**
     * Saves the marker set associated with the given world to a YAML file in the plugin data folder.
     * I/O exceptions are caught and logged as warnings.
     *
     * @param world the world for which the marker set is saved
     */
    private void saveWorldMarkerSet(World world) {
        File file = worldMarkerDataFile(world);
        YamlConfiguration yaml = new YamlConfiguration();

        try (StringWriter markerJsonWriter = new StringWriter()) {
            MarkerGson.INSTANCE.toJson(featureProvider.getMarkerSet().get(world), markerJsonWriter);
            yaml.set("markerSetJson", markerJsonWriter.toString());
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem while saving marker files!", ex);
        }
    }

    /**
     * Loads the marker set for the given world from a YAML file located in the plugin data folder.
     * If no YAML payload exists, attempts migration from legacy JSON file.
     *
     * @param world the world for which the marker set should be loaded
     */
    private void loadWorldMarkerSet(World world) {
        File file = worldMarkerDataFile(world);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String markerJson = yaml.getString("markerSetJson");

        if (markerJson == null || markerJson.isBlank()) {
            migrateLegacyJsonIfExists(world);
            return;
        }

        try (StringReader reader = new StringReader(markerJson)) {
            MarkerSet set = MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
            if (set != null) featureProvider.getMarkerSet().put(world, set);
        }
    }

    private void migrateLegacyJsonIfExists(World world) {
        File legacyFile = worldLegacyMarkerJsonFile(world);
        if (!legacyFile.exists()) {
            copyLegacyJsonFromOldPluginFolder(world, legacyFile);
        }
        if (!legacyFile.exists()) return;

        try (FileReader reader = new FileReader(legacyFile)) {
            MarkerSet set = MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
            if (set == null) return;

            featureProvider.getMarkerSet().put(world, set);
            saveWorldMarkerSet(world);

            File migrated = new File(legacyFile.getParentFile(), legacyFile.getName() + ".migrated");
            if (!legacyFile.renameTo(migrated)) {
                logger.info("Legacy marker JSON migrated but could not rename file: " + legacyFile.getName());
            }
            logger.info("Migrated legacy marker JSON to YAML for world: " + world.getName());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem while migrating legacy marker JSON files!", ex);
        }
    }

    private void copyLegacyJsonFromOldPluginFolder(World world, File destinationLegacyFile) {
        File sourceLegacyFile = worldLegacyMarkerJsonFileFromOldPluginFolder(world);
        if (!sourceLegacyFile.exists()) return;

        try {
            File parent = destinationLegacyFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            Files.copy(sourceLegacyFile.toPath(), destinationLegacyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied legacy marker JSON from old plugin folder: " + sourceLegacyFile.getPath());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem while copying legacy marker JSON from old plugin folder!", ex);
        }
    }

    private File worldMarkerDataFile(World world) {
        String name = MARKER_SET_PREFIX + world.getName() + MARKER_DATA_FILENAME;
        return new File(plugin.getDataFolder(), name);
    }

    private File worldLegacyMarkerJsonFile(World world) {
        String name = MARKER_SET_PREFIX + world.getName() + JSON_FILENAME;
        return new File(plugin.getDataFolder(), name);
    }

    private File worldLegacyMarkerJsonFileFromOldPluginFolder(World world) {
        String name = MARKER_SET_PREFIX + world.getName() + JSON_FILENAME;
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        if (pluginsFolder == null) {
            return new File(plugin.getDataFolder(), name);
        }

        File oldPluginFolder = new File(pluginsFolder, LEGACY_PLUGIN_DATA_FOLDER);
        return new File(oldPluginFolder, name);
    }

}
