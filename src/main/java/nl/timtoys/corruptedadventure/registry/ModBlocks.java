package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.BarrelCactusBlock;
import nl.timtoys.corruptedadventure.block.DeadGrassBlock;
import nl.timtoys.corruptedadventure.block.DesertFlowerBlock;
import nl.timtoys.corruptedadventure.block.DryShrubBlock;
import nl.timtoys.corruptedadventure.block.LooseSandTrigger;
import nl.timtoys.corruptedadventure.block.ModFlowerPots;
import nl.timtoys.corruptedadventure.block.ModTreeGrowers;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;
import nl.timtoys.corruptedadventure.block.PalmSaplingBlock;
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
