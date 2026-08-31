package dev.yanianz.essentials.dependency;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Resolves the required Bukkit plugins of zEssentials at startup.
 *
 * Paper refuses to load a plugin whose hard dependencies are missing, so those
 * dependencies are declared as soft dependencies and resolved here instead:
 * PlaceholderAPI is downloaded from Hangar and loaded immediately, no restart
 * needed. zMenu is a paper-plugin.yml plugin with a bootstrapper, which Paper
 * forbids to register at runtime, so its jar is staged into the plugins folder
 * and a single restart is required the first time only.
 */
public final class PluginDependencyResolver {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;

    private static final String HANGAR_PROJECT_API = "https://hangar.papermc.io/api/v1/projects/%s/versions";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/%s/version";

    private enum Source {
        HANGAR,
        MODRINTH
    }

    private record RequiredPlugin(String name, Source source, String projectSlug) {
    }

    /**
     * The plugins zEssentials cannot work without, and where their latest
     * release can be fetched automatically.
     */
    private static final List<RequiredPlugin> REQUIRED = List.of(
            new RequiredPlugin("zMenu", Source.MODRINTH, "zmenu"),
            new RequiredPlugin("PlaceholderAPI", Source.HANGAR, "PlaceholderAPI"),
            new RequiredPlugin("ProtocolLib", Source.HANGAR, "ProtocolLib")
    );

    private PluginDependencyResolver() {
    }

    /**
     * Makes sure every required plugin is available, downloading and loading
     * what the server allows to load at runtime.
     *
     * @param autoRestart when true and a staged plugin needs a restart, the server
     *                    shuts down by itself after a short countdown, a host with
     *                    auto restart enabled brings it back fully installed.
     * @return true when everything is available and zEssentials can continue to enable,
     * false when a restart is needed first, the caller must abort its enable.
     */
    public static boolean resolveRequired(JavaPlugin plugin, boolean autoRestart) {

        PluginManager pluginManager = Bukkit.getPluginManager();

        List<RequiredPlugin> missing = REQUIRED.stream()
                .filter(required -> pluginManager.getPlugin(required.name()) == null)
                .toList();

        if (missing.isEmpty()) return true;

        plugin.getLogger().warning("Missing required plugins: " +
                missing.stream().map(RequiredPlugin::name).toList() + ", trying to install them...");

        boolean restartRequired = false;

        for (RequiredPlugin required : missing) {
            try {
                if (!install(plugin, pluginManager, required)) {
                    restartRequired = true;
                }
            } catch (Exception exception) {
                plugin.getLogger().severe("Unable to install " + required.name() + ": " + exception.getMessage());
                restartRequired = true;
            }
        }

        if (!restartRequired) return true;

        List<String> stillMissing = missing.stream()
                .map(RequiredPlugin::name)
                .filter(name -> pluginManager.getPlugin(name) == null)
                .toList();

        plugin.getLogger().severe("A restart is required once to finish installing: " + stillMissing +
                ". Every following start will be automatic.");

        if (autoRestart) {
            plugin.getLogger().info("Auto-restart is enabled in the configuration, shutting down the server in 10 seconds...");
            Bukkit.broadcastMessage("§6[zEssentials] §eFinishing the installation of " + stillMissing + ", §cserver restarting in 10 seconds§e...");

            // Some region threaded platforms reject the global task, fall back to a delayed stop
            try {
                Bukkit.getAsyncScheduler().runDelayed(plugin, task -> Bukkit.shutdown(), 10L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Throwable throwable) {
                Thread stopper = new Thread(() -> {
                    try { Thread.sleep(10_000L); } catch (InterruptedException ignored) {}
                    Bukkit.shutdown();
                }, "zessentials-auto-stop");
                stopper.setDaemon(true);
                stopper.start();
            }
        } else {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
        return false;
    }

    /**
     * Downloads the plugin when it is not staged yet and tries to load it right away.
     *
     * @return true when the plugin was loaded and enabled,
     * false when it was staged but the server cannot hot-load it (one restart needed).
     */
    private static boolean install(JavaPlugin plugin, PluginManager pluginManager, RequiredPlugin required) throws Exception {

        Path target = plugin.getDataFolder().toPath().getParent().resolve(required.name() + ".jar");

        // Idempotence: never download twice, an already staged jar waits for its restart
        if (!Files.exists(target) || Files.size(target) <= 0) {
            String downloadUrl = switch (required.source()) {
                case MODRINTH -> fetchLatestModrinthDownload(required.projectSlug());
                case HANGAR -> fetchLatestHangarDownload(required.projectSlug());
            };
            plugin.getLogger().info("Downloading " + required.name() + "...");
            download(target.toFile(), downloadUrl);
        }

        try {
            Plugin loaded = pluginManager.loadPlugin(target.toFile());
            if (loaded == null) throw new IllegalStateException("The server refused to load " + required.name());

            pluginManager.enablePlugin(loaded);
            plugin.getLogger().info("Installed " + loaded.getName() + " " + loaded.getDescription().getVersion() + " successfully.");
            return true;
        } catch (Exception exception) {
            // Paper only supports plain plugins at runtime, plugins shipping a paper-plugin.yml
            // bootstrapper like zMenu must go through a normal startup once
            plugin.getLogger().warning(required.name() + " was downloaded into the plugins folder but cannot be hot-loaded: " + exception.getMessage());
            return false;
        }
    }

    /**
     * Queries the Hangar api for the latest release of a project and returns
     * the download url of its paper build.
     */
    private static String fetchLatestHangarDownload(String slug) throws Exception {

        String versionsApi = String.format(HANGAR_PROJECT_API, slug) + "?limit=1";
        JsonObject versionsResponse = getJson(versionsApi).getAsJsonObject();
        JsonArray versions = versionsResponse.getAsJsonArray("result");
        if (versions.isEmpty()) throw new IllegalStateException("No version found on Hangar");
        String version = versions.get(0).getAsJsonObject().get("name").getAsString();

        JsonObject detail = getJson(String.format(HANGAR_PROJECT_API, slug) + "/" + version).getAsJsonObject();
        JsonObject paperDownloads = detail.getAsJsonObject("downloads").getAsJsonObject("PAPER");

        if (paperDownloads.has("downloadUrl") && !paperDownloads.get("downloadUrl").isJsonNull()) {
            return paperDownloads.get("downloadUrl").getAsString();
        }

        if (paperDownloads.has("externalUrl") && !paperDownloads.get("externalUrl").isJsonNull()) {
            return paperDownloads.get("externalUrl").getAsString();
        }

        throw new IllegalStateException("No download URL found on Hangar for " + slug);
    }

    /**
     * Queries the Modrinth api for the versions of a project and returns the
     * url of the primary file of its latest paper build.
     */
    private static String fetchLatestModrinthDownload(String slug) throws Exception {

        // The loaders filter is url encoded json: ["paper"]
        String query = URLEncoder.encode("[\"paper\"]", StandardCharsets.UTF_8);
        // The Modrinth versions endpoint returns a json array, newest first
        JsonArray versions = getJson(String.format(MODRINTH_API, slug) + "?loaders=" + query).getAsJsonArray();

        for (JsonElement versionElement : versions) {
            JsonArray files = versionElement.getAsJsonObject().getAsJsonArray("files");
            for (JsonElement fileElement : files) {
                JsonObject file = fileElement.getAsJsonObject();
                if (file.get("primary").getAsBoolean()) {
                    return file.get("url").getAsString();
                }
            }
            // No primary file on this version, use the first one available
            if (!files.isEmpty()) {
                return files.get(0).getAsJsonObject().get("url").getAsString();
            }
        }

        throw new IllegalStateException("No paper build found on Modrinth");
    }

    private static JsonElement getJson(String url) throws Exception {

        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "zEssentials-DependencyLoader");

        try (InputStream inputStream = connection.getInputStream()) {
            return JsonParser.parseString(new String(inputStream.readAllBytes()));
        }
    }

    private static void download(File target, String url) throws Exception {

        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "zEssentials-DependencyLoader");

        Path temporary = target.toPath().resolveSibling(target.getName() + ".tmp");
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, temporary, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temporary, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
