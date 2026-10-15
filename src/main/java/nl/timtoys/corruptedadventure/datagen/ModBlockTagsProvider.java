package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
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
        netherRing2();
        ring3();
        netherRing3();
        ring4();
    }

    /**
     * Ring 4. Tidestone is natural ground that carvers cut through, and the host rock of the ring 4 ores
     * ({@code ring_4_stones}); like pumice deliberately not in {@code #base_stone_overworld}. The moonpetal set and its
     * withered trunk burn ({@code #logs_that_burn}), tagged like the palm set. Peat is {@code #dirt}: plants grow on it.
     */
    private void ring4() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.TIDESTONE.get(), ModBlocks.TIDESTONE_STAIRS.get(),
                ModBlocks.TIDESTONE_SLAB.get(), ModBlocks.TIDESTONE_WALL.get(), ModBlocks.COBBLED_TIDESTONE.get(),
                ModBlocks.COBBLED_TIDESTONE_STAIRS.get(), ModBlocks.COBBLED_TIDESTONE_SLAB.get(), ModBlocks.COBBLED_TIDESTONE_WALL.get(),
                ModBlocks.POLISHED_TIDESTONE.get(), ModBlocks.POLISHED_TIDESTONE_STAIRS.get(), ModBlocks.POLISHED_TIDESTONE_SLAB.get(),
                ModBlocks.POLISHED_TIDESTONE_WALL.get(), ModBlocks.TIDESTONE_BRICKS.get(), ModBlocks.TIDESTONE_BRICK_STAIRS.get(),
                ModBlocks.TIDESTONE_BRICK_SLAB.get(), ModBlocks.TIDESTONE_BRICK_WALL.get(), ModBlocks.BUBBLE_SPRING.get());
        tag(BlockTags.STAIRS).add(ModBlocks.TIDESTONE_STAIRS.get(), ModBlocks.COBBLED_TIDESTONE_STAIRS.get(),
                ModBlocks.POLISHED_TIDESTONE_STAIRS.get(), ModBlocks.TIDESTONE_BRICK_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.TIDESTONE_SLAB.get(), ModBlocks.COBBLED_TIDESTONE_SLAB.get(),
                ModBlocks.POLISHED_TIDESTONE_SLAB.get(), ModBlocks.TIDESTONE_BRICK_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.TIDESTONE_WALL.get(), ModBlocks.COBBLED_TIDESTONE_WALL.get(),
                ModBlocks.POLISHED_TIDESTONE_WALL.get(), ModBlocks.TIDESTONE_BRICK_WALL.get());
        tag(Tags.Blocks.COBBLESTONES).add(ModBlocks.COBBLED_TIDESTONE.get());
        tag(ModTags.Blocks.RING_4_STONES).add(ModBlocks.TIDESTONE.get());
        // Peat reaches the carvers through #dirt.
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.TIDESTONE.get());

        tag(ModTags.Blocks.MOONPETAL_LOGS).add(ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_MOONPETAL_WOOD.get());
        tag(ModTags.Blocks.WITHERED_MOONPETAL_LOGS).add(ModBlocks.WITHERED_MOONPETAL_LOG.get(), ModBlocks.WITHERED_MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get());
        tag(BlockTags.LOGS_THAT_BURN).addTag(ModTags.Blocks.MOONPETAL_LOGS).addTag(ModTags.Blocks.WITHERED_MOONPETAL_LOGS);
        tag(BlockTags.PLANKS).add(ModBlocks.MOONPETAL_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.MOONPETAL_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.MOONPETAL_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.MOONPETAL_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.MOONPETAL_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.MOONPETAL_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.MOONPETAL_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.MOONPETAL_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.MOONPETAL_BUTTON.get());
        tag(BlockTags.LEAVES).add(ModBlocks.MOONPETAL_LEAVES.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.MOONPETAL_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(ModBlocks.MOONPETAL_SAPLING.get());
        tag(BlockTags.FLOWER_POTS).add(ModBlocks.POTTED_MOONPETAL_SAPLING.get());
        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_MOONPETAL_WOOD.get(), ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.MOONPETAL_FENCE_GATE.get());

        tag(BlockTags.DIRT).add(ModBlocks.PEAT.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PEAT.get());

        // The hanging moss and roots copy vanilla hanging roots' tags; the glowing flora is plants and fungi.
        tag(BlockTags.REPLACEABLE).add(ModBlocks.HANGING_SWAMP_MOSS.get(), ModBlocks.GLOWING_HANGING_ROOTS.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.HANGING_SWAMP_MOSS.get(), ModBlocks.GLOWING_HANGING_ROOTS.get());
        tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.HANGING_SWAMP_MOSS.get(), ModBlocks.GLOWING_HANGING_ROOTS.get(),
                ModBlocks.GLOWCAP.get(), ModBlocks.BLIGHTED_GLOWCAP.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.HANGING_SWAMP_MOSS.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.GLOWING_HANGING_ROOTS.get());
        // Climbing aid in all of ring 4 (verdant_falls Q3 = B): both are walk-through, so they climb like weeping vines.
        tag(BlockTags.CLIMBABLE).add(ModBlocks.HANGING_SWAMP_MOSS.get(), ModBlocks.GLOWING_HANGING_ROOTS.get());
        tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.GLOW_BLOOM.get());

        ring4Ores();
    }

    /**
     * Ring 4 ores: a diamond pickaxe for both (decision V3 golf 1). Not carver-replaceable: ore features run after the
     * carvers; {@code ores_in_ground} per host, with our own {@code c:ores_in_ground/tidestone}. The crystals carry
     * amethyst's tags; the budding blocks are growth, deliberately in no ground or carver tag.
     */
    private void ring4Ores() {
        tag(ModTags.Blocks.ORES_TIDESILVER).add(ModBlocks.TIDESILVER_ORE.get(), ModBlocks.TIDESTONE_TIDESILVER_ORE.get(),
                ModBlocks.DEEPSLATE_TIDESILVER_ORE.get());
        tag(ModTags.Blocks.ORES_JADE).add(ModBlocks.JADE_ORE.get(), ModBlocks.TIDESTONE_JADE_ORE.get(), ModBlocks.DEEPSLATE_JADE_ORE.get());
        tag(Tags.Blocks.ORES).addTag(ModTags.Blocks.ORES_TIDESILVER).addTag(ModTags.Blocks.ORES_JADE);
        tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(ModTags.Blocks.ORES_TIDESILVER).addTag(ModTags.Blocks.ORES_JADE);
        tag(BlockTags.NEEDS_DIAMOND_TOOL).addTag(ModTags.Blocks.ORES_TIDESILVER).addTag(ModTags.Blocks.ORES_JADE);
        tag(Tags.Blocks.ORE_RATES_SINGULAR).addTag(ModTags.Blocks.ORES_TIDESILVER).addTag(ModTags.Blocks.ORES_JADE);
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(ModBlocks.TIDESILVER_ORE.get(), ModBlocks.JADE_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(ModBlocks.DEEPSLATE_TIDESILVER_ORE.get(), ModBlocks.DEEPSLATE_JADE_ORE.get());
        tag(ModTags.Blocks.ORES_IN_GROUND_TIDESTONE).add(ModBlocks.TIDESTONE_TIDESILVER_ORE.get(), ModBlocks.TIDESTONE_JADE_ORE.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.TOXIC_CRYSTAL_BLOCK.get(), ModBlocks.BUDDING_TOXIC_CRYSTAL.get(),
                ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get(), ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get(),
                ModBlocks.TOXIC_CRYSTAL_CLUSTER.get(), ModBlocks.AQUAMARINE_BLOCK.get(), ModBlocks.BUDDING_AQUAMARINE.get(),
                ModBlocks.SMALL_AQUAMARINE_BUD.get(), ModBlocks.MEDIUM_AQUAMARINE_BUD.get(), ModBlocks.LARGE_AQUAMARINE_BUD.get(),
                ModBlocks.AQUAMARINE_CLUSTER.get());
        tag(BlockTags.CRYSTAL_SOUND_BLOCKS).add(ModBlocks.TOXIC_CRYSTAL_BLOCK.get(), ModBlocks.BUDDING_TOXIC_CRYSTAL.get(),
                ModBlocks.AQUAMARINE_BLOCK.get(), ModBlocks.BUDDING_AQUAMARINE.get());
        tag(Tags.Blocks.BUDDING_BLOCKS).add(ModBlocks.BUDDING_TOXIC_CRYSTAL.get(), ModBlocks.BUDDING_AQUAMARINE.get());
        tag(Tags.Blocks.BUDS).add(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get(), ModBlocks.SMALL_AQUAMARINE_BUD.get(), ModBlocks.MEDIUM_AQUAMARINE_BUD.get(),
                ModBlocks.LARGE_AQUAMARINE_BUD.get());
        tag(Tags.Blocks.CLUSTERS).add(ModBlocks.TOXIC_CRYSTAL_CLUSTER.get(), ModBlocks.AQUAMARINE_CLUSTER.get());

        netherRing4();
    }

    /**
     * Nether ring 4. Bleached netherrack is Nether ground ({@code #base_stone_nether}) and netherrack in every other way
     * too, fire included
     * ({@code #infiniburn_overworld}, which the Nether's infiniburn tag contains). Bone sand is carver-replaceable ground,
     * but deliberately not {@code #sand} (no cactus or sugar cane in the Nether). The purity crystal carries amethyst's
     * tags.
     */
    private void netherRing4() {
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.BONE_SAND.get());
        tag(BlockTags.NETHER_CARVER_REPLACEABLES).add(ModBlocks.BONE_SAND.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BLEACHED_NETHERRACK.get(), ModBlocks.FOSSIL_STONE.get(),
                ModBlocks.FOSSIL_STONE_STAIRS.get(), ModBlocks.FOSSIL_STONE_SLAB.get(), ModBlocks.FOSSIL_STONE_WALL.get(),
                ModBlocks.POLISHED_FOSSIL_STONE.get(), ModBlocks.POLISHED_FOSSIL_STONE_STAIRS.get(),
                ModBlocks.POLISHED_FOSSIL_STONE_SLAB.get(), ModBlocks.POLISHED_FOSSIL_STONE_WALL.get(),
                ModBlocks.FOSSIL_STONE_BRICKS.get(), ModBlocks.FOSSIL_STONE_BRICK_STAIRS.get(), ModBlocks.FOSSIL_STONE_BRICK_SLAB.get(),
                ModBlocks.FOSSIL_STONE_BRICK_WALL.get(), ModBlocks.PURITY_CRYSTAL_BLOCK.get(), ModBlocks.PURITY_CRYSTAL_CLUSTER.get(),
                ModBlocks.PURITY_CRYSTAL_LAMP.get());
        tag(BlockTags.STAIRS).add(ModBlocks.FOSSIL_STONE_STAIRS.get(), ModBlocks.POLISHED_FOSSIL_STONE_STAIRS.get(),
                ModBlocks.FOSSIL_STONE_BRICK_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.FOSSIL_STONE_SLAB.get(), ModBlocks.POLISHED_FOSSIL_STONE_SLAB.get(),
                ModBlocks.FOSSIL_STONE_BRICK_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.FOSSIL_STONE_WALL.get(), ModBlocks.POLISHED_FOSSIL_STONE_WALL.get(),
                ModBlocks.FOSSIL_STONE_BRICK_WALL.get());
        // Fossil stone stays out of #base_stone_nether (review 68): no vanilla Nether ore or debris blobs in the fossil
        // terrain. The carvers still cut through it.
        tag(BlockTags.BASE_STONE_NETHER).add(ModBlocks.BLEACHED_NETHERRACK.get());
        tag(BlockTags.NETHER_CARVER_REPLACEABLES).add(ModBlocks.FOSSIL_STONE.get());
        tag(Tags.Blocks.NETHERRACKS).add(ModBlocks.BLEACHED_NETHERRACK.get());
        tag(BlockTags.INFINIBURN_OVERWORLD).add(ModBlocks.BLEACHED_NETHERRACK.get());

        tag(BlockTags.CRYSTAL_SOUND_BLOCKS).add(ModBlocks.PURITY_CRYSTAL_BLOCK.get(), ModBlocks.PURITY_CRYSTAL_LAMP.get());
        tag(Tags.Blocks.CLUSTERS).add(ModBlocks.PURITY_CRYSTAL_CLUSTER.get());
    }

    /**
     * Nether ring 3. Cold lava and rime netherrack are Nether ground like basalt and netherrack ({@code #base_stone_nether},
     * so Nether carvers cut through them); rime netherrack is not in the {@code #infiniburn_*} tags (fire dies on frozen
     * ground) nor in {@code #nylium}. The Nether ice is in every ice tag of packed ice, and carvers cut through it;
     * primordial ice gets blue ice's tags and is deliberately not carver-replaceable, so caves never eat the pillar cores
     * (nor a growth: it is terrain for {@code FungalTerrainScan}). The rimeshroom set is Nether wood: in {@code #logs} (via
     * its own tag) but not in {@code #logs_that_burn}.
     */
    private void netherRing3() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.COLD_LAVA.get(), ModBlocks.COLD_LAVA_STAIRS.get(),
                ModBlocks.COLD_LAVA_SLAB.get(), ModBlocks.COLD_LAVA_WALL.get(), ModBlocks.POLISHED_COLD_LAVA.get(),
                ModBlocks.POLISHED_COLD_LAVA_STAIRS.get(), ModBlocks.POLISHED_COLD_LAVA_SLAB.get(),
                ModBlocks.POLISHED_COLD_LAVA_WALL.get(), ModBlocks.COLD_LAVA_BRICKS.get(), ModBlocks.COLD_LAVA_BRICK_STAIRS.get(),
                ModBlocks.COLD_LAVA_BRICK_SLAB.get(), ModBlocks.COLD_LAVA_BRICK_WALL.get(), ModBlocks.NETHER_ICE.get(),
                ModBlocks.PACKED_NETHER_ICE.get(), ModBlocks.RIME_NETHERRACK.get(), ModBlocks.ICE_STALACTITE.get(),
                ModBlocks.GLOWING_FISSURE.get(), ModBlocks.PRIMORDIAL_ICE.get());
        tag(BlockTags.STAIRS).add(ModBlocks.COLD_LAVA_STAIRS.get(), ModBlocks.POLISHED_COLD_LAVA_STAIRS.get(),
                ModBlocks.COLD_LAVA_BRICK_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.COLD_LAVA_SLAB.get(), ModBlocks.POLISHED_COLD_LAVA_SLAB.get(),
                ModBlocks.COLD_LAVA_BRICK_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.COLD_LAVA_WALL.get(), ModBlocks.POLISHED_COLD_LAVA_WALL.get(),
                ModBlocks.COLD_LAVA_BRICK_WALL.get());
        tag(BlockTags.BASE_STONE_NETHER).add(ModBlocks.COLD_LAVA.get(), ModBlocks.RIME_NETHERRACK.get());
        tag(Tags.Blocks.NETHERRACKS).add(ModBlocks.RIME_NETHERRACK.get());
        // The ice tags of packed ice (Nether ice) and blue ice (primordial ice).
        tag(BlockTags.ICE).add(ModBlocks.NETHER_ICE.get(), ModBlocks.PACKED_NETHER_ICE.get(), ModBlocks.PRIMORDIAL_ICE.get());
        tag(BlockTags.GEODE_INVALID_BLOCKS).add(ModBlocks.NETHER_ICE.get(), ModBlocks.PACKED_NETHER_ICE.get(),
                ModBlocks.PRIMORDIAL_ICE.get());
        tag(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON).add(ModBlocks.NETHER_ICE.get(), ModBlocks.PACKED_NETHER_ICE.get());
        tag(BlockTags.SNAPS_GOAT_HORN).add(ModBlocks.NETHER_ICE.get(), ModBlocks.PACKED_NETHER_ICE.get());
        tag(BlockTags.NETHER_CARVER_REPLACEABLES).add(ModBlocks.NETHER_ICE.get(), ModBlocks.PACKED_NETHER_ICE.get());

        tag(ModTags.Blocks.RIMESHROOM_STEMS).add(ModBlocks.RIMESHROOM_STEM.get(), ModBlocks.RIMESHROOM_HYPHAE.get(),
                ModBlocks.STRIPPED_RIMESHROOM_STEM.get(), ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get());
        tag(BlockTags.LOGS).addTag(ModTags.Blocks.RIMESHROOM_STEMS);
        tag(BlockTags.PLANKS).add(ModBlocks.RIMESHROOM_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.RIMESHROOM_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.RIMESHROOM_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.RIMESHROOM_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.RIMESHROOM_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.RIMESHROOM_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.RIMESHROOM_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.RIMESHROOM_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.RIMESHROOM_BUTTON.get());
        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_RIMESHROOM_STEM.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.RIMESHROOM_FENCE_GATE.get());
        // Vanilla lists its wart blocks in #mineable/hoe one by one, so the cap goes in by hand.
        tag(BlockTags.WART_BLOCKS).add(ModBlocks.RIMESHROOM_CAP.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.RIMESHROOM_CAP.get());
        // Growths, not terrain, for the Nether features' determinism rule (FungalTerrainScan), like the dripshroom set.
        tag(ModTags.Blocks.FUNGAL_GROWTHS).addTag(ModTags.Blocks.RIMESHROOM_STEMS).add(ModBlocks.RIMESHROOM_CAP.get(),
                ModBlocks.ICE_STALACTITE.get());

        // The glowing fissure warms against the Nether cold and heats like the ring 3 heat sources (contract §3).
        tag(ModTags.Blocks.HEAT_SOURCES).add(ModBlocks.GLOWING_FISSURE.get());
        tag(ModTags.Blocks.WARMTH_SOURCES).add(ModBlocks.GLOWING_FISSURE.get());
    }

    /**
     * Ring 3. Ash and pumice are natural ground that carvers cut through, deliberately not in
     * {@code #base_stone_overworld} (ring 3 picks its own ore hosts through {@code ring_3_stones}). The ash layer is in
     * {@code #replaceable} like snow, but in no snow tag. Charred wood is Nether wood: in {@code #logs} (via its own
     * tag) but not in {@code #logs_that_burn}.
     */
    private void ring3() {
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.ASH_BLOCK.get(), ModBlocks.ASH_LAYER.get());
        tag(BlockTags.REPLACEABLE).add(ModBlocks.ASH_LAYER.get());
        // Like snow: walking through thin ash sounds like ash, not like the block under it.
        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(ModBlocks.ASH_LAYER.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.PUMICE.get(), ModBlocks.PUMICE_STAIRS.get(),
                ModBlocks.PUMICE_SLAB.get(), ModBlocks.PUMICE_WALL.get(), ModBlocks.POLISHED_PUMICE.get(),
                ModBlocks.POLISHED_PUMICE_STAIRS.get(), ModBlocks.POLISHED_PUMICE_SLAB.get(), ModBlocks.POLISHED_PUMICE_WALL.get(),
                ModBlocks.PUMICE_BRICKS.get(), ModBlocks.PUMICE_BRICK_STAIRS.get(), ModBlocks.PUMICE_BRICK_SLAB.get(),
                ModBlocks.PUMICE_BRICK_WALL.get(), ModBlocks.SMOLDERING_STONE.get());
        tag(BlockTags.STAIRS).add(ModBlocks.PUMICE_STAIRS.get(), ModBlocks.POLISHED_PUMICE_STAIRS.get(),
                ModBlocks.PUMICE_BRICK_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.PUMICE_SLAB.get(), ModBlocks.POLISHED_PUMICE_SLAB.get(), ModBlocks.PUMICE_BRICK_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.PUMICE_WALL.get(), ModBlocks.POLISHED_PUMICE_WALL.get(), ModBlocks.PUMICE_BRICK_WALL.get());
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(),
                ModBlocks.SMOLDERING_STONE.get());
        tag(ModTags.Blocks.RING_3_STONES).add(Blocks.BASALT, Blocks.BLACKSTONE, ModBlocks.PUMICE.get(), ModBlocks.ASH_BLOCK.get());
        // Goats only spawn where a biome lists them (volcanic_peaks); this tag only says on what ground: the peak surface,
        // vanilla basalt, smooth basalt and blackstone (39b) plus pumice and ash (39c). Not smouldering stone (a heat source).
        tag(BlockTags.GOATS_SPAWNABLE_ON).add(Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE,
                ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get());

        // Heat (ring 3) and warmth (Nether ring 3) within a few blocks; soul fire is deliberately no heat source.
        tag(ModTags.Blocks.HEAT_SOURCES).add(Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.FIRE, Blocks.CAMPFIRE,
                ModBlocks.SMOLDERING_STONE.get());
        tag(ModTags.Blocks.WARMTH_SOURCES).add(Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CAMPFIRE,
                Blocks.SOUL_CAMPFIRE, Blocks.MAGMA_BLOCK, ModBlocks.SMOLDERING_STONE.get());

        tag(ModTags.Blocks.CHARRED_LOGS).add(ModBlocks.CHARRED_LOG.get(), ModBlocks.CHARRED_WOOD.get(),
                ModBlocks.STRIPPED_CHARRED_LOG.get(), ModBlocks.STRIPPED_CHARRED_WOOD.get());
        tag(BlockTags.LOGS).addTag(ModTags.Blocks.CHARRED_LOGS);
        tag(BlockTags.PLANKS).add(ModBlocks.CHARRED_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.CHARRED_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.CHARRED_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.CHARRED_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.CHARRED_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.CHARRED_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.CHARRED_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.CHARRED_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.CHARRED_BUTTON.get());
        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_CHARRED_LOG.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_CHARRED_WOOD.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.CHARRED_FENCE_GATE.get());

        // Ash crust caves in like loose sand; the smouldering ash is cast material (features), no natural ground.
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.ASH_CRUST.get(), ModBlocks.SMOLDERING_ASH.get());
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.ASH_CRUST.get());
        tag(ModTags.Blocks.COLLAPSING_BLOCKS).add(ModBlocks.LOOSE_SAND.get(), ModBlocks.ASH_CRUST.get());

        // Ember woods and fire flora. One ground rule for the fire flora (a partial ash layer does not count: AshPlantSoil).
        tag(ModTags.Blocks.ASH_PLANT_SOIL).add(ModBlocks.ASH_BLOCK.get(), Blocks.COARSE_DIRT, ModBlocks.ASH_LAYER.get());
        tag(BlockTags.LEAVES).add(ModBlocks.EMBER_LEAVES.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.EMBER_LEAVES.get());
        tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.FIRE_BLOOM.get());
        tag(BlockTags.FLOWER_POTS).add(ModBlocks.POTTED_FIRE_BLOOM.get());
        tag(BlockTags.REPLACEABLE).add(ModBlocks.ASH_SPROUTS.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.ASH_SPROUTS.get());
        // 47e (INT3): a crown reaching over a chunk border takes the ash layer and the fire flora of the neighbouring
        // chunk, whichever chunk decorated first (they keep out of leaves already there), so the world does not depend
        // on that order.
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.ASH_LAYER.get(), ModBlocks.FIRE_BLOOM.get(), ModBlocks.EMBER_BERRY_BUSH.get());
        tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.ASH_SPROUTS.get(), ModBlocks.EMBER_BERRY_BUSH.get());
        // Like the sweet berry bush: landing in it breaks the fall.
        tag(BlockTags.FALL_DAMAGE_RESETTING).add(ModBlocks.EMBER_BERRY_BUSH.get());

        ring3Ores();
        sulfurAndSteam();
    }

    /**
     * Ring 3 ores. Volcanite and fire ruby need an iron pickaxe, sulfur a stone one. Not in
     * {@code #overworld_carver_replaceables}: ore features run after the carvers. {@code ores_in_ground} only where a
     * convention tag exists (stone, deepslate; none for basalt).
     */
    private void ring3Ores() {
        tag(ModTags.Blocks.ORES_VOLCANITE).add(ModBlocks.VOLCANITE_ORE.get(), ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(),
                ModBlocks.BASALT_VOLCANITE_ORE.get());
        tag(ModTags.Blocks.ORES_SULFUR).add(ModBlocks.SULFUR_ORE.get(), ModBlocks.BASALT_SULFUR_ORE.get());
        tag(ModTags.Blocks.ORES_FIRE_RUBY).add(ModBlocks.FIRE_RUBY_ORE.get(), ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get(),
                ModBlocks.BASALT_FIRE_RUBY_ORE.get());
        tag(Tags.Blocks.ORES).addTag(ModTags.Blocks.ORES_VOLCANITE).addTag(ModTags.Blocks.ORES_SULFUR)
                .addTag(ModTags.Blocks.ORES_FIRE_RUBY);
        tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(ModTags.Blocks.ORES_VOLCANITE).addTag(ModTags.Blocks.ORES_SULFUR)
                .addTag(ModTags.Blocks.ORES_FIRE_RUBY);
        tag(BlockTags.NEEDS_IRON_TOOL).addTag(ModTags.Blocks.ORES_VOLCANITE).addTag(ModTags.Blocks.ORES_FIRE_RUBY);
        tag(BlockTags.NEEDS_STONE_TOOL).addTag(ModTags.Blocks.ORES_SULFUR);
        tag(Tags.Blocks.ORE_RATES_SINGULAR).addTag(ModTags.Blocks.ORES_VOLCANITE).addTag(ModTags.Blocks.ORES_FIRE_RUBY);
        tag(Tags.Blocks.ORE_RATES_DENSE).addTag(ModTags.Blocks.ORES_SULFUR);
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(ModBlocks.VOLCANITE_ORE.get(), ModBlocks.SULFUR_ORE.get(),
                ModBlocks.FIRE_RUBY_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(), ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get());

        // Fire crystal: amethyst's tags (the wall chimes underfoot like an amethyst block); no budding block, no growth.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FIRE_CRYSTAL_BLOCK.get(), ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get(),
                ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get(), ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get(), ModBlocks.FIRE_CRYSTAL_CLUSTER.get());
        tag(BlockTags.CRYSTAL_SOUND_BLOCKS).add(ModBlocks.FIRE_CRYSTAL_BLOCK.get());
        tag(Tags.Blocks.BUDS).add(ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get());
        tag(Tags.Blocks.CLUSTERS).add(ModBlocks.FIRE_CRYSTAL_CLUSTER.get());
    }

    /**
     * Sulfur fields and steam fens. Crust and mud are natural ground that carvers cut through; boiling mud is
     * deliberately not in {@code #dirt} (nothing grows on it). The geyser is no heat source (contract §3).
     */
    private void sulfurAndSteam() {
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.SULFUR_CRUST.get(), ModBlocks.BOILING_MUD.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FUMAROLE.get(), ModBlocks.GEYSER.get());
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.SULFUR_CRUST.get(), ModBlocks.BOILING_MUD.get());
        tag(ModTags.Blocks.HEAT_SOURCES).add(ModBlocks.FUMAROLE.get(), ModBlocks.BOILING_MUD.get());
        // The coloured sulfur muds are boiling mud (46b); the sulfur crystal is a crystal like the fire crystal.
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.YELLOW_SULFUR_MUD.get(), ModBlocks.GREEN_SULFUR_MUD.get(),
                ModBlocks.ORANGE_SULFUR_MUD.get());
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.YELLOW_SULFUR_MUD.get(), ModBlocks.GREEN_SULFUR_MUD.get(),
                ModBlocks.ORANGE_SULFUR_MUD.get());
        tag(ModTags.Blocks.HEAT_SOURCES).add(ModBlocks.YELLOW_SULFUR_MUD.get(), ModBlocks.GREEN_SULFUR_MUD.get(),
                ModBlocks.ORANGE_SULFUR_MUD.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD.get(),
                ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD.get(), ModBlocks.LARGE_SULFUR_CRYSTAL_BUD.get(), ModBlocks.SULFUR_CRYSTAL.get());
        tag(Tags.Blocks.BUDS).add(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_SULFUR_CRYSTAL_BUD.get());
        tag(Tags.Blocks.CLUSTERS).add(ModBlocks.SULFUR_CRYSTAL.get());
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
        salt();
    }

    /**
     * Nether ring 2. The dripshroom set is Nether wood: in {@code #logs} (via its own tag) but not in
     * {@code #logs_that_burn}, like crimson and warped. Soaked netherrack is in {@code #base_stone_nether} (vanilla Nether
     * carvers and {@code #base_stone_nether} features work in it) but not in the {@code #infiniburn_*} tags: fire dies on
     * wet ground. Spore moss is deliberately not in {@code #nylium}: no vanilla fungi or roots on it, and bonemeal on
     * vanilla netherrack next to it is not wasted.
     */
    private void netherRing2() {
        tag(ModTags.Blocks.DRIPSHROOM_STEMS).add(ModBlocks.DRIPSHROOM_STEM.get(), ModBlocks.DRIPSHROOM_HYPHAE.get(),
                ModBlocks.STRIPPED_DRIPSHROOM_STEM.get(), ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get());
        tag(BlockTags.LOGS).addTag(ModTags.Blocks.DRIPSHROOM_STEMS);
        tag(BlockTags.PLANKS).add(ModBlocks.DRIPSHROOM_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.DRIPSHROOM_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.DRIPSHROOM_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.DRIPSHROOM_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.DRIPSHROOM_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.DRIPSHROOM_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.DRIPSHROOM_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.DRIPSHROOM_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.DRIPSHROOM_BUTTON.get());
        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_DRIPSHROOM_STEM.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.DRIPSHROOM_FENCE_GATE.get());

        tag(BlockTags.BASE_STONE_NETHER).add(ModBlocks.SOAKED_NETHERRACK.get());
        tag(Tags.Blocks.NETHERRACKS).add(ModBlocks.SOAKED_NETHERRACK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SOAKED_NETHERRACK.get(), ModBlocks.SPORE_MOSS.get(),
                ModBlocks.SLIME_STALACTITE.get());
        tag(BlockTags.NETHER_CARVER_REPLACEABLES).add(ModBlocks.SPORE_MOSS.get());

        // Vanilla lists its wart blocks in #mineable/hoe one by one, so the cap goes in by hand.
        tag(BlockTags.WART_BLOCKS).add(ModBlocks.DRIPSHROOM_CAP.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.DRIPSHROOM_CAP.get(), ModBlocks.DRIPSHROOM_LAMP.get());

        // Glow mould copies glow lichen's tags, the sprouts those of nether sprouts.
        tag(BlockTags.REPLACEABLE).add(ModBlocks.GLOW_MOLD.get(), ModBlocks.SPORE_SPROUTS.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.GLOW_MOLD.get(), ModBlocks.SPORE_SPROUTS.get());
        tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.GLOW_MOLD.get(), ModBlocks.SPORE_SPROUTS.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.GLOW_MOLD.get());
        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(ModBlocks.GLOW_MOLD.get());
        tag(BlockTags.COMBINATION_STEP_SOUND_BLOCKS).add(ModBlocks.SPORE_SPROUTS.get());

        tag(ModTags.Blocks.FUNGAL_GROWTHS).addTag(ModTags.Blocks.DRIPSHROOM_STEMS).add(ModBlocks.DRIPSHROOM_CAP.get(),
                ModBlocks.DRIPSHROOM_LAMP.get(), ModBlocks.SLIME_STALACTITE.get(), ModBlocks.GLOW_MOLD.get(),
                ModBlocks.SPORE_SPROUTS.get());

        // Dew: like vanilla's amethyst (budding block, buds, cluster), mined with a pickaxe. Deliberately in no ground,
        // carver or spreading tag, and not in #crystal_sound_blocks (it is water, not crystal). The stages are growth.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.DEW_SOURCE.get(), ModBlocks.SMALL_DEW_CRYSTAL_BUD.get(),
                ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get(), ModBlocks.LARGE_DEW_CRYSTAL_BUD.get(), ModBlocks.DEW_CRYSTAL_CLUSTER.get());
        tag(Tags.Blocks.BUDDING_BLOCKS).add(ModBlocks.DEW_SOURCE.get());
        tag(Tags.Blocks.BUDS).add(ModBlocks.SMALL_DEW_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_DEW_CRYSTAL_BUD.get());
        tag(Tags.Blocks.CLUSTERS).add(ModBlocks.DEW_CRYSTAL_CLUSTER.get());
        tag(ModTags.Blocks.FUNGAL_GROWTHS).add(ModBlocks.SMALL_DEW_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_DEW_CRYSTAL_BUD.get(), ModBlocks.DEW_CRYSTAL_CLUSTER.get());
    }

    /**
     * The salt set. Crust and salt block are natural ground that carvers cut through, but deliberately in no plant or
     * ground tag ({@code #sand}, {@code #dirt}, {@code #base_stone_overworld}, {@code desert_plant_soil},
     * {@code #dead_bush_may_place_on}, {@code #animals_spawnable_on}): the salt flats stay bare, and vanilla's desert and
     * underground rules stay off salt.
     */
    private void salt() {
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.SALT_CRUST.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SALT_BLOCK.get(), ModBlocks.POLISHED_SALT.get(),
                ModBlocks.POLISHED_SALT_STAIRS.get(), ModBlocks.POLISHED_SALT_SLAB.get(), ModBlocks.POLISHED_SALT_WALL.get(),
                ModBlocks.SALT_CRYSTAL.get());
        tag(BlockTags.STAIRS).add(ModBlocks.POLISHED_SALT_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.POLISHED_SALT_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.POLISHED_SALT_WALL.get());
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(ModBlocks.SALT_CRUST.get(), ModBlocks.SALT_BLOCK.get());
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
