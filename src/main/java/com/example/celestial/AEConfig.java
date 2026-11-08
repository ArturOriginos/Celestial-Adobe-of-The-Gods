package com.example.celestial;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_COST = BUILDER
            .comment("Максимальная цена в уровнях опыта, которую может запросить стол (по умолчанию 100).")
            .defineInRange("maxLevelCost", 100, 1, 1000);

    public static final ModConfigSpec.BooleanValue ALLOW_ABOVE_MAX = BUILDER
            .comment("Разрешить уровни зачарований выше ванильного максимума (Острота 6, 7, ...).",
                    "Зачарования с ванильным максимумом 1 (Починка, Шёлковое касание) всегда остаются 1.")
            .define("allowAboveVanillaMax", false);

    public static final ModConfigSpec.IntValue ABSOLUTE_MAX_LEVEL = BUILDER
            .comment("Максимальный уровень, если allowAboveVanillaMax = true.")
            .defineInRange("absoluteMaxLevel", 10, 1, 255);

    public static final ModConfigSpec.IntValue SPREAD_INTERVAL = BUILDER
            .comment("Проклятый Нифельхельм в верхнем мире: как часто (в тиках) происходит шаг распространения (20 тиков = 1 секунда).")
            .defineInRange("niflheimSpreadIntervalTicks", 1200, 1, 72000);

    public static final ModConfigSpec.IntValue SPREAD_CHUNKS_PER_STEP = BUILDER
            .comment("Сколько чанков заражается за один шаг распространения.")
            .defineInRange("niflheimChunksPerStep", 1, 1, 256);

    public static final ModConfigSpec.IntValue SPREAD_MAX_CHUNKS = BUILDER
            .comment("Максимум заражённых чанков (распространение останавливается, когда лимит достигнут).")
            .defineInRange("niflheimMaxChunks", 40000, 1, 10000000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AEConfig() {}
}
