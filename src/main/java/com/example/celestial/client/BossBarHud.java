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
    private static final int W = 280, H = 87;            // размер рамки на экране
    private static final int IX = 37, IY = 55, IW = 207, IH = 9; // внутренняя дорожка
    private static final int FILL_W = 415, FILL_H = 18;

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
        int y = event.getY() - 10;
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));

        g.blit(FRAME, x, y, W, H, 0, 0, 560, 174, 560, 174);
        int fw = Math.round(IW * progress);
        if (fw > 0) {
            g.blit(FILL, x + IX, y + IY, fw, IH, 0, 0, Math.max(1, Math.round(FILL_W * progress)), FILL_H, FILL_W, FILL_H);
        }

        Component name = Component.translatable("entity.celestial.guardian_of_gods");
        int nameW = font.width(name);
        g.fill(x + W / 2 - nameW / 2 - 6, y + 34, x + W / 2 + nameW / 2 + 6, y + 47, 0xAA10142A);
        g.drawString(font, name, x + W / 2 - nameW / 2, y + 37, 0xFFF6D77A, true);

        int hp = (int) Math.ceil(progress * GuardianOfGods.MAX_HEALTH);
        String text = hp + " / " + (int) GuardianOfGods.MAX_HEALTH;
        g.drawString(font, text, x + IX + IW - font.width(text), y + IY + IH + 3, 0xFFFFFFFF, true);

        event.setIncrement(H - 24);
    }
}
