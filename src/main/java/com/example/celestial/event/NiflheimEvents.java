package com.example.celestial.event;

import com.example.celestial.AEConfig;
import com.example.celestial.CelestialMod;
import com.example.celestial.world.NiflheimSpread;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * /niflheim start   - биом «Проклятый Нифельхельм» появляется вокруг вас в верхнем мире и начинает расползаться
 * /niflheim stop    - остановить распространение (уже заражённое остаётся)
 * /niflheim status  - сколько чанков заражено
 * Нужны права оператора (уровень 2).
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class NiflheimEvents {
    private NiflheimEvents() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("niflheim")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
                .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int start(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.translatable("commands.celestial.niflheim.wrong_dimension"));
            return 0;
        }
        ServerLevel overworld = source.getServer().overworld();
        NiflheimSpread data = NiflheimSpread.get(overworld);
        ChunkPos center = new ChunkPos(BlockPos.containing(source.getPosition()));
        // зародыш 3x3 чанка вокруг игрока, дальше биом расползается сам
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                data.infect(overworld, ChunkPos.asLong(center.x + dx, center.z + dz));
            }
        }
        data.setActive(true);
        source.sendSuccess(() -> Component.translatable("commands.celestial.niflheim.started", center.x, center.z), true);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        NiflheimSpread data = NiflheimSpread.get(source.getServer().overworld());
        data.setActive(false);
        source.sendSuccess(() -> Component.translatable("commands.celestial.niflheim.stopped"), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        NiflheimSpread data = NiflheimSpread.get(source.getServer().overworld());
        source.sendSuccess(() -> Component.translatable("commands.celestial.niflheim.status",
                data.infectedCount(), Component.translatable(data.isActive()
                        ? "commands.celestial.niflheim.yes" : "commands.celestial.niflheim.no")), false);
        return data.infectedCount();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        int interval = AEConfig.SPREAD_INTERVAL.get();
        if (com.example.celestial.world.WorldState.get(level.getServer()).cerberusSlain()) {
            interval = Math.max(1, interval / 2); // после гибели Цербера Нифельхельм распространяется вдвое быстрее
        }
        if (level.getGameTime() % interval != 0) return;
        NiflheimSpread.get(level).step(level);
    }

    /** Заражённый чанк, который не был загружен в момент заражения, меняется при загрузке. */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        NiflheimSpread data = NiflheimSpread.get(level);
        if (data.isPending(chunk.getPos())) {
            NiflheimSpread.corrupt(level, chunk, false);
            data.markDone(chunk.getPos());
        }
    }
}
