package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
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
import nl.timtoys.corruptedadventure.block.AshSproutsBlock;
import nl.timtoys.corruptedadventure.block.BarrelCactusBlock;
import nl.timtoys.corruptedadventure.block.EmberBerryBushBlock;
import nl.timtoys.corruptedadventure.block.EmberLeavesBlock;
import nl.timtoys.corruptedadventure.block.FireBloomBlock;
import nl.timtoys.corruptedadventure.block.DeadGrassBlock;
import nl.timtoys.corruptedadventure.block.DesertFlowerBlock;
import nl.timtoys.corruptedadventure.block.DewSourceBlock;
import nl.timtoys.corruptedadventure.block.DryShrubBlock;
import nl.timtoys.corruptedadventure.block.GlowMoldBlock;
import nl.timtoys.corruptedadventure.block.LooseSandTrigger;
import nl.timtoys.corruptedadventure.block.ModFlowerPots;
import nl.timtoys.corruptedadventure.block.ModTreeGrowers;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;
import nl.timtoys.corruptedadventure.block.PalmSaplingBlock;
import nl.timtoys.corruptedadventure.block.SoakedNetherrackBlock;
import nl.timtoys.corruptedadventure.block.SporeMossBlock;
import nl.timtoys.corruptedadventure.block.SporeSproutsBlock;
import nl.timtoys.corruptedadventure.block.StalactiteBlock;
import nl.timtoys.corruptedadventure.block.StrippableLogBlock;
import nl.timtoys.corruptedadventure.block.WitheredDirtBlock;

import java.util.Collection;
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
    /** Ring 3 ores are this much harder than the vanilla ore of their host ("one step above ring 2"). */
    static final float RING_3_ORE_EXTRA_HARDNESS = 0.5F;

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
            () -> new AmethystClusterBlock(3.0F, 4.0F, dewCrystal(Blocks.SMALL_AMETHYST_BUD, 1)));
    public static final DeferredBlock<AmethystClusterBlock> MEDIUM_DEW_CRYSTAL_BUD = register("medium_dew_crystal_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, dewCrystal(Blocks.MEDIUM_AMETHYST_BUD, 2)));
    public static final DeferredBlock<AmethystClusterBlock> LARGE_DEW_CRYSTAL_BUD = register("large_dew_crystal_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, dewCrystal(Blocks.LARGE_AMETHYST_BUD, 4)));
    /** Full-grown stage; the default state faces up. */
    public static final DeferredBlock<AmethystClusterBlock> DEW_CRYSTAL_CLUSTER = register("dew_crystal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, dewCrystal(Blocks.AMETHYST_CLUSTER, 5)));

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
     * A crust of ash over caves and lava tubes: falls like sand and caves in when a player walks under it (the
     * {@code collapsing_blocks} tag, {@link LooseSandTrigger}). Breaks into plain ash.
     */
    public static final DeferredBlock<ColoredFallingBlock> ASH_CRUST = register("ash_crust",
            () -> new ColoredFallingBlock(new ColorRGBA(ASH_DUST_COLOR), Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_GRAY)));
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

    /** A dew crystal stage: the amethyst stage's behaviour and sounds, light level given per stage, light blue on the map. */
    private static Properties dewCrystal(Block vanilla, int light) {
        return Properties.ofFullCopy(vanilla).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> light);
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
}
