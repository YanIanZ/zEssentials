package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class SlimeWatcher extends AgeableWatcher {
    @Override
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = super.buildWatcher();
        // Slime size (index 16)
        watcher.setObject(16, getData(MetaIndices.SLIME_SIZE));
        return watcher;
    }

    public void setSize(int size) {
        if (size < 1) size = 1;
        if (size > 50) size = 50;
        sendData(MetaIndices.SLIME_SIZE, size);
    }

    public int getSize() {
        return getData(MetaIndices.SLIME_SIZE);
    }
}
