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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AEConfig() {}
}
