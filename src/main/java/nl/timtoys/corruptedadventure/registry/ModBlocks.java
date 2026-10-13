package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.AshCrustBlock;
import nl.timtoys.corruptedadventure.block.AshCrustCollapse;
import nl.timtoys.corruptedadventure.block.AshSproutsBlock;
import nl.timtoys.corruptedadventure.block.BarrelCactusBlock;
import nl.timtoys.corruptedadventure.block.BoilingMudBlock;
import nl.timtoys.corruptedadventure.block.BubbleSpringBlock;
import nl.timtoys.corruptedadventure.block.BuddingCrystalBlock;
import nl.timtoys.corruptedadventure.block.GiantLilyPadBlock;
import nl.timtoys.corruptedadventure.block.GlowcapBlock;
import nl.timtoys.corruptedadventure.block.HangingSwampMossBlock;
import nl.timtoys.corruptedadventure.block.EmberBerryBushBlock;
import nl.timtoys.corruptedadventure.block.EmberLeavesBlock;
import nl.timtoys.corruptedadventure.block.FireBloomBlock;
import nl.timtoys.corruptedadventure.block.DeadGrassBlock;
import nl.timtoys.corruptedadventure.block.DesertFlowerBlock;
import nl.timtoys.corruptedadventure.block.DewSourceBlock;
import nl.timtoys.corruptedadventure.block.DryShrubBlock;
import nl.timtoys.corruptedadventure.block.FumaroleBlock;
import nl.timtoys.corruptedadventure.block.GeyserBlock;
import nl.timtoys.corruptedadventure.block.GlowMoldBlock;
import nl.timtoys.corruptedadventure.block.LooseSandTrigger;
import nl.timtoys.corruptedadventure.block.ModFlowerPots;
import nl.timtoys.corruptedadventure.block.ModTreeGrowers;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;
import nl.timtoys.corruptedadventure.block.PalmSaplingBlock;
import nl.timtoys.corruptedadventure.block.ReedsBlock;
import nl.timtoys.corruptedadventure.block.SoakedNetherrackBlock;
import nl.timtoys.corruptedadventure.block.SporeMossBlock;
import nl.timtoys.corruptedadventure.block.SporeSproutsBlock;
import nl.timtoys.corruptedadventure.block.StalactiteBlock;
import nl.timtoys.corruptedadventure.block.StrippableLogBlock;
import nl.timtoys.corruptedadventure.block.WitheredDirtBlock;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * All blocks, each with its block item in {@link ModItems} (except potted plants). Registration order is the creative
 * tab order: per ring, ground first, then the wood set in vanilla order.
 */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CorruptedAdventure.MOD_ID);
    /** Vanilla sand's falling dust colour. */
    private static final int SAND_DUST_COLOR = 0xDBD3A0;
    /** Falling dust colour of the ash crust: warm ash grey. */
    private static final int ASH_DUST_COLOR = 0x8A8580;
    /** Falling dust colour of the bone sand: pale bone white. */
    private static final int BONE_DUST_COLOR = 0xE3DCC6;
    /** Ring 3 ores are this much harder than the vanilla ore of their host ("one step above ring 2"). */
    static final float RING_3_ORE_EXTRA_HARDNESS = 0.5F;
    /** Ring 4 ores are this much harder than the vanilla ore of their host (a step above ring 3; decision V3 golf 1). */
    static final float RING_4_ORE_EXTRA_HARDNESS = 1.0F;

    // ---- Ring 1 ----
    public static final DeferredBlock<DeadGrassBlock> DEAD_GRASS_BLOCK = register("dead_grass_block",
            () -> new DeadGrassBlock(Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.WOOD)));
    public static final DeferredBlock<WitheredDirtBlock> WITHERED_DIRT = register("withered_dirt",
            () -> new WitheredDirtBlock(Properties.ofFullCopy(Blocks.DIRT)));

    // Qualified forward references: the suppliers run at registration, after all fields are set.
    public static final DeferredBlock<StrippableLogBlock> DEAD_LOG = register("dead_log",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_DEAD_LOG, Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<StrippableLogBlock> DEAD_WOOD = register("dead_wood",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_DEAD_WOOD, Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_DEAD_LOG = register("stripped_dead_log",
            () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_DEAD_WOOD = register("stripped_dead_wood",
            () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));
    public static final DeferredBlock<Block> DEAD_PLANKS = register("dead_planks",
            () -> new Block(Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<StairBlock> DEAD_STAIRS = register("dead_stairs",
            () -> new StairBlock(DEAD_PLANKS.get().defaultBlockState(), Properties.ofFullCopy(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<SlabBlock> DEAD_SLAB = register("dead_slab",
            () -> new SlabBlock(Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<FenceBlock> DEAD_FENCE = register("dead_fence",
            () -> new FenceBlock(Properties.ofFullCopy(Blocks.OAK_FENCE)));
    public static final DeferredBlock<FenceGateBlock> DEAD_FENCE_GATE = register("dead_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.DEAD, Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> DEAD_DOOR = registerDoor("dead_door",
            () -> new DoorBlock(ModWoodTypes.DEAD_SET, Properties.ofFullCopy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> DEAD_TRAPDOOR = register("dead_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.DEAD_SET, Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> DEAD_PRESSURE_PLATE = register("dead_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.DEAD_SET, Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> DEAD_BUTTON = register("dead_button",
            () -> new ButtonBlock(ModWoodTypes.DEAD_SET, 30, Properties.ofFullCopy(Blocks.OAK_BUTTON)));

    // ---- Nether ring 1 ----
    /** Soul crystal in netherrack; drops {@link ModItems#SOUL_CRYSTAL}. Faint glow, no particles. */
    public static final DeferredBlock<DropExperienceBlock> SOUL_CRYSTAL_ORE = register("soul_crystal_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), Properties.ofFullCopy(Blocks.NETHER_QUARTZ_ORE)
                    .lightLevel(state -> 3)));
    /** Soul crystal in crimson nylium: looks like nylium but is an ore (no spreading, no bonemeal, not {@code #nylium}). */
    public static final DeferredBlock<DropExperienceBlock> CRIMSON_SOUL_CRYSTAL_ORE = register("crimson_soul_crystal_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), Properties.ofFullCopy(Blocks.NETHER_QUARTZ_ORE)
                    .mapColor(MapColor.CRIMSON_NYLIUM).sound(SoundType.NYLIUM).lightLevel(state -> 3)));

    // ---- Ring 2 ----
    // Canyon stone: three natural tints that worldgen layers into strata, plus cobblestone and bricks.
    public static final DeferredBlock<Block> RED_CANYON_STONE = register("red_canyon_stone",
            () -> new Block(Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<Block> ORANGE_CANYON_STONE = register("orange_canyon_stone",
            () -> new Block(Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<Block> CREAM_CANYON_STONE = register("cream_canyon_stone",
            () -> new Block(Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<Block> CANYON_COBBLESTONE = register("canyon_cobblestone",
            () -> new Block(Properties.ofFullCopy(Blocks.COBBLESTONE).mapColor(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<StairBlock> CANYON_COBBLESTONE_STAIRS = register("canyon_cobblestone_stairs",
            () -> new StairBlock(CANYON_COBBLESTONE.get().defaultBlockState(),
                    Properties.ofFullCopy(Blocks.COBBLESTONE_STAIRS).mapColor(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<SlabBlock> CANYON_COBBLESTONE_SLAB = register("canyon_cobblestone_slab",
            () -> new SlabBlock(Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB).mapColor(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<WallBlock> CANYON_COBBLESTONE_WALL = register("canyon_cobblestone_wall",
            () -> new WallBlock(Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).mapColor(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<Block> CANYON_STONE_BRICKS = register("canyon_stone_bricks",
            () -> new Block(Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<StairBlock> CANYON_STONE_BRICK_STAIRS = register("canyon_stone_brick_stairs",
            () -> new StairBlock(CANYON_STONE_BRICKS.get().defaultBlockState(),
                    Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS).mapColor(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<SlabBlock> CANYON_STONE_BRICK_SLAB = register("canyon_stone_brick_slab",
            () -> new SlabBlock(Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB).mapColor(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<WallBlock> CANYON_STONE_BRICK_WALL = register("canyon_stone_brick_wall",
            () -> new WallBlock(Properties.ofFullCopy(Blocks.STONE_BRICK_WALL).mapColor(MapColor.TERRACOTTA_RED)));

    // Scorched sandstone: the same set as vanilla sandstone.
    public static final DeferredBlock<Block> SCORCHED_SANDSTONE = register("scorched_sandstone",
            () -> new Block(scorched(Blocks.SANDSTONE)));
    public static final DeferredBlock<StairBlock> SCORCHED_SANDSTONE_STAIRS = register("scorched_sandstone_stairs",
            () -> new StairBlock(SCORCHED_SANDSTONE.get().defaultBlockState(), scorched(Blocks.SANDSTONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> SCORCHED_SANDSTONE_SLAB = register("scorched_sandstone_slab",
            () -> new SlabBlock(scorched(Blocks.SANDSTONE_SLAB)));
    public static final DeferredBlock<WallBlock> SCORCHED_SANDSTONE_WALL = register("scorched_sandstone_wall",
            () -> new WallBlock(scorched(Blocks.SANDSTONE_WALL)));
    public static final DeferredBlock<Block> CUT_SCORCHED_SANDSTONE = register("cut_scorched_sandstone",
            () -> new Block(scorched(Blocks.CUT_SANDSTONE)));
    public static final DeferredBlock<SlabBlock> CUT_SCORCHED_SANDSTONE_SLAB = register("cut_scorched_sandstone_slab",
            () -> new SlabBlock(scorched(Blocks.CUT_SANDSTONE_SLAB)));
    public static final DeferredBlock<Block> CHISELED_SCORCHED_SANDSTONE = register("chiseled_scorched_sandstone",
            () -> new Block(scorched(Blocks.CHISELED_SANDSTONE)));
    public static final DeferredBlock<Block> SMOOTH_SCORCHED_SANDSTONE = register("smooth_scorched_sandstone",
            () -> new Block(scorched(Blocks.SMOOTH_SANDSTONE)));
    public static final DeferredBlock<StairBlock> SMOOTH_SCORCHED_SANDSTONE_STAIRS = register("smooth_scorched_sandstone_stairs",
            () -> new StairBlock(SMOOTH_SCORCHED_SANDSTONE.get().defaultBlockState(), scorched(Blocks.SMOOTH_SANDSTONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> SMOOTH_SCORCHED_SANDSTONE_SLAB = register("smooth_scorched_sandstone_slab",
            () -> new SlabBlock(scorched(Blocks.SMOOTH_SANDSTONE_SLAB)));

    /**
     * Looks like sand and falls like sand; it also caves in when a player walks under it ({@link LooseSandTrigger}).
     * A plain vanilla class: all of its own behaviour lives in the trigger.
     */
    public static final DeferredBlock<ColoredFallingBlock> LOOSE_SAND = register("loose_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(SAND_DUST_COLOR), Properties.ofFullCopy(Blocks.SAND)));

    // Desert flora and its ground.
    public static final DeferredBlock<Block> CRACKED_CLAY = register("cracked_clay",
            () -> new Block(Properties.ofFullCopy(Blocks.CLAY).mapColor(MapColor.DIRT).sound(SoundType.PACKED_MUD)));
    public static final DeferredBlock<DryShrubBlock> DRY_SHRUB = register("dry_shrub",
            () -> new DryShrubBlock(Properties.ofFullCopy(Blocks.DEAD_BUSH)));
    /** Cactus properties without random ticks (it never grows). */
    public static final DeferredBlock<BarrelCactusBlock> BARREL_CACTUS = register("barrel_cactus",
            () -> new BarrelCactusBlock(Properties.of().mapColor(MapColor.PLANT).strength(0.4F).sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DesertFlowerBlock> DESERT_FLOWER = register("desert_flower",
            () -> new DesertFlowerBlock(MobEffects.FIRE_RESISTANCE, 4.0F, Properties.ofFullCopy(Blocks.POPPY)));
    public static final DeferredBlock<FlowerPotBlock> POTTED_DRY_SHRUB = registerPot("potted_dry_shrub", DRY_SHRUB);
    public static final DeferredBlock<FlowerPotBlock> POTTED_BARREL_CACTUS = registerPot("potted_barrel_cactus", BARREL_CACTUS);
    public static final DeferredBlock<FlowerPotBlock> POTTED_DESERT_FLOWER = registerPot("potted_desert_flower", DESERT_FLOWER);

    // Palm: light sand-yellow wood under grey-brown bark (map colours: bark on the sides, wood on the ends).
    public static final DeferredBlock<StrippableLogBlock> PALM_LOG = register("palm_log",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_PALM_LOG, Properties.ofFullCopy(Blocks.OAK_LOG)
                    .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y
                            ? MapColor.SAND : MapColor.TERRACOTTA_BROWN)));
    public static final DeferredBlock<StrippableLogBlock> PALM_WOOD = register("palm_wood",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_PALM_WOOD, Properties.ofFullCopy(Blocks.OAK_WOOD)
                    .mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_PALM_LOG = register("stripped_palm_log",
            () -> new RotatedPillarBlock(palm(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_PALM_WOOD = register("stripped_palm_wood",
            () -> new RotatedPillarBlock(palm(Blocks.STRIPPED_OAK_WOOD)));
    public static final DeferredBlock<Block> PALM_PLANKS = register("palm_planks",
            () -> new Block(palm(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<StairBlock> PALM_STAIRS = register("palm_stairs",
            () -> new StairBlock(PALM_PLANKS.get().defaultBlockState(), palm(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<SlabBlock> PALM_SLAB = register("palm_slab",
            () -> new SlabBlock(palm(Blocks.OAK_SLAB)));
    public static final DeferredBlock<FenceBlock> PALM_FENCE = register("palm_fence",
            () -> new FenceBlock(palm(Blocks.OAK_FENCE)));
    public static final DeferredBlock<FenceGateBlock> PALM_FENCE_GATE = register("palm_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.PALM, palm(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> PALM_DOOR = registerDoor("palm_door",
            () -> new DoorBlock(ModWoodTypes.PALM_SET, palm(Blocks.OAK_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> PALM_TRAPDOOR = register("palm_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.PALM_SET, palm(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> PALM_PRESSURE_PLATE = register("palm_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.PALM_SET, palm(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> PALM_BUTTON = register("palm_button",
            () -> new ButtonBlock(ModWoodTypes.PALM_SET, 30, palm(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<LeavesBlock> PALM_LEAVES = register("palm_leaves",
            () -> new LeavesBlock(Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    /** Grows into {@code corruptedadventure:palm_tree} (change 23); until then bonemeal is used up without growth. */
    public static final DeferredBlock<PalmSaplingBlock> PALM_SAPLING = register("palm_sapling",
            () -> new PalmSaplingBlock(ModTreeGrowers.PALM, Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<FlowerPotBlock> POTTED_PALM_SAPLING = registerPot("potted_palm_sapling", PALM_SAPLING);

    // Ores: one block per host rock, so each blends into its own stratum (worldgen picks the variant per rock).
    /** Sunstone in red canyon stone; drops {@link ModItems#RAW_SUNSTONE}. Faint glow (light 3, emissive overlay). */
    public static final DeferredBlock<DropExperienceBlock> RED_CANYON_SUNSTONE_ORE = register("red_canyon_sunstone_ore",
            () -> sunstoneOre(MapColor.TERRACOTTA_RED));
    public static final DeferredBlock<DropExperienceBlock> ORANGE_CANYON_SUNSTONE_ORE = register("orange_canyon_sunstone_ore",
            () -> sunstoneOre(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<DropExperienceBlock> CREAM_CANYON_SUNSTONE_ORE = register("cream_canyon_sunstone_ore",
            () -> sunstoneOre(MapColor.TERRACOTTA_WHITE));
    public static final DeferredBlock<DropExperienceBlock> SCORCHED_SANDSTONE_SUNSTONE_ORE = register("scorched_sandstone_sunstone_ore",
            () -> sunstoneOre(MapColor.TERRACOTTA_BROWN));
    /** Turquoise in red canyon stone; drops {@link ModItems#RAW_TURQUOISE}. No light: a matte gem that has to be found. */
    public static final DeferredBlock<DropExperienceBlock> RED_CANYON_TURQUOISE_ORE = register("red_canyon_turquoise_ore",
            () -> turquoiseOre(MapColor.TERRACOTTA_RED));
    public static final DeferredBlock<DropExperienceBlock> ORANGE_CANYON_TURQUOISE_ORE = register("orange_canyon_turquoise_ore",
            () -> turquoiseOre(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<DropExperienceBlock> CREAM_CANYON_TURQUOISE_ORE = register("cream_canyon_turquoise_ore",
            () -> turquoiseOre(MapColor.TERRACOTTA_WHITE));

    // Salt: the crust and block of the salt flats, polished salt to build with, and a crystal for decoration.
    /**
     * Top layer of the salt flats; drops {@link ModItems#SALT}. Sand's strength and sound, but it does not fall: the
     * flats lie above caves, and a falling crust would cave in at every hole.
     */
    public static final DeferredBlock<Block> SALT_CRUST = register("salt_crust",
            () -> new Block(Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.SNOW)));
    public static final DeferredBlock<Block> SALT_BLOCK = register("salt_block",
            () -> new Block(Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ)));
    public static final DeferredBlock<Block> POLISHED_SALT = register("polished_salt",
            () -> new Block(Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ)));
    public static final DeferredBlock<StairBlock> POLISHED_SALT_STAIRS = register("polished_salt_stairs",
            () -> new StairBlock(POLISHED_SALT.get().defaultBlockState(), Properties.ofFullCopy(POLISHED_SALT.get())));
    public static final DeferredBlock<SlabBlock> POLISHED_SALT_SLAB = register("polished_salt_slab",
            () -> new SlabBlock(Properties.ofFullCopy(POLISHED_SALT.get())));
    /** Solid for support like vanilla walls ({@code forceSolidOn}), which a copy of a full block does not carry. */
    public static final DeferredBlock<WallBlock> POLISHED_SALT_WALL = register("polished_salt_wall",
            () -> new WallBlock(Properties.ofFullCopy(POLISHED_SALT.get()).forceSolidOn()));
    /**
     * Vanilla's amethyst cluster behaviour (any face, waterloggable, breaks without support) in its shape, with a faint
     * glow (light 3). The default state faces up, which worldgen places on the crust.
     */
    public static final DeferredBlock<AmethystClusterBlock> SALT_CRYSTAL = register("salt_crystal",
            () -> new AmethystClusterBlock(7.0F, 3.0F, Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER)
                    .mapColor(MapColor.QUARTZ).lightLevel(state -> 3)));

    // ---- Nether ring 2 ----
    // Wet fungal Nether: soaked netherrack with spore moss on top, the dripshroom wood set, cap and lamp, and the
    // growths on walls, ceilings and floor. Nether wood: no fire odds, no furnace fuel (like crimson and warped).
    public static final DeferredBlock<SoakedNetherrackBlock> SOAKED_NETHERRACK = register("soaked_netherrack",
            () -> new SoakedNetherrackBlock(Properties.ofFullCopy(Blocks.NETHERRACK)
                    .mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.MUD)));
    public static final DeferredBlock<SporeMossBlock> SPORE_MOSS = register("spore_moss",
            () -> new SporeMossBlock(Properties.ofFullCopy(Blocks.CRIMSON_NYLIUM)
                    .mapColor(MapColor.TERRACOTTA_GREEN).randomTicks()));

    public static final DeferredBlock<StrippableLogBlock> DRIPSHROOM_STEM = register("dripshroom_stem",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_DRIPSHROOM_STEM, dripshroomBark(Blocks.CRIMSON_STEM)));
    public static final DeferredBlock<StrippableLogBlock> DRIPSHROOM_HYPHAE = register("dripshroom_hyphae",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE, dripshroomBark(Blocks.CRIMSON_HYPHAE)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_DRIPSHROOM_STEM = register("stripped_dripshroom_stem",
            () -> new RotatedPillarBlock(dripshroom(Blocks.STRIPPED_CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_DRIPSHROOM_HYPHAE = register("stripped_dripshroom_hyphae",
            () -> new RotatedPillarBlock(dripshroom(Blocks.STRIPPED_CRIMSON_HYPHAE)));
    public static final DeferredBlock<Block> DRIPSHROOM_PLANKS = register("dripshroom_planks",
            () -> new Block(dripshroom(Blocks.CRIMSON_PLANKS)));
    public static final DeferredBlock<StairBlock> DRIPSHROOM_STAIRS = register("dripshroom_stairs",
            () -> new StairBlock(DRIPSHROOM_PLANKS.get().defaultBlockState(), dripshroom(Blocks.CRIMSON_STAIRS)));
    public static final DeferredBlock<SlabBlock> DRIPSHROOM_SLAB = register("dripshroom_slab",
            () -> new SlabBlock(dripshroom(Blocks.CRIMSON_SLAB)));
    public static final DeferredBlock<FenceBlock> DRIPSHROOM_FENCE = register("dripshroom_fence",
            () -> new FenceBlock(dripshroom(Blocks.CRIMSON_FENCE)));
    public static final DeferredBlock<FenceGateBlock> DRIPSHROOM_FENCE_GATE = register("dripshroom_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.DRIPSHROOM, dripshroom(Blocks.CRIMSON_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> DRIPSHROOM_DOOR = registerDoor("dripshroom_door",
            () -> new DoorBlock(ModWoodTypes.DRIPSHROOM_SET, dripshroom(Blocks.CRIMSON_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> DRIPSHROOM_TRAPDOOR = register("dripshroom_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.DRIPSHROOM_SET, dripshroom(Blocks.CRIMSON_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> DRIPSHROOM_PRESSURE_PLATE = register("dripshroom_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.DRIPSHROOM_SET, dripshroom(Blocks.CRIMSON_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> DRIPSHROOM_BUTTON = register("dripshroom_button",
            () -> new ButtonBlock(ModWoodTypes.DRIPSHROOM_SET, 30, dripshroom(Blocks.CRIMSON_BUTTON)));

    /** The giant fungus' cap: a wart block (hoe, drops itself). */
    public static final DeferredBlock<Block> DRIPSHROOM_CAP = register("dripshroom_cap",
            () -> new Block(Properties.ofFullCopy(Blocks.NETHER_WART_BLOCK).mapColor(MapColor.TERRACOTTA_PURPLE)));
    /** The giant fungus' lamp: shroomlight (light 15, hoe, drops itself). */
    public static final DeferredBlock<Block> DRIPSHROOM_LAMP = register("dripshroom_lamp",
            () -> new Block(Properties.ofFullCopy(Blocks.SHROOMLIGHT).mapColor(MapColor.COLOR_LIGHT_GREEN)));

    /** A multiface layer on walls, ceilings and floors; glow lichen's light (7), shears and bonemeal. */
    public static final DeferredBlock<GlowMoldBlock> GLOW_MOLD = register("glow_mold",
            () -> new GlowMoldBlock(Properties.ofFullCopy(Blocks.GLOW_LICHEN).mapColor(MapColor.WARPED_WART_BLOCK)
                    .lightLevel(GlowLichenBlock.emission(7))));
    /**
     * Pointed dripstone's properties minus random ticks (it neither grows nor drips here), with slime sounds. Falls
     * when its support goes; the column falls as one.
     */
    public static final DeferredBlock<StalactiteBlock> SLIME_STALACTITE = register("slime_stalactite",
            () -> new StalactiteBlock(Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).forceSolidOn()
                    .instrument(NoteBlockInstrument.BASEDRUM).noOcclusion().sound(SoundType.SLIME_BLOCK)
                    .strength(1.5F, 3.0F).dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, level, pos) -> false)));
    /** Small ground plant on spore moss and soaked netherrack. Plucked by hand: no random ticks, nothing to grow. */
    public static final DeferredBlock<SporeSproutsBlock> SPORE_SPROUTS = register("spore_sprouts",
            () -> new SporeSproutsBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(MapColor.TERRACOTTA_GREEN)));

    /**
     * Budding amethyst's properties (hard to break, pushed away by pistons) plus random ticks, set explicitly: it grows
     * dew crystal on its sides. Drops nothing, so every source in the world comes from worldgen.
     */
    public static final DeferredBlock<DewSourceBlock> DEW_SOURCE = register("dew_source",
            () -> new DewSourceBlock(Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).mapColor(MapColor.WATER).randomTicks()));
    /** The dew crystal stages are vanilla's amethyst bud behaviour and shapes, with the same light levels (1, 2, 4, 5). */
    public static final DeferredBlock<AmethystClusterBlock> SMALL_DEW_CRYSTAL_BUD = register("small_dew_crystal_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, crystalStage(Blocks.SMALL_AMETHYST_BUD, MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_DEW_CRYSTAL_BUD = register("medium_dew_crystal_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, crystalStage(Blocks.MEDIUM_AMETHYST_BUD, MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_DEW_CRYSTAL_BUD = register("large_dew_crystal_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, crystalStage(Blocks.LARGE_AMETHYST_BUD, MapColor.COLOR_LIGHT_BLUE)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> DEW_CRYSTAL_CLUSTER = register("dew_crystal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, crystalStage(Blocks.AMETHYST_CLUSTER, MapColor.COLOR_LIGHT_BLUE)));

    // ---- Ring 3 ----
    // Volcanic ring: ash ground, pumice, smouldering stone and the charred wood set.
    /** Plain ash ground: sand's strength and sound, but it does not fall. */
    public static final DeferredBlock<Block> ASH_BLOCK = register("ash_block",
            () -> new Block(Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_GRAY)));
    /**
     * Vanilla's snow layer (1–8 layers, stacking, survival, no drops without a shovel) with properties that leave out
     * the random ticks: a snow layer only melts from a random tick, so ash never melts. In no snow tag. Like any full
     * light-occluder it smothers grass under it, even at one layer (vanilla only spares grass under one snow layer).
     */
    public static final DeferredBlock<SnowLayerBlock> ASH_LAYER = register("ash_layer",
            () -> new SnowLayerBlock(Properties.of().mapColor(MapColor.COLOR_GRAY).replaceable().forceSolidOff()
                    .strength(0.1F).requiresCorrectToolForDrops().sound(SoundType.SAND)
                    .isViewBlocking((state, level, pos) -> state.getValue(SnowLayerBlock.LAYERS) >= SnowLayerBlock.MAX_HEIGHT)
                    .pushReaction(PushReaction.DESTROY)));
    /**
     * A crust of ash over caves and lava tubes: hangs (no fall from updates) until a player walks under it, then the
     * connected hanging crust caves in (38c; the {@code collapsing_blocks} tag, {@link LooseSandTrigger},
     * {@link AshCrustCollapse}). Breaks into plain ash.
     */
    public static final DeferredBlock<AshCrustBlock> ASH_CRUST = register("ash_crust",
            () -> new AshCrustBlock(new ColorRGBA(ASH_DUST_COLOR), Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_GRAY)));
    /** Ash with glowing cracks, the material of the glowing ash casts: light 2 and an emissive glow overlay. */
    public static final DeferredBlock<Block> SMOLDERING_ASH = register("smoldering_ash",
            () -> new Block(Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_BLACK).lightLevel(state -> 2)));

    // Pumice: the tuff family's shapes and sounds in a light porous stone.
    public static final DeferredBlock<Block> PUMICE = register("pumice", () -> new Block(pumice(Blocks.TUFF)));
    public static final DeferredBlock<StairBlock> PUMICE_STAIRS = register("pumice_stairs",
            () -> new StairBlock(PUMICE.get().defaultBlockState(), pumice(Blocks.TUFF_STAIRS)));
    public static final DeferredBlock<SlabBlock> PUMICE_SLAB = register("pumice_slab",
            () -> new SlabBlock(pumice(Blocks.TUFF_SLAB)));
    public static final DeferredBlock<WallBlock> PUMICE_WALL = register("pumice_wall",
            () -> new WallBlock(pumice(Blocks.TUFF_WALL)));
    public static final DeferredBlock<Block> POLISHED_PUMICE = register("polished_pumice",
            () -> new Block(pumice(Blocks.POLISHED_TUFF)));
    public static final DeferredBlock<StairBlock> POLISHED_PUMICE_STAIRS = register("polished_pumice_stairs",
            () -> new StairBlock(POLISHED_PUMICE.get().defaultBlockState(), pumice(Blocks.POLISHED_TUFF_STAIRS)));
    public static final DeferredBlock<SlabBlock> POLISHED_PUMICE_SLAB = register("polished_pumice_slab",
            () -> new SlabBlock(pumice(Blocks.POLISHED_TUFF_SLAB)));
    public static final DeferredBlock<WallBlock> POLISHED_PUMICE_WALL = register("polished_pumice_wall",
            () -> new WallBlock(pumice(Blocks.POLISHED_TUFF_WALL)));
    public static final DeferredBlock<Block> PUMICE_BRICKS = register("pumice_bricks",
            () -> new Block(pumice(Blocks.TUFF_BRICKS)));
    public static final DeferredBlock<StairBlock> PUMICE_BRICK_STAIRS = register("pumice_brick_stairs",
            () -> new StairBlock(PUMICE_BRICKS.get().defaultBlockState(), pumice(Blocks.TUFF_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> PUMICE_BRICK_SLAB = register("pumice_brick_slab",
            () -> new SlabBlock(pumice(Blocks.TUFF_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> PUMICE_BRICK_WALL = register("pumice_brick_wall",
            () -> new WallBlock(pumice(Blocks.TUFF_BRICK_WALL)));

    /**
     * Blackstone with glowing cracks (light 7; only the cracks glow in the dark, through the emissive overlay of its
     * model). It does no damage itself; the heat comes from the conditions (it is in {@code #heat_sources}).
     */
    public static final DeferredBlock<Block> SMOLDERING_STONE = register("smoldering_stone",
            () -> new Block(Properties.ofFullCopy(Blocks.BLACKSTONE).lightLevel(state -> 7)));

    // Charred wood: burnt-out wood that no longer burns (decision 2026-10-06), so the crimson set is the template
    // (no fire odds, not ignited by lava, no furnace fuel): no forest fires next to the lava.
    public static final DeferredBlock<StrippableLogBlock> CHARRED_LOG = register("charred_log",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_CHARRED_LOG, charred(Blocks.CRIMSON_STEM)));
    public static final DeferredBlock<StrippableLogBlock> CHARRED_WOOD = register("charred_wood",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_CHARRED_WOOD, charred(Blocks.CRIMSON_HYPHAE)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHARRED_LOG = register("stripped_charred_log",
            () -> new RotatedPillarBlock(charred(Blocks.STRIPPED_CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHARRED_WOOD = register("stripped_charred_wood",
            () -> new RotatedPillarBlock(charred(Blocks.STRIPPED_CRIMSON_HYPHAE)));
    public static final DeferredBlock<Block> CHARRED_PLANKS = register("charred_planks",
            () -> new Block(charred(Blocks.CRIMSON_PLANKS)));
    public static final DeferredBlock<StairBlock> CHARRED_STAIRS = register("charred_stairs",
            () -> new StairBlock(CHARRED_PLANKS.get().defaultBlockState(), charred(Blocks.CRIMSON_STAIRS)));
    public static final DeferredBlock<SlabBlock> CHARRED_SLAB = register("charred_slab",
            () -> new SlabBlock(charred(Blocks.CRIMSON_SLAB)));
    public static final DeferredBlock<FenceBlock> CHARRED_FENCE = register("charred_fence",
            () -> new FenceBlock(charred(Blocks.CRIMSON_FENCE)));
    public static final DeferredBlock<FenceGateBlock> CHARRED_FENCE_GATE = register("charred_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.CHARRED, charred(Blocks.CRIMSON_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> CHARRED_DOOR = registerDoor("charred_door",
            () -> new DoorBlock(ModWoodTypes.CHARRED_SET, charred(Blocks.CRIMSON_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> CHARRED_TRAPDOOR = register("charred_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.CHARRED_SET, charred(Blocks.CRIMSON_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> CHARRED_PRESSURE_PLATE = register("charred_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.CHARRED_SET, charred(Blocks.CRIMSON_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> CHARRED_BUTTON = register("charred_button",
            () -> new ButtonBlock(ModWoodTypes.CHARRED_SET, 30, charred(Blocks.CRIMSON_BUTTON)));

    // Ember woods and fire flora: smouldering leaves on the charred trunks, flora on ash ground. Like the charred wood
    // none of it burns (no fire odds, not ignited by lava): no fires next to the lava.
    /**
     * Never decays (the trunk is dead); no light of its own, only the emissive glow overlay of its model (V3 = A).
     * Nothing spawns on it.
     */
    public static final DeferredBlock<EmberLeavesBlock> EMBER_LEAVES = register("ember_leaves",
            () -> new EmberLeavesBlock(Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.2F).sound(SoundType.GRASS)
                    .noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, level, pos) -> false)));
    /** Poppy's properties on ash ground; Fire Resistance in suspicious stew, like the desert flower. */
    public static final DeferredBlock<FireBloomBlock> FIRE_BLOOM = register("fire_bloom",
            () -> new FireBloomBlock(MobEffects.FIRE_RESISTANCE, 4.0F, Properties.ofFullCopy(Blocks.POPPY).mapColor(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<FlowerPotBlock> POTTED_FIRE_BLOOM = registerPot("potted_fire_bloom", FIRE_BLOOM);
    /** Nether sprouts' behaviour (replaceable, instant, shears) in short grass' outline, on ash ground. */
    public static final DeferredBlock<AshSproutsBlock> ASH_SPROUTS = register("ash_sprouts",
            () -> new AshSproutsBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(MapColor.COLOR_GRAY)
                    .offsetType(BlockBehaviour.OffsetType.XYZ)));
    /** Sweet berry bush properties (random ticks: it grows back after picking); rare, ember woods and dormant calderas. */
    public static final DeferredBlock<EmberBerryBushBlock> EMBER_BERRY_BUSH = register("ember_berry_bush",
            () -> new EmberBerryBushBlock(Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).mapColor(MapColor.COLOR_ORANGE)));

    // Ores: one block per host rock (stone, deepslate, basalt; no blackstone). One step above ring 2: half a point
    // harder than the vanilla ore of that host, more XP.
    /** Volcanite, ring 3's main metal: iron pickaxe, XP 3–6, matte; drops {@link ModItems#RAW_VOLCANITE}. */
    public static final DeferredBlock<DropExperienceBlock> VOLCANITE_ORE = register("volcanite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), oreIn(Blocks.IRON_ORE)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_VOLCANITE_ORE = register("deepslate_volcanite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), oreIn(Blocks.DEEPSLATE_IRON_ORE)));
    public static final DeferredBlock<DropExperienceBlock> BASALT_VOLCANITE_ORE = register("basalt_volcanite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), oreInBasalt(Blocks.IRON_ORE)));
    /** Sulfur: any stone pickaxe, XP 1–3, matte; drops several {@link ModItems#SULFUR}. */
    public static final DeferredBlock<DropExperienceBlock> SULFUR_ORE = register("sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), oreIn(Blocks.COAL_ORE)));
    public static final DeferredBlock<DropExperienceBlock> BASALT_SULFUR_ORE = register("basalt_sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), oreInBasalt(Blocks.COAL_ORE)));
    /** Fire ruby, a rare gem: iron pickaxe, XP 4–8, faint glow (light 3, emissive overlay); drops {@link ModItems#FIRE_RUBY}. */
    public static final DeferredBlock<DropExperienceBlock> FIRE_RUBY_ORE = register("fire_ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 8), oreIn(Blocks.IRON_ORE).lightLevel(state -> 3)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_FIRE_RUBY_ORE = register("deepslate_fire_ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 8), oreIn(Blocks.DEEPSLATE_IRON_ORE).lightLevel(state -> 3)));
    /** In the basalt walls of magma chambers (decision 2026-10-07, beyond the contract's first set). */
    public static final DeferredBlock<DropExperienceBlock> BASALT_FIRE_RUBY_ORE = register("basalt_fire_ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 8), oreInBasalt(Blocks.IRON_ORE).lightLevel(state -> 3)));

    // Fire crystal: the geode wall and four stages, like amethyst. No growth (only worldgen places them); use in phase 2.
    /** The geode wall: amethyst block behaviour (chime when hit), no light and no glow. */
    public static final DeferredBlock<AmethystBlock> FIRE_CRYSTAL_BLOCK = register("fire_crystal_block",
            () -> new AmethystBlock(Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_ORANGE)));
    /** The stages are vanilla's amethyst bud behaviour, shapes and light levels (1, 2, 4, 5), like the dew crystal. */
    public static final DeferredBlock<AmethystClusterBlock> SMALL_FIRE_CRYSTAL_BUD = register("small_fire_crystal_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, crystalStage(Blocks.SMALL_AMETHYST_BUD, MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_FIRE_CRYSTAL_BUD = register("medium_fire_crystal_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, crystalStage(Blocks.MEDIUM_AMETHYST_BUD, MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_FIRE_CRYSTAL_BUD = register("large_fire_crystal_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, crystalStage(Blocks.LARGE_AMETHYST_BUD, MapColor.COLOR_ORANGE)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> FIRE_CRYSTAL_CLUSTER = register("fire_crystal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, crystalStage(Blocks.AMETHYST_CLUSTER, MapColor.COLOR_ORANGE)));

    // Sulfur fields and steam fens: the crust, the vents and the mud. Their heat comes from the conditions
    // (#heat_sources); vanilla particles and sounds until atmosphere delivers its own (wave 2).
    /** Top layer of the sulfur fields: sand's strength and sound, but it does not fall (like the salt crust). */
    public static final DeferredBlock<Block> SULFUR_CRUST = register("sulfur_crust",
            () -> new Block(Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_YELLOW)));
    /** A vent in basalt that puffs steam (client-side only). */
    public static final DeferredBlock<FumaroleBlock> FUMAROLE = register("fumarole",
            () -> new FumaroleBlock(Properties.ofFullCopy(Blocks.BASALT).mapColor(MapColor.TERRACOTTA_YELLOW)));
    /** Mud's shape (you sink in a little) with honey's slowness: speed 0.4, jump 0.5. */
    public static final DeferredBlock<BoilingMudBlock> BOILING_MUD = register("boiling_mud",
            () -> new BoilingMudBlock(boilingMud(MapColor.TERRACOTTA_BROWN)));
    /** Throws what stands on it up now and then; random ticks only start the cycle of a worldgen geyser. */
    public static final DeferredBlock<GeyserBlock> GEYSER = register("geyser",
            () -> new GeyserBlock(Properties.ofFullCopy(Blocks.BASALT).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).randomTicks()));

    // The coloured boiling pools and crystals of the sulfur fields (46b, decision 2 of teststop 4b): boiling mud with
    // sulfur steam rising from it, and a sulfur crystal in the amethyst stages. No growth (only worldgen places them).
    public static final DeferredBlock<BoilingMudBlock> YELLOW_SULFUR_MUD = register("yellow_sulfur_mud",
            () -> new BoilingMudBlock(boilingMud(MapColor.TERRACOTTA_YELLOW), ModParticleTypes.SULFUR_STEAM));
    public static final DeferredBlock<BoilingMudBlock> GREEN_SULFUR_MUD = register("green_sulfur_mud",
            () -> new BoilingMudBlock(boilingMud(MapColor.TERRACOTTA_LIGHT_GREEN), ModParticleTypes.SULFUR_STEAM));
    public static final DeferredBlock<BoilingMudBlock> ORANGE_SULFUR_MUD = register("orange_sulfur_mud",
            () -> new BoilingMudBlock(boilingMud(MapColor.TERRACOTTA_ORANGE), ModParticleTypes.SULFUR_STEAM));
    /** Vanilla's amethyst bud behaviour and shapes, with a faint glow like the salt crystal (1, 2, 2, 3). */
    public static final DeferredBlock<AmethystClusterBlock> SMALL_SULFUR_CRYSTAL_BUD = register("small_sulfur_crystal_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, sulfurCrystal(Blocks.SMALL_AMETHYST_BUD, 1)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_SULFUR_CRYSTAL_BUD = register("medium_sulfur_crystal_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, sulfurCrystal(Blocks.MEDIUM_AMETHYST_BUD, 2)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_SULFUR_CRYSTAL_BUD = register("large_sulfur_crystal_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, sulfurCrystal(Blocks.LARGE_AMETHYST_BUD, 2)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> SULFUR_CRYSTAL = register("sulfur_crystal",
            () -> new AmethystClusterBlock(7.0F, 3.0F, sulfurCrystal(Blocks.AMETHYST_CLUSTER, 3)));

    // ---- Nether ring 3 ----
    // Frozen Nether: cold lava with its shapes, Nether ice, rime netherrack, the rimeshroom wood set and cap, ice
    // stalactites and the warm glowing fissures. Rimeshroom is Nether wood: no fire odds, no furnace fuel.
    // Cold lava: basalt's behaviour and sound in three building variants with their shapes (like the pumice set).
    public static final DeferredBlock<Block> COLD_LAVA = register("cold_lava", () -> new Block(coldLava()));
    public static final DeferredBlock<StairBlock> COLD_LAVA_STAIRS = register("cold_lava_stairs",
            () -> new StairBlock(COLD_LAVA.get().defaultBlockState(), coldLava()));
    public static final DeferredBlock<SlabBlock> COLD_LAVA_SLAB = register("cold_lava_slab", () -> new SlabBlock(coldLava()));
    /** Solid for support like vanilla walls ({@code forceSolidOn}), which a copy of a full block does not carry. */
    public static final DeferredBlock<WallBlock> COLD_LAVA_WALL = register("cold_lava_wall",
            () -> new WallBlock(coldLava().forceSolidOn()));
    public static final DeferredBlock<Block> POLISHED_COLD_LAVA = register("polished_cold_lava", () -> new Block(coldLava()));
    public static final DeferredBlock<StairBlock> POLISHED_COLD_LAVA_STAIRS = register("polished_cold_lava_stairs",
            () -> new StairBlock(POLISHED_COLD_LAVA.get().defaultBlockState(), coldLava()));
    public static final DeferredBlock<SlabBlock> POLISHED_COLD_LAVA_SLAB = register("polished_cold_lava_slab",
            () -> new SlabBlock(coldLava()));
    public static final DeferredBlock<WallBlock> POLISHED_COLD_LAVA_WALL = register("polished_cold_lava_wall",
            () -> new WallBlock(coldLava().forceSolidOn()));
    public static final DeferredBlock<Block> COLD_LAVA_BRICKS = register("cold_lava_bricks", () -> new Block(coldLava()));
    public static final DeferredBlock<StairBlock> COLD_LAVA_BRICK_STAIRS = register("cold_lava_brick_stairs",
            () -> new StairBlock(COLD_LAVA_BRICKS.get().defaultBlockState(), coldLava()));
    public static final DeferredBlock<SlabBlock> COLD_LAVA_BRICK_SLAB = register("cold_lava_brick_slab",
            () -> new SlabBlock(coldLava()));
    public static final DeferredBlock<WallBlock> COLD_LAVA_BRICK_WALL = register("cold_lava_brick_wall",
            () -> new WallBlock(coldLava().forceSolidOn()));

    /**
     * Packed ice's properties: opaque and solid (decision 2026-10-06), as slippery as ice, and no random ticks, so it
     * never melts. Mobs spawn on it (the Nether floor). Only Silk Touch takes it along, like vanilla ice.
     */
    public static final DeferredBlock<Block> NETHER_ICE = register("nether_ice",
            () -> new Block(Properties.ofFullCopy(Blocks.PACKED_ICE)));
    public static final DeferredBlock<Block> PACKED_NETHER_ICE = register("packed_nether_ice",
            () -> new Block(Properties.ofFullCopy(Blocks.PACKED_ICE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
    /**
     * Frosted netherrack: netherrack's strength and sound, but frozen ground (decision 2026-10-07): fire does not burn on it
     * forever (not in the infiniburn tags), and as a plain {@code Block} (not a {@code NetherrackBlock}) bone meal does not
     * spread nylium onto it.
     */
    public static final DeferredBlock<Block> RIME_NETHERRACK = register("rime_netherrack",
            () -> new Block(Properties.ofFullCopy(Blocks.NETHERRACK).mapColor(MapColor.TERRACOTTA_LIGHT_BLUE)));

    public static final DeferredBlock<StrippableLogBlock> RIMESHROOM_STEM = register("rimeshroom_stem",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_RIMESHROOM_STEM, rimeshroomBark(Blocks.CRIMSON_STEM)));
    public static final DeferredBlock<StrippableLogBlock> RIMESHROOM_HYPHAE = register("rimeshroom_hyphae",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE, rimeshroomBark(Blocks.CRIMSON_HYPHAE)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_RIMESHROOM_STEM = register("stripped_rimeshroom_stem",
            () -> new RotatedPillarBlock(rimeshroom(Blocks.STRIPPED_CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_RIMESHROOM_HYPHAE = register("stripped_rimeshroom_hyphae",
            () -> new RotatedPillarBlock(rimeshroom(Blocks.STRIPPED_CRIMSON_HYPHAE)));
    public static final DeferredBlock<Block> RIMESHROOM_PLANKS = register("rimeshroom_planks",
            () -> new Block(rimeshroom(Blocks.CRIMSON_PLANKS)));
    public static final DeferredBlock<StairBlock> RIMESHROOM_STAIRS = register("rimeshroom_stairs",
            () -> new StairBlock(RIMESHROOM_PLANKS.get().defaultBlockState(), rimeshroom(Blocks.CRIMSON_STAIRS)));
    public static final DeferredBlock<SlabBlock> RIMESHROOM_SLAB = register("rimeshroom_slab",
            () -> new SlabBlock(rimeshroom(Blocks.CRIMSON_SLAB)));
    public static final DeferredBlock<FenceBlock> RIMESHROOM_FENCE = register("rimeshroom_fence",
            () -> new FenceBlock(rimeshroom(Blocks.CRIMSON_FENCE)));
    public static final DeferredBlock<FenceGateBlock> RIMESHROOM_FENCE_GATE = register("rimeshroom_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.RIMESHROOM, rimeshroom(Blocks.CRIMSON_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> RIMESHROOM_DOOR = registerDoor("rimeshroom_door",
            () -> new DoorBlock(ModWoodTypes.RIMESHROOM_SET, rimeshroom(Blocks.CRIMSON_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> RIMESHROOM_TRAPDOOR = register("rimeshroom_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.RIMESHROOM_SET, rimeshroom(Blocks.CRIMSON_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> RIMESHROOM_PRESSURE_PLATE = register("rimeshroom_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.RIMESHROOM_SET, rimeshroom(Blocks.CRIMSON_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> RIMESHROOM_BUTTON = register("rimeshroom_button",
            () -> new ButtonBlock(ModWoodTypes.RIMESHROOM_SET, 30, rimeshroom(Blocks.CRIMSON_BUTTON)));
    /** The frozen fungus' cap: a wart block (hoe, drops itself). */
    public static final DeferredBlock<Block> RIMESHROOM_CAP = register("rimeshroom_cap",
            () -> new Block(Properties.ofFullCopy(Blocks.NETHER_WART_BLOCK).mapColor(MapColor.ICE)));

    /** The slime stalactite's behaviour (hangs or stands, falls without support, never grows) in ice: glass sounds, chime. */
    public static final DeferredBlock<StalactiteBlock> ICE_STALACTITE = register("ice_stalactite",
            () -> new StalactiteBlock(Properties.ofFullCopy(SLIME_STALACTITE.get()).mapColor(MapColor.ICE).sound(SoundType.GLASS)
                    .instrument(NoteBlockInstrument.CHIME)));
    /**
     * A warm crack in the frozen ground: light 7 (decision 2026-10-07; only the glowing crack shines in the dark, through the emissive overlay
     * of its model) and a warmth and heat source for the conditions. No damage of its own.
     */
    public static final DeferredBlock<Block> GLOWING_FISSURE = register("glowing_fissure",
            () -> new Block(Properties.ofFullCopy(Blocks.NETHERRACK).mapColor(MapColor.COLOR_ORANGE).lightLevel(state -> 7)));
    /**
     * The key material of Nether ring 3, the core of the ice pillars: blue ice's hardness and slipperiness (opaque, never
     * melts), but obsidian's blast resistance (decision 2026-10-07), so ghast fireballs and creepers cannot destroy it
     * without a drop. Only Silk Touch takes it along; otherwise it breaks without a drop.
     */
    public static final DeferredBlock<Block> PRIMORDIAL_ICE = register("primordial_ice",
            () -> new Block(Properties.ofFullCopy(Blocks.BLUE_ICE).explosionResistance(Blocks.OBSIDIAN.getExplosionResistance())));

    // ---- Ring 4 ----
    // Mythical water ring (contract ring 4 §3, package 66): tidestone, the moonpetal wood set and its withered trunk,
    // the swamp and water flora, the glowing flora and the bubble spring.
    // Tidestone: the stone family's shapes and behaviour in a blue-grey layered rock; it breaks into cobbled tidestone.
    public static final DeferredBlock<Block> TIDESTONE = register("tidestone", () -> new Block(tidestone(Blocks.STONE)));
    public static final DeferredBlock<StairBlock> TIDESTONE_STAIRS = register("tidestone_stairs",
            () -> new StairBlock(TIDESTONE.get().defaultBlockState(), tidestone(Blocks.STONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> TIDESTONE_SLAB = register("tidestone_slab",
            () -> new SlabBlock(tidestone(Blocks.STONE_SLAB)));
    /** Solid for support like vanilla walls ({@code forceSolidOn}); vanilla has no plain stone wall to copy. */
    public static final DeferredBlock<WallBlock> TIDESTONE_WALL = register("tidestone_wall",
            () -> new WallBlock(tidestone(Blocks.STONE).forceSolidOn()));
    public static final DeferredBlock<Block> COBBLED_TIDESTONE = register("cobbled_tidestone",
            () -> new Block(tidestone(Blocks.COBBLESTONE)));
    public static final DeferredBlock<StairBlock> COBBLED_TIDESTONE_STAIRS = register("cobbled_tidestone_stairs",
            () -> new StairBlock(COBBLED_TIDESTONE.get().defaultBlockState(), tidestone(Blocks.COBBLESTONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> COBBLED_TIDESTONE_SLAB = register("cobbled_tidestone_slab",
            () -> new SlabBlock(tidestone(Blocks.COBBLESTONE_SLAB)));
    public static final DeferredBlock<WallBlock> COBBLED_TIDESTONE_WALL = register("cobbled_tidestone_wall",
            () -> new WallBlock(tidestone(Blocks.COBBLESTONE_WALL)));
    public static final DeferredBlock<Block> POLISHED_TIDESTONE = register("polished_tidestone",
            () -> new Block(tidestone(Blocks.POLISHED_ANDESITE)));
    public static final DeferredBlock<StairBlock> POLISHED_TIDESTONE_STAIRS = register("polished_tidestone_stairs",
            () -> new StairBlock(POLISHED_TIDESTONE.get().defaultBlockState(), tidestone(Blocks.POLISHED_ANDESITE_STAIRS)));
    public static final DeferredBlock<SlabBlock> POLISHED_TIDESTONE_SLAB = register("polished_tidestone_slab",
            () -> new SlabBlock(tidestone(Blocks.POLISHED_ANDESITE_SLAB)));
    public static final DeferredBlock<WallBlock> POLISHED_TIDESTONE_WALL = register("polished_tidestone_wall",
            () -> new WallBlock(tidestone(Blocks.POLISHED_ANDESITE).forceSolidOn()));
    public static final DeferredBlock<Block> TIDESTONE_BRICKS = register("tidestone_bricks",
            () -> new Block(tidestone(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<StairBlock> TIDESTONE_BRICK_STAIRS = register("tidestone_brick_stairs",
            () -> new StairBlock(TIDESTONE_BRICKS.get().defaultBlockState(), tidestone(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> TIDESTONE_BRICK_SLAB = register("tidestone_brick_slab",
            () -> new SlabBlock(tidestone(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> TIDESTONE_BRICK_WALL = register("tidestone_brick_wall",
            () -> new WallBlock(tidestone(Blocks.STONE_BRICK_WALL)));

    // Moonpetal: the blossom tree of the cascades, white-silver bark around pale wood. Burnable like vanilla wood, with
    // the cherry set's sounds.
    public static final DeferredBlock<StrippableLogBlock> MOONPETAL_LOG = register("moonpetal_log",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_MOONPETAL_LOG, Properties.ofFullCopy(Blocks.CHERRY_LOG)
                    .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y
                            ? MapColor.TERRACOTTA_WHITE : MapColor.QUARTZ)));
    public static final DeferredBlock<StrippableLogBlock> MOONPETAL_WOOD = register("moonpetal_wood",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_MOONPETAL_WOOD, Properties.ofFullCopy(Blocks.CHERRY_WOOD)
                    .mapColor(MapColor.QUARTZ)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MOONPETAL_LOG = register("stripped_moonpetal_log",
            () -> new RotatedPillarBlock(moonpetal(Blocks.STRIPPED_CHERRY_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MOONPETAL_WOOD = register("stripped_moonpetal_wood",
            () -> new RotatedPillarBlock(moonpetal(Blocks.STRIPPED_CHERRY_WOOD)));
    public static final DeferredBlock<Block> MOONPETAL_PLANKS = register("moonpetal_planks",
            () -> new Block(moonpetal(Blocks.CHERRY_PLANKS)));
    public static final DeferredBlock<StairBlock> MOONPETAL_STAIRS = register("moonpetal_stairs",
            () -> new StairBlock(MOONPETAL_PLANKS.get().defaultBlockState(), moonpetal(Blocks.CHERRY_STAIRS)));
    public static final DeferredBlock<SlabBlock> MOONPETAL_SLAB = register("moonpetal_slab",
            () -> new SlabBlock(moonpetal(Blocks.CHERRY_SLAB)));
    public static final DeferredBlock<FenceBlock> MOONPETAL_FENCE = register("moonpetal_fence",
            () -> new FenceBlock(moonpetal(Blocks.CHERRY_FENCE)));
    public static final DeferredBlock<FenceGateBlock> MOONPETAL_FENCE_GATE = register("moonpetal_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.MOONPETAL, moonpetal(Blocks.CHERRY_FENCE_GATE)));
    public static final DeferredBlock<DoorBlock> MOONPETAL_DOOR = registerDoor("moonpetal_door",
            () -> new DoorBlock(ModWoodTypes.MOONPETAL_SET, moonpetal(Blocks.CHERRY_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> MOONPETAL_TRAPDOOR = register("moonpetal_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.MOONPETAL_SET, moonpetal(Blocks.CHERRY_TRAPDOOR)));
    public static final DeferredBlock<PressurePlateBlock> MOONPETAL_PRESSURE_PLATE = register("moonpetal_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.MOONPETAL_SET, moonpetal(Blocks.CHERRY_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> MOONPETAL_BUTTON = register("moonpetal_button",
            () -> new ButtonBlock(ModWoodTypes.MOONPETAL_SET, 30, moonpetal(Blocks.CHERRY_BUTTON)));
    /** The pink-lilac blossom crown: cherry leaves' properties as plain leaves (no vanilla petal particles). */
    public static final DeferredBlock<LeavesBlock> MOONPETAL_LEAVES = register("moonpetal_leaves",
            () -> new LeavesBlock(Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));
    /** Grows into {@code corruptedadventure:ring_4/moonpetal_tree} (75); until then bonemeal is used up without growth. */
    public static final DeferredBlock<SaplingBlock> MOONPETAL_SAPLING = register("moonpetal_sapling",
            () -> new SaplingBlock(ModTreeGrowers.MOONPETAL, Properties.ofFullCopy(Blocks.CHERRY_SAPLING)));
    public static final DeferredBlock<FlowerPotBlock> POTTED_MOONPETAL_SAPLING = registerPot("potted_moonpetal_sapling", MOONPETAL_SAPLING);

    // Withered moonpetal: only the dead trunk of the drowned forest (no leaves, no planks set); still burnable.
    public static final DeferredBlock<StrippableLogBlock> WITHERED_MOONPETAL_LOG = register("withered_moonpetal_log",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG, Properties.ofFullCopy(Blocks.OAK_LOG)
                    .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y
                            ? MapColor.COLOR_LIGHT_GRAY : MapColor.COLOR_GRAY)));
    public static final DeferredBlock<StrippableLogBlock> WITHERED_MOONPETAL_WOOD = register("withered_moonpetal_wood",
            () -> new StrippableLogBlock(ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD, Properties.ofFullCopy(Blocks.OAK_WOOD)
                    .mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WITHERED_MOONPETAL_LOG = register("stripped_withered_moonpetal_log",
            () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WITHERED_MOONPETAL_WOOD = register("stripped_withered_moonpetal_wood",
            () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.COLOR_LIGHT_GRAY)));

    // Swamp and water flora.
    /** Marsh reeds, 2–3 high: a column like sugar cane that grows next to water (decision golf 1, V2 = B). */
    public static final DeferredBlock<ReedsBlock> REEDS = register("reeds",
            () -> new ReedsBlock(Properties.ofFullCopy(Blocks.SUGAR_CANE)));
    /** The surface of the peat moors: dirt's behaviour (plants grow on it), burnable and a furnace fuel. */
    public static final DeferredBlock<Block> PEAT = register("peat",
            () -> new Block(Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.COLOR_BROWN).sound(SoundType.ROOTED_DIRT)));
    /** Hanging roots' behaviour, in strands under the swamp trees. Only shears take it along. */
    public static final DeferredBlock<HangingSwampMossBlock> HANGING_SWAMP_MOSS = register("hanging_swamp_moss",
            () -> new HangingSwampMossBlock(Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(MapColor.COLOR_GREEN)
                    .sound(SoundType.MOSS_CARPET)));
    /** A 2 × 2 lily pad you can walk on (decision golf 1); the item places all four parts. */
    public static final DeferredBlock<GiantLilyPadBlock> GIANT_LILY_PAD = registerOnWater("giant_lily_pad",
            () -> new GiantLilyPadBlock(Properties.ofFullCopy(Blocks.LILY_PAD)));
    /** A pink-white lotus floating flat on the water: vanilla's lily pad behaviour (decision golf 1, V1 = A). */
    public static final DeferredBlock<WaterlilyBlock> LOTUS = registerOnWater("lotus",
            () -> new WaterlilyBlock(Properties.ofFullCopy(Blocks.LILY_PAD).mapColor(MapColor.COLOR_PINK)));

    // Glowing flora (subtle; light levels chosen by stream B): the glow sits in an emissive layer of the model.
    /** A blue-purple glowing mushroom, light 6. */
    public static final DeferredBlock<GlowcapBlock> GLOWCAP = register("glowcap",
            () -> new GlowcapBlock(glowcap(MapColor.COLOR_LIGHT_BLUE, 6)));
    /** A glowing flower by the lakes, light 7; Night Vision in suspicious stew, like the torchflower. */
    public static final DeferredBlock<FlowerBlock> GLOW_BLOOM = register("glow_bloom",
            () -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, Properties.ofFullCopy(Blocks.POPPY)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 7)));
    /** Vanilla's hanging roots under the islands and root arches, light 5. */
    public static final DeferredBlock<HangingRootsBlock> GLOWING_HANGING_ROOTS = register("glowing_hanging_roots",
            () -> new HangingRootsBlock(Properties.ofFullCopy(Blocks.HANGING_ROOTS).lightLevel(state -> 5)));
    /** The glowcap's sickly variant in the reed marsh: same shape and class, a weak glow (light 3), no poison source. */
    public static final DeferredBlock<GlowcapBlock> BLIGHTED_GLOWCAP = register("blighted_glowcap",
            () -> new GlowcapBlock(glowcap(MapColor.COLOR_LIGHT_GREEN, 3)));

    /** A spring on the pool floor that bubbles while water stands on it (looks only); tidestone's strength. */
    public static final DeferredBlock<BubbleSpringBlock> BUBBLE_SPRING = register("bubble_spring",
            () -> new BubbleSpringBlock(tidestone(Blocks.STONE)));

    // Ores (package 67): one block per host rock (stone, tidestone, deepslate). A clear step above ring 3 (decision golf
    // 1, V3 = B): a whole point harder than the vanilla ore of that host and a diamond pickaxe.
    /** Tidesilver, ring 4's mythical metal: XP 4–7, faint glow (light 3, emissive overlay); drops {@link ModItems#RAW_TIDESILVER}. */
    public static final DeferredBlock<DropExperienceBlock> TIDESILVER_ORE = register("tidesilver_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 7), ring4Ore(Blocks.IRON_ORE).lightLevel(state -> 3)));
    public static final DeferredBlock<DropExperienceBlock> TIDESTONE_TIDESILVER_ORE = register("tidestone_tidesilver_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 7), ring4Ore(Blocks.IRON_ORE)
                    .mapColor(MapColor.TERRACOTTA_LIGHT_BLUE).lightLevel(state -> 3)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_TIDESILVER_ORE = register("deepslate_tidesilver_ore",
            () -> new DropExperienceBlock(UniformInt.of(4, 7), ring4Ore(Blocks.DEEPSLATE_IRON_ORE).lightLevel(state -> 3)));
    /** Jade, a rare gem deep in the flooded tunnels: XP 5–9, matte; drops {@link ModItems#JADE}. */
    public static final DeferredBlock<DropExperienceBlock> JADE_ORE = register("jade_ore",
            () -> new DropExperienceBlock(UniformInt.of(5, 9), ring4Ore(Blocks.DIAMOND_ORE)));
    public static final DeferredBlock<DropExperienceBlock> TIDESTONE_JADE_ORE = register("tidestone_jade_ore",
            () -> new DropExperienceBlock(UniformInt.of(5, 9), ring4Ore(Blocks.DIAMOND_ORE).mapColor(MapColor.TERRACOTTA_LIGHT_BLUE)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_JADE_ORE = register("deepslate_jade_ore",
            () -> new DropExperienceBlock(UniformInt.of(5, 9), ring4Ore(Blocks.DEEPSLATE_DIAMOND_ORE)));

    // Toxic crystal and aquamarine in the amethyst pattern: wall, budding block (grows the stages on random ticks,
    // server-side) and four stages with amethyst's shapes and light (1, 2, 4, 5). Toxic crystal in the poison caves,
    // aquamarine in geodes under water (its buds grow waterlogged).
    public static final DeferredBlock<AmethystBlock> TOXIC_CRYSTAL_BLOCK = register("toxic_crystal_block",
            () -> new AmethystBlock(Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN)));
    /** Budding amethyst's properties (hard, pushed away, no drop) with random ticks, set explicitly: it grows. */
    public static final DeferredBlock<BuddingCrystalBlock> BUDDING_TOXIC_CRYSTAL = register("budding_toxic_crystal",
            () -> new BuddingCrystalBlock(() -> List.of(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get(),
                    ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get(), ModBlocks.TOXIC_CRYSTAL_CLUSTER.get()),
                    Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).mapColor(MapColor.COLOR_LIGHT_GREEN).randomTicks()));
    public static final DeferredBlock<AmethystClusterBlock> SMALL_TOXIC_CRYSTAL_BUD = register("small_toxic_crystal_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, crystalStage(Blocks.SMALL_AMETHYST_BUD, MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_TOXIC_CRYSTAL_BUD = register("medium_toxic_crystal_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, crystalStage(Blocks.MEDIUM_AMETHYST_BUD, MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_TOXIC_CRYSTAL_BUD = register("large_toxic_crystal_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, crystalStage(Blocks.LARGE_AMETHYST_BUD, MapColor.COLOR_LIGHT_GREEN)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> TOXIC_CRYSTAL_CLUSTER = register("toxic_crystal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, crystalStage(Blocks.AMETHYST_CLUSTER, MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<AmethystBlock> AQUAMARINE_BLOCK = register("aquamarine_block",
            () -> new AmethystBlock(Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<BuddingCrystalBlock> BUDDING_AQUAMARINE = register("budding_aquamarine",
            () -> new BuddingCrystalBlock(() -> List.of(ModBlocks.SMALL_AQUAMARINE_BUD.get(), ModBlocks.MEDIUM_AQUAMARINE_BUD.get(),
                    ModBlocks.LARGE_AQUAMARINE_BUD.get(), ModBlocks.AQUAMARINE_CLUSTER.get()),
                    Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).mapColor(MapColor.COLOR_CYAN).randomTicks()));
    public static final DeferredBlock<AmethystClusterBlock> SMALL_AQUAMARINE_BUD = register("small_aquamarine_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, crystalStage(Blocks.SMALL_AMETHYST_BUD, MapColor.COLOR_CYAN)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_AQUAMARINE_BUD = register("medium_aquamarine_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, crystalStage(Blocks.MEDIUM_AMETHYST_BUD, MapColor.COLOR_CYAN)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_AQUAMARINE_BUD = register("large_aquamarine_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, crystalStage(Blocks.LARGE_AMETHYST_BUD, MapColor.COLOR_CYAN)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> AQUAMARINE_CLUSTER = register("aquamarine_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, crystalStage(Blocks.AMETHYST_CLUSTER, MapColor.COLOR_CYAN)));

    // ---- Nether ring 4 ----
    // Dry crystal and bone desert (contract ring 4 §3, package 68): bone sand, bleached netherrack, the fossil stone set
    // and the purity crystal. The ribcages of the sea creatures are vanilla bone blocks.
    /** Falls like sand, with a pale bone dust. */
    public static final DeferredBlock<ColoredFallingBlock> BONE_SAND = register("bone_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(BONE_DUST_COLOR), Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.SAND)));
    /**
     * Netherrack's strength and sound in a beige, bleached rock. A plain {@code Block} like rime netherrack: bone meal
     * does not spread nylium onto it.
     */
    public static final DeferredBlock<Block> BLEACHED_NETHERRACK = register("bleached_netherrack",
            () -> new Block(Properties.ofFullCopy(Blocks.NETHERRACK).mapColor(MapColor.TERRACOTTA_WHITE)));
    // Fossil stone: the stone family's shapes and behaviour in a bone-white rock with fossils (like the tidestone set).
    public static final DeferredBlock<Block> FOSSIL_STONE = register("fossil_stone", () -> new Block(fossilStone(Blocks.STONE)));
    public static final DeferredBlock<StairBlock> FOSSIL_STONE_STAIRS = register("fossil_stone_stairs",
            () -> new StairBlock(FOSSIL_STONE.get().defaultBlockState(), fossilStone(Blocks.STONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> FOSSIL_STONE_SLAB = register("fossil_stone_slab",
            () -> new SlabBlock(fossilStone(Blocks.STONE_SLAB)));
    /** Solid for support like vanilla walls ({@code forceSolidOn}); vanilla has no plain stone wall to copy. */
    public static final DeferredBlock<WallBlock> FOSSIL_STONE_WALL = register("fossil_stone_wall",
            () -> new WallBlock(fossilStone(Blocks.STONE).forceSolidOn()));
    public static final DeferredBlock<Block> POLISHED_FOSSIL_STONE = register("polished_fossil_stone",
            () -> new Block(fossilStone(Blocks.POLISHED_ANDESITE)));
    public static final DeferredBlock<StairBlock> POLISHED_FOSSIL_STONE_STAIRS = register("polished_fossil_stone_stairs",
            () -> new StairBlock(POLISHED_FOSSIL_STONE.get().defaultBlockState(), fossilStone(Blocks.POLISHED_ANDESITE_STAIRS)));
    public static final DeferredBlock<SlabBlock> POLISHED_FOSSIL_STONE_SLAB = register("polished_fossil_stone_slab",
            () -> new SlabBlock(fossilStone(Blocks.POLISHED_ANDESITE_SLAB)));
    public static final DeferredBlock<WallBlock> POLISHED_FOSSIL_STONE_WALL = register("polished_fossil_stone_wall",
            () -> new WallBlock(fossilStone(Blocks.POLISHED_ANDESITE).forceSolidOn()));
    public static final DeferredBlock<Block> FOSSIL_STONE_BRICKS = register("fossil_stone_bricks",
            () -> new Block(fossilStone(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<StairBlock> FOSSIL_STONE_BRICK_STAIRS = register("fossil_stone_brick_stairs",
            () -> new StairBlock(FOSSIL_STONE_BRICKS.get().defaultBlockState(), fossilStone(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> FOSSIL_STONE_BRICK_SLAB = register("fossil_stone_brick_slab",
            () -> new SlabBlock(fossilStone(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> FOSSIL_STONE_BRICK_WALL = register("fossil_stone_brick_wall",
            () -> new WallBlock(fossilStone(Blocks.STONE_BRICK_WALL)));
    // Purity crystal: the crystal of the spires. No budding block and no growth (only worldgen places it).
    /** The crystal wall: amethyst block behaviour (chime when hit), no light. */
    public static final DeferredBlock<AmethystBlock> PURITY_CRYSTAL_BLOCK = register("purity_crystal_block",
            () -> new AmethystBlock(Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.SNOW)));
    /** The amethyst cluster's behaviour, shape and light (5); drops {@link ModItems#PURITY_CRYSTAL_SHARD}. Faces up by default. */
    public static final DeferredBlock<AmethystClusterBlock> PURITY_CRYSTAL_CLUSTER = register("purity_crystal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, crystalStage(Blocks.AMETHYST_CLUSTER, MapColor.SNOW)));
    /** A crystal lamp: the crystal wall's behaviour with light 15. */
    public static final DeferredBlock<AmethystBlock> PURITY_CRYSTAL_LAMP = register("purity_crystal_lamp",
            () -> new AmethystBlock(Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.SNOW).lightLevel(state -> 15)));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    /** All blocks in registration order (datagen: loot tables and models must cover each of them). */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    private static Properties scorched(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_BROWN);
    }

    /** Iron ore's hardness and tool rule (harder than its host rock), XP 2–5, light 3. */
    private static DropExperienceBlock sunstoneOre(MapColor color) {
        return new DropExperienceBlock(UniformInt.of(2, 5), Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(color)
                .lightLevel(state -> 3));
    }

    /** Iron ore's hardness and tool rule, XP 3–7, no light. */
    private static DropExperienceBlock turquoiseOre(MapColor color) {
        return new DropExperienceBlock(UniformInt.of(3, 7), Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(color));
    }

    /** Palm wood without bark (planks family, stripped): light sand-yellow on the map. */
    private static Properties palm(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.SAND);
    }

    /** Dripshroom wood (planks family, stripped): crimson's behaviour in a light grey-green on the map. */
    private static Properties dripshroom(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_LIGHT_GREEN);
    }

    /**
     * A crystal stage (dew, fire): the amethyst stage's behaviour, sounds and light level (1, 2, 4, 5), in its own map
     * colour.
     */
    private static Properties crystalStage(Block vanillaStage, MapColor color) {
        return Properties.ofFullCopy(vanillaStage).mapColor(color);
    }

    /** A sulfur crystal stage: the amethyst stage's behaviour and sounds, yellow on the map, with its own faint light. */
    private static Properties sulfurCrystal(Block vanillaStage, int light) {
        return crystalStage(vanillaStage, MapColor.COLOR_YELLOW).lightLevel(state -> light);
    }

    /** Boiling mud: mud's shape (you sink in a little) with honey's slowness, speed 0.4 and jump 0.5. */
    private static Properties boilingMud(MapColor color) {
        return Properties.ofFullCopy(Blocks.MUD).mapColor(color).speedFactor(0.4F).jumpFactor(0.5F);
    }

    /** Dripshroom stem and hyphae (bark outside): dark grey-green on the map. */
    private static Properties dripshroomBark(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_GRAY);
    }

    /**
     * A ring 3 ore in a vanilla host: that vanilla ore's look, sound and blast resistance, half a point harder. The tool
     * tier comes from the {@code needs_*_tool} tags.
     */
    private static Properties oreIn(Block vanillaOre) {
        return Properties.ofFullCopy(vanillaOre).destroyTime(vanillaOre.defaultDestroyTime() + RING_3_ORE_EXTRA_HARDNESS);
    }

    /** {@link #oreIn} with basalt's colour and sound (the stone variant's ore as the template). */
    private static Properties oreInBasalt(Block vanillaOre) {
        return oreIn(vanillaOre).mapColor(MapColor.COLOR_BLACK).sound(SoundType.BASALT);
    }

    /** Pumice (all shapes): the tuff family's behaviour and sounds, light grey on the map. */
    private static Properties pumice(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.COLOR_LIGHT_GRAY);
    }

    /** Charred wood (all shapes): crimson's behaviour and sounds, black on the map. */
    private static Properties charred(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.COLOR_BLACK);
    }

    /** Cold lava (all shapes and variants): basalt's strength and sound. */
    private static Properties coldLava() {
        return Properties.ofFullCopy(Blocks.BASALT);
    }

    /** Rimeshroom wood (planks family, stripped): crimson's behaviour in a pale ice blue on the map. */
    private static Properties rimeshroom(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.COLOR_LIGHT_BLUE);
    }

    /** Rimeshroom stem and hyphae (frosted bark outside): dark cyan on the map. */
    private static Properties rimeshroomBark(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_CYAN);
    }

    /**
     * A ring 4 ore in a vanilla host: that vanilla ore's look, sound and blast resistance, a whole point harder. The tool
     * tier (diamond) comes from {@code #needs_diamond_tool}.
     */
    private static Properties ring4Ore(Block vanillaOre) {
        return Properties.ofFullCopy(vanillaOre).destroyTime(vanillaOre.defaultDestroyTime() + RING_4_ORE_EXTRA_HARDNESS);
    }

    /** Tidestone (all shapes, and the bubble spring): the vanilla stone variant's behaviour, blue-grey on the map. */
    private static Properties tidestone(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_LIGHT_BLUE);
    }

    /** Fossil stone (all shapes): the vanilla stone variant's behaviour, bone white on the map. */
    private static Properties fossilStone(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.QUARTZ);
    }

    /** Moonpetal wood without bark (planks family, stripped): cherry's behaviour and sounds, pale on the map. */
    private static Properties moonpetal(Block vanilla) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_WHITE);
    }

    /**
     * A glowcap: a mushroom's feel (walk-through, instant, fungus sounds, destroyed by pistons) with its own light, and
     * without the random ticks of vanilla mushrooms (it never spreads).
     */
    private static Properties glowcap(MapColor color, int light) {
        return Properties.of().mapColor(color).noCollission().instabreak().sound(SoundType.FUNGUS)
                .lightLevel(state -> light).pushReaction(PushReaction.DESTROY);
    }

    private static <B extends Block> DeferredBlock<B> register(String name, Supplier<B> block) {
        DeferredBlock<B> holder = BLOCKS.register(name, block);
        ModItems.ITEMS.registerSimpleBlockItem(holder);
        return holder;
    }

    /** A block without an item (potted plants: the pot item is vanilla's). The creative tab skips it. */
    private static <B extends Block> DeferredBlock<B> registerWithoutItem(String name, Supplier<B> block) {
        return BLOCKS.register(name, block);
    }

    /** A flower pot holding {@code plant}; {@link ModFlowerPots} lets the empty vanilla pot accept the plant. */
    private static DeferredBlock<FlowerPotBlock> registerPot(String name, Supplier<? extends Block> plant) {
        return registerWithoutItem(name, () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, plant,
                Properties.ofFullCopy(Blocks.FLOWER_POT)));
    }

    /** Doors need a {@link DoubleHighBlockItem}, which places both halves. */
    private static <B extends DoorBlock> DeferredBlock<B> registerDoor(String name, Supplier<B> block) {
        DeferredBlock<B> holder = BLOCKS.register(name, block);
        ModItems.ITEMS.registerItem(name, properties -> new DoubleHighBlockItem(holder.get(), properties));
        return holder;
    }

    /** Water plants need a {@link PlaceOnWaterBlockItem}, which places them on the water surface like a lily pad. */
    private static <B extends Block> DeferredBlock<B> registerOnWater(String name, Supplier<B> block) {
        DeferredBlock<B> holder = BLOCKS.register(name, block);
        ModItems.ITEMS.registerItem(name, properties -> new PlaceOnWaterBlockItem(holder.get(), properties));
        return holder;
    }
}
