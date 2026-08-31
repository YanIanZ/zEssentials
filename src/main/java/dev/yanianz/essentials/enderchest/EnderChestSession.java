package dev.yanianz.essentials.enderchest;

import java.util.UUID;

/**
 * Per-player view state for the zMenu ender chest GUI: whose chest is being
 * viewed, on which page, and whether the view is read-only.
 */
public class EnderChestSession {

    private final EnderChestData data;
    private final UUID viewerId;
    private final boolean readOnly;
    private final int visiblePages;
    private final int allowedPages;
    private int page;

    public EnderChestSession(UUID viewerId, EnderChestData data, boolean readOnly,
                             int visiblePages, int allowedPages) {
        this.viewerId = viewerId;
        this.data = data;
        this.readOnly = readOnly;
        this.visiblePages = visiblePages;
        this.allowedPages = allowedPages;
        this.page = 0;
    }

    public EnderChestData getData() {
        return data;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public int getVisiblePages() {
        return visiblePages;
    }

    public int getAllowedPages() {
        return allowedPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(page, Math.max(0, visiblePages - 1)));
    }
}
