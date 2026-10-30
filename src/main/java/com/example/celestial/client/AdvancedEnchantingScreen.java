package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.AEConfig;
import com.example.celestial.EnchantLogic;
import com.example.celestial.menu.AdvancedEnchantingMenu;
import com.example.celestial.network.EnchantRequestPayload;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI «Милость Богов Селестии»: мраморная рамка с золотом, список зачарований.
 * Управление: клик по названию - вкл/выкл; [+]/[-] - уровень (Shift: максимум/сброс); колесо - прокрутка.
 */
public class AdvancedEnchantingScreen extends AbstractContainerScreen<AdvancedEnchantingMenu> {
    private static final ResourceLocation FRAME =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/gui/frame.png");
    private static final int TW = 352, TH = 366, C = 28, D = 14;

    private static final int LIST_X = 38, LIST_Y = 32, LIST_W = 158, ROW_H = 14, VISIBLE = 6;
    private static final int MINUS_X0 = LIST_W - 54, MINUS_X1 = LIST_W - 42;
    private static final int PLUS_X0 = LIST_W - 16, PLUS_X1 = LIST_W - 4;
    private static final int BTN_X = 118, BTN_Y = 122, BTN_W = 76, BTN_H = 18;

    private static final int GOLD = 0xFFC9A227, BROWN = 0xFF6B4A1E;

    private final Map<Holder<Enchantment>, Integer> selected = new LinkedHashMap<>();
    private List<Holder.Reference<Enchantment>> available = List.of();
    private ItemStack lastStack = ItemStack.EMPTY;
    private int scroll;
    private boolean canEnchant;

    public AdvancedEnchantingScreen(AdvancedEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 208;
        this.imageHeight = 244;
    }

    // ---------------------------------------------------------------- логика
    private ItemStack currentStack() {
        return menu.getSlot(AdvancedEnchantingMenu.INPUT_SLOT).getItem();
    }

    private void rebuildAvailable(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || stack.isEmpty()) {
            available = List.of();
            return;
        }
        Registry<Enchantment> registry = mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        available = registry.holders()
                .filter(h -> EnchantLogic.canApply(h, stack))
                .sorted(Comparator.comparing((Holder.Reference<Enchantment> h) ->
                        h.value().description().getString().toLowerCase()))
                .toList();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack stack = currentStack();
        if (!ItemStack.matches(stack, lastStack)) {
            lastStack = stack.copy();
            selected.clear();
            scroll = 0;
            rebuildAvailable(stack);
        }
        int cost = EnchantLogic.cost(stack, selected);
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        boolean enough = creative || (minecraft != null && minecraft.player != null
                && minecraft.player.experienceLevel >= cost);
        canEnchant = cost > 0 && enough;
    }

    private void setLevel(Holder<Enchantment> ench, int level) {
        if (level <= 0) {
            selected.remove(ench);
            return;
        }
        selected.keySet().removeIf(other -> !other.equals(ench) && !Enchantment.areCompatible(other, ench));
        selected.put(ench, level);
    }

    private void change(Holder<Enchantment> ench, int delta, boolean big) {
        int max = EnchantLogic.maxLevel(ench);
        int cur = selected.getOrDefault(ench, 0);
        int next = big ? (delta > 0 ? max : 0) : Mth.clamp(cur + delta, 0, max);
        setLevel(ench, next);
    }

    private void sendRequest() {
        if (selected.isEmpty()) return;
        Map<ResourceLocation, Integer> request = new LinkedHashMap<>();
        selected.forEach((holder, level) ->
                holder.unwrapKey().ifPresent(key -> request.put(key.location(), level)));
        PacketDistributor.sendToServer(new EnchantRequestPayload(request));
        selected.clear();
    }

    private void clickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ----------------------------------------------------------------- ввод
    private int rowAt(double mx, double my) {
        double rx = mx - leftPos - LIST_X;
        double ry = my - topPos - LIST_Y;
        if (rx < 0 || rx >= LIST_W || ry < 0 || ry >= VISIBLE * ROW_H) return -1;
        int idx = scroll + (int) (ry / ROW_H);
        return idx < available.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (mx >= leftPos + BTN_X && mx < leftPos + BTN_X + BTN_W
                    && my >= topPos + BTN_Y && my < topPos + BTN_Y + BTN_H) {
                if (canEnchant) {
                    clickSound();
                    sendRequest();
                }
                return true;
            }
            int idx = rowAt(mx, my);
            if (idx >= 0) {
                Holder.Reference<Enchantment> ench = available.get(idx);
                double rx = mx - leftPos - LIST_X;
                boolean shift = hasShiftDown();
                if (rx >= MINUS_X0 && rx < MINUS_X1) change(ench, -1, shift);
                else if (rx >= PLUS_X0 && rx < PLUS_X1) change(ench, +1, shift);
                else setLevel(ench, selected.containsKey(ench) ? 0 : 1);
                clickSound();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        double rx = mx - leftPos - LIST_X;
        double ry = my - topPos - LIST_Y;
        if (rx >= 0 && rx < LIST_W + 8 && ry >= 0 && ry < VISIBLE * ROW_H) {
            int maxScroll = Math.max(0, available.size() - VISIBLE);
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    // ------------------------------------------------------------- рендер
    /** Рамка из текстуры (9-slice): углы фиксированы, края и центр растягиваются. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.blit(FRAME, x, y, D, D, 0, 0, C, C, TW, TH);
        g.blit(FRAME, x + w - D, y, D, D, TW - C, 0, C, C, TW, TH);
        g.blit(FRAME, x, y + h - D, D, D, 0, TH - C, C, C, TW, TH);
        g.blit(FRAME, x + w - D, y + h - D, D, D, TW - C, TH - C, C, C, TW, TH);
        g.blit(FRAME, x + D, y, w - 2 * D, D, C, 0, TW - 2 * C, C, TW, TH);
        g.blit(FRAME, x + D, y + h - D, w - 2 * D, D, C, TH - C, TW - 2 * C, C, TW, TH);
        g.blit(FRAME, x, y + D, D, h - 2 * D, 0, C, C, TH - 2 * C, TW, TH);
        g.blit(FRAME, x + w - D, y + D, D, h - 2 * D, TW - C, C, C, TH - 2 * C, TW, TH);
        g.blit(FRAME, x + D, y + D, w - 2 * D, h - 2 * D, C, C, TW - 2 * C, TH - 2 * C, TW, TH);
    }

    private static void box(GuiGraphics g, int x0, int y0, int x1, int y1, int border, int fill) {
        g.fill(x0, y0, x1, y1, border);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        // картуш с названием
        box(g, leftPos + 12, topPos + 8, leftPos + imageWidth - 12, topPos + 26, GOLD, 0xFFF3E9D2);

        // слоты
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x, sy = topPos + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFE8E2D0);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF5C6373);
        }
        Slot in = menu.getSlot(AdvancedEnchantingMenu.INPUT_SLOT);
        box(g, leftPos + in.x - 2, topPos + in.y - 2, leftPos + in.x + 18, topPos + in.y + 18, GOLD, 0xFF5C6373);

        // список зачарований
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        box(g, lx - 2, ly - 2, lx + LIST_W + 2, ly + VISIBLE * ROW_H + 2, GOLD, 0xFF171B38);

        ItemStack stack = currentStack();
        if (stack.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.celestial.insert_item"),
                    lx + LIST_W / 2, ly + VISIBLE * ROW_H / 2 - 4, 0xFFBFC6E8);
        } else if (available.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.celestial.nothing_available"),
                    lx + LIST_W / 2, ly + VISIBLE * ROW_H / 2 - 4, 0xFFFF7777);
        }

        for (int i = 0; i < VISIBLE; i++) {
            int idx = scroll + i;
            if (idx >= available.size()) break;
            Holder.Reference<Enchantment> ench = available.get(idx);
            int ry = ly + i * ROW_H;
            int level = selected.getOrDefault(ench, 0);
            int max = EnchantLogic.maxLevel(ench);
            int have = EnchantLogic.existing(stack).getLevel(ench);

            boolean hovered = mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            g.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, level > 0 ? 0xFF4B3A7A : (hovered ? 0xFF2A3166 : 0xFF1F2548));

            String name = ench.value().description().getString();
            if (have > 0) name += " (" + have + ")";
            name = font.plainSubstrByWidth(name, MINUS_X0 - 6);
            g.drawString(font, name, lx + 3, ry + 3, level > 0 ? 0xFFFFD86B : 0xFFF2E6C4, false);

            g.fill(lx + MINUS_X0, ry + 1, lx + MINUS_X1, ry + ROW_H - 2, 0xFF8A6B1E);
            g.drawCenteredString(font, "-", lx + (MINUS_X0 + MINUS_X1) / 2, ry + 3, 0xFFFFFFFF);
            g.drawCenteredString(font, level + "/" + max, lx + (MINUS_X1 + PLUS_X0) / 2, ry + 3,
                    level > 0 ? 0xFFFFD86B : 0xFF8F95B8);
            g.fill(lx + PLUS_X0, ry + 1, lx + PLUS_X1, ry + ROW_H - 2, 0xFF8A6B1E);
            g.drawCenteredString(font, "+", lx + (PLUS_X0 + PLUS_X1) / 2, ry + 3, 0xFFFFFFFF);
        }
        if (available.size() > VISIBLE) {
            int trackH = VISIBLE * ROW_H, tx = lx + LIST_W + 3;
            g.fill(tx, ly, tx + 3, ly + trackH, 0xFF3B3F63);
            int thumbH = Math.max(8, trackH * VISIBLE / available.size());
            int thumbY = (trackH - thumbH) * scroll / (available.size() - VISIBLE);
            g.fill(tx, ly + thumbY, tx + 3, ly + thumbY + thumbH, GOLD);
        }

        // кнопка "Зачаровать"
        int bx = leftPos + BTN_X, by = topPos + BTN_Y;
        boolean btnHover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
        int fill = !canEnchant ? 0xFF9A9486 : (btnHover ? 0xFFF3D36A : 0xFFE3BC42);
        box(g, bx, by, bx + BTN_W, by + BTN_H, !canEnchant ? 0xFF6F6A5E : 0xFFA9811C, fill);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 13, BROWN, false);

        int cost = EnchantLogic.cost(currentStack(), selected);
        int have = minecraft != null && minecraft.player != null ? minecraft.player.experienceLevel : 0;
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        int color = cost <= 0 ? BROWN : (creative || have >= cost ? 0xFF2E7D32 : 0xFFB71C1C);
        g.drawString(font, Component.translatable("gui.celestial.cost", cost, AEConfig.MAX_COST.get()), 14, 123, color, false);
        g.drawString(font, Component.translatable("gui.celestial.you_have", have), 14, 134, BROWN, false);

        Component btn = Component.translatable("gui.celestial.enchant");
        g.drawString(font, btn, BTN_X + (BTN_W - font.width(btn)) / 2, BTN_Y + 5, canEnchant ? 0xFF4A2F08 : 0xFF3F3C34, false);
        g.drawString(font, playerInventoryTitle, 23, 146, BROWN, false);
    }
}
