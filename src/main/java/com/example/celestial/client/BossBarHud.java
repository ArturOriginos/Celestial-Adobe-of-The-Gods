package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.CelestiaGod;
import com.example.celestial.entity.CursedBoss;
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

/** Кастомные полоски здоровья: Хранитель Богов, Проклятый, Эндер Дракон и Визер. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class BossBarHud {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/" + name + ".png");
    }

    /** Описание одной полоски: текстуры, размеры, положение дорожки, текста и максимум здоровья. */
    private record Style(ResourceLocation frame, ResourceLocation fill, int texW, int texH, int w, int h,
                         int fillW, int fillH, int tx, int ty, int tw, int th,
                         int nameY, int numY, int nameColor, int numColor, double maxHealth,
                         boolean plate, int yOffset, int increment, boolean bakedName, int numRight) {}

    private static final Map<String, Style> STYLES = Map.of(
            "entity.celestial.guardian_of_gods",
            new Style(tex("boss_frame"), tex("boss_fill"), 660, 204, 330, 102, 492, 20, 42, 62, 246, 10,
                    0, 77, 0xFFF6D77A, 0xFFFFF1C2, GuardianOfGods.MAX_HEALTH, false, 3, 108, true, 285),
            "entity.minecraft.ender_dragon",
            new Style(tex("boss_dragon_frame"), tex("boss_dragon_fill"), 560, 161, 280, 80, 426, 16, 33, 39, 213, 8,
                    51, 62, 0xFFC84BFF, 0xFFD9A0FF, 200.0, false, -4, 84, false, 0),
            "entity.minecraft.wither",
            new Style(tex("boss_wither_frame"), tex("boss_wither_fill"), 560, 199, 280, 100, 419, 17, 35, 60, 210, 9,
                    72, 83, 0xFFFF4545, 0xFFFF8A8A, 300.0, false, -4, 104, false, 0));

    // --- полоска Проклятого: рамка 542x174, дорожка из двух половин по 172x18 ---
    private static final ResourceLocation CURSED_FRAME = tex("cursed_boss_frame");
    private static final ResourceLocation CURSED_FILL = tex("cursed_boss_fill");
    private static final int CF_W = 542, CF_H = 174, CF_FILL_W = 344, CF_FILL_H = 18, CF_SEG = 172;
    private static final int CF_LEFT_X = 69, CF_RIGHT_X = 301, CF_TRACK_Y = 80;

    // --- полоска Бога Селестии: отдельная рамка (2172x724, канал пустой) и отдельная золотая полоса (1776x133) ---
    private static final ResourceLocation GOD_FRAME = tex("celestia_god_frame");
    private static final ResourceLocation GOD_FILL = tex("celestia_god_fill");
    private static final int GD_W = 2172, GD_H = 724;
    private static final int GD_FILL_W = 1776, GD_FILL_H = 133;
    // канал внутри рамки (в пикселях рамки): между верхними и нижними орнаментами и между боковыми
    private static final int GD_CH_X0 = 230, GD_CH_X1 = 1940, GD_CH_Y0 = 291, GD_CH_Y1 = 421;

    private BossBarHud() {}

    private static void drawGod(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss, boolean immortal) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 400;
        float sc = w / (float) GD_W;
        int h = Math.round(GD_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 14;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        int c0 = x + Math.round(GD_CH_X0 * sc), c1 = x + Math.round(GD_CH_X1 * sc);
        int y0 = y + Math.round(GD_CH_Y0 * sc), y1 = y + Math.round(GD_CH_Y1 * sc);
        int fillW = Math.round((c1 - c0) * progress);

        g.fill(c0, y0, c1, y1, 0xFF0F1230);                       // пустой канал
        if (fillW > 0) {                                          // золотая полоса: чем меньше здоровья, тем короче
            g.blit(GOD_FILL, c0, y0, fillW, y1 - y0, 0, 0,
                    Math.max(1, Math.round(GD_FILL_W * progress)), GD_FILL_H, GD_FILL_W, GD_FILL_H);
            if (immortal) g.fill(c0, y0, c0 + fillW, y1, 0x6088A8FF); // бессмертие: голубое свечение
        }
        g.fill((c0 + c1) / 2, y0, (c0 + c1) / 2 + 1, y1, 0x90B07A10); // отметка второй стадии (50%)

        g.blit(GOD_FRAME, x, y, w, h, 0, 0, GD_W, GD_H, GD_W, GD_H);  // рамка поверх

        Component name = boss.getName();
        g.drawString(font, name, cx - font.width(name) / 2, y - 9, immortal ? 0xFFBFD4FF : 0xFFFFE08A, true);
        int hp = (int) Math.ceil(progress * CelestiaGod.MAX_HEALTH);
        String text = hp + " / " + (int) CelestiaGod.MAX_HEALTH;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 1, 0xFFFFF1C2, true);

        event.setIncrement(h + 24);
    }

    private static void drawCursed(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 340;
        float sc = w / (float) CF_W;
        int h = Math.round(CF_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 12;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        g.blit(CURSED_FRAME, x, y, w, h, 0, 0, CF_W, CF_H, CF_W, CF_H);

        int fw = Math.round(CF_FILL_W * progress);
        int seg1 = Math.min(fw, CF_SEG);
        int seg2 = Math.max(0, fw - CF_SEG);
        int ty = y + Math.round(CF_TRACK_Y * sc);
        int th = Math.max(1, Math.round(CF_FILL_H * sc));
        if (seg1 > 0) {
            g.blit(CURSED_FILL, x + Math.round(CF_LEFT_X * sc), ty, Math.max(1, Math.round(seg1 * sc)), th,
                    0, 0, seg1, CF_FILL_H, CF_FILL_W, CF_FILL_H);
        }
        if (seg2 > 0) {
            g.blit(CURSED_FILL, x + Math.round(CF_RIGHT_X * sc), ty, Math.max(1, Math.round(seg2 * sc)), th,
                    CF_SEG, 0, seg2, CF_FILL_H, CF_FILL_W, CF_FILL_H);
        }

        Component name = boss.getName();
        g.drawString(font, name, cx - font.width(name) / 2, y - 9, 0xFFFF3B3B, true);
        int max = (int) CursedBoss.MAX_HEALTH;
        int hp = (int) Math.ceil(progress * CursedBoss.MAX_HEALTH);
        String text = hp + " / " + max;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 1, 0xFFFFB0B0, true);

        event.setIncrement(h + 22);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        if (tc.getKey().equals("entity.celestial.celestia_god")) {
            drawGod(event, boss, false);
            return;
        }
        if (tc.getKey().equals("entity.celestial.celestia_god.immortal")) {
            drawGod(event, boss, true);
            return;
        }
        if (tc.getKey().equals("entity.celestial.cursed_boss")) {
            drawCursed(event, boss);
            return;
        }
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
        if (s.plate()) { // тёмная плашка под названием (у новых полосок название нарисовано в рамке)
            g.fill(cx - nameW / 2 - 9, y - 14, cx + nameW / 2 + 9, y - 1, 0xFFC9A227);
            g.fill(cx - nameW / 2 - 8, y - 13, cx + nameW / 2 + 8, y - 2, 0xFF12162E);
        }

        g.blit(s.frame(), x, y, s.w(), s.h(), 0, 0, s.texW(), s.texH(), s.texW(), s.texH());
        int fw = Math.round(s.tw() * progress);
        if (fw > 0) {
            g.blit(s.fill(), x + s.tx(), y + s.ty(), fw, s.th(), 0, 0,
                    Math.max(1, Math.round(s.fillW() * progress)), s.fillH(), s.fillW(), s.fillH());
        }

        if (!s.bakedName()) g.drawString(font, name, cx - nameW / 2, y + s.nameY(), s.nameColor(), true);
        int max = (int) s.maxHealth();
        int hp = (int) Math.ceil(progress * s.maxHealth());
        String text = hp + " / " + max;
        int numX = s.numRight() > 0 ? x + s.numRight() - font.width(text) : cx - font.width(text) / 2;
        g.drawString(font, text, numX, y + s.numY(), s.numColor(), true);

        event.setIncrement(s.increment());
    }
}
