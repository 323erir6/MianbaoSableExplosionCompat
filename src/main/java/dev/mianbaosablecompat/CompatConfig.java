package dev.mianbaosablecompat;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CompatConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue THERMAL_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENTITY_QUERIES;
    public static final ModConfigSpec.BooleanValue LOAD_EXPLOSION_CHUNKS;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Bridge custom Mianbao block effects between terrain and Sable plots.")
                .define("enabled", true);
        THERMAL_EFFECTS = b.comment("Also project fire, scorching and lava replacement effects.")
                .define("thermalEffects", true);
        ENTITY_QUERIES = b.comment("Move plot-local explosion entity searches to world coordinates.")
                .define("entityQueries", true);
        LOAD_EXPLOSION_CHUNKS = b.comment("Temporarily load every terrain chunk touched by a Mianbao explosion.")
                .define("loadExplosionChunks", true);
        SPEC = b.build();
    }
    private CompatConfig() {}
}
