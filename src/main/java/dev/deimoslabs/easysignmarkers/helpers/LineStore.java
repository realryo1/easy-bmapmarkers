package dev.deimoslabs.easysignmarkers.helpers;

import dev.deimoslabs.easysignmarkers.FeatureProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static dev.deimoslabs.easysignmarkers.Constants.LINE_DATA_FILENAME;
import static dev.deimoslabs.easysignmarkers.Constants.LINE_DATA_PREFIX;

/**
 * Stores line-point data in memory and persists it per world in YAML files.
 */
public class LineStore {

    private final FeatureProvider featureProvider;
    private final Map<World, Map<String, TreeMap<Integer, Location>>> linesByWorld = new ConcurrentHashMap<>();

    public LineStore(FeatureProvider featureProvider) {
        this.featureProvider = featureProvider;
        loadAllWorlds();
    }

    public synchronized void loadAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            loadWorld(world);
        }
    }

    public synchronized void saveAll() {
        for (World world : Bukkit.getWorlds()) {
            saveWorld(world);
        }
    }

    public synchronized void loadWorld(World world) {
        File file = getWorldFile(world);
        if (!file.exists()) {
            createFileIfMissing(file);
            linesByWorld.put(world, new HashMap<>());
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, TreeMap<Integer, Location>> worldData = new HashMap<>();

        ConfigurationSection linesSection = yaml.getConfigurationSection("lines");
        if (linesSection != null) {
            for (String lineId : linesSection.getKeys(false)) {
                ConfigurationSection lineSection = linesSection.getConfigurationSection(lineId);
                if (lineSection == null) continue;

                TreeMap<Integer, Location> ordered = new TreeMap<>();
                for (String orderKey : lineSection.getKeys(false)) {
                    int order;
                    try {
                        order = Integer.parseInt(orderKey);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }

                    String basePath = "lines." + lineId + "." + orderKey;
                    double x = yaml.getDouble(basePath + ".x");
                    double y = yaml.getDouble(basePath + ".y");
                    double z = yaml.getDouble(basePath + ".z");
                    ordered.put(order, new Location(world, x, y, z));
                }

                if (!ordered.isEmpty()) {
                    worldData.put(lineId, ordered);
                }
            }
        }

        linesByWorld.put(world, worldData);
    }

    public synchronized void saveWorld(World world) {
        File file = getWorldFile(world);
        createFileIfMissing(file);

        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.getOrDefault(world, Collections.emptyMap());

        for (Map.Entry<String, TreeMap<Integer, Location>> lineEntry : worldData.entrySet()) {
            String lineId = lineEntry.getKey();
            for (Map.Entry<Integer, Location> pointEntry : lineEntry.getValue().entrySet()) {
                String basePath = "lines." + lineId + "." + pointEntry.getKey();
                Location loc = pointEntry.getValue();
                yaml.set(basePath + ".x", loc.getX());
                yaml.set(basePath + ".y", loc.getY());
                yaml.set(basePath + ".z", loc.getZ());
            }
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            featureProvider.getLogger().log(Level.WARNING, "Problem while saving line data files!", ex);
        }
    }

    public synchronized void put(World world, String lineId, int order, Location location) {
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.computeIfAbsent(world, ignored -> new HashMap<>());
        TreeMap<Integer, Location> lineData = worldData.computeIfAbsent(lineId, ignored -> new TreeMap<>());
        lineData.put(order, location.clone());
    }

    public synchronized List<Location> getOrderedLocations(World world, String lineId) {
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.get(world);
        if (worldData == null) return Collections.emptyList();

        TreeMap<Integer, Location> lineData = worldData.get(lineId);
        if (lineData == null) return Collections.emptyList();

        return new ArrayList<>(lineData.values());
    }

    public synchronized Set<String> getLineIds(World world) {
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.get(world);
        if (worldData == null) return Collections.emptySet();
        return new HashSet<>(worldData.keySet());
    }

    public synchronized Set<String> removePointsAtLocation(World world, Location target) {
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.get(world);
        if (worldData == null || worldData.isEmpty()) return Collections.emptySet();

        Set<String> touchedLineIds = new HashSet<>();
        List<String> emptyLineIds = new ArrayList<>();

        for (Map.Entry<String, TreeMap<Integer, Location>> lineEntry : worldData.entrySet()) {
            String lineId = lineEntry.getKey();
            TreeMap<Integer, Location> points = lineEntry.getValue();
            int beforeSize = points.size();
            points.entrySet().removeIf(point -> sameBlock(point.getValue(), target));
            int afterSize = points.size();

            boolean removedFromThisLine = afterSize < beforeSize;
            if (!removedFromThisLine) {
                continue;
            }

            touchedLineIds.add(lineId);

            if (points.isEmpty()) {
                emptyLineIds.add(lineId);
            }
        }

        for (String emptyLineId : emptyLineIds) {
            worldData.remove(emptyLineId);
        }

        return touchedLineIds;
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private File getWorldFile(World world) {
        String fileName = LINE_DATA_PREFIX + world.getName() + LINE_DATA_FILENAME;
        return new File(featureProvider.getPlugin().getDataFolder(), fileName);
    }

    private void createFileIfMissing(File file) {
        File folder = featureProvider.getPlugin().getDataFolder();
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }

        if (file.exists()) return;

        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException ex) {
            featureProvider.getLogger().log(Level.WARNING, "Problem while creating line data files!", ex);
        }
    }

    public synchronized Map<String, TreeMap<Integer, Location>> getLineMap(World world) {
        Map<String, TreeMap<Integer, Location>> worldData = linesByWorld.get(world);
        if (worldData == null) return Collections.emptyMap();
        return worldData;
    }

    public synchronized Collection<Map<String, TreeMap<Integer, Location>>> getAll() {
        return new ArrayList<>(linesByWorld.values());
    }
}
