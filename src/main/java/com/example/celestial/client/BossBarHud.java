package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.GuardianOfGods;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/** Кастомные полоски здоровья: Хранитель Богов, Эндер Дракон и Визер. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class BossBarHud {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/" + name + ".png");
    }

    /** Описание одной полоски: текстуры, размеры, положение дорожки, текста и максимум здоровья. */
    private record Style(ResourceLocation frame, ResourceLocation fill, int texW, int texH, int w, int h,
                         int fillW, int fillH, int tx, int ty, int tw, int th,
                         int nameY, int numY, int nameColor, int numColor, double maxHealth,
                         boolean plate, int yOffset, int increment) {}

    private static final Map<String, Style> STYLES = Map.of(
            "entity.celestial.guardian_of_gods",
            new Style(tex("boss_frame"), tex("boss_fill"), 600, 58, 300, 29, 523, 23, 19, 10, 261, 11,
                    -11, 30, 0xFFF6D77A, 0xFFFFFFFF, GuardianOfGods.MAX_HEALTH, true, 10, 55),
            "entity.minecraft.ender_dragon",
            new Style(tex("boss_dragon_frame"), tex("boss_dragon_fill"), 560, 161, 280, 80, 426, 16, 33, 39, 213, 8,
                    51, 62, 0xFFC84BFF, 0xFFD9A0FF, 200.0, false, -4, 84),
            "entity.minecraft.wither",
            new Style(tex("boss_wither_frame"), tex("boss_wither_fill"), 560, 199, 280, 100, 419, 17, 35, 60, 210, 9,
                    72, 83, 0xFFFF4545, 0xFFFF8A8A, 300.0, false, -4, 104));

    private BossBarHud() {}

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        Style s = STYLES.get(tc.getKey());
        if (s == null) return;

        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = (g.guiWidth() - s.w()) / 2;
        int y = event.getY() + s.yOffset();
        int cx = x + s.w() / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        Component name = boss.getName();
        int nameW = font.width(name);
        if (s.plate()) { // у Хранителя название на тёмной плашке с золотой окантовкой
            g.fill(cx - nameW / 2 - 9, y - 14, cx + nameW / 2 + 9, y - 1, 0xFFC9A227);
            g.fill(cx - nameW / 2 - 8, y - 13, cx + nameW / 2 + 8, y - 2, 0xFF12162E);
        }

        g.blit(s.frame(), x, y, s.w(), s.h(), 0, 0, s.texW(), s.texH(), s.texW(), s.texH());
        int fw = Math.round(s.tw() * progress);
        if (fw > 0) {
            g.blit(s.fill(), x + s.tx(), y + s.ty(), fw, s.th(), 0, 0,
                    Math.max(1, Math.round(s.fillW() * progress)), s.fillH(), s.fillW(), s.fillH());
        }

        g.drawString(font, name, cx - nameW / 2, y + s.nameY(), s.nameColor(), true);
        int max = (int) s.maxHealth();
        int hp = (int) Math.ceil(progress * s.maxHealth());
        String text = hp + " / " + max;
        g.drawString(font, text, cx - font.width(text) / 2, y + s.numY(), s.numColor(), true);

        event.setIncrement(s.increment());
    }
}
