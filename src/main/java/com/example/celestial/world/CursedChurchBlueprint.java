package com.example.celestial.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Чертёж «Церкви Проклятых» (формат blueprintwand-1): layers[y].rows[z].charAt(x) - ключ палитры, '.' - воздух.
 * Читается один раз из data/celestial/blueprints/cursed_church.json.
 */
public final class CursedChurchBlueprint {
    /** Один непустой блок чертежа в локальных координатах. */
    public record Entry(int x, int y, int z, BlockState state) {}

    public static final int SIZE_X = 43, SIZE_Y = 22, SIZE_Z = 49;
    /** Где стоит Проклятый: центр главного зала (локальные координаты, на кирпичном возвышении). */
    public static final int BOSS_X = 21, BOSS_Y = 2, BOSS_Z = 15;

    private static final String PATH = "/data/celestial/blueprints/cursed_church.json";
    private static volatile CursedChurchBlueprint instance;

    public final List<Entry> blocks = new ArrayList<>();
    /** Локальные координаты воздуха (y >= 1): внутри здания расчищаем рельеф. */
    public final List<int[]> air = new ArrayList<>();

    private CursedChurchBlueprint() {}

    public static CursedChurchBlueprint get() {
        CursedChurchBlueprint bp = instance;
        if (bp == null) {
            synchronized (CursedChurchBlueprint.class) {
                bp = instance;
                if (bp == null) {
                    bp = load();
                    instance = bp;
                }
            }
        }
        return bp;
    }

    private static CursedChurchBlueprint load() {
        CursedChurchBlueprint bp = new CursedChurchBlueprint();
        try (InputStream in = CursedChurchBlueprint.class.getResourceAsStream(PATH)) {
            if (in == null) throw new IllegalStateException("Нет чертежа " + PATH);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            Map<Character, BlockState> palette = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("palette").entrySet()) {
                BlockState state;
                try {
                    state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), e.getValue().getAsString(), false)
                            .blockState();
                } catch (Exception ex) {
                    state = Blocks.AIR.defaultBlockState();
                }
                palette.put(e.getKey().charAt(0), state);
            }

            JsonArray layers = root.getAsJsonArray("layers");
            for (JsonElement le : layers) {
                JsonObject layer = le.getAsJsonObject();
                int y = layer.get("y").getAsInt();
                JsonArray rows = layer.getAsJsonArray("rows");
                for (int z = 0; z < rows.size(); z++) {
                    String row = rows.get(z).getAsString();
                    for (int x = 0; x < row.length(); x++) {
                        char c = row.charAt(x);
                        BlockState state = c == '.' ? Blocks.AIR.defaultBlockState() : palette.getOrDefault(c, Blocks.AIR.defaultBlockState());
                        if (state.isAir()) {
                            if (y >= 1) bp.air.add(new int[]{x, y, z});
                        } else {
                            bp.blocks.add(new Entry(x, y, z, state));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать чертёж Церкви Проклятых", e);
        }
        return bp;
    }
}
