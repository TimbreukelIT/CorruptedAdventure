package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Our own NeoForge data map types (common: registered on both sides, synced to clients by NeoForge). Their contents come
 * from datagen ({@code ModDataMapProvider}).
 */
public final class ModDataMaps {
    /**
     * {@code corruptedadventure:healthy_block}: the healthy counterpart of a corrupted block (ring 5,
     * {@code docs/plans/ring-5/contract.md} §3), so phase 5 can heal or decay the world. Every block in
     * {@code #corruptedadventure:corrupted} has one (contents by stream B, 88). Synced, not mandatory.
     */
    public static final DataMapType<Block, Block> HEALTHY_BLOCK = DataMapType
            .builder(CorruptedAdventure.id("healthy_block"), Registries.BLOCK, BuiltInRegistries.BLOCK.byNameCodec())
            .synced(BuiltInRegistries.BLOCK.byNameCodec(), false)
            .build();

    private ModDataMaps() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModDataMaps::registerTypes);
    }

    private static void registerTypes(RegisterDataMapTypesEvent event) {
        event.register(HEALTHY_BLOCK);
    }
}
