package com.example.celestial.jei;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Категория JEI «Солнечная наковальня». */
public class SolarAnvilCategory implements IRecipeCategory<SolarAnvilJeiRecipe> {
    public static final RecipeType<SolarAnvilJeiRecipe> TYPE =
            RecipeType.create(MODID, "solar_anvil", SolarAnvilJeiRecipe.class);

    private static final int WIDTH = 124;
    private static final int HEIGHT = 44;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;

    public SolarAnvilCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.SOLAR_ANVIL_ITEM.get()));
    }

    @Override
    public RecipeType<SolarAnvilJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.celestial.solar_anvil");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SolarAnvilJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 8)
                .setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(recipe.tool());
        builder.addSlot(RecipeIngredientRole.CATALYST, 32, 8)
                .setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(recipe.catalyst());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 8)
                .setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(SolarAnvilJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, "+", 24, 12, 0xFF404040, false);
        graphics.drawString(font, "=>", 64, 12, 0xFF404040, false);
        Component cost = Component.translatable("gui.celestial.solar_cost", recipe.cost());
        graphics.drawString(font, cost, (WIDTH - font.width(cost)) / 2, 32, 0xFF404040, false);
    }
}
