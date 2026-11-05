package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.world.CelestiaBlueprintPiece;
import com.example.celestial.world.CelestiaBlueprintStructure;
import com.example.celestial.world.CursedChurchPiece;
import com.example.celestial.world.CursedChurchStructure;
import com.example.celestial.world.GiantTreePiece;
import com.example.celestial.world.GiantTreeStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<GiantTreeStructure>> GIANT_TREE =
            STRUCTURE_TYPES.register("giant_celestial_tree",
                    () -> (StructureType<GiantTreeStructure>) () -> GiantTreeStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> GIANT_TREE_PIECE =
            PIECE_TYPES.register("giant_celestial_tree",
                    () -> (StructurePieceType.ContextlessType) GiantTreePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<CursedChurchStructure>> CURSED_CHURCH =
            STRUCTURE_TYPES.register("cursed_church",
                    () -> (StructureType<CursedChurchStructure>) () -> CursedChurchStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CURSED_CHURCH_PIECE =
            PIECE_TYPES.register("cursed_church",
                    () -> (StructurePieceType.ContextlessType) CursedChurchPiece::new);

    /** Универсальная структура из чертежа (Celestial Stash, Celestial Adobe). */
    public static final DeferredHolder<StructureType<?>, StructureType<CelestiaBlueprintStructure>> CELESTIA_BLUEPRINT =
            STRUCTURE_TYPES.register("celestia_blueprint",
                    () -> (StructureType<CelestiaBlueprintStructure>) () -> CelestiaBlueprintStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CELESTIA_BLUEPRINT_PIECE =
            PIECE_TYPES.register("celestia_blueprint",
                    () -> (StructurePieceType.ContextlessType) CelestiaBlueprintPiece::new);

    private ModStructures() {}
}
