package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class LivingWatcher extends FlagWatcher {
    @Override
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = super.buildWatcher();
        // Health (index 6)
        watcher.setObject(6, getData(MetaIndices.LIVING_HEALTH));
        return watcher;
    }

    public void setHealth(float health) {
        sendData(MetaIndices.LIVING_HEALTH, health);
    }

    public float getHealth() {
        return getData(MetaIndices.LIVING_HEALTH);
    }
}
