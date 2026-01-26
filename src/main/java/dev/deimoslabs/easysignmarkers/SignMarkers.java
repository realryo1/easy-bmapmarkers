package dev.deimoslabs.easysignmarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.deimoslabs.easysignmarkers.helpers.IconHelper;
import dev.deimoslabs.easysignmarkers.watcher.SignWatcher;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignMarkers extends JavaPlugin {

    private Path webRoot;
    private Logger logger;
    public static Map<World, MarkerSet> markerSet = new ConcurrentHashMap<>();

    private final static String MARKER_SET_PREFIX = "marker-set-";
    private final static String SIGN_MARKERS_PREFIX = "Sign Markers For ";
    private final static String JSON_FILENAME = ".json";

    @Override
    public void onEnable() {
        logger = getLogger();
        createFiles();
        for (World world : Bukkit.getWorlds()) {
            loadWorldMarkerSet(world);
            registerWorld(world);
        }
        BlueMapAPI.onEnable(api -> {
                    try {
                        JarFile jar = new JarFile(this.getFile());
                        webRoot = api.getWebApp().getWebRoot();
                        boolean result = IconHelper.copyMarkers(jar, webRoot, logger);
                        Bukkit.getPluginManager().registerEvents(new SignWatcher(logger, webRoot), this);
                        logger.info("Plugin init status: " + (result ? "ok!" : "failed!"));
                    } catch (IOException e) {
                        logger.warning("Something crashed during init: " + e.getMessage());
                    }
                }
        );
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) saveWorldMarkerSet(world);
    }

    private void createFiles() {
        for (World world : Bukkit.getWorlds()) {
            String name = MARKER_SET_PREFIX + world.getName() + JSON_FILENAME;
            File file = new File(this.getDataFolder(), name);
            try {
                File folder = this.getDataFolder();
                if (!folder.exists()) folder.mkdirs();
                if (!file.exists()) file.createNewFile();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Problem while creating plugin files!", ex);
            }
        }
    }

    private void saveWorldMarkerSet(World world) {
        String name = MARKER_SET_PREFIX + world.getName() + JSON_FILENAME;
        File file = new File(this.getDataFolder(), name);
        try (FileWriter writer = new FileWriter(file)) {
            MarkerGson.INSTANCE.toJson(markerSet.get(world), writer);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem while saving marker files!", ex);
        }
    }

    private void loadWorldMarkerSet(World world) {
        String name = MARKER_SET_PREFIX + world.getName() + JSON_FILENAME;
        File file = new File(this.getDataFolder(), name);
        try (FileReader reader = new FileReader(file)) {
            MarkerSet set = MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
            if (set != null) markerSet.put(world, set);
        } catch (FileNotFoundException ignored) {
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem while loading marker files!", ex);
        }
    }

    private void registerWorld(World world) {
        BlueMapAPI.onEnable(api ->
                api.getWorld(world).ifPresent(blueWorld -> {
                    for (BlueMapMap map : blueWorld.getMaps()) {
                        String label = SIGN_MARKERS_PREFIX + world.getName();
                        MarkerSet set = markerSet.get(world);
                        if (set == null) set = MarkerSet.builder().label(label).build();
                        map.getMarkerSets().put(label, set);
                        markerSet.put(world, set);
                    }
                })
        );
    }
}
