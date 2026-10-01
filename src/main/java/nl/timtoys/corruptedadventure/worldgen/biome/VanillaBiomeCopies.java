package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ring 1's own copies ({@code corruptedadventure:<vanilla path>}) of the vanilla overworld biomes, minus mushroom
 * fields. The datagen bootstrap runs vanilla's own {@link BiomeData#bootstrap} and keeps only those biomes, so the
 * copies stay identical to vanilla without retyping {@code OverworldBiomes}. The only difference is the cave sound of
 * a few copies ({@link #MOODS}).
 * <p>
 * Vanilla data that names biomes by key instead of by tag has to learn the copies too; for wolf variants that is
 * {@link #bootstrapWolfVariants}.
 */
public final class VanillaBiomeCopies {
    /** Vanilla keys that get a copy: every biome of vanilla's overworld climate list except mushroom fields. */
    public static final Set<ResourceKey<Biome>> OVERWORLD_COPIES =
            MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD.usedBiomes()
                    .filter(key -> !key.equals(Biomes.MUSHROOM_FIELDS))
                    .collect(Collectors.toUnmodifiableSet());
    /** Copies with another cave mood sound than vanilla's: the silent zones of ring 1. */
    static final Map<ResourceKey<Biome>, Optional<AmbientMoodSettings>> MOODS = Map.of(
            Biomes.DARK_FOREST, BiomeAmbience.SILENT,
            Biomes.OLD_GROWTH_PINE_TAIGA, BiomeAmbience.SILENT,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA, BiomeAmbience.SILENT,
            Biomes.DRIPSTONE_CAVES, BiomeAmbience.SILENT);

    private VanillaBiomeCopies() {
    }

    public static ResourceKey<Biome> copyOf(ResourceKey<Biome> vanilla) {
        return ResourceKey.create(Registries.BIOME, CorruptedAdventure.id(vanilla.location().getPath()));
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        BiomeData.bootstrap(new CopyingContext(context));
    }

    /**
     * Datagen bootstrap that overrides the vanilla wolf variants whose spawn biomes are a list of biome keys: the list
     * keeps the vanilla biomes and gains their copies. Variants that spawn by tag are left alone; the tags are mirrored.
     */
    public static void bootstrapWolfVariants(BootstrapContext<WolfVariant> context) {
        WolfVariants.bootstrap(new WolfVariantContext(context));
    }

    /** Passes lookups through; registers only {@link #OVERWORLD_COPIES}, under {@link #copyOf}, with {@link #MOODS}. */
    private record CopyingContext(BootstrapContext<Biome> delegate) implements BootstrapContext<Biome> {
        @Override
        public Holder.Reference<Biome> register(ResourceKey<Biome> key, Biome value, Lifecycle lifecycle) {
            if (!OVERWORLD_COPIES.contains(key)) {
                // BiomeData ignores the returned holder, so skipped biomes can return null.
                return null;
            }
            Optional<AmbientMoodSettings> mood = MOODS.get(key);
            return delegate.register(copyOf(key), mood == null ? value : BiomeAmbience.withMood(value, mood), lifecycle);
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

    /** Passes lookups through; re-registers only variants with a direct biome list that contains copied biomes. */
    private record WolfVariantContext(BootstrapContext<WolfVariant> delegate) implements BootstrapContext<WolfVariant> {
        @Override
        public Holder.Reference<WolfVariant> register(ResourceKey<WolfVariant> key, WolfVariant variant, Lifecycle lifecycle) {
            List<Holder<Biome>> vanilla = variant.biomes().unwrap().right().orElse(List.of());
            HolderGetter<Biome> biomes = delegate.lookup(Registries.BIOME);
            List<Holder<Biome>> copies = vanilla.stream()
                    .flatMap(biome -> biome.unwrapKey().stream())
                    .filter(OVERWORLD_COPIES::contains)
                    .<Holder<Biome>>map(biome -> biomes.getOrThrow(copyOf(biome)))
                    .toList();
            if (copies.isEmpty()) {
                // WolfVariants ignores the returned holder.
                return null;
            }
            return delegate.register(key, new WolfVariant(
                    texture(variant.wildTexture()), texture(variant.tameTexture()), texture(variant.angryTexture()),
                    HolderSet.direct(Stream.concat(vanilla.stream(), copies.stream()).toList())), lifecycle);
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
            return delegate.lookup(registryKey);
        }

        @Override
        public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
            return delegate.registryLookup(registry);
        }

        /** The getters return {@code textures/<path>.png}; the constructor wants {@code <path>}. */
        private static ResourceLocation texture(ResourceLocation full) {
            return full.withPath(path -> path.substring("textures/".length(), path.length() - ".png".length()));
        }
    }
}
