package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.RubbleConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.RubbleFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelFeature;

/** Feature types with their own code; the configured and placed features come from datagen. */
public final class ModFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:bare_patch} */
    public static final DeferredHolder<Feature<?>, Feature<BarePatchConfiguration>> BARE_PATCH =
            FEATURES.register("bare_patch", () -> new BarePatchFeature(BarePatchConfiguration.CODEC));
    /** {@code corruptedadventure:unnatural_tunnel} */
    public static final DeferredHolder<Feature<?>, Feature<UnnaturalTunnelConfiguration>> UNNATURAL_TUNNEL =
            FEATURES.register("unnatural_tunnel", () -> new UnnaturalTunnelFeature(UnnaturalTunnelConfiguration.CODEC));
    /** {@code corruptedadventure:rubble} */
    public static final DeferredHolder<Feature<?>, Feature<RubbleConfiguration>> RUBBLE =
            FEATURES.register("rubble", () -> new RubbleFeature(RubbleConfiguration.CODEC));
    /** {@code corruptedadventure:canyon_spring} */
    public static final DeferredHolder<Feature<?>, Feature<CanyonSpringConfiguration>> CANYON_SPRING =
            FEATURES.register("canyon_spring", () -> new CanyonSpringFeature(CanyonSpringConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
