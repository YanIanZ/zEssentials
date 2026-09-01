package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class VillagerWatcher extends AgeableWatcher {
    @Override
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = super.buildWatcher();
        // VillagerData (index 18): int array [type, profession, level]
        int[] data = getData(MetaIndices.VILLAGER_DATA);
        // Write as serialized VillagerData - use Registry friendly int
        // On modern versions, villager data is a registry entry with 3 ints
        // WrappedDataWatcher supports writing raw objects
        try {
            watcher.setObject(18, data[0]); // type
            watcher.setObject(19, data[1]); // profession
            watcher.setObject(20, data[2]); // level
        } catch (Exception ignored) {
            // Fallback: write as single int array
            watcher.setObject(18, data);
        }
        return watcher;
    }

    public void setProfession(int profession) {
        int[] data = getData(MetaIndices.VILLAGER_DATA);
        data[1] = profession;
        sendData(MetaIndices.VILLAGER_DATA, data);
    }

    public void setType(int type) {
        int[] data = getData(MetaIndices.VILLAGER_DATA);
        data[0] = type;
        sendData(MetaIndices.VILLAGER_DATA, data);
    }

    public void setLevel(int level) {
        int[] data = getData(MetaIndices.VILLAGER_DATA);
        data[2] = Math.max(1, Math.min(5, level));
        sendData(MetaIndices.VILLAGER_DATA, data);
    }
}
