package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Название с плавным переливанием цветов в бело-жёлтых тонах (цвет зависит от времени). */
public final class ShimmerText {
    private ShimmerText() {}

    public static MutableComponent of(Component base) {
        String text = base.getString();
        double time = System.currentTimeMillis() / 240.0;
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double t = (Math.sin(time - i * 0.45) + 1.0) / 2.0;       // 0..1
            int r = 255;
            int g = (int) Math.round(255 - 50 * t);                    // белый -> золотой
            int b = (int) Math.round(255 - 195 * t);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(base.getStyle().withColor(rgb)));
        }
        return out;
    }

    /** Паксель: переливание оранжевый -> красный -> тёмно-бордовый -> красный -> оранжевый. */
    public static MutableComponent ofPaxel(Component base) {
        String text = base.getString();
        double time = System.currentTimeMillis() / 300.0;
        int[][] stops = {{255, 150, 24}, {225, 32, 28}, {66, 8, 12}};
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double wave = (Math.sin(time - i * 0.45) + 1.0) / 2.0;   // 0..1
            double pos = wave * 2.0;                                  // 0..2 по трём цветам
            int a = (int) Math.min(1, Math.floor(pos));
            double f = pos - a;
            int r = (int) Math.round(stops[a][0] + (stops[a + 1][0] - stops[a][0]) * f);
            int g = (int) Math.round(stops[a][1] + (stops[a + 1][1] - stops[a][1]) * f);
            int b = (int) Math.round(stops[a][2] + (stops[a + 1][2] - stops[a][2]) * f);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(base.getStyle().withColor(rgb)));
        }
        return out;
    }

    /** Совершенный паксель: переливание золото -> белый -> лаванда -> белый -> золото. */
    public static MutableComponent ofPerfectPaxel(Component base) {
        String text = base.getString();
        double time = System.currentTimeMillis() / 280.0;
        int[][] stops = {{255, 200, 64}, {255, 250, 235}, {170, 150, 255}};
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double wave = (Math.sin(time - i * 0.4) + 1.0) / 2.0;
            double pos = wave * 2.0;
            int a = (int) Math.min(1, Math.floor(pos));
            double f = pos - a;
            int r = (int) Math.round(stops[a][0] + (stops[a + 1][0] - stops[a][0]) * f);
            int g = (int) Math.round(stops[a][1] + (stops[a + 1][1] - stops[a][1]) * f);
            int b = (int) Math.round(stops[a][2] + (stops[a + 1][2] - stops[a][2]) * f);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(base.getStyle().withColor(rgb)));
        }
        return out;
    }

    /** Название с переливанием в красно-чёрных тонах (от тёмно-бордового до ярко-красного). */
    public static MutableComponent ofCursed(Component base) {
        String text = base.getString();
        double time = System.currentTimeMillis() / 260.0;
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double t = (Math.sin(time - i * 0.5) + 1.0) / 2.0;        // 0..1
            int r = (int) Math.round(90 + 165 * t);                    // тёмно-бордовый -> ярко-красный
            int g = (int) Math.round(4 + 26 * t);
            int b = (int) Math.round(10 + 26 * t);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(base.getStyle().withColor(rgb)));
        }
        return out;
    }
}
