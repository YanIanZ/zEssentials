package dev.yanianz.essentials.disguise.watcher;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagWatcher {
    private final Map<Integer, Object> userDefinedValues = new ConcurrentHashMap<>();

    public <T> void sendData(MetaIndex<T> metaIndex, T value) {
        userDefinedValues.put(metaIndex.index(), value);
    }

    public <T> T getData(MetaIndex<T> metaIndex) {
        Object value = userDefinedValues.get(metaIndex.index());
        if (value != null) return (T) value;
        return metaIndex.defaultValue();
    }

    public boolean hasValue(MetaIndex<?> metaIndex) {
        return userDefinedValues.containsKey(metaIndex.index());
    }

    /**
     * Builds a WrappedDataWatcher with all defined metadata values.
     * Subclasses override to add mob-specific indices.
     */
    public WrappedDataWatcher buildWatcher() {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        // Entity flags (index 0)
        if (hasValue(MetaIndices.ENTITY_META)) {
            watcher.setObject(0, getData(MetaIndices.ENTITY_META));
        } else {
            watcher.setObject(0, (byte) 0);
        }
        return watcher;
    }
}
