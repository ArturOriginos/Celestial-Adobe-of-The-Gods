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
}
