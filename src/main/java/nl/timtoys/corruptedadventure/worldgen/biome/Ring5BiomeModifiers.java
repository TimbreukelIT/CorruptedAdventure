package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5OreFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;

import java.util.List;

/**
 * Datagen bootstrap of the ring 5 biome modifiers (like {@link Ring4BiomeModifiers}). Owned by stream W-biomes
 * ({@code docs/plans/ring-5/waves.md}).
 * <p>
 * {@code add_ring_5_ores} (package 95, V11-A): vanilla's ores tapered for ring 5 ({@link Ring5OreFeatures#FACTORS}; no
 * diamond) in vanilla's order, in {@code UNDERGROUND_ORES} of the six ring 5 biomes, after the ring's own ores. One
 * modifier with one list: no feature order cycle.
 */
public final class Ring5BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_RING_5_ORES =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id("add_ring_5_ores"));

    private Ring5BiomeModifiers() {
    }

    /** The features of {@code add_ring_5_ores}, in order. */
    public static List<ResourceKey<PlacedFeature>> ores() {
        return RingVanillaOres.keys(Ring.RING_5, Ring5OreFeatures.FACTORS);
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        context.register(ADD_RING_5_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(Ring5Biomes.ALL.stream().map(biomes::getOrThrow).toList()),
                HolderSet.direct(ores().stream().map(features::getOrThrow).toList()), GenerationStep.Decoration.UNDERGROUND_ORES));
    }
}
