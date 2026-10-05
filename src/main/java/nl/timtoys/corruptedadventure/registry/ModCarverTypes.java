package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.carver.RingNetherCarver;

/** Carver types with our own code; the configured carvers are datagen ({@code NetherRing2Carvers}, {@code Ring2Carvers}). */
public final class ModCarverTypes {
    private static final DeferredRegister<WorldCarver<?>> CARVERS = DeferredRegister.create(Registries.CARVER, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_nether_cave} (change 35) */
    public static final DeferredHolder<WorldCarver<?>, RingNetherCarver> RING_NETHER_CAVE =
            CARVERS.register("ring_nether_cave", () -> new RingNetherCarver(CaveCarverConfiguration.CODEC));

    private ModCarverTypes() {
    }

    public static void register(IEventBus modBus) {
        CARVERS.register(modBus);
    }
}
