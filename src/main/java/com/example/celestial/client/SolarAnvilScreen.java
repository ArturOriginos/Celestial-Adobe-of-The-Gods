package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.menu.SolarAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI Солнечной наковальни: фон нарисован по референсу, слоты совпадают с рисунком. */
public class SolarAnvilScreen extends AbstractContainerScreen<SolarAnvilMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/solar_anvil.png");
    private static final int BROWN = 0xFF6B4A1E;

    public SolarAnvilScreen(SolarAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 183;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0, 0, 352, 366, 352, 366);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 15, BROWN, false);
        g.drawString(font, playerInventoryTitle, 10, 84, BROWN, false);
        int cost = menu.getCost();
        if (cost > 0) {
            boolean ok = minecraft != null && minecraft.player != null
                    && (minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= cost);
            Component text = Component.translatable("gui.celestial.solar_cost", cost);
            g.drawString(font, text, (imageWidth - font.width(text)) / 2, 76, ok ? 0xFF2E7D32 : 0xFFB71C1C, false);
        }
    }
}
