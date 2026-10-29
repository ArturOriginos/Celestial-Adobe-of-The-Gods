package com.example.celestial.client;

import com.example.celestial.menu.SolarAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** GUI Солнечной наковальни (рисуется без внешней текстуры). */
public class SolarAnvilScreen extends AbstractContainerScreen<SolarAnvilMenu> {
    public SolarAnvilScreen(SolarAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 2, 0xFFFFFFFF);
        g.fill(leftPos + 1, topPos + 1, leftPos + 2, topPos + imageHeight - 1, 0xFFFFFFFF);

        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x - 1;
            int sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
        // золотая подсветка слотов ввода и результата
        for (int i = 0; i < 3; i++) {
            Slot s = menu.getSlot(i);
            g.fill(leftPos + s.x - 2, topPos + s.y - 2, leftPos + s.x + 18, topPos + s.y - 1, 0xFFE0A800);
            g.fill(leftPos + s.x - 2, topPos + s.y + 17, leftPos + s.x + 18, topPos + s.y + 18, 0xFFE0A800);
        }
        g.drawString(font, "+", leftPos + 55, topPos + 51, 0xFF404040, false);
        g.drawString(font, "->", leftPos + 105, topPos + 51, 0xFF404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int cost = menu.getCost();
        if (cost > 0) {
            boolean ok = minecraft != null && minecraft.player != null
                    && (minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= cost);
            g.drawString(font, Component.translatable("gui.celestial.solar_cost", cost),
                    8, 24, ok ? 0xFF2E7D32 : 0xFFB71C1C, false);
        }
    }
}
