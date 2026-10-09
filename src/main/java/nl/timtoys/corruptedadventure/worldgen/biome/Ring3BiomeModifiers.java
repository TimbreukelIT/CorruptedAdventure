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
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring3DressingFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring3OreFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;

import java.util.List;
import java.util.stream.Stream;

/**
 * Datagen bootstrap of the ring 3 biome modifiers (change 41; the riverbed stumps of change 47): its ores ({@code add_ring_3_ores},
 * {@code UNDERGROUND_ORES}) in {@code #is_ring_3} and in the transition {@code dry_riverbeds} (backlog r. 123), whose
 * ring 3 half gets the ring 3 set: every ore of the list has a {@code ring_chance} on ring 3, so it stays out of the
 * ring 2 half. Of the ring 2 set ({@code add_ring_2_ores}) only vanilla's tapered ores stay out of the ring 3 half;
 * sunstone and turquoise have no {@code ring_chance} and follow the canyon stone into it (backlog). One modifier with one list,
 * so the order is fixed: vanilla's tapered ores in vanilla's order, then {@link Ring3OreFeatures#OWN}. NeoForge applies
 * modifiers alphabetically, so in {@code dry_riverbeds} the ring 2 ores come first, everywhere else this list follows
 * the biome's own ores: no feature order cycle.
 */
public final class Ring3BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_RING_3_ORES =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id("add_ring_3_ores"));
    /**
     * The charred stumps on the ash patches of {@code dry_riverbeds} (change 47, backlog r. 122): the biome is ring 2's
     * ({@code TransitionBiomes}), so ring 3's addition to it comes through a modifier, after its own vegetation.
     */
    public static final ResourceKey<BiomeModifier> ADD_RING_3_RIVERBED_STUMPS =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id("add_ring_3_riverbed_stumps"));

    private Ring3BiomeModifiers() {
    }

    /** The features of {@code add_ring_3_ores}, in order. */
    public static List<ResourceKey<PlacedFeature>> ores() {
        return Stream.concat(RingVanillaOres.keys(Ring.RING_3).stream(), Ring3OreFeatures.OWN.stream()).toList();
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> targets = new OrHolderSet<>(List.of(biomes.getOrThrow(ModTags.Biomes.IS_RING_3),
                HolderSet.direct(biomes.getOrThrow(ModBiomes.DRY_RIVERBEDS))));
        context.register(ADD_RING_3_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(targets,
                HolderSet.direct(ores().stream().map(features::getOrThrow).toList()), GenerationStep.Decoration.UNDERGROUND_ORES));
        context.register(ADD_RING_3_RIVERBED_STUMPS, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(ModBiomes.DRY_RIVERBEDS)),
                HolderSet.direct(features.getOrThrow(Ring3DressingFeatures.DRY_RIVERBEDS_CHARRED_STUMPS)), GenerationStep.Decoration.VEGETAL_DECORATION));
    }
}
