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
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;

import java.util.List;
import java.util.stream.Stream;

/**
 * Datagen bootstrap of the biome modifiers of ring 2: its ores ({@code add_ring_2_ores}, {@code #is_ring_2},
 * {@code UNDERGROUND_ORES}). One modifier with one list, so the order is fixed (NeoForge applies several modifiers
 * alphabetically): vanilla's tapered ores in vanilla's order, then sunstone and turquoise. They exist only in ring 2
 * biomes, so there is no feature order cycle with ring 1.
 */
public final class Ring2BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_RING_2_ORES =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id("add_ring_2_ores"));

    private Ring2BiomeModifiers() {
    }

    /** The features of {@code add_ring_2_ores}, in order. */
    public static List<ResourceKey<PlacedFeature>> ores() {
        return Stream.concat(RingVanillaOres.keys(Ring.RING_2).stream(),
                Stream.of(ModPlacedFeatures.SUNSTONE_ORE, ModPlacedFeatures.TURQUOISE_ORE)).toList();
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        context.register(ADD_RING_2_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(ModTags.Biomes.IS_RING_2),
                HolderSet.direct(ores().stream().map(features::getOrThrow).toList()), GenerationStep.Decoration.UNDERGROUND_ORES));
    }
}
