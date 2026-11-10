package com.example.celestial.network;

import com.example.celestial.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Обработчики пакетов на стороне клиента. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void handleMiniSoul(MiniSoulSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(payload.entityId());
            // на входе в мир собственный игрок может быть ещё не добавлен в уровень
            if (entity == null && mc.player != null && mc.player.getId() == payload.entityId()) {
                entity = mc.player;
            }
            if (entity instanceof Player player) {
                player.setData(ModAttachments.MINI_SOUL.get(), payload.value());
            }
        });
    }

    public static void handleSkyEyes(SkyEyesPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.example.celestial.client.SkyEyesRenderer.set(payload.on(), payload.animate()));
    }

    private static final int BOOK_LINES_PER_PAGE = 13;
    private static final int BOOK_PAGE_WIDTH = 114;

    /** Открывает Всезнающую книгу: текст из языкового файла (book.celestial.all_knowing_tome.story) режется на страницы. */
    public static void handleOpenTome(OpenTomePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            String text = Language.getInstance().getOrDefault("book.celestial.all_knowing_tome.story");
            List<Component> pages = new ArrayList<>();
            // части сюжета разделены символом \f: каждая часть начинается с новой страницы
            for (String section : text.split("\f")) {
                List<FormattedText> lines = mc.font.getSplitter().splitLines(FormattedText.of(section), BOOK_PAGE_WIDTH, Style.EMPTY);
                for (int i = 0; i < lines.size(); i += BOOK_LINES_PER_PAGE) {
                    StringBuilder page = new StringBuilder();
                    for (int j = i; j < Math.min(lines.size(), i + BOOK_LINES_PER_PAGE); j++) {
                        if (j > i) page.append('\n');
                        page.append(lines.get(j).getString());
                    }
                    pages.add(Component.literal(page.toString()));
                }
            }
            if (pages.isEmpty()) pages.add(Component.empty());
            mc.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        });
    }
}
