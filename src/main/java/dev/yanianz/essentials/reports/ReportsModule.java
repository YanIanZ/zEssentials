package dev.yanianz.essentials.reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.dto.ReportDTO;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Players report others with /report Player Reason, every staff member
 * online receives a clickable alert and reports are resolved from
 * /reports.
 */
public class ReportsModule extends ZModule {

    private int cooldownSeconds;
    private int resolvedHistory;
    private boolean notifyStaffOnJoin;

    @NonLoadable
    private final Map<Integer, Report> reports = new ConcurrentHashMap<>();
    @NonLoadable
    private final AtomicInteger idCounter = new AtomicInteger(1);
    @NonLoadable
    private final Map<UUID, Long> lastReportAt = new HashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ReportsModule(ZEssentialsPlugin plugin) {
        super(plugin, "reports");
    }

    public static final class Report {
        final int id;
        final UUID reporterUuid;
        final String reporterName;
        final UUID targetUuid;
        final String targetName;
        final String reason;
        final long createdAt;
        volatile boolean resolved = false;

        Report(int id, UUID reporterUuid, String reporterName, UUID targetUuid, String targetName, String reason, long createdAt) {
            this.id = id;
            this.reporterUuid = reporterUuid;
            this.reporterName = reporterName;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.reason = reason;
            this.createdAt = createdAt;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.cooldownSeconds = Math.max(0, config.getInt("cooldown-seconds", 60));
        this.resolvedHistory = Math.max(1, config.getInt("resolved-history", 100));
        this.notifyStaffOnJoin = config.getBoolean("notify-staff-on-join", true);

        this.reports.clear();
        this.idCounter.set(1);
        migrateOldStorage();
        loadStorage();
    }

    /**
     * Alerts joining moderators when unresolved reports wait for them.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!this.isEnable || !this.notifyStaffOnJoin) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("essentials.report.view")) return;

        long open = this.reports.values().stream().filter(report -> !report.resolved).count();
        if (open > 0) {
            player.sendMessage(ColorUtil.component("&c&l[REPORTS] &7" + open + " unresolved report(s) waiting, use &f/reports&7."));
        }
    }

    /**
     * Creates a report respecting the cooldown of the reporting player.
     *
     * @return SUCCESS, COOLDOWN with the remaining seconds or SELF.
     */
    public Result create(Player reporter, UUID targetUuid, String targetName, String reason) {

        UUID reporterUuid = reporter.getUniqueId();

        if (reporterUuid.equals(targetUuid)) return Result.SELF;

        long now = System.currentTimeMillis();
        long last = this.lastReportAt.getOrDefault(reporterUuid, 0L);
        if (now - last < this.cooldownSeconds * 1000L) {
            long remainingSeconds = (this.cooldownSeconds * 1000L - (now - last) + 999) / 1000L;
            plugin.getEssentialsServer().sendMessage(reporterUuid,
                    fr.maxlego08.essentials.api.messages.Message.REPORT_COOLDOWN,
                    "%seconds%", String.valueOf(remainingSeconds));
            return Result.COOLDOWN;
        }

        int id = this.idCounter.getAndIncrement();
        Report report = new Report(id, reporterUuid, reporter.getName(), targetUuid, targetName, reason, now);
        this.reports.put(id, report);
        this.lastReportAt.put(reporterUuid, now);
        getStorage().upsertReport(new ReportDTO(id, reporterUuid, reporter.getName(), targetUuid, targetName, reason, now, false));

        // Clean up old resolved reports above the history limit
        List<Report> resolvedReports = this.reports.values().stream()
                .filter(entry -> entry.resolved)
                .sorted((a, b) -> Long.compare(b.createdAt, a.createdAt))
                .toList();
        for (int index = resolvedHistory; index < resolvedReports.size(); index++) {
            int reportId = resolvedReports.get(index).id;
            this.reports.remove(reportId);
            getStorage().deleteReport(reportId);
        }

        // Alert every moderator online with a clickable teleport action
        Component alert = ColorUtil.component("&8[&#ff4d4d&lREPORT&8] &f" + reporter.getName()
                + " &7reported &f" + targetName + "&7: &f" + reason);
        Component clickable = alert
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        ColorUtil.component("&7Report &f#" + id + " &8• &7Click to teleport to the target")))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/reports tp " + id));

        for (Player moderator : Bukkit.getOnlinePlayers()) {
            if (moderator.hasPermission("essentials.report.view")) {
                moderator.sendMessage(clickable);
                moderator.playSound(moderator.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f);
            }
        }

        reporter.sendMessage(ColorUtil.component("&aYour report about &f" + targetName + " &awas sent to the staff!"));
        return Result.SUCCESS;
    }

    public enum Result {
        SUCCESS,
        COOLDOWN,
        SELF
    }

    public List<Report> getOpenReports() {
        return this.reports.values().stream()
                .sorted((a, b) -> Integer.compare(a.id, b.id))
                .toList();
    }

    public Report getReport(int id) {
        return this.reports.get(id);
    }

    /**
     * Marks a report resolved or reopens it.
     */
    public void setResolved(int id, boolean resolved) {
        Report report = this.reports.get(id);
        if (report != null) {
            report.resolved = resolved;
            getStorage().upsertReport(new ReportDTO(report.id, report.reporterUuid, report.reporterName,
                    report.targetUuid, report.targetName, report.reason, report.createdAt, resolved));
        }
    }

    /**
     * Returns the stored target uuid of a report id, used by the teleport action.
     */
    public UUID getTargetUuid(int id) {
        Report report = this.reports.get(id);
        return report == null ? null : report.targetUuid;
    }

    private File getStorageFile() {
        return new File(getFolder(), "reports.json");
    }

    private void loadStorage() {
        List<ReportDTO> dtos = getStorage().getReports();
        for (ReportDTO dto : dtos) {
            Report report = new Report(dto.id(), dto.reporterUuid(), dto.reporterName(),
                    dto.targetUuid(), dto.targetName(), dto.reason(), dto.createdAt());
            report.resolved = dto.resolved();
            this.reports.put(dto.id(), report);
            if (dto.id() >= this.idCounter.get()) {
                this.idCounter.set(dto.id() + 1);
            }
        }
    }

    private void migrateOldStorage() {
        File file = getStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            RawStorage raw = this.gson.fromJson(json, RawStorage.class);
            if (raw != null && raw.entries != null) {
                for (RawReport rawReport : raw.entries) {
                    try {
                        getStorage().upsertReport(new ReportDTO(rawReport.id,
                                UUID.fromString(rawReport.reporter_uuid), rawReport.reporter_name,
                                UUID.fromString(rawReport.target_uuid), rawReport.target_name,
                                rawReport.reason, rawReport.created_at, rawReport.resolved));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            File migrated = new File(getFolder(), "reports.json.migrated");
            if (!file.renameTo(migrated)) {
                file.delete();
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private static final class RawStorage {
        RawReport[] entries = new RawReport[0];
    }

    private static final class RawReport {
        int id;
        String reporter_uuid;
        String reporter_name;
        String target_uuid;
        String target_name;
        String reason;
        long created_at;
        boolean resolved;
    }
}
