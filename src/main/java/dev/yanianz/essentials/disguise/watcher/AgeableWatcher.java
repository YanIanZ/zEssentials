package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class AgeableWatcher extends LivingWatcher {
    @Override
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = super.buildWatcher();
        // Age (index 15): -24000 = baby, 0 = adult
        watcher.setObject(15, getData(MetaIndices.AGEABLE_AGE));
        return watcher;
    }

    public void setBaby(boolean baby) {
        sendData(MetaIndices.AGEABLE_AGE, baby ? -24000 : 0);
    }

    public boolean isBaby() {
        return getData(MetaIndices.AGEABLE_AGE) < 0;
    }
}
