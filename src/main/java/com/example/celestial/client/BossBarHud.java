package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.GuardianOfGods;
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

/** Рисует полоску здоровья Хранителя Богов вместо ванильной. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class BossBarHud {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/boss_frame.png");
    private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/boss_fill.png");
    private static final int W = 300, H = 29;                 // размер рамки на экране
    private static final int TX = 19, TY = 10, TW = 261, TH = 11; // дорожка внутри рамки
    private static final int FILL_W = 523, FILL_H = 23;       // размер текстуры заливки

    private BossBarHud() {}

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)
                || !tc.getKey().equals("entity.celestial.guardian_of_gods")) return;

        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = (g.guiWidth() - W) / 2;
        int y = event.getY() + 10;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        // название на тёмной плашке с золотой окантовкой
        Component name = Component.translatable("entity.celestial.guardian_of_gods");
        int nameW = font.width(name);
        int cx = x + W / 2;
        g.fill(cx - nameW / 2 - 9, y - 14, cx + nameW / 2 + 9, y - 1, 0xFFC9A227);
        g.fill(cx - nameW / 2 - 8, y - 13, cx + nameW / 2 + 8, y - 2, 0xFF12162E);
        g.drawString(font, name, cx - nameW / 2, y - 11, 0xFFF6D77A, true);

        // рамка, заливка и числа
        g.blit(FRAME, x, y, W, H, 0, 0, 600, 58, 600, 58);
        int fw = Math.round(TW * progress);
        if (fw > 0) {
            g.blit(FILL, x + TX, y + TY, fw, TH, 0, 0, Math.max(1, Math.round(FILL_W * progress)), FILL_H, FILL_W, FILL_H);
        }
        int hp = (int) Math.ceil(progress * GuardianOfGods.MAX_HEALTH);
        String text = hp + " / " + (int) GuardianOfGods.MAX_HEALTH;
        g.drawString(font, text, x + TX + TW - font.width(text), y + H + 1, 0xFFFFFFFF, true);

        event.setIncrement(H + 26);
    }
}
