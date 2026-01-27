package dev.deimoslabs.easysignmarkers.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.deimoslabs.easysignmarkers.FeatureProvider;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Periodically checks Modrinth for newer plugin versions and logs a message if an update is available.
 * The check is executed asynchronously to avoid blocking the main server thread.
 */
public class UpdateNotifier {

    /** Modrinth project slug used to query the project's versions. */
    private final String modrinthSlug;
    /** Bukkit plugin instance obtained from FeatureProvider. */
    private final Plugin plugin;

    /**
     * Constructs an UpdateNotifier.
     *
     * @param featureProvider provider used to obtain the plugin instance
     * @param modrinthSlug the Modrinth project slug to query for versions
     */
    public UpdateNotifier(FeatureProvider featureProvider, String modrinthSlug) {
        this.modrinthSlug = modrinthSlug;
        this.plugin = featureProvider.getPlugin();
    }

    /**
     * Kicks off an asynchronous check for the latest plugin version on Modrinth.
     * <p>
     * The method schedules an asynchronous task which fetches the latest version number
     * and logs warnings/info depending on whether the running version is outdated.
     */
    @SuppressWarnings("UnstableApiUsage")
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String current = plugin.getPluginMeta().getVersion();
                String latest = fetchLatestVersion();

                if (latest == null) {
                    plugin.getLogger().warning("Cannot verify latest version number.");
                    return;
                }

                if (SemverComparator.isNewer(latest, current)) {
                    plugin.getLogger().warning("You are running version " + current + ". There is new version available: " + latest);
                    plugin.getLogger().warning("Get the new version at: https://modrinth.com/plugin/" + modrinthSlug);
                } else {
                    plugin.getLogger().info("Your plugin version is up to date (version: " + current + ").");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Cannot verify latest version number: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches the latest version string from Modrinth's project version API.
     *
     * @return latest version number string, or {@code null} if the retrieval/parsing failed
     */
    private String fetchLatestVersion() {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + modrinthSlug + "/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "MinecraftPluginUpdateChecker");

            JsonArray array = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonArray();
            JsonElement latest = array.get(0);

            return latest.getAsJsonObject().get("version_number").getAsString();

        } catch (Exception e) {
            return null;
        }
    }
}
