package nl.timtoys.corruptedadventure.worldgen.carver;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import nl.timtoys.corruptedadventure.registry.ModCarverTypes;
import nl.timtoys.corruptedadventure.registry.ModConfiguredCarvers;

/**
 * Datagen bootstrap of Nether ring 2's carver (change 35): vanilla's {@code nether_cave} with its own settings, but of the
 * type {@link RingNetherCarver}, so the tunnels do not fill with lava above the ring's lower lava sea. The settings are read
 * from vanilla's bootstrap (run on a recording context), not copied as numbers.
 */
public final class NetherRing2Carvers {
    private NetherRing2Carvers() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredWorldCarver<?>> context) {
        ConfiguredWorldCarver<?> vanilla = Ring2Carvers.vanilla(context).get(Carvers.NETHER_CAVE);
        if (vanilla == null || !(vanilla.config() instanceof CaveCarverConfiguration config)) {
            throw new IllegalStateException("vanilla no longer has a nether_cave cave carver");
        }
        context.register(ModConfiguredCarvers.NETHER_RING_2_CAVE, new ConfiguredWorldCarver<>(ModCarverTypes.RING_NETHER_CAVE.get(), config));
    }
}
