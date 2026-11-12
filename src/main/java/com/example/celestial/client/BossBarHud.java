package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.AllKnowingAngel;
import com.example.celestial.entity.CelestiaGod;
import com.example.celestial.entity.CursedBoss;
import com.example.celestial.entity.Fenrir;
import com.example.celestial.entity.Cerberus;
import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.entity.Hades;
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
            "entity.minecraft.ender_dragon",
            new Style(tex("boss_dragon_frame"), tex("boss_dragon_fill"), 560, 161, 280, 80, 426, 16, 33, 39, 213, 8,
                    51, 62, 0xFFC84BFF, 0xFFD9A0FF, 200.0, false, -4, 84, false, 0),
            "entity.minecraft.wither",
            new Style(tex("boss_wither_frame"), tex("boss_wither_fill"), 560, 199, 280, 100, 419, 17, 35, 60, 210, 9,
                    72, 83, 0xFFFF4545, 0xFFFF8A8A, 300.0, false, -4, 104, false, 0));

    // --- полоска Хранителя Богов: золотая рамка с крыльями 2098x450 (канал пустой) и фиолетовая полоса 1541x93 ---
    private static final ResourceLocation GUARD_FRAME = tex("guardian_frame");
    private static final ResourceLocation GUARD_FILL = tex("guardian_fill");
    private static final int GG_W = 2098, GG_H = 450;
    private static final int GG_FILL_W = 1541, GG_FILL_H = 93;
    // канал внутри рамки (в пикселях рамки), между верхней и нижней золотыми шинами
    private static final int GG_CH_X0 = 278, GG_CH_X1 = 1819, GG_CH_Y0 = 166, GG_CH_Y1 = 259;

    // --- полоска Аида: рамка 2172x724 (название на пластинах нарисовано, канал прозрачный) и пламенная заливка 1762x424 ---
    private static final ResourceLocation HADES_FRAME = tex("hades_frame");
    private static final ResourceLocation HADES_FRAME_EN = tex("hades_frame_en"); // та же рамка с надписями God of Death / Hades
    private static final ResourceLocation HADES_FILL = tex("hades_fill");
    private static final int HD_W = 2172, HD_H = 724;
    private static final int HD_CH_X0 = 205, HD_CH_X1 = 1967, HD_CH_Y0 = 358, HD_CH_Y1 = 416;
    private static final int HD_FILL_X = 205, HD_FILL_Y = 124, HD_FILL_W = 1762, HD_FILL_H = 424;

    // --- полоска Цербера: рамка с тремя головами 2139x440 (канал пустой) и красная полоса 1856x78 ---
    private static final ResourceLocation CERB_FRAME = tex("cerberus_frame");
    private static final ResourceLocation CERB_FILL = tex("cerberus_fill");
    private static final int CB_W = 2139, CB_H = 440;
    private static final int CB_FILL_W = 1856, CB_FILL_H = 78;
    private static final int CB_CH_X0 = 141, CB_CH_X1 = 1997, CB_CH_Y0 = 196, CB_CH_Y1 = 274;

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

    // --- полоска Всезнающего Ангела: рамка 2025x470 (канал 1565x61 прозрачный) и отдельная фиолетовая полоса ---
    private static final ResourceLocation ANGEL_FRAME = tex("all_knowing_frame");
    private static final ResourceLocation ANGEL_FILL = tex("all_knowing_fill");
    private static final int AK_W = 2025, AK_H = 470, AK_FILL_W = 1565, AK_FILL_H = 61;
    private static final int AK_CH_X = 230, AK_CH_Y = 290;

    // --- полоска Фенрира: рамка 2157x520 с нарисованным именем (окно под заливку вырезано), заливка 2134x79 ---
    private static final ResourceLocation FENRIR_FRAME = tex("fenrir_frame");        // название нарисовано по-русски
    private static final ResourceLocation FENRIR_FRAME_EN = tex("fenrir_frame_en");  // то же, но с надписью Fenrir
    private static final ResourceLocation FENRIR_FILL = tex("fenrir_fill");
    private static final int FN_W = 2157, FN_H = 520, FN_FILL_W = 2134, FN_FILL_H = 79;
    private static final int FN_CH_X0 = 329, FN_CH_X1 = 1872, FN_CH_Y0 = 208, FN_CH_Y1 = 272;

    private BossBarHud() {}

    private static void drawAngel(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss, boolean immortal) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 380;
        float sc = w / (float) AK_W;
        int h = Math.round(AK_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 4;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        int c0 = x + Math.round(AK_CH_X * sc);
        int cy0 = y + Math.round(AK_CH_Y * sc);
        int cw = Math.round(AK_FILL_W * sc);
        int ch = Math.max(1, Math.round(AK_FILL_H * sc));
        g.fill(c0, cy0, c0 + cw, cy0 + ch, 0xFF0C0612);                       // пустой канал
        int fw = Math.round(cw * progress);
        if (fw > 0) {
            g.blit(ANGEL_FILL, c0, cy0, fw, ch, 0, 0,
                    Math.max(1, Math.round(AK_FILL_W * progress)), AK_FILL_H, AK_FILL_W, AK_FILL_H);
            if (immortal) g.fill(c0, cy0, c0 + fw, cy0 + ch, 0x70C8A0FF); // бессмертие: полоса светится
        }
        int phaseX = c0 + Math.round(cw * (float) (AllKnowingAngel.PHASE2_HEALTH / AllKnowingAngel.MAX_HEALTH));
        g.fill(phaseX, cy0, phaseX + 1, cy0 + ch, 0xA0FFD27A);                // отметка второй фазы (5000)
        g.blit(ANGEL_FRAME, x, y, w, h, 0, 0, AK_W, AK_H, AK_W, AK_H);        // рамка поверх

        Component name = boss.getName();
        g.drawString(font, name, cx - font.width(name) / 2, y + h - 3, 0xFFE9A8FF, true);
        int hp = (int) Math.ceil(progress * AllKnowingAngel.MAX_HEALTH);
        String text = hp + " / " + (int) AllKnowingAngel.MAX_HEALTH;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 8, 0xFFD9B4FF, true);

        event.setIncrement(h + 22);
    }

    private static void drawHades(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        if (HadesCutscene.isActive()) { // во время катсцены вместо полосы - чёрные полосы и реплики
            HadesCutscene.drawOverlay(g);
            return;
        }
        Font font = Minecraft.getInstance().font;
        int w = 440;
        float sc = w / (float) HD_W;
        int h = Math.round(HD_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 6;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        g.fill(x + Math.round(HD_CH_X0 * sc), y + Math.round(HD_CH_Y0 * sc),
                x + Math.round(HD_CH_X1 * sc), y + Math.round(HD_CH_Y1 * sc), 0xFF14040A); // пустой канал
        int fx = x + Math.round(HD_FILL_X * sc), fy = y + Math.round(HD_FILL_Y * sc);
        int fw = Math.round(HD_FILL_W * sc), fh = Math.round(HD_FILL_H * sc);
        int shown = Math.round(fw * progress);
        if (shown > 0) { // пламенная заливка под рамкой: чем меньше здоровья, тем короче
            g.blit(HADES_FILL, fx, fy, shown, fh, 0, 0,
                    Math.max(1, Math.round(HD_FILL_W * progress)), HD_FILL_H, HD_FILL_W, HD_FILL_H);
        }
        // название нарисовано в самой рамке, поэтому рамку выбираем по языку клиента: русский -> Бог Смерти / Аид, остальные -> English
        boolean ru = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
        g.blit(ru ? HADES_FRAME : HADES_FRAME_EN, x, y, w, h, 0, 0, HD_W, HD_H, HD_W, HD_H); // рамка поверх

        int hp = (int) Math.ceil(progress * Hades.MAX_HEALTH);
        String text = hp + " / " + (int) Hades.MAX_HEALTH;
        g.drawString(font, text, cx - font.width(text) / 2, y + h - 4, 0xFFE0C2FF, true);
        event.setIncrement(h + 6);
    }

    private static void drawCerberus(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 420;
        float sc = w / (float) CB_W;
        int h = Math.round(CB_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 14;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        int c0 = x + Math.round(CB_CH_X0 * sc), c1 = x + Math.round(CB_CH_X1 * sc);
        int y0 = y + Math.round(CB_CH_Y0 * sc), y1 = y + Math.round(CB_CH_Y1 * sc);
        int fillW = Math.round((c1 - c0) * progress);

        g.fill(c0, y0, c1, y1, 0xFF120408);                       // пустой канал
        if (fillW > 0) {                                          // красная полоса: чем меньше здоровья, тем короче
            g.blit(CERB_FILL, c0, y0, fillW, y1 - y0, 0, 0,
                    Math.max(1, Math.round(CB_FILL_W * progress)), CB_FILL_H, CB_FILL_W, CB_FILL_H);
        }
        g.blit(CERB_FRAME, x, y, w, h, 0, 0, CB_W, CB_H, CB_W, CB_H); // рамка с головами поверх

        Component name = boss.getName();
        g.drawString(font, name, cx - font.width(name) / 2, y - 8, 0xFFFF5A3C, true);
        int hp = (int) Math.ceil(progress * Cerberus.MAX_HEALTH);
        String text = hp + " / " + (int) Cerberus.MAX_HEALTH;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 1, 0xFFFFD2B0, true);

        event.setIncrement(h + 24);
    }

    private static void drawGuardian(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 400;
        float sc = w / (float) GG_W;
        int h = Math.round(GG_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 14;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        int c0 = x + Math.round(GG_CH_X0 * sc), c1 = x + Math.round(GG_CH_X1 * sc);
        int y0 = y + Math.round(GG_CH_Y0 * sc), y1 = y + Math.round(GG_CH_Y1 * sc);
        int fillW = Math.round((c1 - c0) * progress);

        g.fill(c0, y0, c1, y1, 0xFF0C0618);                       // пустой канал
        if (fillW > 0) {                                          // фиолетовая полоса: чем меньше здоровья, тем короче
            g.blit(GUARD_FILL, c0, y0, fillW, y1 - y0, 0, 0,
                    Math.max(1, Math.round(GG_FILL_W * progress)), GG_FILL_H, GG_FILL_W, GG_FILL_H);
        }
        g.blit(GUARD_FRAME, x, y, w, h, 0, 0, GG_W, GG_H, GG_W, GG_H); // золотая рамка поверх

        Component name = boss.getName();
        g.drawString(font, name, cx - font.width(name) / 2, y - 8, 0xFFF6D77A, true);
        int hp = (int) Math.ceil(progress * GuardianOfGods.MAX_HEALTH);
        String text = hp + " / " + (int) GuardianOfGods.MAX_HEALTH;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 1, 0xFFFFF1C2, true);

        event.setIncrement(h + 24);
    }

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

    private static void drawFenrir(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss, boolean howling) {
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 420;
        float sc = w / (float) FN_W;
        int h = Math.round(FN_H * sc);
        int x = (g.guiWidth() - w) / 2;
        int y = event.getY() + 4;
        int cx = x + w / 2;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        int c0 = x + Math.round(FN_CH_X0 * sc), c1 = x + Math.round(FN_CH_X1 * sc);
        int y0 = y + Math.round(FN_CH_Y0 * sc), y1 = y + Math.round(FN_CH_Y1 * sc);
        int fillW = Math.round((c1 - c0) * progress);

        g.fill(c0, y0, c1, y1, 0xFF12060C);                       // пустой канал
        if (fillW > 0) {                                          // красная полоса: чем меньше здоровья, тем короче
            g.blit(FENRIR_FILL, c0, y0, fillW, y1 - y0, 0, 0,
                    Math.max(1, Math.round(FN_FILL_W * progress)), FN_FILL_H, FN_FILL_W, FN_FILL_H);
            if (howling) g.fill(c0, y0, c0 + fillW, y1, 0x70D0A8FF); // вой: неуязвим и лечится, полоса светится
        }
        int mid = (c0 + c1) / 2;                                  // отметка второй стадии (4000 из 8000)
        g.fill(mid, y0, mid + 1, y1, 0xA0FFD27A);

        // название нарисовано в самой рамке, поэтому рамку выбираем по языку клиента: русский -> Фенрир, остальные -> Fenrir
        boolean ru = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
        g.blit(ru ? FENRIR_FRAME : FENRIR_FRAME_EN, x, y, w, h, 0, 0, FN_W, FN_H, FN_W, FN_H); // рамка с именем поверх

        int max = (int) Fenrir.MAX_HEALTH;
        int hp = Math.round(progress * max);
        String text = hp + " / " + max;
        g.drawString(font, text, cx - font.width(text) / 2, y + h + 1, howling ? 0xFFD9C4FF : 0xFFFFB8C8, true);

        event.setIncrement(h + 16);
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
        if (tc.getKey().equals("entity.celestial.hades")) {
            drawHades(event, boss);
            return;
        }
        if (tc.getKey().equals("entity.celestial.cerberus")) {
            drawCerberus(event, boss);
            return;
        }
        if (tc.getKey().equals("entity.celestial.guardian_of_gods")) {
            drawGuardian(event, boss);
            return;
        }
        if (tc.getKey().equals("entity.celestial.celestia_god")) {
            drawGod(event, boss, false);
            return;
        }
        if (tc.getKey().equals("entity.celestial.celestia_god.immortal")) {
            drawGod(event, boss, true);
            return;
        }
        if (tc.getKey().equals("entity.celestial.all_knowing_angel")) {
            drawAngel(event, boss, false);
            return;
        }
        if (tc.getKey().equals("entity.celestial.all_knowing_angel.immortal")) {
            drawAngel(event, boss, true);
            return;
        }
        if (tc.getKey().equals("entity.celestial.fenrir")) {
            drawFenrir(event, boss, false);
            return;
        }
        if (tc.getKey().equals("entity.celestial.fenrir.howl")) {
            drawFenrir(event, boss, true);
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
