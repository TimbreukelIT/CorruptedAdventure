package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tapers vanilla's ores per ring (changes 25, 41, 61, 81): per ring and ore a placed feature
 * {@code corruptedadventure:ring_<n>/<vanilla path>} = vanilla's own placed feature (same configured feature, same
 * modifiers) with one {@link RingChanceFilter} ({@code progress}, fixed chance) right before the biome filter. Its
 * chance is 0 outside the ring, so a tapered ore never shows up in another ring.
 * <p>
 * Vanilla's placements are read by running {@link OrePlacements#bootstrap} on a context that only records, so the
 * numbers are never copied and follow a Minecraft update. A ring's biomes must not get vanilla's own ore features
 * (see {@code Ring2Biomes.generation}); they get {@link #keys} through a biome modifier instead.
 */
public final class RingVanillaOres {
    /** The ores of vanilla's {@code BiomeDefaultFeatures.addDefaultOres(builder, false)}, in its order (not {@code underwater_magma}). */
    public static final List<ResourceKey<PlacedFeature>> ORES = List.of(
            OrePlacements.ORE_COAL_UPPER, OrePlacements.ORE_COAL_LOWER,
            OrePlacements.ORE_IRON_UPPER, OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL,
            OrePlacements.ORE_GOLD, OrePlacements.ORE_GOLD_LOWER,
            OrePlacements.ORE_REDSTONE, OrePlacements.ORE_REDSTONE_LOWER,
            OrePlacements.ORE_DIAMOND, OrePlacements.ORE_DIAMOND_MEDIUM, OrePlacements.ORE_DIAMOND_LARGE, OrePlacements.ORE_DIAMOND_BURIED,
            OrePlacements.ORE_LAPIS, OrePlacements.ORE_LAPIS_BURIED,
            OrePlacements.ORE_COPPER);

    private RingVanillaOres() {
    }

    /**
     * The chance per vanilla ore placement attempt, by kind: coal, iron and copper; gold, redstone and lapis; diamond.
     * Each in (0, 1].
     */
    public static Map<ResourceKey<PlacedFeature>, Double> factors(double coalIronCopper, double goldRedstoneLapis, double diamond) {
        Map<ResourceKey<PlacedFeature>, Double> factors = new HashMap<>();
        for (ResourceKey<PlacedFeature> ore : ORES) {
            String path = ore.location().getPath();
            double factor = path.startsWith("ore_diamond") ? diamond
                    : path.startsWith("ore_gold") || path.startsWith("ore_redstone") || path.startsWith("ore_lapis") ? goldRedstoneLapis
                    : coalIronCopper;
            if (!(factor > 0.0 && factor <= 1.0)) {
                throw new IllegalArgumentException("ore factor must be in (0, 1], got " + factor + " for " + path);
            }
            factors.put(ore, factor);
        }
        return Map.copyOf(factors);
    }

    /**
     * Registers the tapered ores of {@code ring}; {@code factors} needs an entry for every ore of {@link #ORES}.
     * {@code extra} filters follow the chance (ring 4: no ore in the islands, backlog r. 420).
     */
    public static void placed(BootstrapContext<PlacedFeature> context, Ring ring, Map<ResourceKey<PlacedFeature>, Double> factors,
                              PlacementModifier... extra) {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> vanilla = vanillaPlacements(context);
        for (ResourceKey<PlacedFeature> ore : ORES) {
            Double factor = factors.get(ore);
            if (factor == null) {
                throw new IllegalArgumentException("no factor for " + ore.location() + " in " + ring.getSerializedName());
            }
            PlacedFeature original = vanilla.get(ore);
            if (original == null) {
                throw new IllegalStateException("vanilla no longer registers " + ore.location());
            }
            context.register(key(ring, ore), tapered(original, RingChanceFilter.progress(ring, factor, factor, 0.0, 1.0), extra));
        }
    }

    /** The keys of the tapered ores of {@code ring}, in vanilla's order (for the ring's biome modifier). */
    public static List<ResourceKey<PlacedFeature>> keys(Ring ring) {
        return ORES.stream().map(ore -> key(ring, ore)).toList();
    }

    /** {@code corruptedadventure:ring_<n>/<vanilla path>}. */
    public static ResourceKey<PlacedFeature> key(Ring ring, ResourceKey<PlacedFeature> ore) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id(ring.getSerializedName() + "/" + ore.location().getPath()));
    }

    /** The same feature and modifiers, with {@code chance} (and {@code extra}) inserted right before the (single) biome filter. */
    static PlacedFeature tapered(PlacedFeature original, PlacementModifier chance, PlacementModifier... extra) {
        List<PlacementModifier> modifiers = new ArrayList<>(original.placement());
        int biome = -1;
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i) instanceof BiomeFilter) {
                if (biome >= 0) {
                    throw new IllegalStateException("more than one biome filter in " + original);
                }
                biome = i;
            }
        }
        if (biome < 0) {
            throw new IllegalStateException("no biome filter in " + original);
        }
        modifiers.add(biome, chance);
        modifiers.addAll(biome + 1, List.of(extra));
        return new PlacedFeature(original.feature(), List.copyOf(modifiers));
    }

    /** Vanilla's ore placed features, built on {@code context}'s lookups (so their configured features resolve there). */
    static Map<ResourceKey<PlacedFeature>, PlacedFeature> vanillaPlacements(BootstrapContext<PlacedFeature> context) {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> recorded = new HashMap<>();
        OrePlacements.bootstrap(new RecordingContext(context, recorded));
        return recorded;
    }

    /** Passes lookups through and only records what is registered. */
    private record RecordingContext(BootstrapContext<PlacedFeature> delegate, Map<ResourceKey<PlacedFeature>, PlacedFeature> recorded)
            implements BootstrapContext<PlacedFeature> {
        @Override
        public Holder.Reference<PlacedFeature> register(ResourceKey<PlacedFeature> key, PlacedFeature value, Lifecycle lifecycle) {
            recorded.put(key, value);
            // OrePlacements ignores the returned holder.
            return null;
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
            return delegate.lookup(registryKey);
        }

        @Override
        public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
            return delegate.registryLookup(registry);
        }
    }
}
