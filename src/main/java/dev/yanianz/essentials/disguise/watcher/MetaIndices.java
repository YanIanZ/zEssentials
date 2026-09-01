package dev.yanianz.essentials.disguise.watcher;

public final class MetaIndices {
    private MetaIndices() {}

    // Entity (index 0 = flags byte, 2 = custom name, 3 = custom name visible, 6 = health)
    public static final MetaIndex<Byte> ENTITY_META = new MetaIndex<>(0, (byte) 0);
    public static final MetaIndex<String> CUSTOM_NAME = new MetaIndex<>(2, null);
    public static final MetaIndex<Boolean> CUSTOM_NAME_VISIBLE = new MetaIndex<>(3, false);
    public static final MetaIndex<Float> LIVING_HEALTH = new MetaIndex<>(6, 20.0f);

    // Zombie (index 15 = baby boolean)
    public static final MetaIndex<Boolean> ZOMBIE_BABY = new MetaIndex<>(15, false);

    // Slime (index 16 = size int)
    public static final MetaIndex<Integer> SLIME_SIZE = new MetaIndex<>(16, 1);

    // Villager (index 18 = VillagerData as int array [type, profession, level])
    public static final MetaIndex<int[]> VILLAGER_DATA = new MetaIndex<>(18, new int[]{0, 0, 1});

    // Ageable (index 15 = age int, -24000 for baby, 0 for adult)
    public static final MetaIndex<Integer> AGEABLE_AGE = new MetaIndex<>(15, 0);
}
