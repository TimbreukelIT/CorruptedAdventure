package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.concurrent.CompletableFuture;

/**
 * Block tags. The wood set relies on vanilla's tag chains: {@code #minecraft:wooden_*}, {@code #planks} and
 * {@code #logs} are already in {@code #mineable/axe}, NeoForge's furnace fuels and {@code c:fences/wooden}.
 * Dead grass and withered dirt are deliberately not in {@code #animals_spawnable_on} (no animals on dead ground).
 */
final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFileHelper) {
        super(output, lookup, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Blocks.DEAD_LOGS).add(
                ModBlocks.DEAD_LOG.get(), ModBlocks.DEAD_WOOD.get(), ModBlocks.STRIPPED_DEAD_LOG.get(), ModBlocks.STRIPPED_DEAD_WOOD.get());
        tag(BlockTags.LOGS_THAT_BURN).addTag(ModTags.Blocks.DEAD_LOGS);
        tag(BlockTags.PLANKS).add(ModBlocks.DEAD_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.DEAD_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.DEAD_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.DEAD_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.DEAD_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.DEAD_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.DEAD_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.DEAD_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.DEAD_BUTTON.get());

        // #dirt: plants grow on it, carvers cut through it (#overworld_carver_replaceables), endermen pick it up.
        tag(BlockTags.DIRT).add(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.WITHERED_DIRT.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.WITHERED_DIRT.get());

        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_DEAD_LOG.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_DEAD_WOOD.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.DEAD_FENCE_GATE.get());

        // Soul crystal ores. The crimson variant is deliberately not in #nylium (no fungi or roots on it) and has no
        // ores_in_ground tag (there is no convention tag for nylium).
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SOUL_CRYSTAL_ORE.get(), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.SOUL_CRYSTAL_ORE.get(), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get());
        tag(ModTags.Blocks.ORES_SOUL_CRYSTAL).add(ModBlocks.SOUL_CRYSTAL_ORE.get(), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get());
        tag(Tags.Blocks.ORES).addTag(ModTags.Blocks.ORES_SOUL_CRYSTAL);
        tag(Tags.Blocks.ORE_RATES_SINGULAR).add(ModBlocks.SOUL_CRYSTAL_ORE.get(), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).add(ModBlocks.SOUL_CRYSTAL_ORE.get());

        ring2();
    }

    /**
     * Ring 2. Canyon stone is deliberately not in {@code #base_stone_overworld} (no grey blobs of granite, dirt or
     * gravel in the coloured strata; ring 2 picks its own ores through {@code canyon_stones}). Loose sand is not in
     * {@code #sand}: it is a ceiling block, not plant ground.
     */
    private void ring2() {
        tag(ModTags.Blocks.CANYON_STONES).add(
                ModBlocks.RED_CANYON_STONE.get(), ModBlocks.ORANGE_CANYON_STONE.get(), ModBlocks.CREAM_CANYON_STONE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(ModTags.Blocks.CANYON_STONES).add(
                ModBlocks.CANYON_COBBLESTONE.get(), ModBlocks.CANYON_COBBLESTONE_STAIRS.get(),
                ModBlocks.CANYON_COBBLESTONE_SLAB.get(), ModBlocks.CANYON_COBBLESTONE_WALL.get(),
                ModBlocks.CANYON_STONE_BRICKS.get(), ModBlocks.CANYON_STONE_BRICK_STAIRS.get(),
                ModBlocks.CANYON_STONE_BRICK_SLAB.get(), ModBlocks.CANYON_STONE_BRICK_WALL.get(),
                ModBlocks.SCORCHED_SANDSTONE.get(), ModBlocks.SCORCHED_SANDSTONE_STAIRS.get(),
                ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), ModBlocks.SCORCHED_SANDSTONE_WALL.get(),
                ModBlocks.CUT_SCORCHED_SANDSTONE.get(), ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(),
                ModBlocks.CHISELED_SCORCHED_SANDSTONE.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE.get(),
                ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.CRACKED_CLAY.get(), ModBlocks.LOOSE_SAND.get());
        tag(BlockTags.STAIRS).add(ModBlocks.CANYON_COBBLESTONE_STAIRS.get(), ModBlocks.CANYON_STONE_BRICK_STAIRS.get(),
                ModBlocks.SCORCHED_SANDSTONE_STAIRS.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.CANYON_COBBLESTONE_SLAB.get(), ModBlocks.CANYON_STONE_BRICK_SLAB.get(),
                ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(),
                ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.CANYON_COBBLESTONE_WALL.get(), ModBlocks.CANYON_STONE_BRICK_WALL.get(),
                ModBlocks.SCORCHED_SANDSTONE_WALL.get());
        // Caves, ravines and our unnatural tunnels cut through the natural ring 2 ground.
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).addTag(ModTags.Blocks.CANYON_STONES).add(
                ModBlocks.CANYON_COBBLESTONE.get(), ModBlocks.SCORCHED_SANDSTONE.get(), ModBlocks.CRACKED_CLAY.get(),
                ModBlocks.LOOSE_SAND.get());
        tag(Tags.Blocks.COBBLESTONES).add(ModBlocks.CANYON_COBBLESTONE.get());
        tag(Tags.Blocks.SANDSTONE_BLOCKS).add(ModBlocks.SCORCHED_SANDSTONE.get(), ModBlocks.CUT_SCORCHED_SANDSTONE.get(),
                ModBlocks.CHISELED_SCORCHED_SANDSTONE.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE.get());
        tag(Tags.Blocks.SANDSTONE_SLABS).add(ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(),
                ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get());
        tag(Tags.Blocks.SANDSTONE_STAIRS).add(ModBlocks.SCORCHED_SANDSTONE_STAIRS.get(),
                ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get());

        // Desert flora: one ground rule for all three plants (and later the palm sapling).
        tag(ModTags.Blocks.DESERT_PLANT_SOIL).addTag(BlockTags.SAND).addTag(BlockTags.TERRACOTTA).addTag(BlockTags.DIRT)
                .add(ModBlocks.CRACKED_CLAY.get());
        tag(BlockTags.DEAD_BUSH_MAY_PLACE_ON).add(ModBlocks.CRACKED_CLAY.get());
        // The dry shrub copies the dead bush's tags, the flower the poppy's; the cactus only what fits (no #replaceable).
        tag(BlockTags.REPLACEABLE).add(ModBlocks.DRY_SHRUB.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.DRY_SHRUB.get());
        tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.DRY_SHRUB.get());
        tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.DESERT_FLOWER.get());
        tag(BlockTags.ENDERMAN_HOLDABLE).add(ModBlocks.BARREL_CACTUS.get());
        tag(BlockTags.FLOWER_POTS).add(
                ModBlocks.POTTED_DRY_SHRUB.get(), ModBlocks.POTTED_BARREL_CACTUS.get(), ModBlocks.POTTED_DESERT_FLOWER.get(),
                ModBlocks.POTTED_PALM_SAPLING.get());

        palm();
        ring2Ores();
    }

    /**
     * Sunstone and turquoise ores, every host rock variant. No {@code ores_in_ground} tag (there is none for canyon stone
     * or sandstone) and not in {@code #overworld_carver_replaceables}: ore features run after the carvers.
     */
    private void ring2Ores() {
        tag(ModTags.Blocks.ORES_SUNSTONE).add(ModBlocks.RED_CANYON_SUNSTONE_ORE.get(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.get(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.get(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.get());
        tag(ModTags.Blocks.ORES_TURQUOISE).add(ModBlocks.RED_CANYON_TURQUOISE_ORE.get(),
                ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.get(), ModBlocks.CREAM_CANYON_TURQUOISE_ORE.get());
        tag(Tags.Blocks.ORES).addTag(ModTags.Blocks.ORES_SUNSTONE).addTag(ModTags.Blocks.ORES_TURQUOISE);
        tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(ModTags.Blocks.ORES_SUNSTONE).addTag(ModTags.Blocks.ORES_TURQUOISE);
        tag(BlockTags.NEEDS_IRON_TOOL).addTag(ModTags.Blocks.ORES_SUNSTONE).addTag(ModTags.Blocks.ORES_TURQUOISE);
        tag(Tags.Blocks.ORE_RATES_SINGULAR).addTag(ModTags.Blocks.ORES_SUNSTONE).addTag(ModTags.Blocks.ORES_TURQUOISE);
    }

    /**
     * The palm set, tagged like the dead wood set. {@code #leaves} gives the leaves sword mining, tree replaceability
     * ({@code #replaceable_by_trees}, needed by the palm tree feature) and parrot spawns through vanilla's tag chains;
     * {@code #mineable/hoe} lists vanilla's leaves one by one, so ours goes in by hand. {@code #logs} (via
     * {@code #logs_that_burn}) keeps the leaves from decaying.
     */
    private void palm() {
        tag(ModTags.Blocks.PALM_LOGS).add(
                ModBlocks.PALM_LOG.get(), ModBlocks.PALM_WOOD.get(), ModBlocks.STRIPPED_PALM_LOG.get(), ModBlocks.STRIPPED_PALM_WOOD.get());
        tag(BlockTags.LOGS_THAT_BURN).addTag(ModTags.Blocks.PALM_LOGS);
        tag(BlockTags.PLANKS).add(ModBlocks.PALM_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.PALM_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.PALM_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.PALM_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.PALM_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.PALM_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.PALM_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.PALM_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.PALM_BUTTON.get());
        tag(BlockTags.LEAVES).add(ModBlocks.PALM_LEAVES.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.PALM_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(ModBlocks.PALM_SAPLING.get());
        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_PALM_LOG.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_PALM_WOOD.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.PALM_FENCE_GATE.get());
    }
}
