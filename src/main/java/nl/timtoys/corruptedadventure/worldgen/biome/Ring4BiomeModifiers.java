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
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4OreFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;

import java.util.List;
import java.util.stream.Stream;

/**
 * Datagen bootstrap of the ring 4 biome modifiers (like {@link Ring3BiomeModifiers}). Owned by stream W-biomes
 * ({@code docs/plans/ring-4/waves.md}).
 * <p>
 * {@code add_ring_4_ores} (package 77): vanilla's ores tapered for ring 4 in vanilla's order, then tidesilver and jade,
 * in {@code UNDERGROUND_ORES} of the ring's own biomes and {@code sundered_reach} (its ring 5 half keeps none: every
 * ore of the list has a {@code ring_chance} on ring 4). Not in {@code verdant_falls} (golf 2: no 75–77 feature there;
 * ores must not open its exact walls). One modifier with one list, so the order is fixed and follows the biomes' own
 * ores: no feature order cycle.
 */
public final class Ring4BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_RING_4_ORES =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id("add_ring_4_ores"));

    private Ring4BiomeModifiers() {
    }

    /** The features of {@code add_ring_4_ores}, in order. */
    public static List<ResourceKey<PlacedFeature>> ores() {
        return Stream.concat(RingVanillaOres.keys(Ring.RING_4).stream(), Ring4OreFeatures.OWN.stream()).toList();
    }

    /** The biomes of {@code add_ring_4_ores}: the ring's own and the zone 4→5. */
    public static List<ResourceKey<Biome>> oreBiomes() {
        return Stream.concat(Ring4Biomes.ALL.stream(), Stream.of(ModBiomes.SUNDERED_REACH)).toList();
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        context.register(ADD_RING_4_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(oreBiomes().stream().map(biomes::getOrThrow).toList()),
                HolderSet.direct(ores().stream().map(features::getOrThrow).toList()), GenerationStep.Decoration.UNDERGROUND_ORES));
    }
}
