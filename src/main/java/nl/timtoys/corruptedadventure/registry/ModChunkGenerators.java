package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;

public final class ModChunkGenerators {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_noise} */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RingChunkGenerator>> RING_NOISE =
            CHUNK_GENERATORS.register("ring_noise", () -> RingChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }

    public static void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
    }
}
