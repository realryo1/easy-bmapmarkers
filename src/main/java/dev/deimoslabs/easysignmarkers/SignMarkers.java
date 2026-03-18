package dev.deimoslabs.easysignmarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.deimoslabs.easysignmarkers.command.EditModeCommand;
import dev.deimoslabs.easysignmarkers.helpers.IconHelper;
import dev.deimoslabs.easysignmarkers.helpers.LineMarkerManager;
import dev.deimoslabs.easysignmarkers.helpers.LineStore;
import dev.deimoslabs.easysignmarkers.helpers.MarkerHelper;
import dev.deimoslabs.easysignmarkers.helpers.MarkerVisibilityService;
import dev.deimoslabs.easysignmarkers.helpers.UpdateNotifier;
import dev.deimoslabs.easysignmarkers.watcher.MarkerVisibilityWatcher;
import dev.deimoslabs.easysignmarkers.watcher.SignWatcher;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static dev.deimoslabs.easysignmarkers.Constants.EDIT_MODE_COMMAND;
import static dev.deimoslabs.easysignmarkers.Constants.MODRINTH_SLUG;

/**
 * Main plugin handling sign markers for BlueMap.
 * Responsible for initialization, loading/saving marker sets and copying icons to BlueMap's webroot.
 */
public class SignMarkers extends JavaPlugin implements FeatureProvider {

    /**
     * Path to BlueMap's webroot. Populated when BlueMap API becomes available.
     * May be {@code null} before BlueMap initializes.
     */
    private Path webRoot;

    /**
     * Plugin logger instance. Initialized from {@link JavaPlugin#getLogger()} on enable.
     */
    private Logger logger;

    /**
     * Helper instance that manages marker persistence and lifecycle operations.
     */
    private MarkerHelper markerHelper;

    /** Stores line-point data by world/lineId/order. */
    private LineStore lineStore;

    /** Handles BlueMap LineMarker rendering lifecycle. */
    private LineMarkerManager lineMarkerManager;

    /** Controls player-specific visibility for marker signs. */
    private MarkerVisibilityService markerVisibilityService;

    /**
     * Concurrent map associating each loaded Bukkit {@link World} with its BlueMap {@link MarkerSet}.
     */
    private final Map<World, MarkerSet> markerSet = new ConcurrentHashMap<>();

    /**
     * Called when the plugin is enabled.
     * <p>
     * This method performs the following actions:
     * <ul>
     *     <li>initializes the logger;</li>
     *     <li>creates data files for each world;</li>
     *     <li>loads existing marker sets and registers them with BlueMap;</li>
     *     <li>copies marker icons from the plugin JAR into BlueMap's webroot;</li>
     *     <li>registers the sign watcher (SignWatcher).</li>
     * </ul>
     * I/O errors are handled locally and logged as warnings.
     */
    @Override
    public void onEnable() {
        logger = getLogger();
        new UpdateNotifier(this, MODRINTH_SLUG).checkForUpdates();
        markerHelper = new MarkerHelper(this);
        lineStore = new LineStore(this);
        lineMarkerManager = new LineMarkerManager(this, lineStore);
        markerVisibilityService = new MarkerVisibilityService(this, lineStore);
        EditModeCommand editModeCommand = new EditModeCommand(markerVisibilityService);
        if (getCommand(EDIT_MODE_COMMAND) != null) {
            getCommand(EDIT_MODE_COMMAND).setExecutor(editModeCommand);
            getCommand(EDIT_MODE_COMMAND).setTabCompleter(editModeCommand);
        } else {
            logger.warning("Command '/" + EDIT_MODE_COMMAND + "' is not registered in plugin.yml");
        }

        Bukkit.getPluginManager().registerEvents(new MarkerVisibilityWatcher(markerVisibilityService), this);
        BlueMapAPI.onEnable(api -> {
                    try {
                        JarFile jar = new JarFile(this.getFile());
                        webRoot = api.getWebApp().getWebRoot();
                        boolean result = IconHelper.copyMarkers(jar, webRoot, logger);
                        Bukkit.getPluginManager().registerEvents(new SignWatcher(this, lineMarkerManager, markerVisibilityService), this);
                        lineMarkerManager.rebuildAllLines();
                        markerVisibilityService.applyVisibilityToAllOnlinePlayers();
                        logger.info("Plugin init status: " + (result ? "ok!" : "failed!"));
                    } catch (IOException e) {
                        logger.warning("Something crashed during init: " + e.getMessage());
                    }
                }
        );
    }

    /**
     * Called when the plugin is disabled.
     * <p>
     * Delegates shutdown operations to the MarkerHelper.
     */
    @Override
    public void onDisable() {
        markerHelper.onPluginDisable();
        if (lineStore != null) {
            lineStore.saveAll();
        }
    }

    /**
     * Returns the underlying Bukkit {@link Plugin} instance for cases where the raw plugin reference is needed.
     *
     * @return this plugin instance
     */
    @Override
    public Plugin getPlugin() {
        return this;
    }

    /**
     * Returns the path to BlueMap's webroot where static files (e.g. icons) are stored.
     * May be {@code null} if BlueMap has not initialized yet.
     *
     * @return path to BlueMap webroot or {@code null}
     */
    @Override
    public Path getWebRoot() {
        return this.webRoot;
    }

    /**
     * Returns the map of world-to-marker-set associations.
     * The map is concurrent (ConcurrentHashMap) and may be modified by multiple threads.
     *
     * @return non-null map of World -> MarkerSet
     */
    @Override
    public Map<World, MarkerSet> getMarkerSet() {
        return this.markerSet;
    }
}
