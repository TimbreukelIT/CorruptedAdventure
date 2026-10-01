package nl.timtoys.corruptedadventure.registry;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.DeadGrassBlock;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;
import nl.timtoys.corruptedadventure.block.StrippableLogBlock;
import nl.timtoys.corruptedadventure.block.WitheredDirtBlock;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * All blocks, each with its block item in {@link ModItems}. Registration order is the creative tab order:
 * per ring, ground first, then the wood set in vanilla order.
 */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CorruptedAdventure.MOD_ID);

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

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    /** All blocks in registration order (datagen: loot tables and models must cover each of them). */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    private static <B extends Block> DeferredBlock<B> register(String name, Supplier<B> block) {
        DeferredBlock<B> holder = BLOCKS.register(name, block);
        ModItems.ITEMS.registerSimpleBlockItem(holder);
        return holder;
    }

    /** Doors need a {@link DoubleHighBlockItem}, which places both halves. */
    private static <B extends DoorBlock> DeferredBlock<B> registerDoor(String name, Supplier<B> block) {
        DeferredBlock<B> holder = BLOCKS.register(name, block);
        ModItems.ITEMS.registerItem(name, properties -> new DoubleHighBlockItem(holder.get(), properties));
        return holder;
    }
}
