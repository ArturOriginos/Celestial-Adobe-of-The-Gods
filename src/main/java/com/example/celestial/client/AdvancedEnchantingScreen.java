package com.example.celestial.client;

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
import net.minecraft.client.gui.components.Button;
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
 * GUI улучшенного стола зачарования. Рисуется без внешних текстур (только fill),
 * поэтому не требует PNG-файла интерфейса.
 *
 * Управление:
 *  - клик по названию: включить/выключить зачарование (уровень 1)
 *  - [+] / [-]: изменить уровень, Shift + [+] = максимум, Shift + [-] = убрать
 *  - колесо мыши: прокрутка списка
 */
public class AdvancedEnchantingScreen extends AbstractContainerScreen<AdvancedEnchantingMenu> {
    private static final int LIST_X = 30;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 192;
    private static final int ROW_H = 16;
    private static final int VISIBLE = 6;
    // зоны кнопок внутри строки (относительно левого края списка)
    private static final int MINUS_X0 = LIST_W - 58;
    private static final int MINUS_X1 = LIST_W - 44;
    private static final int PLUS_X0 = LIST_W - 16;
    private static final int PLUS_X1 = LIST_W - 2;

    private final Map<Holder<Enchantment>, Integer> selected = new LinkedHashMap<>();
    private List<Holder.Reference<Enchantment>> available = List.of();
    private ItemStack lastStack = ItemStack.EMPTY;
    private int scroll;
    private Button enchantButton;

    public AdvancedEnchantingScreen(AdvancedEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 232;
        this.inventoryLabelX = AdvancedEnchantingMenu.INV_LEFT;
        this.inventoryLabelY = 140;
    }

    @Override
    protected void init() {
        super.init();
        enchantButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.celestial.enchant"), b -> sendRequest())
                .bounds(leftPos + 148, topPos + 118, 74, 20)
                .build());
        enchantButton.active = false;
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
        enchantButton.active = cost > 0 && enough;
    }

    private void setLevel(Holder<Enchantment> ench, int level) {
        if (level <= 0) {
            selected.remove(ench);
            return;
        }
        // убираем выбранные зачарования, несовместимые с этим (Острота <-> Небесная кара и т.п.)
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
            int idx = rowAt(mx, my);
            if (idx >= 0) {
                Holder.Reference<Enchantment> ench = available.get(idx);
                double rx = mx - leftPos - LIST_X;
                boolean shift = hasShiftDown();
                if (rx >= MINUS_X0 && rx < MINUS_X1) {
                    change(ench, -1, shift);
                } else if (rx >= PLUS_X0 && rx < PLUS_X1) {
                    change(ench, +1, shift);
                } else {
                    setLevel(ench, selected.containsKey(ench) ? 0 : 1);
                }
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // панель
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 2, 0xFFFFFFFF);
        g.fill(leftPos + 1, topPos + 1, leftPos + 2, topPos + imageHeight - 1, 0xFFFFFFFF);

        // рамки слотов
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x - 1;
            int sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }

        // список зачарований
        int lx = leftPos + LIST_X;
        int ly = topPos + LIST_Y;
        g.fill(lx - 1, ly - 1, lx + LIST_W + 1, ly + VISIBLE * ROW_H + 1, 0xFF373737);
        g.fill(lx, ly, lx + LIST_W, ly + VISIBLE * ROW_H, 0xFF1E1E1E);

        ItemStack stack = currentStack();
        if (stack.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.celestial.insert_item"),
                    lx + LIST_W / 2, ly + VISIBLE * ROW_H / 2 - 4, 0xFFAAAAAA);
        } else if (available.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.celestial.nothing_available"),
                    lx + LIST_W / 2, ly + VISIBLE * ROW_H / 2 - 4, 0xFFFF5555);
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
            int bg = level > 0 ? 0xFF4B3A7A : (hovered ? 0xFF3C3C3C : 0xFF2A2A2A);
            g.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, bg);

            String name = ench.value().description().getString();
            if (have > 0) name += " (" + have + ")";
            name = font.plainSubstrByWidth(name, MINUS_X0 - 6);
            g.drawString(font, name, lx + 4, ry + 4, level > 0 ? 0xFFFFD700 : 0xFFFFFFFF, false);

            // [-]
            g.fill(lx + MINUS_X0, ry + 1, lx + MINUS_X1, ry + ROW_H - 2, 0xFF555555);
            g.drawCenteredString(font, "-", lx + (MINUS_X0 + MINUS_X1) / 2, ry + 4, 0xFFFFFFFF);
            // уровень
            g.drawCenteredString(font, level + "/" + max, lx + (MINUS_X1 + PLUS_X0) / 2, ry + 4,
                    level > 0 ? 0xFFFFD700 : 0xFF888888);
            // [+]
            g.fill(lx + PLUS_X0, ry + 1, lx + PLUS_X1, ry + ROW_H - 2, 0xFF555555);
            g.drawCenteredString(font, "+", lx + (PLUS_X0 + PLUS_X1) / 2, ry + 4, 0xFFFFFFFF);
        }

        // полоса прокрутки
        if (available.size() > VISIBLE) {
            int trackH = VISIBLE * ROW_H;
            int tx = lx + LIST_W + 2;
            g.fill(tx, ly, tx + 4, ly + trackH, 0xFF555555);
            int thumbH = Math.max(8, trackH * VISIBLE / available.size());
            int thumbY = (trackH - thumbH) * scroll / (available.size() - VISIBLE);
            g.fill(tx, ly + thumbY, tx + 4, ly + thumbY + thumbH, 0xFFDDDDDD);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int cost = EnchantLogic.cost(currentStack(), selected);
        int have = minecraft != null && minecraft.player != null ? minecraft.player.experienceLevel : 0;
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        int color = cost <= 0 ? 0xFF404040 : (creative || have >= cost ? 0xFF2E7D32 : 0xFFB71C1C);

        g.drawString(font, Component.translatable("gui.celestial.cost", cost, AEConfig.MAX_COST.get()),
                LIST_X, 120, color, false);
        g.drawString(font, Component.translatable("gui.celestial.you_have", have),
                LIST_X, 130, 0xFF404040, false);
    }
}
