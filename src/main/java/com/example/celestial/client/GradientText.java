package com.example.celestial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Текст, где цвет плавно переходит от жёлтого к серому. */
public final class GradientText {
    private static final int[] FROM = {255, 214, 51};
    private static final int[] TO = {140, 140, 140};

    private GradientText() {}

    public static MutableComponent of(String text) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            float t = (float) i / n;
            int r = Math.round(FROM[0] + (TO[0] - FROM[0]) * t);
            int g = Math.round(FROM[1] + (TO[1] - FROM[1]) * t);
            int b = Math.round(FROM[2] + (TO[2] - FROM[2]) * t);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(s -> s.withColor(rgb)));
        }
        return out;
    }
}
