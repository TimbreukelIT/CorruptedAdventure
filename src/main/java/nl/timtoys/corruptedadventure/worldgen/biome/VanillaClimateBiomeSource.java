package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code corruptedadventure:vanilla_climate}: a vanilla multi-noise preset with every biome swapped for our copy
 * ({@link VanillaBiomeCopies#copyOf}) or, for biomes without a copy, for the biome given in {@code replace}.
 * Only {@code preset} and {@code replace} are saved, so {@code level.dat} stays as small as with vanilla's preset.
 */
public final class VanillaClimateBiomeSource extends BiomeSource {
    private static final Codec<Map<ResourceKey<Biome>, ResourceKey<Biome>>> REPLACE_CODEC =
            Codec.unboundedMap(ResourceKey.codec(Registries.BIOME), ResourceKey.codec(Registries.BIOME));

    public static final MapCodec<VanillaClimateBiomeSource> CODEC = RecordCodecBuilder.<Definition>mapCodec(instance -> instance.group(
            MultiNoiseBiomeSourceParameterList.Preset.CODEC.fieldOf("preset").forGetter(Definition::preset),
            REPLACE_CODEC.optionalFieldOf("replace", Map.of()).forGetter(Definition::replace),
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, Definition::new)).flatXmap(VanillaClimateBiomeSource::create, source -> DataResult.success(source.definition));

    private final Definition definition;
    private final Climate.ParameterList<Holder<Biome>> parameters;

    private VanillaClimateBiomeSource(Definition definition, Climate.ParameterList<Holder<Biome>> parameters) {
        this.definition = definition;
        this.parameters = parameters;
    }

    /** Builds the parameter list once (R-tree); fails with the missing keys instead of throwing. */
    static DataResult<VanillaClimateBiomeSource> create(Definition definition) {
        Climate.ParameterList<ResourceKey<Biome>> vanilla = MultiNoiseBiomeSourceParameterList.knownPresets().get(definition.preset());
        Set<ResourceKey<Biome>> used = new LinkedHashSet<>();
        vanilla.values().forEach(entry -> used.add(entry.getSecond()));

        List<ResourceKey<Biome>> unknown = definition.replace().keySet().stream().filter(key -> !used.contains(key)).toList();
        if (!unknown.isEmpty()) {
            return DataResult.error(() -> "vanilla_climate replaces biomes that preset " + definition.preset().id() + " does not use: " + unknown);
        }

        Set<ResourceKey<Biome>> missing = new LinkedHashSet<>();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>(vanilla.values().size());
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : vanilla.values()) {
            ResourceKey<Biome> target = definition.replace().getOrDefault(entry.getSecond(), VanillaBiomeCopies.copyOf(entry.getSecond()));
            Optional<? extends Holder<Biome>> holder = definition.biomes().get(target);
            if (holder.isPresent()) {
                values.add(Pair.of(entry.getFirst(), holder.get()));
            } else {
                missing.add(target);
            }
        }
        if (!missing.isEmpty()) {
            return DataResult.error(() -> "vanilla_climate is missing biomes for preset " + definition.preset().id() + ": " + missing);
        }
        return DataResult.success(new VanillaClimateBiomeSource(definition, new Climate.ParameterList<>(values)));
    }

    Climate.ParameterList<Holder<Biome>> parameters() {
        return parameters;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return parameters.values().stream().map(Pair::getSecond).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return parameters.findValue(sampler.sample(x, y, z));
    }

    /** The saved form; {@code biomes} comes from the registry context and is never written. */
    record Definition(MultiNoiseBiomeSourceParameterList.Preset preset, Map<ResourceKey<Biome>, ResourceKey<Biome>> replace,
                      HolderGetter<Biome> biomes) {
    }
}
