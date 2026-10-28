package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.timtoys.corruptedadventure.block.AshCrustBlock;
import nl.timtoys.corruptedadventure.block.BoilingMudBlock;
import nl.timtoys.corruptedadventure.block.LagoonGlowgrassBlock;
import nl.timtoys.corruptedadventure.block.MoonpetalLeavesBlock;
import nl.timtoys.corruptedadventure.block.VerdantVineBlock;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;
import nl.timtoys.corruptedadventure.block.VoidstoneBlock;
import nl.timtoys.corruptedadventure.item.weakness.SunstoneFlashItem;
import nl.timtoys.corruptedadventure.item.weakness.VolcaniteHornItem;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.item.DoseDrinkItem;
import nl.timtoys.corruptedadventure.item.DoseFoodItem;
import nl.timtoys.corruptedadventure.item.EchoShellItem;
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.item.EffectArrowItem;
import nl.timtoys.corruptedadventure.item.SkyGlassLensItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModBlocksTest {
    /**
     * Blocks without an item of their own besides pots and liquids: the swamp gas (invisible, cannot be aimed at), the
     * wall bromeliad (placed by the bromeliad's item), the vine stem and the fruit pod (worldgen only), the wall signs
     * (placed by the sign items) and the ember berry bush (planted by its berries, like vanilla's).
     */
    private static final Set<DeferredHolder<Block, ? extends Block>> WITHOUT_ITEM = Set.of(ModBlocks.SWAMP_GAS,
            ModBlocks.EMBER_BERRY_BUSH,
            ModBlocks.VERDANT_WALL_BROMELIAD, ModBlocks.VERDANT_VINE_PLANT, ModBlocks.VERDANT_FRUIT_POD,
            ModBlocks.CORRUPTED_WALL_SIGN, ModBlocks.CORRUPTED_WALL_HANGING_SIGN);

    @Test
    void everyBlockExceptPottedPlantsHasABlockItemWithTheSameName() {
        assertFalse(ModBlocks.entries().isEmpty());
        for (DeferredHolder<Block, ? extends Block> block : ModBlocks.entries()) {
            Item item = block.get().asItem();
            // Potted plants, liquids (a bucket instead) and blocks that only worldgen places have no item.
            if (block.get() instanceof FlowerPotBlock || block.get() instanceof LiquidBlock || WITHOUT_ITEM.contains(block)) {
                assertNotEquals(block.getId(), BuiltInRegistries.ITEM.getKey(item), block.getId().toString());
                continue;
            }
            assertInstanceOf(BlockItem.class, item, block.getId().toString());
            assertEquals(block.getId(), BuiltInRegistries.ITEM.getKey(item));
        }
    }

    @Test
    void pottedPlantsHoldTheirPlant() {
        assertSame(ModBlocks.DRY_SHRUB.get(), ModBlocks.POTTED_DRY_SHRUB.get().getPotted());
        assertSame(ModBlocks.BARREL_CACTUS.get(), ModBlocks.POTTED_BARREL_CACTUS.get().getPotted());
        assertSame(ModBlocks.DESERT_FLOWER.get(), ModBlocks.POTTED_DESERT_FLOWER.get().getPotted());
        assertSame(ModBlocks.PALM_SAPLING.get(), ModBlocks.POTTED_PALM_SAPLING.get().getPotted());
        assertSame(ModBlocks.FIRE_BLOOM.get(), ModBlocks.POTTED_FIRE_BLOOM.get().getPotted());
    }

    @Test
    void looseSandSitsBetweenTheScorchedSandstoneSetAndTheFlora() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int looseSand = shown.indexOf(ModBlocks.LOOSE_SAND.asItem());
        assertTrue(looseSand > 0);
        assertEquals(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.asItem(), shown.get(looseSand - 1));
        assertEquals(ModBlocks.CRACKED_CLAY.asItem(), shown.get(looseSand + 1));
    }

    @Test
    void palmSetFollowsTheDesertFloraFromLogToSapling() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int log = shown.indexOf(ModBlocks.PALM_LOG.asItem());
        assertEquals(ModBlocks.DESERT_FLOWER.asItem(), shown.get(log - 1));
        assertEquals(List.of(ModBlocks.PALM_LOG.asItem(), ModBlocks.PALM_WOOD.asItem(), ModBlocks.STRIPPED_PALM_LOG.asItem(),
                ModBlocks.STRIPPED_PALM_WOOD.asItem(), ModBlocks.PALM_PLANKS.asItem(), ModBlocks.PALM_STAIRS.asItem(),
                ModBlocks.PALM_SLAB.asItem(), ModBlocks.PALM_FENCE.asItem(), ModBlocks.PALM_FENCE_GATE.asItem(),
                ModBlocks.PALM_DOOR.asItem(), ModBlocks.PALM_TRAPDOOR.asItem(), ModBlocks.PALM_PRESSURE_PLATE.asItem(),
                ModBlocks.PALM_BUTTON.asItem(), ModBlocks.PALM_LEAVES.asItem(), ModBlocks.PALM_SAPLING.asItem()),
                shown.subList(log, log + 15));
    }

    @Test
    void ring2OresFollowThePalmSetSunstoneFirst() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int sapling = shown.indexOf(ModBlocks.PALM_SAPLING.asItem());
        assertEquals(List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE.asItem(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.asItem(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.asItem(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.asItem(),
                ModBlocks.RED_CANYON_TURQUOISE_ORE.asItem(), ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.asItem(),
                ModBlocks.CREAM_CANYON_TURQUOISE_ORE.asItem()), shown.subList(sapling + 1, sapling + 8));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        int soulCrystal = standalone.indexOf(ModItems.SOUL_CRYSTAL.get());
        assertEquals(List.of(ModItems.RAW_SUNSTONE.get(), ModItems.RAW_TURQUOISE.get()),
                standalone.subList(soulCrystal + 1, soulCrystal + 3));
    }

    @Test
    void saltSetFollowsTheRing2OresAndSaltFollowsRawTurquoise() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int ore = shown.indexOf(ModBlocks.CREAM_CANYON_TURQUOISE_ORE.asItem());
        assertEquals(List.of(ModBlocks.SALT_CRUST.asItem(), ModBlocks.SALT_BLOCK.asItem(), ModBlocks.POLISHED_SALT.asItem(),
                ModBlocks.POLISHED_SALT_STAIRS.asItem(), ModBlocks.POLISHED_SALT_SLAB.asItem(),
                ModBlocks.POLISHED_SALT_WALL.asItem(), ModBlocks.SALT_CRYSTAL.asItem()), shown.subList(ore + 1, ore + 8));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        assertEquals(standalone.indexOf(ModItems.RAW_TURQUOISE.get()) + 1, standalone.indexOf(ModItems.SALT.get()));
    }

    /** Worldgen (change 30) places the default state on the crust, so it must face up and glow faintly. */
    @Test
    void saltCrystalStandsUpDryAndGlowsFaintly() {
        BlockState crystal = ModBlocks.SALT_CRYSTAL.get().defaultBlockState();
        assertEquals(Direction.UP, crystal.getValue(AmethystClusterBlock.FACING));
        assertFalse(crystal.getValue(AmethystClusterBlock.WATERLOGGED));
        assertEquals(3, crystal.getLightEmission());
    }

    @Test
    void saltCrustDoesNotFall() {
        assertFalse(ModBlocks.SALT_CRUST.get() instanceof FallingBlock);
    }

    @Test
    void sunstoneOresGlowAndTurquoiseOresDoNot() {
        for (Block ore : List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE.get(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.get(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.get(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.get())) {
            assertEquals(3, ore.defaultBlockState().getLightEmission(), ore.toString());
        }
        for (Block ore : List.of(ModBlocks.RED_CANYON_TURQUOISE_ORE.get(), ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.get(),
                ModBlocks.CREAM_CANYON_TURQUOISE_ORE.get())) {
            assertEquals(0, ore.defaultBlockState().getLightEmission(), ore.toString());
        }
    }

    /** XP only drops when a player breaks the block (not testable over RCON); the codec exposes the range. */
    @Test
    void ring2OresDropSunstoneTwoToFiveAndTurquoiseThreeToSevenXp() {
        for (DeferredBlock<DropExperienceBlock> ore : List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE, ModBlocks.ORANGE_CANYON_SUNSTONE_ORE,
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE, ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE)) {
            assertEquals(UniformInt.of(2, 5).toString(), experience(ore.get()), ore.getId().toString());
        }
        for (DeferredBlock<DropExperienceBlock> ore : List.of(ModBlocks.RED_CANYON_TURQUOISE_ORE,
                ModBlocks.ORANGE_CANYON_TURQUOISE_ORE, ModBlocks.CREAM_CANYON_TURQUOISE_ORE)) {
            assertEquals(UniformInt.of(3, 7).toString(), experience(ore.get()), ore.getId().toString());
        }
    }

    @Test
    void netherRing2FollowsTheSaltSetInOrderAndEndsWithTheSprouts() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int soaked = shown.indexOf(ModBlocks.SOAKED_NETHERRACK.asItem());
        // The storage blocks (phase 2 golf 1), the inlaid sandstone set and the salt lamp (golf 2) sit between the salt
        // set and Nether ring 2.
        assertEquals(List.of(ModBlocks.SALT_CRYSTAL.asItem(), ModBlocks.RAW_SUNSTONE_BLOCK.asItem(),
                ModBlocks.RAW_TURQUOISE_BLOCK.asItem(), ModBlocks.SUNSTONE_BLOCK.asItem(), ModBlocks.TURQUOISE_BLOCK.asItem(),
                ModBlocks.TURQUOISE_INLAID_SANDSTONE.asItem(), ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS.asItem(),
                ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB.asItem(), ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL.asItem(),
                ModBlocks.SALT_LAMP.asItem()),
                shown.subList(soaked - 10, soaked));
        assertEquals(List.of(ModBlocks.SOAKED_NETHERRACK.asItem(), ModBlocks.SPORE_MOSS.asItem(),
                ModBlocks.DRIPSHROOM_STEM.asItem(), ModBlocks.DRIPSHROOM_HYPHAE.asItem(), ModBlocks.STRIPPED_DRIPSHROOM_STEM.asItem(),
                ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.asItem(), ModBlocks.DRIPSHROOM_PLANKS.asItem(), ModBlocks.DRIPSHROOM_STAIRS.asItem(),
                ModBlocks.DRIPSHROOM_SLAB.asItem(), ModBlocks.DRIPSHROOM_FENCE.asItem(), ModBlocks.DRIPSHROOM_FENCE_GATE.asItem(),
                ModBlocks.DRIPSHROOM_DOOR.asItem(), ModBlocks.DRIPSHROOM_TRAPDOOR.asItem(), ModBlocks.DRIPSHROOM_PRESSURE_PLATE.asItem(),
                ModBlocks.DRIPSHROOM_BUTTON.asItem(), ModBlocks.DRIPSHROOM_CAP.asItem(), ModBlocks.DRIPSHROOM_LAMP.asItem(),
                ModBlocks.GLOW_MOLD.asItem(), ModBlocks.SLIME_STALACTITE.asItem(), ModBlocks.SPORE_SPROUTS.asItem()),
                shown.subList(soaked, soaked + 20));
    }

    @Test
    void dewBlocksEndTheNetherRing2SectionAndTheDewCrystalEndsTheLooseItems() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int sprouts = shown.indexOf(ModBlocks.SPORE_SPROUTS.asItem());
        assertEquals(List.of(ModBlocks.DEW_SOURCE.asItem(), ModBlocks.SMALL_DEW_CRYSTAL_BUD.asItem(),
                ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.asItem(), ModBlocks.LARGE_DEW_CRYSTAL_BUD.asItem(),
                ModBlocks.DEW_CRYSTAL_CLUSTER.asItem()), shown.subList(sprouts + 1, sprouts + 6));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem))
                // The smithing templates and gear of phase 2 close their ring sections; these checks are about the materials.
                .filter(item -> !(item instanceof SmithingTemplateItem) && !(item instanceof TieredItem)
                        && !(item instanceof ArmorItem))
                // So do the consumables of golf 2 (canteen, cactus juice, salted jerky).
                .filter(item -> !(item instanceof CanteenItem || item instanceof DoseDrinkItem || item instanceof DoseFoodItem))
                // And the weakness items of phase 4.
                .filter(item -> !(item instanceof SunstoneFlashItem || item instanceof VolcaniteHornItem))
                .toList();
        assertEquals(List.of(ModItems.SALT.get(), ModItems.SUNSTONE_INGOT.get(), ModItems.TURQUOISE.get(),
                ModItems.DEW_CRYSTAL.get()), standalone.subList(standalone.indexOf(ModItems.SALT.get()),
                standalone.indexOf(ModItems.DEW_CRYSTAL.get()) + 1));
    }

    @Test
    void netherRing2BlocksTickLightAndSoundAsPlanned() {
        assertTrue(ModBlocks.SPORE_MOSS.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SOAKED_NETHERRACK.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SPORE_SPROUTS.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SLIME_STALACTITE.get().defaultBlockState().isRandomlyTicking());
        assertEquals(SoundType.MUD, ModBlocks.SOAKED_NETHERRACK.get().defaultBlockState().getSoundType());
        assertEquals(15, ModBlocks.DRIPSHROOM_LAMP.get().defaultBlockState().getLightEmission());
        // Glow mould has no face by default (dark); with a face it glows like glow lichen.
        assertEquals(0, ModBlocks.GLOW_MOLD.get().defaultBlockState().getLightEmission());
        assertEquals(7, ModBlocks.GLOW_MOLD.get().defaultBlockState()
                .setValue(MultifaceBlock.getFaceProperty(Direction.UP), true).getLightEmission());
        assertEquals(0, ModBlocks.SPORE_SPROUTS.get().defaultBlockState().getLightEmission());
    }

    /** Ring 3 follows Nether ring 2 directly (contract §3), in this order. */
    @Test
    void ring3FollowsTheDewBlocksInOrder() {
        List<Item> shown = blockItems();
        // The dew crystal block (phase 2 golf 3) ends the Nether ring 2 section.
        int cluster = shown.indexOf(ModBlocks.DEW_CRYSTAL_CLUSTER.asItem()) + 1;
        assertEquals(ModBlocks.DEW_CRYSTAL_BLOCK.asItem(), shown.get(cluster));
        List<Item> ring3 = Stream.of(ModBlocks.ASH_BLOCK, ModBlocks.ASH_LAYER, ModBlocks.ASH_CRUST, ModBlocks.SMOLDERING_ASH,
                ModBlocks.PUMICE, ModBlocks.PUMICE_STAIRS, ModBlocks.PUMICE_SLAB, ModBlocks.PUMICE_WALL,
                ModBlocks.POLISHED_PUMICE, ModBlocks.POLISHED_PUMICE_STAIRS, ModBlocks.POLISHED_PUMICE_SLAB,
                ModBlocks.POLISHED_PUMICE_WALL, ModBlocks.PUMICE_BRICKS, ModBlocks.PUMICE_BRICK_STAIRS,
                ModBlocks.PUMICE_BRICK_SLAB, ModBlocks.PUMICE_BRICK_WALL, ModBlocks.SMOLDERING_STONE,
                ModBlocks.CHARRED_LOG, ModBlocks.CHARRED_WOOD, ModBlocks.STRIPPED_CHARRED_LOG, ModBlocks.STRIPPED_CHARRED_WOOD,
                ModBlocks.CHARRED_PLANKS, ModBlocks.CHARRED_STAIRS, ModBlocks.CHARRED_SLAB, ModBlocks.CHARRED_FENCE,
                ModBlocks.CHARRED_FENCE_GATE, ModBlocks.CHARRED_DOOR, ModBlocks.CHARRED_TRAPDOOR,
                ModBlocks.CHARRED_PRESSURE_PLATE, ModBlocks.CHARRED_BUTTON,
                ModBlocks.EMBER_LEAVES, ModBlocks.FIRE_BLOOM, ModBlocks.ASH_SPROUTS, ModBlocks.CHARRED_SHRUB, ModBlocks.EMBER_BERRY_BUSH,
                ModBlocks.VOLCANITE_ORE, ModBlocks.DEEPSLATE_VOLCANITE_ORE, ModBlocks.BASALT_VOLCANITE_ORE,
                ModBlocks.SULFUR_ORE, ModBlocks.BASALT_SULFUR_ORE, ModBlocks.FIRE_RUBY_ORE,
                ModBlocks.DEEPSLATE_FIRE_RUBY_ORE, ModBlocks.BASALT_FIRE_RUBY_ORE,
                ModBlocks.FIRE_CRYSTAL_BLOCK, ModBlocks.SMALL_FIRE_CRYSTAL_BUD, ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD,
                ModBlocks.LARGE_FIRE_CRYSTAL_BUD, ModBlocks.FIRE_CRYSTAL_CLUSTER,
                ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD, ModBlocks.GEYSER,
                ModBlocks.YELLOW_SULFUR_MUD, ModBlocks.GREEN_SULFUR_MUD, ModBlocks.ORANGE_SULFUR_MUD,
                ModBlocks.SMALL_SULFUR_CRYSTAL_BUD, ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD, ModBlocks.LARGE_SULFUR_CRYSTAL_BUD,
                ModBlocks.SULFUR_CRYSTAL).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(ring3, shown.subList(cluster + 1, cluster + 1 + ring3.size()));
    }

    @Test
    void smolderingStoneGlowsAtSevenOnlyThroughItsOverlay() {
        BlockState stone = ModBlocks.SMOLDERING_STONE.get().defaultBlockState();
        assertEquals(7, stone.getLightEmission());
        // Two layers (decision 2026-10-06): the block is lit normally, only the model's glow overlay is full-bright.
        assertFalse(stone.emissiveRendering(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(stone.isRandomlyTicking());
    }

    /** Decision 2026-10-06: charred wood does not burn, so lava never sets it alight (no fire odds either: S). */
    @Test
    void charredWoodIsNotIgnitedByLava() {
        for (DeferredBlock<?> block : List.of(ModBlocks.CHARRED_LOG, ModBlocks.CHARRED_WOOD, ModBlocks.STRIPPED_CHARRED_LOG,
                ModBlocks.STRIPPED_CHARRED_WOOD, ModBlocks.CHARRED_PLANKS, ModBlocks.CHARRED_STAIRS, ModBlocks.CHARRED_SLAB,
                ModBlocks.CHARRED_FENCE, ModBlocks.CHARRED_FENCE_GATE, ModBlocks.CHARRED_DOOR, ModBlocks.CHARRED_TRAPDOOR,
                ModBlocks.CHARRED_PRESSURE_PLATE, ModBlocks.CHARRED_BUTTON)) {
            assertFalse(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
        assertTrue(Blocks.OAK_PLANKS.defaultBlockState().ignitedByLava());
    }

    @Test
    void ashBlockDoesNotFallButTheCrustDoes() {
        assertFalse(ModBlocks.ASH_BLOCK.get() instanceof FallingBlock);
        assertInstanceOf(FallingBlock.class, ModBlocks.ASH_CRUST.get());
        // 38c: the crust only falls when a player walks under it.
        assertInstanceOf(AshCrustBlock.class, ModBlocks.ASH_CRUST.get());
    }

    /** V3 = A: the ember leaves only glow in their texture; the smouldering ash glows faintly (light 2). */
    @Test
    void emberLeavesNeverTickOrDecayAndGiveNoLight() {
        for (BlockState state : ModBlocks.EMBER_LEAVES.get().getStateDefinition().getPossibleStates()) {
            assertFalse(state.isRandomlyTicking(), state.toString());
            assertEquals(0, state.getLightEmission(), state.toString());
            assertFalse(state.ignitedByLava(), state.toString());
        }
        assertEquals(2, ModBlocks.SMOLDERING_ASH.get().defaultBlockState().getLightEmission());
    }

    @Test
    void fireFloraDoesNotBurnButTheBerryBushStillGrows() {
        for (DeferredBlock<?> plant : List.of(ModBlocks.FIRE_BLOOM, ModBlocks.ASH_SPROUTS, ModBlocks.CHARRED_SHRUB, ModBlocks.EMBER_BERRY_BUSH)) {
            assertFalse(plant.get().defaultBlockState().ignitedByLava(), plant.getId().toString());
        }
        assertTrue(ModBlocks.EMBER_BERRY_BUSH.get().defaultBlockState().isRandomlyTicking());
    }

    /** One step above ring 2: XP volcanite 3–6, sulfur 1–3, fire ruby 4–8; half a point harder than the host's vanilla ore. */
    @Test
    void ring3OresHaveTheirXpLightAndHardness() {
        Map<UniformInt, List<DeferredBlock<DropExperienceBlock>>> xp = Map.of(
                UniformInt.of(3, 6), List.of(ModBlocks.VOLCANITE_ORE, ModBlocks.DEEPSLATE_VOLCANITE_ORE, ModBlocks.BASALT_VOLCANITE_ORE),
                UniformInt.of(1, 3), List.of(ModBlocks.SULFUR_ORE, ModBlocks.BASALT_SULFUR_ORE),
                UniformInt.of(4, 8), List.of(ModBlocks.FIRE_RUBY_ORE, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE, ModBlocks.BASALT_FIRE_RUBY_ORE));
        xp.forEach((range, ores) -> ores.forEach(ore -> assertEquals(range.toString(), experience(ore.get()), ore.getId().toString())));
        assertEquals(3, ModBlocks.FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(3, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(3, ModBlocks.BASALT_FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.VOLCANITE_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.SULFUR_ORE.get().defaultBlockState().getLightEmission());
        float extra = ModBlocks.RING_3_ORE_EXTRA_HARDNESS;
        assertEquals(Blocks.IRON_ORE.defaultDestroyTime() + extra, ModBlocks.VOLCANITE_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.DEEPSLATE_IRON_ORE.defaultDestroyTime() + extra, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.COAL_ORE.defaultDestroyTime() + extra, ModBlocks.SULFUR_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.COAL_ORE.defaultDestroyTime() + extra, ModBlocks.BASALT_SULFUR_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.IRON_ORE.getExplosionResistance(), ModBlocks.VOLCANITE_ORE.get().getExplosionResistance());
        assertEquals(SoundType.BASALT, ModBlocks.BASALT_SULFUR_ORE.get().defaultBlockState().getSoundType());
    }

    @Test
    void ring3RawMaterialsFollowTheDewCrystal() {
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        // The dripshroom stew (phase 2 golf 3) ends the Nether ring 2 section.
        int dew = standalone.indexOf(ModItems.DEW_CRYSTAL.get()) + 1;
        assertEquals(ModItems.DRIPSHROOM_STEW.get(), standalone.get(dew));
        assertEquals(List.of(ModItems.RAW_VOLCANITE.get(), ModItems.SULFUR.get(), ModItems.FIRE_RUBY.get(), ModItems.FIRE_CRYSTAL.get()),
                standalone.subList(dew + 1, dew + 5));
    }

    /** Like the dew crystal stages: light 1, 2, 4, 5, facing up and dry by default; the wall gives no light and never ticks. */
    @Test
    void fireCrystalStagesGlowAndTheWallDoesNot() {
        List<DeferredBlock<AmethystClusterBlock>> stages = List.of(ModBlocks.SMALL_FIRE_CRYSTAL_BUD, ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD,
                ModBlocks.LARGE_FIRE_CRYSTAL_BUD, ModBlocks.FIRE_CRYSTAL_CLUSTER);
        int[] light = {1, 2, 4, 5};
        for (int i = 0; i < stages.size(); i++) {
            BlockState stage = stages.get(i).get().defaultBlockState();
            assertEquals(light[i], stage.getLightEmission(), stages.get(i).getId().toString());
            assertEquals(Direction.UP, stage.getValue(AmethystClusterBlock.FACING));
            assertFalse(stage.getValue(AmethystClusterBlock.WATERLOGGED));
            assertFalse(stage.isRandomlyTicking());
        }
        assertEquals(0, ModBlocks.FIRE_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission());
        assertFalse(ModBlocks.FIRE_CRYSTAL_BLOCK.get().defaultBlockState().isRandomlyTicking());
    }

    /** Only the geyser ticks (to start its cycle); the mud slows like honey; neither crust nor mud falls or glows. */
    @Test
    void sulfurAndSteamBlocksBehaveAsPlanned() {
        assertTrue(ModBlocks.GEYSER.get().defaultBlockState().isRandomlyTicking());
        for (DeferredBlock<?> block : List.of(ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD)) {
            assertFalse(block.get().defaultBlockState().isRandomlyTicking(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD, ModBlocks.GEYSER)) {
            assertEquals(0, block.get().defaultBlockState().getLightEmission(), block.getId().toString());
            assertFalse(block.get() instanceof FallingBlock, block.getId().toString());
        }
        assertEquals(Blocks.HONEY_BLOCK.getSpeedFactor(), ModBlocks.BOILING_MUD.get().getSpeedFactor());
        assertEquals(Blocks.HONEY_BLOCK.getJumpFactor(), ModBlocks.BOILING_MUD.get().getJumpFactor());
        assertEquals(Blocks.MUD.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                ModBlocks.BOILING_MUD.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /** The coloured sulfur muds are boiling mud in another colour, with sulfur steam (46b). */
    @Test
    void sulfurMudsAreBoilingMud() {
        for (DeferredBlock<BoilingMudBlock> mud : List.of(ModBlocks.YELLOW_SULFUR_MUD, ModBlocks.GREEN_SULFUR_MUD,
                ModBlocks.ORANGE_SULFUR_MUD)) {
            BlockState state = mud.get().defaultBlockState();
            assertEquals(ModBlocks.BOILING_MUD.get().getSpeedFactor(), mud.get().getSpeedFactor(), mud.getId().toString());
            assertEquals(ModBlocks.BOILING_MUD.get().getJumpFactor(), mud.get().getJumpFactor(), mud.getId().toString());
            assertEquals(ModBlocks.BOILING_MUD.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
            assertEquals(0, state.getLightEmission());
            assertFalse(state.isRandomlyTicking());
        }
    }

    /** Like the salt crystal, a faint glow (1, 2, 2, 3), facing up and dry by default, no growth. */
    @Test
    void sulfurCrystalStagesGlowFaintly() {
        List<DeferredBlock<AmethystClusterBlock>> stages = List.of(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD,
                ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD, ModBlocks.LARGE_SULFUR_CRYSTAL_BUD, ModBlocks.SULFUR_CRYSTAL);
        int[] light = {1, 2, 2, 3};
        List<Block> vanilla = List.of(Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD,
                Blocks.AMETHYST_CLUSTER);
        for (int i = 0; i < stages.size(); i++) {
            BlockState stage = stages.get(i).get().defaultBlockState();
            assertEquals(light[i], stage.getLightEmission(), stages.get(i).getId().toString());
            assertEquals(Direction.UP, stage.getValue(AmethystClusterBlock.FACING));
            assertFalse(stage.getValue(AmethystClusterBlock.WATERLOGGED));
            assertFalse(stage.isRandomlyTicking());
            assertEquals(vanilla.get(i).defaultBlockState().getSoundType(), stage.getSoundType());
        }
    }

    /** Phase 2 golf 3 (§6.4): the sulfur lamp shines at 13, the dew crystal block chimes, the raft floats on lava only. */
    @Test
    void golf3Ring3BlocksHaveTheirLightSoundAndGround() {
        assertEquals(13, ModBlocks.SULFUR_LAMP.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.DEW_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission());
        assertEquals(SoundType.AMETHYST, ModBlocks.DEW_CRYSTAL_BLOCK.get().defaultBlockState().getSoundType());
        assertEquals(Blocks.COAL_BLOCK.defaultDestroyTime(), ModBlocks.SULFUR_BLOCK.get().defaultDestroyTime());
        assertInstanceOf(PlaceOnWaterBlockItem.class, ModBlocks.PUMICE_RAFT.asItem());
        assertSame(ModItems.EMBER_BERRIES.get(), ModBlocks.EMBER_BERRY_BUSH.asItem());
    }

    /** Nether ring 3 follows ring 3 directly (contract §3), in this order. */
    @Test
    void netherRing3FollowsRing3InOrder() {
        List<Item> shown = blockItems();
        // The storage blocks (phase 2 golf 1) and the golf 3 extras end the ring 3 section.
        int crystal = shown.indexOf(ModBlocks.SULFUR_CRYSTAL.asItem());
        List<Item> end = Stream.of(ModBlocks.RAW_VOLCANITE_BLOCK, ModBlocks.VOLCANITE_BLOCK, ModBlocks.FIRE_RUBY_BLOCK,
                ModBlocks.SULFUR_BLOCK, ModBlocks.FIRE_RUBY_INLAID_PUMICE, ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS,
                ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB, ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL, ModBlocks.SULFUR_LAMP,
                ModBlocks.PUMICE_RAFT).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(end, shown.subList(crystal + 1, crystal + 1 + end.size()));
        int lastRing3 = crystal + end.size();
        List<Item> netherRing3 = Stream.of(ModBlocks.COLD_LAVA, ModBlocks.COLD_LAVA_STAIRS, ModBlocks.COLD_LAVA_SLAB,
                ModBlocks.COLD_LAVA_WALL, ModBlocks.POLISHED_COLD_LAVA, ModBlocks.POLISHED_COLD_LAVA_STAIRS,
                ModBlocks.POLISHED_COLD_LAVA_SLAB, ModBlocks.POLISHED_COLD_LAVA_WALL, ModBlocks.COLD_LAVA_BRICKS,
                ModBlocks.COLD_LAVA_BRICK_STAIRS, ModBlocks.COLD_LAVA_BRICK_SLAB, ModBlocks.COLD_LAVA_BRICK_WALL,
                ModBlocks.NETHER_ICE, ModBlocks.PACKED_NETHER_ICE, ModBlocks.RIME_NETHERRACK,
                ModBlocks.RIMESHROOM_STEM, ModBlocks.RIMESHROOM_HYPHAE, ModBlocks.STRIPPED_RIMESHROOM_STEM,
                ModBlocks.STRIPPED_RIMESHROOM_HYPHAE, ModBlocks.RIMESHROOM_PLANKS, ModBlocks.RIMESHROOM_STAIRS,
                ModBlocks.RIMESHROOM_SLAB, ModBlocks.RIMESHROOM_FENCE, ModBlocks.RIMESHROOM_FENCE_GATE,
                ModBlocks.RIMESHROOM_DOOR, ModBlocks.RIMESHROOM_TRAPDOOR, ModBlocks.RIMESHROOM_PRESSURE_PLATE,
                ModBlocks.RIMESHROOM_BUTTON, ModBlocks.RIMESHROOM_CAP, ModBlocks.ICE_STALACTITE,
                ModBlocks.GLOWING_FISSURE, ModBlocks.PRIMORDIAL_ICE).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(netherRing3, shown.subList(lastRing3 + 1, lastRing3 + 1 + netherRing3.size()));
    }

    /**
     * Decision 2026-10-06: Nether ice is opaque and solid and never melts (no random ticks), with the hardness and
     * slipperiness of its vanilla source: packed ice for the Nether ice, blue ice for primordial ice. No light.
     */
    @Test
    void netherIceIsOpaqueAndNeverMeltsWithItsSourcesHardnessAndSlipperiness() {
        Map<DeferredBlock<Block>, Block> sources = Map.of(ModBlocks.NETHER_ICE, Blocks.PACKED_ICE,
                ModBlocks.PACKED_NETHER_ICE, Blocks.PACKED_ICE, ModBlocks.PRIMORDIAL_ICE, Blocks.BLUE_ICE);
        sources.forEach((ice, source) -> {
            BlockState state = ice.get().defaultBlockState();
            assertTrue(state.canOcclude(), ice.getId().toString());
            assertFalse(state.isRandomlyTicking(), ice.getId().toString());
            assertEquals(0, state.getLightEmission(), ice.getId().toString());
            assertEquals(source.getFriction(), ice.get().getFriction(), ice.getId().toString());
            assertEquals(source.defaultDestroyTime(), ice.get().defaultDestroyTime(), ice.getId().toString());
        });
    }

    /** Decision 2026-10-07: ghast fireballs and creepers cannot blow the pillar cores away (obsidian's resistance). */
    @Test
    void primordialIceResistsExplosionsLikeObsidian() {
        assertEquals(Blocks.OBSIDIAN.getExplosionResistance(), ModBlocks.PRIMORDIAL_ICE.get().getExplosionResistance());
    }

    @Test
    void netherRing3WoodDoesNotBurnAndTheFissureGlowsAtSevenOnlyThroughItsOverlay() {
        for (DeferredBlock<?> block : List.of(ModBlocks.RIMESHROOM_STEM, ModBlocks.RIMESHROOM_HYPHAE,
                ModBlocks.STRIPPED_RIMESHROOM_STEM, ModBlocks.STRIPPED_RIMESHROOM_HYPHAE, ModBlocks.RIMESHROOM_PLANKS,
                ModBlocks.RIMESHROOM_STAIRS, ModBlocks.RIMESHROOM_SLAB, ModBlocks.RIMESHROOM_FENCE,
                ModBlocks.RIMESHROOM_FENCE_GATE, ModBlocks.RIMESHROOM_DOOR, ModBlocks.RIMESHROOM_TRAPDOOR,
                ModBlocks.RIMESHROOM_PRESSURE_PLATE, ModBlocks.RIMESHROOM_BUTTON)) {
            assertFalse(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
        BlockState fissure = ModBlocks.GLOWING_FISSURE.get().defaultBlockState();
        assertEquals(7, fissure.getLightEmission());
        assertFalse(fissure.emissiveRendering(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(ModBlocks.ICE_STALACTITE.get().defaultBlockState().isRandomlyTicking());
        assertEquals(SoundType.GLASS, ModBlocks.ICE_STALACTITE.get().defaultBlockState().getSoundType());
        assertEquals(NoteBlockInstrument.CHIME, ModBlocks.ICE_STALACTITE.get().defaultBlockState().instrument());
    }

    /** Nether wood is no furnace fuel: every rimeshroom item is in the generated {@code #non_flammable_wood}. */
    @Test
    void everyRimeshroomItemIsNonFlammableWood() {
        String relative = "src/generated/resources/data/minecraft/tags/item/non_flammable_wood.json";
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve(relative))) {
            dir = dir.getParent();
        }
        Set<String> values = new HashSet<>();
        WorldgenData.read(dir.resolve(relative)).getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        List<String> rimeshroomWood = ModBlocks.entries().stream()
                .filter(block -> block.get().asItem() != Items.AIR && !block.getId().equals(ModBlocks.RIMESHROOM_CAP.getId()))
                .map(block -> block.getId().toString()).filter(id -> id.contains("rimeshroom")).toList();
        assertEquals(13, rimeshroomWood.size());
        rimeshroomWood.forEach(id -> assertTrue(values.contains(id), id));
    }

    /**
     * The hanging moss and glowing roots are ring 4's climbing aid (verdant_falls Q3 = B): in the generated
     * {@code #climbable}, and walk-through, so a player stands in them like in weeping vines.
     */
    @Test
    void ring4HangingMossAndRootsAreClimbable() {
        String relative = "src/generated/resources/data/minecraft/tags/block/climbable.json";
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve(relative))) {
            dir = dir.getParent();
        }
        Set<String> values = new HashSet<>();
        WorldgenData.read(dir.resolve(relative)).getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        for (DeferredBlock<?> block : List.of(ModBlocks.HANGING_SWAMP_MOSS, ModBlocks.GLOWING_HANGING_ROOTS)) {
            assertTrue(values.contains(block.getId().toString()), block.getId().toString());
            assertTrue(block.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(),
                    block.getId() + " is walk-through");
        }
    }

    /** Ring 4 follows Nether ring 3 directly (contract ring 4 §3), in this order (the potted sapling has no item). */
    @Test
    void ring4NaturalBlocksFollowNetherRing3InOrder() {
        List<Item> shown = blockItems();
        int lastNetherRing3 = shown.indexOf(ModBlocks.PRIMORDIAL_ICE.asItem());
        List<Item> ring4 = Stream.of(ModBlocks.TIDESTONE, ModBlocks.TIDESTONE_STAIRS, ModBlocks.TIDESTONE_SLAB,
                ModBlocks.TIDESTONE_WALL, ModBlocks.COBBLED_TIDESTONE, ModBlocks.COBBLED_TIDESTONE_STAIRS,
                ModBlocks.COBBLED_TIDESTONE_SLAB, ModBlocks.COBBLED_TIDESTONE_WALL, ModBlocks.POLISHED_TIDESTONE,
                ModBlocks.POLISHED_TIDESTONE_STAIRS, ModBlocks.POLISHED_TIDESTONE_SLAB, ModBlocks.POLISHED_TIDESTONE_WALL,
                ModBlocks.TIDESTONE_BRICKS, ModBlocks.TIDESTONE_BRICK_STAIRS, ModBlocks.TIDESTONE_BRICK_SLAB,
                ModBlocks.TIDESTONE_BRICK_WALL, ModBlocks.MOONPETAL_LOG, ModBlocks.MOONPETAL_WOOD,
                ModBlocks.STRIPPED_MOONPETAL_LOG, ModBlocks.STRIPPED_MOONPETAL_WOOD, ModBlocks.MOONPETAL_PLANKS,
                ModBlocks.MOONPETAL_STAIRS, ModBlocks.MOONPETAL_SLAB, ModBlocks.MOONPETAL_FENCE, ModBlocks.MOONPETAL_FENCE_GATE,
                ModBlocks.MOONPETAL_DOOR, ModBlocks.MOONPETAL_TRAPDOOR, ModBlocks.MOONPETAL_PRESSURE_PLATE,
                ModBlocks.MOONPETAL_BUTTON, ModBlocks.MOONPETAL_LEAVES, ModBlocks.MOONPETAL_SAPLING,
                ModBlocks.WITHERED_MOONPETAL_LOG, ModBlocks.WITHERED_MOONPETAL_WOOD, ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG,
                ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD, ModBlocks.REEDS, ModBlocks.PEAT, ModBlocks.HANGING_SWAMP_MOSS,
                ModBlocks.GIANT_LILY_PAD, ModBlocks.LOTUS, ModBlocks.GLOWCAP, ModBlocks.GLOW_BLOOM,
                ModBlocks.GLOWING_HANGING_ROOTS, ModBlocks.BLIGHTED_GLOWCAP, ModBlocks.BUBBLE_SPRING)
                .<Item>map(DeferredBlock::asItem).toList();
        assertEquals(ring4, shown.subList(lastNetherRing3 + 1, lastNetherRing3 + 1 + ring4.size()));
        assertSame(ModBlocks.MOONPETAL_SAPLING.get(), ModBlocks.POTTED_MOONPETAL_SAPLING.get().getPotted());
        // Water plants are placed on the water surface, like the lily pad.
        assertInstanceOf(PlaceOnWaterBlockItem.class, ModBlocks.GIANT_LILY_PAD.asItem());
        assertInstanceOf(PlaceOnWaterBlockItem.class, ModBlocks.LOTUS.asItem());
    }

    /** Glowing flora: the light levels stream B chose (plan golf 1); the glowcaps never tick (no spreading). */
    @Test
    void ring4GlowingFloraGlowsSubtlyAndNeverTicks() {
        assertEquals(6, ModBlocks.GLOWCAP.get().defaultBlockState().getLightEmission());
        assertEquals(7, ModBlocks.GLOW_BLOOM.get().defaultBlockState().getLightEmission());
        assertEquals(5, ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState().getLightEmission());
        assertEquals(3, ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState().getLightEmission());
        for (DeferredBlock<?> block : List.of(ModBlocks.GLOWCAP, ModBlocks.BLIGHTED_GLOWCAP, ModBlocks.GLOW_BLOOM,
                ModBlocks.GLOWING_HANGING_ROOTS, ModBlocks.HANGING_SWAMP_MOSS, ModBlocks.BUBBLE_SPRING, ModBlocks.PEAT)) {
            assertFalse(block.get().defaultBlockState().isRandomlyTicking(), block.getId().toString());
        }
        // Strands of hanging moss hang straight (no sideways offset like hanging roots).
        assertEquals(Vec3.ZERO, ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState()
                .getOffset(EmptyBlockGetter.INSTANCE, new BlockPos(5, 64, 7)));
        // The reeds grow like sugar cane; the bubble spring and tidestone are stone.
        assertTrue(ModBlocks.REEDS.get().defaultBlockState().isRandomlyTicking());
        assertEquals(Blocks.STONE.defaultDestroyTime(), ModBlocks.BUBBLE_SPRING.get().defaultDestroyTime());
        assertEquals(Blocks.STONE.defaultDestroyTime(), ModBlocks.TIDESTONE.get().defaultDestroyTime());
    }

    /** All ring 4 wood burns like vanilla wood (contract §3), unlike the Nether and charred sets. */
    @Test
    void ring4WoodIsIgnitedByLava() {
        for (DeferredBlock<?> block : List.of(ModBlocks.MOONPETAL_LOG, ModBlocks.MOONPETAL_PLANKS, ModBlocks.MOONPETAL_LEAVES,
                ModBlocks.WITHERED_MOONPETAL_LOG, ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD)) {
            assertTrue(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
    }

    /** The ores and crystals follow the bubble spring; their loose items follow the fire crystal. */
    @Test
    void ring4OresAndCrystalsFollowTheNaturalBlocksInOrder() {
        List<Item> shown = blockItems();
        int spring = shown.indexOf(ModBlocks.BUBBLE_SPRING.asItem());
        List<Item> ores = Stream.of(ModBlocks.TIDESILVER_ORE, ModBlocks.TIDESTONE_TIDESILVER_ORE, ModBlocks.DEEPSLATE_TIDESILVER_ORE,
                ModBlocks.JADE_ORE, ModBlocks.TIDESTONE_JADE_ORE, ModBlocks.DEEPSLATE_JADE_ORE, ModBlocks.TOXIC_CRYSTAL_BLOCK,
                ModBlocks.BUDDING_TOXIC_CRYSTAL, ModBlocks.SMALL_TOXIC_CRYSTAL_BUD, ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD,
                ModBlocks.LARGE_TOXIC_CRYSTAL_BUD, ModBlocks.TOXIC_CRYSTAL_CLUSTER, ModBlocks.AQUAMARINE_BLOCK,
                ModBlocks.BUDDING_AQUAMARINE, ModBlocks.SMALL_AQUAMARINE_BUD, ModBlocks.MEDIUM_AQUAMARINE_BUD,
                ModBlocks.LARGE_AQUAMARINE_BUD, ModBlocks.AQUAMARINE_CLUSTER).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(ores, shown.subList(spring + 1, spring + 1 + ores.size()));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem))
                // The smithing templates, gear, consumables and arrows of phase 2 close their ring sections; these checks are
                // about the materials.
                .filter(item -> !(item instanceof SmithingTemplateItem) && !(item instanceof TieredItem)
                        && !(item instanceof ArmorItem) && !(item instanceof DoseDrinkItem)
                        && !(item instanceof HandWarmerItem) && !(item instanceof EffectArrowItem)
                        && !(item instanceof SunstoneFlashItem) && !(item instanceof VolcaniteHornItem)).toList();
        int fireCrystal = standalone.indexOf(ModItems.FIRE_CRYSTAL.get());
        assertEquals(List.of(ModItems.VOLCANITE_INGOT.get(), ModItems.RAW_TIDESILVER.get(), ModItems.JADE.get(),
                ModItems.TOXIC_CRYSTAL_SHARD.get(), ModItems.AQUAMARINE_SHARD.get()), standalone.subList(fireCrystal + 1, fireCrystal + 6));
    }

    /** Decision V3 golf 1: a whole point harder than the vanilla host ore, XP 4–7 / 5–9, only tidesilver glows. */
    @Test
    void ring4OresHaveTheirXpLightAndHardness() {
        Map<DeferredBlock<DropExperienceBlock>, Block> hosts = Map.of(ModBlocks.TIDESILVER_ORE, Blocks.IRON_ORE,
                ModBlocks.TIDESTONE_TIDESILVER_ORE, Blocks.IRON_ORE, ModBlocks.DEEPSLATE_TIDESILVER_ORE, Blocks.DEEPSLATE_IRON_ORE,
                ModBlocks.JADE_ORE, Blocks.DIAMOND_ORE, ModBlocks.TIDESTONE_JADE_ORE, Blocks.DIAMOND_ORE,
                ModBlocks.DEEPSLATE_JADE_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        hosts.forEach((ore, vanilla) -> {
            assertEquals(vanilla.defaultDestroyTime() + 1.0F, ore.get().defaultDestroyTime(), ore.getId().toString());
            boolean tidesilver = ore.getId().getPath().contains("tidesilver");
            assertEquals(tidesilver ? UniformInt.of(4, 7).toString() : UniformInt.of(5, 9).toString(), experience(ore.get()));
            assertEquals(tidesilver ? 3 : 0, ore.get().defaultBlockState().getLightEmission(), ore.getId().toString());
        });
    }

    /** Like amethyst: the stages give light 1, 2, 4, 5 and face up dry by default; the walls give no light. */
    @Test
    void ring4CrystalStagesGlowLikeAmethyst() {
        List<Block> vanilla = List.of(Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD,
                Blocks.AMETHYST_CLUSTER);
        for (List<DeferredBlock<AmethystClusterBlock>> stages : List.of(
                List.of(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD, ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD, ModBlocks.LARGE_TOXIC_CRYSTAL_BUD,
                        ModBlocks.TOXIC_CRYSTAL_CLUSTER),
                List.of(ModBlocks.SMALL_AQUAMARINE_BUD, ModBlocks.MEDIUM_AQUAMARINE_BUD, ModBlocks.LARGE_AQUAMARINE_BUD,
                        ModBlocks.AQUAMARINE_CLUSTER))) {
            for (int i = 0; i < stages.size(); i++) {
                BlockState stage = stages.get(i).get().defaultBlockState();
                assertEquals(vanilla.get(i).defaultBlockState().getLightEmission(), stage.getLightEmission());
                assertEquals(Direction.UP, stage.getValue(AmethystClusterBlock.FACING));
                assertFalse(stage.getValue(AmethystClusterBlock.WATERLOGGED));
            }
        }
        assertEquals(0, ModBlocks.TOXIC_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState().getLightEmission());
    }

    /** The gameplay blocks (72) follow the crystals; gas and toxic pool have no item, the bucket follows the shards. */
    @Test
    void ring4GameplayBlocksFollowTheCrystalsInOrder() {
        List<Item> shown = blockItems();
        int cluster = shown.indexOf(ModBlocks.AQUAMARINE_CLUSTER.asItem());
        List<Item> gameplay = Stream.of(ModBlocks.SUCKING_MUD, ModBlocks.UPDRAFT, ModBlocks.CLAWED_TIDESTONE,
                ModBlocks.SLIME_TRAIL, ModBlocks.CREATURE_NEST).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(gameplay, shown.subList(cluster + 1, cluster + 1 + gameplay.size()));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        assertEquals(ModItems.TOXIC_POOL_BUCKET.get(), standalone.get(standalone.indexOf(ModItems.AQUAMARINE_SHARD.get()) + 1));
        assertSame(Items.AIR, ModBlocks.SWAMP_GAS.get().asItem());
        assertSame(Items.AIR, ModBlocks.TOXIC_POOL.get().asItem());
        assertSame(ModFluids.TOXIC_POOL.get(), ModBlocks.TOXIC_POOL.get().defaultBlockState().getFluidState().getType());
        assertSame(ModFluidTypes.TOXIC_POOL.get(), ModFluids.FLOWING_TOXIC_POOL.get().getFluidType());
        assertSame(ModFluids.TOXIC_POOL.get(), ModItems.TOXIC_POOL_BUCKET.get().content);
    }

    /**
     * The gas, the updraft and the slime trail are walk-through and replaceable; the gas and the updraft cannot be aimed
     * at. The mud is solid for an item but has no floor for a living entity (checked in {@code SuckingMudBlockTest}).
     */
    @Test
    void ring4GameplayBlocksAreWalkThroughWhereTheyShouldBe() {
        for (DeferredBlock<?> block : List.of(ModBlocks.SWAMP_GAS, ModBlocks.UPDRAFT, ModBlocks.SLIME_TRAIL)) {
            BlockState state = block.get().defaultBlockState();
            assertTrue(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(), block.getId().toString());
            assertTrue(state.canBeReplaced(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.SWAMP_GAS, ModBlocks.UPDRAFT)) {
            assertTrue(block.get().defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty());
            assertFalse(block.get().defaultBlockState().isRandomlyTicking());
        }
        assertFalse(ModBlocks.SUCKING_MUD.get().defaultBlockState().isSuffocating(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertEquals(ModBlocks.TIDESTONE.get().defaultDestroyTime(), ModBlocks.CLAWED_TIDESTONE.get().defaultDestroyTime());
        assertTrue(ModBlocks.CREATURE_NEST.get().defaultBlockState().ignitedByLava());
        assertInstanceOf(MoonpetalLeavesBlock.class, ModBlocks.MOONPETAL_LEAVES.get());
    }

    /** The verdant flora (72b) follows the gameplay blocks; the fruit follows the bucket. */
    @Test
    void verdantFloraFollowsTheGameplayBlocksInOrder() {
        List<Item> shown = blockItems().stream().distinct().toList();
        int nest = shown.indexOf(ModBlocks.CREATURE_NEST.asItem());
        List<Item> verdant = Stream.of(ModBlocks.VERDANT_LOG, ModBlocks.VERDANT_WOOD, ModBlocks.STRIPPED_VERDANT_LOG,
                ModBlocks.STRIPPED_VERDANT_WOOD, ModBlocks.VERDANT_PLANKS, ModBlocks.VERDANT_STAIRS, ModBlocks.VERDANT_SLAB,
                ModBlocks.VERDANT_FENCE, ModBlocks.VERDANT_FENCE_GATE, ModBlocks.VERDANT_DOOR, ModBlocks.VERDANT_TRAPDOOR,
                ModBlocks.VERDANT_PRESSURE_PLATE, ModBlocks.VERDANT_BUTTON, ModBlocks.VERDANT_LEAVES, ModBlocks.VERDANT_SAPLING,
                ModBlocks.VERDANT_GIANT_LEAF, ModBlocks.VERDANT_FERN, ModBlocks.VERDANT_TALL_FERN, ModBlocks.VERDANT_BROMELIAD,
                ModBlocks.VERDANT_ORCHID, ModBlocks.VERDANT_HANGING_ORCHID, ModBlocks.VERDANT_BLOSSOM_BUSH,
                ModBlocks.VERDANT_VINE, ModBlocks.VERDANT_WATER_BLOOM).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(verdant, shown.subList(nest + 1, nest + 1 + verdant.size()));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        assertEquals(ModItems.VERDANT_FRUIT.get(), standalone.get(standalone.indexOf(ModItems.TOXIC_POOL_BUCKET.get()) + 1));
        assertSame(ModBlocks.VERDANT_SAPLING.get(), ModBlocks.POTTED_VERDANT_SAPLING.get().getPotted());
        assertInstanceOf(StandingAndWallBlockItem.class, ModBlocks.VERDANT_BROMELIAD.asItem());
        assertSame(ModBlocks.VERDANT_BROMELIAD.asItem(), ModBlocks.VERDANT_WALL_BROMELIAD.asItem());
        assertInstanceOf(DoubleHighBlockItem.class, ModBlocks.VERDANT_GIANT_LEAF.asItem());
        assertInstanceOf(DoubleHighBlockItem.class, ModBlocks.VERDANT_TALL_FERN.asItem());
        assertInstanceOf(PlaceOnWaterBlockItem.class, ModBlocks.VERDANT_WATER_BLOOM.asItem());
    }

    /**
     * The verdant flora behaves as planned: the glow is only a model layer (no block light, F8 = A), the wood burns,
     * the plants are bonemealable, the fern is replaceable but the giant leaf is not, the pod grows like cocoa.
     */
    @Test
    void verdantFloraBehavesAsPlanned() {
        for (DeferredBlock<?> block : List.of(ModBlocks.VERDANT_BROMELIAD, ModBlocks.VERDANT_WALL_BROMELIAD,
                ModBlocks.VERDANT_ORCHID, ModBlocks.VERDANT_LEAVES, ModBlocks.VERDANT_VINE)) {
            assertEquals(0, block.get().defaultBlockState().getLightEmission(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.VERDANT_LOG, ModBlocks.VERDANT_PLANKS, ModBlocks.VERDANT_LEAVES)) {
            assertTrue(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.VERDANT_GIANT_LEAF, ModBlocks.VERDANT_FERN, ModBlocks.VERDANT_TALL_FERN,
                ModBlocks.VERDANT_BROMELIAD, ModBlocks.VERDANT_WALL_BROMELIAD, ModBlocks.VERDANT_ORCHID,
                ModBlocks.VERDANT_HANGING_ORCHID, ModBlocks.VERDANT_BLOSSOM_BUSH, ModBlocks.VERDANT_VINE,
                ModBlocks.VERDANT_VINE_PLANT, ModBlocks.VERDANT_WATER_BLOOM, ModBlocks.VERDANT_FRUIT_POD,
                ModBlocks.VERDANT_SAPLING)) {
            assertInstanceOf(BonemealableBlock.class, block.get(), block.getId().toString());
        }
        assertTrue(ModBlocks.VERDANT_FERN.get().defaultBlockState().canBeReplaced());
        assertFalse(ModBlocks.VERDANT_GIANT_LEAF.get().defaultBlockState().canBeReplaced());
        assertTrue(ModBlocks.VERDANT_FRUIT_POD.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.VERDANT_VINE.get().defaultBlockState().getValue(VerdantVineBlock.FLOWERING));
        assertTrue(ModBlocks.VERDANT_VINE_PLANT.get().defaultBlockState().hasProperty(VerdantVineBlock.FLOWERING));
    }

    /** The verdant vine replaces the hanging moss on the climb routes: both of its blocks are climbable and walk-through. */
    @Test
    void verdantVineIsClimbable() {
        String relative = "src/generated/resources/data/minecraft/tags/block/climbable.json";
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve(relative))) {
            dir = dir.getParent();
        }
        Set<String> values = new HashSet<>();
        WorldgenData.read(dir.resolve(relative)).getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        for (DeferredBlock<?> block : List.of(ModBlocks.VERDANT_VINE, ModBlocks.VERDANT_VINE_PLANT)) {
            assertTrue(values.contains(block.getId().toString()), block.getId().toString());
            assertTrue(block.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty());
        }
    }

    /**
     * The decor rollout blocks follow the verdant flora; the plants grow on their ground tags, the glowgrass stands in
     * water, and the glow is modest (glowgrass 5, aquamarine stone 7).
     */
    @Test
    void ring4DecorBlocksFollowTheVerdantFloraAndBehaveAsPlanned() {
        List<Item> shown = blockItems();
        int waterBloom = shown.indexOf(ModBlocks.VERDANT_WATER_BLOOM.asItem());
        List<Item> decor = Stream.of(ModBlocks.MOSSY_TIDESTONE, ModBlocks.MOSSY_TIDESTONE_STAIRS, ModBlocks.MOSSY_TIDESTONE_SLAB,
                ModBlocks.MOSSY_TIDESTONE_WALL, ModBlocks.AQUAMARINE_STONE, ModBlocks.FJORD_MOSS, ModBlocks.MIST_FERN,
                ModBlocks.MOONPETAL_SHRUB, ModBlocks.LAGOON_GLOWGRASS).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(decor, shown.subList(waterBloom + 1, waterBloom + 1 + decor.size()));

        assertEquals(5, ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().getLightEmission());
        assertEquals(7, ModBlocks.AQUAMARINE_STONE.get().defaultBlockState().getLightEmission());
        for (DeferredBlock<?> block : List.of(ModBlocks.FJORD_MOSS, ModBlocks.MIST_FERN, ModBlocks.MOONPETAL_SHRUB,
                ModBlocks.MOSSY_TIDESTONE)) {
            assertEquals(0, block.get().defaultBlockState().getLightEmission(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.MOSSY_TIDESTONE, ModBlocks.AQUAMARINE_STONE)) {
            assertEquals(Blocks.STONE.defaultDestroyTime(), block.get().defaultDestroyTime(), block.getId().toString());
            assertTrue(block.get().defaultBlockState().requiresCorrectToolForDrops(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.FJORD_MOSS, ModBlocks.MIST_FERN, ModBlocks.MOONPETAL_SHRUB,
                ModBlocks.LAGOON_GLOWGRASS)) {
            assertInstanceOf(BonemealableBlock.class, block.get(), block.getId().toString());
            assertFalse(block.get().defaultBlockState().isRandomlyTicking(), block.getId().toString());
            assertTrue(block.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty());
        }
        // Grass and fern are replaceable, the shrub is a flower and is not.
        assertTrue(ModBlocks.FJORD_MOSS.get().defaultBlockState().canBeReplaced());
        assertTrue(ModBlocks.MIST_FERN.get().defaultBlockState().canBeReplaced());
        assertTrue(ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().canBeReplaced());
        assertFalse(ModBlocks.MOONPETAL_SHRUB.get().defaultBlockState().canBeReplaced());
        BlockState wet = ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().setValue(LagoonGlowgrassBlock.WATERLOGGED, true);
        assertTrue(wet.getFluidState().isSource());
        // Standing in water it does not burn (like seagrass).
        assertEquals(0, wet.getFlammability(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP));
        assertTrue(ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().getFluidState().isEmpty());
    }

    /**
     * The golf 3 blocks (66c) follow the decor blocks: the lagoon sand falls like sand with sand's strength and sound,
     * the claw marks keep the strength of their rock and need a pickaxe.
     */
    @Test
    void ring4Golf3BlocksFollowTheDecorBlocksAndKeepTheirRock() {
        List<Item> shown = blockItems();
        int glowgrass = shown.indexOf(ModBlocks.LAGOON_GLOWGRASS.asItem());
        List<Item> golf3 = Stream.of(ModBlocks.LAGOON_SAND, ModBlocks.LAGOON_SANDSTONE, ModBlocks.LAGOON_SANDSTONE_STAIRS,
                ModBlocks.LAGOON_SANDSTONE_SLAB, ModBlocks.LAGOON_SANDSTONE_WALL, ModBlocks.CUT_LAGOON_SANDSTONE,
                ModBlocks.CUT_LAGOON_SANDSTONE_SLAB, ModBlocks.CHISELED_LAGOON_SANDSTONE, ModBlocks.SMOOTH_LAGOON_SANDSTONE,
                ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS, ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB,
                ModBlocks.CLAWED_STONE, ModBlocks.CLAWED_DEEPSLATE).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(golf3, shown.subList(glowgrass + 1, glowgrass + 1 + golf3.size()));

        assertInstanceOf(ColoredFallingBlock.class, ModBlocks.LAGOON_SAND.get());
        assertEquals(Blocks.SAND.defaultDestroyTime(), ModBlocks.LAGOON_SAND.get().defaultDestroyTime());
        assertSame(SoundType.SAND, ModBlocks.LAGOON_SAND.get().defaultBlockState().getSoundType());
        assertFalse(ModBlocks.LAGOON_SAND.get().defaultBlockState().requiresCorrectToolForDrops());
        assertEquals(Blocks.STONE.defaultDestroyTime(), ModBlocks.CLAWED_STONE.get().defaultDestroyTime());
        assertEquals(Blocks.STONE.getExplosionResistance(), ModBlocks.CLAWED_STONE.get().getExplosionResistance());
        assertSame(SoundType.STONE, ModBlocks.CLAWED_STONE.get().defaultBlockState().getSoundType());
        assertEquals(Blocks.DEEPSLATE.defaultDestroyTime(), ModBlocks.CLAWED_DEEPSLATE.get().defaultDestroyTime());
        assertEquals(Blocks.DEEPSLATE.getExplosionResistance(), ModBlocks.CLAWED_DEEPSLATE.get().getExplosionResistance());
        assertSame(SoundType.DEEPSLATE, ModBlocks.CLAWED_DEEPSLATE.get().defaultBlockState().getSoundType());
        for (DeferredBlock<?> block : List.of(ModBlocks.CLAWED_STONE, ModBlocks.CLAWED_DEEPSLATE)) {
            assertTrue(block.get().defaultBlockState().requiresCorrectToolForDrops(), block.getId().toString());
        }
    }

    /** The lagoon sandstone set (66f) keeps the strength, blast resistance, sound and tool rule of its vanilla twin. */
    @Test
    void lagoonSandstoneMirrorsVanillaSandstone() {
        Map<DeferredBlock<?>, Block> twins = new LinkedHashMap<>();
        twins.put(ModBlocks.LAGOON_SANDSTONE, Blocks.SANDSTONE);
        twins.put(ModBlocks.LAGOON_SANDSTONE_STAIRS, Blocks.SANDSTONE_STAIRS);
        twins.put(ModBlocks.LAGOON_SANDSTONE_SLAB, Blocks.SANDSTONE_SLAB);
        twins.put(ModBlocks.LAGOON_SANDSTONE_WALL, Blocks.SANDSTONE_WALL);
        twins.put(ModBlocks.CUT_LAGOON_SANDSTONE, Blocks.CUT_SANDSTONE);
        twins.put(ModBlocks.CUT_LAGOON_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE_SLAB);
        twins.put(ModBlocks.CHISELED_LAGOON_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        twins.put(ModBlocks.SMOOTH_LAGOON_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
        twins.put(ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_STAIRS);
        twins.put(ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB);
        twins.forEach((lagoon, vanilla) -> {
            Block block = lagoon.get();
            String id = lagoon.getId().toString();
            assertSame(vanilla.getClass(), block.getClass(), id);
            assertEquals(vanilla.defaultDestroyTime(), block.defaultDestroyTime(), id);
            assertEquals(vanilla.getExplosionResistance(), block.getExplosionResistance(), id);
            assertSame(vanilla.defaultBlockState().getSoundType(), block.defaultBlockState().getSoundType(), id);
            assertEquals(vanilla.defaultBlockState().requiresCorrectToolForDrops(),
                    block.defaultBlockState().requiresCorrectToolForDrops(), id);
        });
    }

    /** The inlaid sets (phase 2 golf 4) keep the strength, sound, tool rule and map colour of the stone they inlay. */
    @Test
    void inlaidSetsMirrorTheirStone() {
        Map<DeferredBlock<?>, DeferredBlock<?>> twins = new LinkedHashMap<>();
        twins.put(ModBlocks.JADE_INLAID_TIDESTONE, ModBlocks.TIDESTONE);
        twins.put(ModBlocks.JADE_INLAID_TIDESTONE_STAIRS, ModBlocks.TIDESTONE_STAIRS);
        twins.put(ModBlocks.JADE_INLAID_TIDESTONE_SLAB, ModBlocks.TIDESTONE_SLAB);
        twins.put(ModBlocks.JADE_INLAID_TIDESTONE_WALL, ModBlocks.TIDESTONE_WALL);
        twins.put(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE, ModBlocks.LAGOON_SANDSTONE);
        twins.put(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS, ModBlocks.LAGOON_SANDSTONE_STAIRS);
        twins.put(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB, ModBlocks.LAGOON_SANDSTONE_SLAB);
        twins.put(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL, ModBlocks.LAGOON_SANDSTONE_WALL);
        twins.forEach((inlaid, stone) -> {
            Block block = inlaid.get();
            Block base = stone.get();
            String id = inlaid.getId().toString();
            assertSame(base.getClass(), block.getClass(), id);
            assertEquals(base.defaultDestroyTime(), block.defaultDestroyTime(), id);
            assertEquals(base.getExplosionResistance(), block.getExplosionResistance(), id);
            assertSame(base.defaultBlockState().getSoundType(), block.defaultBlockState().getSoundType(), id);
            assertEquals(base.defaultBlockState().requiresCorrectToolForDrops(),
                    block.defaultBlockState().requiresCorrectToolForDrops(), id);
            assertSame(base.defaultMapColor(), block.defaultMapColor(), id);
        });
    }

    /** Nether ring 4 follows ring 4 directly (contract ring 4 §3); its shard ends the loose items. */
    @Test
    void netherRing4FollowsRing4InOrderAndBehavesAsPlanned() {
        List<Item> shown = blockItems();
        // The storage blocks (phase 2 golf 1) and the inlaid sets (golf 4) end the ring 4 section.
        Item lastRing4Block = ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL.asItem();
        assertEquals(Stream.of(ModBlocks.CLAWED_DEEPSLATE, ModBlocks.RAW_TIDESILVER_BLOCK, ModBlocks.TIDESILVER_BLOCK,
                ModBlocks.JADE_BLOCK, ModBlocks.JADE_INLAID_TIDESTONE, ModBlocks.JADE_INLAID_TIDESTONE_STAIRS,
                ModBlocks.JADE_INLAID_TIDESTONE_SLAB, ModBlocks.JADE_INLAID_TIDESTONE_WALL,
                ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE, ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS,
                ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB, ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL)
                        .<Item>map(DeferredBlock::asItem).toList(),
                shown.subList(shown.indexOf(ModBlocks.CLAWED_DEEPSLATE.asItem()), shown.indexOf(lastRing4Block) + 1));
        int lastRing4 = shown.indexOf(lastRing4Block);
        List<Item> netherRing4 = Stream.of(ModBlocks.BONE_SAND, ModBlocks.BLEACHED_NETHERRACK, ModBlocks.FOSSIL_STONE,
                ModBlocks.FOSSIL_STONE_STAIRS, ModBlocks.FOSSIL_STONE_SLAB, ModBlocks.FOSSIL_STONE_WALL,
                ModBlocks.POLISHED_FOSSIL_STONE, ModBlocks.POLISHED_FOSSIL_STONE_STAIRS, ModBlocks.POLISHED_FOSSIL_STONE_SLAB,
                ModBlocks.POLISHED_FOSSIL_STONE_WALL, ModBlocks.FOSSIL_STONE_BRICKS, ModBlocks.FOSSIL_STONE_BRICK_STAIRS,
                ModBlocks.FOSSIL_STONE_BRICK_SLAB, ModBlocks.FOSSIL_STONE_BRICK_WALL, ModBlocks.PURITY_CRYSTAL_BLOCK,
                ModBlocks.PURITY_CRYSTAL_CLUSTER, ModBlocks.PURITY_CRYSTAL_LAMP,
                // The sky glass (phase 2 golf 5) ends the Nether ring 4 section.
                ModBlocks.SKY_SAND, ModBlocks.SKY_GLASS, ModBlocks.SKY_GLASS_PANE).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(netherRing4, shown.subList(lastRing4 + 1, lastRing4 + 1 + netherRing4.size()));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem))
                // The smithing templates, arrows and gear sets of phase 2 close their ring sections; these checks are
                // about the materials.
                .filter(item -> !(item instanceof SmithingTemplateItem) && !(item instanceof EffectArrowItem))
                .filter(item -> !(item instanceof TieredItem) && !(item instanceof ArmorItem))
                // So do the consumables of golf 5 (sky light, echo shell).
                .filter(item -> !(item instanceof DoseDrinkItem || item instanceof EchoShellItem))
                // Nor is the sky glass lens (golf 5), the tool that closes Nether ring 4.
                .filter(item -> !(item instanceof SkyGlassLensItem)).toList();
        int shard = standalone.indexOf(ModItems.PURITY_CRYSTAL_SHARD.get());
        assertEquals(List.of(ModItems.PURITY_CRYSTAL_SHARD.get(), ModItems.GLITCH_SHARD.get(), ModItems.RAW_NULLITE.get(),
                ModItems.NULLITE_INGOT.get(), ModItems.WHITE_CORE.get()), standalone.subList(shard, standalone.size()));

        assertInstanceOf(FallingBlock.class, ModBlocks.BONE_SAND.get());
        assertFalse(ModBlocks.BLEACHED_NETHERRACK.get() instanceof NetherrackBlock);
        assertEquals(Blocks.NETHERRACK.defaultDestroyTime(), ModBlocks.BLEACHED_NETHERRACK.get().defaultDestroyTime());
        assertEquals(15, ModBlocks.PURITY_CRYSTAL_LAMP.get().defaultBlockState().getLightEmission());
        assertEquals(5, ModBlocks.PURITY_CRYSTAL_CLUSTER.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.PURITY_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission());
        assertFalse(ModBlocks.PURITY_CRYSTAL_CLUSTER.get().defaultBlockState().isRandomlyTicking());
    }

    /** Phase 2 golf 5: the sky sand falls like sand; the sky glass and its pane keep vanilla glass's behaviour. */
    @Test
    void skyGlassBehavesLikeVanillaGlass() {
        assertInstanceOf(ColoredFallingBlock.class, ModBlocks.SKY_SAND.get());
        assertEquals(Blocks.SAND.defaultDestroyTime(), ModBlocks.SKY_SAND.get().defaultDestroyTime());
        assertInstanceOf(TransparentBlock.class, ModBlocks.SKY_GLASS.get());
        assertInstanceOf(IronBarsBlock.class, ModBlocks.SKY_GLASS_PANE.get());
        Map<Block, Block> twins = Map.of(ModBlocks.SKY_GLASS.get(), Blocks.GLASS, ModBlocks.SKY_GLASS_PANE.get(), Blocks.GLASS_PANE);
        twins.forEach((block, vanilla) -> {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            assertEquals(vanilla.defaultDestroyTime(), block.defaultDestroyTime(), id);
            assertEquals(vanilla.getExplosionResistance(), block.getExplosionResistance(), id);
            assertSame(vanilla.defaultBlockState().getSoundType(), block.defaultBlockState().getSoundType(), id);
            assertEquals(vanilla.defaultBlockState().canOcclude(), block.defaultBlockState().canOcclude(), id);
        });
    }

    /**
     * Ring 5 follows Nether ring 4 directly (contract ring 5 §3): the nine corrupted variants and the wood set (each sign
     * item once: the wall signs place through it), then glitch, voidstone and nullite; Nether ring 5 ends the blocks.
     */
    @Test
    void ring5AndNetherRing5FollowNetherRing4InOrder() {
        List<Item> shown = blockItems().stream().distinct().toList();
        int lastNetherRing4 = shown.indexOf(ModBlocks.SKY_GLASS_PANE.asItem());
        List<Item> ring5 = Stream.of(ModBlocks.CORRUPTED_GRASS_BLOCK, ModBlocks.CORRUPTED_DIRT, ModBlocks.CORRUPTED_STONE,
                ModBlocks.CORRUPTED_COBBLESTONE, ModBlocks.CORRUPTED_SAND, ModBlocks.CORRUPTED_LOG, ModBlocks.CORRUPTED_WOOD,
                ModBlocks.STRIPPED_CORRUPTED_LOG, ModBlocks.STRIPPED_CORRUPTED_WOOD, ModBlocks.CORRUPTED_PLANKS,
                ModBlocks.CORRUPTED_STAIRS, ModBlocks.CORRUPTED_SLAB, ModBlocks.CORRUPTED_FENCE, ModBlocks.CORRUPTED_FENCE_GATE,
                ModBlocks.CORRUPTED_DOOR, ModBlocks.CORRUPTED_TRAPDOOR, ModBlocks.CORRUPTED_PRESSURE_PLATE,
                ModBlocks.CORRUPTED_BUTTON, ModBlocks.CORRUPTED_SIGN, ModBlocks.CORRUPTED_HANGING_SIGN,
                ModBlocks.CORRUPTED_LEAVES, ModBlocks.GLITCH_BLOCK, ModBlocks.VOIDSTONE, ModBlocks.NULLITE_ORE,
                ModBlocks.VOIDSTONE_NULLITE_ORE, ModBlocks.RAW_NULLITE_BLOCK, ModBlocks.NULLITE_BLOCK,
                ModBlocks.GLITCH_SHARD_BLOCK)
                .<Item>map(DeferredBlock::asItem).toList();
        assertEquals(ring5, shown.subList(lastNetherRing4 + 1, lastNetherRing4 + 1 + ring5.size()));
        List<Item> netherRing5 = Stream.of(ModBlocks.CHALKRACK, ModBlocks.WHITE_STONE, ModBlocks.WHITE_STONE_STAIRS,
                ModBlocks.WHITE_STONE_SLAB, ModBlocks.WHITE_STONE_WALL, ModBlocks.POLISHED_WHITE_STONE,
                ModBlocks.POLISHED_WHITE_STONE_STAIRS, ModBlocks.POLISHED_WHITE_STONE_SLAB, ModBlocks.POLISHED_WHITE_STONE_WALL,
                ModBlocks.WHITE_STONE_BRICKS, ModBlocks.WHITE_STONE_BRICK_STAIRS, ModBlocks.WHITE_STONE_BRICK_SLAB,
                ModBlocks.WHITE_STONE_BRICK_WALL, ModBlocks.WHITE_STONE_PILLAR, ModBlocks.MIRROR_STONE, ModBlocks.STILL_LIGHT,
                ModBlocks.WHITE_CORE_ORE).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(17, netherRing5.size());
        assertEquals(netherRing5, shown.subList(lastNetherRing4 + 1 + ring5.size(), shown.size()));
        assertInstanceOf(SignItem.class, ModBlocks.CORRUPTED_SIGN.asItem());
        assertInstanceOf(HangingSignItem.class, ModBlocks.CORRUPTED_HANGING_SIGN.asItem());
        assertEquals(16, ModBlocks.CORRUPTED_SIGN.asItem().getDefaultMaxStackSize());
        assertEquals(16, ModBlocks.CORRUPTED_HANGING_SIGN.asItem().getDefaultMaxStackSize());
        assertInstanceOf(DoubleHighBlockItem.class, ModBlocks.CORRUPTED_DOOR.asItem());
    }

    /** Contract ring 5 §11: the grass never spreads, dies or turns into grass; sand falls; leaves decay like oak's. */
    @Test
    void corruptedVariantsBehaveLikeTheirCounterpartsExceptTheGrass() {
        BlockState grass = ModBlocks.CORRUPTED_GRASS_BLOCK.get().defaultBlockState();
        assertFalse(grass.isRandomlyTicking());
        assertSame(Block.class, ModBlocks.CORRUPTED_GRASS_BLOCK.get().getClass());
        assertEquals(Blocks.GRASS_BLOCK.defaultDestroyTime(), ModBlocks.CORRUPTED_GRASS_BLOCK.get().defaultDestroyTime());
        assertSame(SoundType.GRASS, grass.getSoundType());
        assertSame(Block.class, ModBlocks.CORRUPTED_DIRT.get().getClass());
        assertFalse(ModBlocks.CORRUPTED_DIRT.get().defaultBlockState().isRandomlyTicking());

        assertInstanceOf(ColoredFallingBlock.class, ModBlocks.CORRUPTED_SAND.get());
        assertEquals(Blocks.SAND.defaultDestroyTime(), ModBlocks.CORRUPTED_SAND.get().defaultDestroyTime());

        // Persistent leaves (placed by a player) never tick; natural ones do, and decay without a log in reach.
        BlockState leaves = ModBlocks.CORRUPTED_LEAVES.get().defaultBlockState();
        assertInstanceOf(LeavesBlock.class, leaves.getBlock());
        assertTrue(leaves.setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE)
                .isRandomlyTicking());
        assertFalse(leaves.setValue(LeavesBlock.PERSISTENT, true).isRandomlyTicking());

        Map<DeferredBlock<?>, Block> twins = new LinkedHashMap<>();
        twins.put(ModBlocks.CORRUPTED_DIRT, Blocks.DIRT);
        twins.put(ModBlocks.CORRUPTED_STONE, Blocks.STONE);
        twins.put(ModBlocks.CORRUPTED_COBBLESTONE, Blocks.COBBLESTONE);
        twins.put(ModBlocks.CORRUPTED_LOG, Blocks.OAK_LOG);
        twins.put(ModBlocks.CORRUPTED_PLANKS, Blocks.OAK_PLANKS);
        twins.put(ModBlocks.CORRUPTED_LEAVES, Blocks.OAK_LEAVES);
        twins.put(ModBlocks.CORRUPTED_SIGN, Blocks.OAK_SIGN);
        twins.put(ModBlocks.CORRUPTED_HANGING_SIGN, Blocks.OAK_HANGING_SIGN);
        twins.forEach((corrupted, vanilla) -> {
            String id = corrupted.getId().toString();
            assertEquals(vanilla.defaultDestroyTime(), corrupted.get().defaultDestroyTime(), id);
            assertEquals(vanilla.getExplosionResistance(), corrupted.get().getExplosionResistance(), id);
            assertSame(vanilla.defaultBlockState().getSoundType(), corrupted.get().defaultBlockState().getSoundType(), id);
        });
    }

    @Test
    void anAxeStripsTheCorruptedLogAndWoodKeepingTheAxis() {
        assertStripsTo(ModBlocks.CORRUPTED_LOG.get(), ModBlocks.STRIPPED_CORRUPTED_LOG.get());
        assertStripsTo(ModBlocks.CORRUPTED_WOOD.get(), ModBlocks.STRIPPED_CORRUPTED_WOOD.get());
    }

    /** The wall signs drop the standing sign (not oak's, which a full copy of the oak wall sign would keep). */
    @Test
    void corruptedWallSignsDropTheirStandingSign() {
        assertEquals(ModBlocks.CORRUPTED_SIGN.get().getLootTable(), ModBlocks.CORRUPTED_WALL_SIGN.get().getLootTable());
        assertEquals(ModBlocks.CORRUPTED_HANGING_SIGN.get().getLootTable(),
                ModBlocks.CORRUPTED_WALL_HANGING_SIGN.get().getLootTable());
    }

    /** Vanilla's sign block entity types accept the corrupted signs (ModSigns), so the signs keep their block entity. */
    @Test
    void vanillaSignBlockEntityTypesAcceptTheCorruptedSigns() {
        assertTrue(BlockEntityType.SIGN.isValid(ModBlocks.CORRUPTED_SIGN.get().defaultBlockState()));
        assertTrue(BlockEntityType.SIGN.isValid(ModBlocks.CORRUPTED_WALL_SIGN.get().defaultBlockState()));
        assertTrue(BlockEntityType.HANGING_SIGN.isValid(ModBlocks.CORRUPTED_HANGING_SIGN.get().defaultBlockState()));
        assertTrue(BlockEntityType.HANGING_SIGN.isValid(ModBlocks.CORRUPTED_WALL_HANGING_SIGN.get().defaultBlockState()));
        assertTrue(BlockEntityType.SIGN.isValid(Blocks.OAK_SIGN.defaultBlockState()));
        assertSame(ModWoodTypes.CORRUPTED, ModBlocks.CORRUPTED_SIGN.get().type());
        assertSame(ModWoodTypes.CORRUPTED, ModBlocks.CORRUPTED_WALL_HANGING_SIGN.get().type());
    }

    /**
     * 89 (OV2 = A): no block light anywhere (the stars and crystals are emissive in the models), no random ticks;
     * voidstone has deepslate's strength, nullite 1.5 above the vanilla ore of its host, XP 5-9.
     */
    @Test
    void glitchVoidAndNulliteHaveTheirStrengthXpAndNoLight() {
        for (DeferredBlock<?> block : List.of(ModBlocks.GLITCH_BLOCK, ModBlocks.VOIDSTONE, ModBlocks.NULLITE_ORE,
                ModBlocks.VOIDSTONE_NULLITE_ORE)) {
            BlockState state = block.get().defaultBlockState();
            assertEquals(0, state.getLightEmission(), block.getId().toString());
            assertFalse(state.isRandomlyTicking(), block.getId().toString());
            assertTrue(state.canOcclude(), block.getId().toString());
            assertTrue(state.requiresCorrectToolForDrops(), block.getId().toString());
        }
        assertEquals(Blocks.STONE.defaultDestroyTime(), ModBlocks.GLITCH_BLOCK.get().defaultDestroyTime());
        assertInstanceOf(VoidstoneBlock.class, ModBlocks.VOIDSTONE.get());
        assertEquals(Blocks.DEEPSLATE.defaultDestroyTime(), ModBlocks.VOIDSTONE.get().defaultDestroyTime());
        assertEquals(Blocks.DEEPSLATE.getExplosionResistance(), ModBlocks.VOIDSTONE.get().getExplosionResistance());
        assertSame(SoundType.DEEPSLATE, ModBlocks.VOIDSTONE.get().defaultBlockState().getSoundType());
        Map<DeferredBlock<DropExperienceBlock>, Block> hosts = Map.of(ModBlocks.NULLITE_ORE, Blocks.DIAMOND_ORE,
                ModBlocks.VOIDSTONE_NULLITE_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        hosts.forEach((ore, vanilla) -> {
            assertEquals(vanilla.defaultDestroyTime() + 1.5F, ore.get().defaultDestroyTime(), ore.getId().toString());
            assertEquals(UniformInt.of(5, 9).toString(), experience(ore.get()), ore.getId().toString());
        });
    }

    /**
     * 90 (OV2 = A): still light shines like shroomlight without sound or particles, the white core glows softly (3) and
     * sits 1.5 above diamond ore with XP 6-10; chalkrack is netherrack-hard but no nylium host; mirror stone is not slippery.
     */
    @Test
    void netherRing5BlocksBehaveAsPlanned() {
        assertEquals(15, ModBlocks.STILL_LIGHT.get().defaultBlockState().getLightEmission());
        assertSame(SoundType.GLASS, ModBlocks.STILL_LIGHT.get().defaultBlockState().getSoundType());
        assertSame(Block.class, ModBlocks.STILL_LIGHT.get().getClass());
        assertEquals(3, ModBlocks.WHITE_CORE_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(Blocks.DIAMOND_ORE.defaultDestroyTime() + 1.5F, ModBlocks.WHITE_CORE_ORE.get().defaultDestroyTime());
        assertEquals(UniformInt.of(6, 10).toString(), experience(ModBlocks.WHITE_CORE_ORE.get()));
        assertFalse(ModBlocks.CHALKRACK.get() instanceof NetherrackBlock);
        assertEquals(Blocks.NETHERRACK.defaultDestroyTime(), ModBlocks.CHALKRACK.get().defaultDestroyTime());
        assertEquals(Blocks.STONE.getFriction(), ModBlocks.MIRROR_STONE.get().getFriction());
        assertInstanceOf(RotatedPillarBlock.class, ModBlocks.WHITE_STONE_PILLAR.get());
        for (DeferredBlock<?> block : List.of(ModBlocks.CHALKRACK, ModBlocks.WHITE_STONE, ModBlocks.POLISHED_WHITE_STONE,
                ModBlocks.WHITE_STONE_BRICKS, ModBlocks.WHITE_STONE_PILLAR, ModBlocks.MIRROR_STONE)) {
            assertEquals(0, block.get().defaultBlockState().getLightEmission(), block.getId().toString());
            assertFalse(block.get().defaultBlockState().isRandomlyTicking(), block.getId().toString());
        }
        assertFalse(ModBlocks.STILL_LIGHT.get().defaultBlockState().isRandomlyTicking());
    }

    private static void assertStripsTo(Block log, Block stripped) {
        BlockHitResult hit = new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false);
        UseOnContext context = new UseOnContext(null, null, InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE), hit) {
        };
        BlockState state = log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState result = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);
        assertSame(stripped, result.getBlock());
        assertEquals(Direction.Axis.X, result.getValue(RotatedPillarBlock.AXIS));
    }

    private static List<Item> blockItems() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        return shown;
    }

    private static String experience(DropExperienceBlock ore) {
        JsonObject json = DropExperienceBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, ore).getOrThrow().getAsJsonObject();
        return IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("experience")).getOrThrow().toString();
    }
}
