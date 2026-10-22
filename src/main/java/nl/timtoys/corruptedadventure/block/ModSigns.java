package nl.timtoys.corruptedadventure.block;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Lets vanilla's sign and hanging sign block entity types accept our signs (NeoForge's
 * {@link BlockEntityTypeAddBlocksEvent}). Vanilla's sign blocks create those vanilla block entities, so with this their
 * text, sync, edit screens and renderers serve our signs without a block entity type of our own.
 */
public final class ModSigns {
    private ModSigns() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModSigns::addSignBlocks);
    }

    private static void addSignBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, ModBlocks.CORRUPTED_SIGN.get(), ModBlocks.CORRUPTED_WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, ModBlocks.CORRUPTED_HANGING_SIGN.get(), ModBlocks.CORRUPTED_WALL_HANGING_SIGN.get());
    }
}
