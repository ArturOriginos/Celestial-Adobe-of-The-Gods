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
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Универсальный чертёж структуры мира Селестии (формат blueprintwand-1):
 * layers[y].rows[z].charAt(x) - ключ палитры, '.' - воздух. Читается из data/celestial/blueprints/&lt;имя&gt;.json.
 */
public final class CelestiaBlueprint {
    public record Entry(int x, int y, int z, BlockState state) {}

    private static final Map<String, CelestiaBlueprint> CACHE = new ConcurrentHashMap<>();

    public final String name;
    public final int sizeX, sizeY, sizeZ;
    public final List<Entry> blocks = new ArrayList<>();
    /** Воздух на высоте y >= 1: внутри здания расчищаем рельеф. */
    public final List<int[]> air = new ArrayList<>();

    private CelestiaBlueprint(String name, int sx, int sy, int sz) {
        this.name = name;
        this.sizeX = sx;
        this.sizeY = sy;
        this.sizeZ = sz;
    }

    public static CelestiaBlueprint get(String name) {
        return CACHE.computeIfAbsent(name, n -> load(n, true));
    }

    /** То же, но без списка воздуха (для больших чертежей, которые ставятся в пустое измерение). */
    public static CelestiaBlueprint getNoAir(String name) {
        return CACHE.computeIfAbsent(name + "#noair", n -> load(name, false));
    }

    private static CelestiaBlueprint load(String name, boolean keepAir) {
        String path = "/data/celestial/blueprints/" + name + ".json";
        try (InputStream in = CelestiaBlueprint.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Нет чертежа " + path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject size = root.getAsJsonObject("size");
            CelestiaBlueprint bp = new CelestiaBlueprint(name,
                    size.get("x").getAsInt(), size.get("y").getAsInt(), size.get("z").getAsInt());

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

            for (JsonElement le : root.getAsJsonArray("layers")) {
                JsonObject layer = le.getAsJsonObject();
                int y = layer.get("y").getAsInt();
                JsonArray rows = layer.getAsJsonArray("rows");
                for (int z = 0; z < rows.size(); z++) {
                    String row = rows.get(z).getAsString();
                    for (int x = 0; x < row.length(); x++) {
                        char c = row.charAt(x);
                        BlockState state = c == '.' ? Blocks.AIR.defaultBlockState()
                                : palette.getOrDefault(c, Blocks.AIR.defaultBlockState());
                        if (state.isAir()) {
                            if (y >= 1 && keepAir) bp.air.add(new int[]{x, y, z});
                        } else {
                            bp.blocks.add(new Entry(x, y, z, state));
                        }
                    }
                }
            }
            return bp;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать чертёж " + name, e);
        }
    }
}
