package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class ZombieWatcher extends InsentientWatcher {
    @Override
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = super.buildWatcher();
        // Zombie baby flag (index 15 for zombies)
        watcher.setObject(15, getData(MetaIndices.ZOMBIE_BABY));
        return watcher;
    }

    public void setBaby(boolean baby) {
        sendData(MetaIndices.ZOMBIE_BABY, baby);
    }

    public boolean isBaby() {
        return getData(MetaIndices.ZOMBIE_BABY);
    }
}
