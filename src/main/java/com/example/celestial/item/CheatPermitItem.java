package com.example.celestial.item;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** «Официальное разрешение на читы»: ничего не делает, название переливается в серо-тёмных тонах. */
public class CheatPermitItem extends Item {
    public CheatPermitItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String text = Language.getInstance().getOrDefault(this.getDescriptionId());
        long t = Util.getMillis();
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double wave = 0.5D + 0.5D * Math.sin(t / 380.0D - i * 0.5D);
            int v = 0x36 + (int) (wave * (0xC4 - 0x36));            // от тёмно-серого до светло-серого
            int rgb = (v << 16) | (v << 8) | Math.min(255, v + 10);  // лёгкий холодный оттенок
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.celestial.official_cheat_permit.desc"));
    }
}
