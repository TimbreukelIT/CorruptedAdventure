package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.biome.RingBiomeSource;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaClimateBiomeSource;

public final class ModBiomeSources {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring} */
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<RingBiomeSource>> RING =
            BIOME_SOURCES.register("ring", () -> RingBiomeSource.CODEC);

    /** {@code corruptedadventure:vanilla_climate} */
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<VanillaClimateBiomeSource>> VANILLA_CLIMATE =
            BIOME_SOURCES.register("vanilla_climate", () -> VanillaClimateBiomeSource.CODEC);

    private ModBiomeSources() {
    }

    public static void register(IEventBus modBus) {
        BIOME_SOURCES.register(modBus);
    }
}
