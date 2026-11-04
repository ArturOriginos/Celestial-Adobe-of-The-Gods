package com.example.celestial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Текст, где цвет плавно переходит от жёлтого к серому. */
public final class GradientText {
    private static final int[] FROM = {255, 214, 51};
    private static final int[] TO = {140, 140, 140};

    private static final int[] RED_FROM = {235, 30, 40};
    private static final int[] RED_TO = {110, 20, 28};

    private GradientText() {}

    /** Оранжево-красно-тёмный градиент для описания Пакселя Богов и Проклятых. */
    public static MutableComponent ofPaxel(String text) {
        int[][] stops = {{255, 150, 24}, {225, 32, 28}, {96, 12, 16}};
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            double pos = 2.0 * i / n;
            int a = (int) Math.min(1, Math.floor(pos));
            double f = pos - a;
            int r = (int) Math.round(stops[a][0] + (stops[a + 1][0] - stops[a][0]) * f);
            int g = (int) Math.round(stops[a][1] + (stops[a + 1][1] - stops[a][1]) * f);
            int b = (int) Math.round(stops[a][2] + (stops[a + 1][2] - stops[a][2]) * f);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(s -> s.withColor(rgb)));
        }
        return out;
    }

    /** Красный градиент для описаний предметов проклятых. */
    public static MutableComponent ofCursed(String text) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            float t = (float) i / n;
            int r = Math.round(RED_FROM[0] + (RED_TO[0] - RED_FROM[0]) * t);
            int g = Math.round(RED_FROM[1] + (RED_TO[1] - RED_FROM[1]) * t);
            int b = Math.round(RED_FROM[2] + (RED_TO[2] - RED_FROM[2]) * t);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(s -> s.withColor(rgb)));
        }
        return out;
    }

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
