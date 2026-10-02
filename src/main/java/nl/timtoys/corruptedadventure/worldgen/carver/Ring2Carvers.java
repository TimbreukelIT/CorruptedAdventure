package nl.timtoys.corruptedadventure.worldgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import nl.timtoys.corruptedadventure.registry.ModConfiguredCarvers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Datagen bootstrap for the carvers of ring 2 (change 26, choice of the user): vanilla's {@code cave},
 * {@code cave_extra_underground} and {@code canyon}, but starting no higher than {@link #MAX_START_Y}. Carvers hollow
 * out after the surface rules, so near the surface they leave sand, red sand and gravel hanging over their tunnels;
 * deep down they still cut through the sand caves' loose sand ceilings. The probability shrinks with the start range,
 * so below the cap there are as many tunnels as in vanilla (not vanilla's whole count packed into the lower part).
 * <p>
 * The settings are read from vanilla's own bootstrap (run on a recording context), not copied as numbers.
 */
public final class Ring2Carvers {
    /** Highest start of a carver tunnel or ravine in ring 2 (tuned in change 26: no hanging blocks at the surface). */
    public static final int MAX_START_Y = 40;
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_MAX_Y = 319;
    /** Ring 2 carver → the vanilla carver it copies, in vanilla's order. */
    public static final Map<ResourceKey<ConfiguredWorldCarver<?>>, ResourceKey<ConfiguredWorldCarver<?>>> COPIES = copies();

    private Ring2Carvers() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredWorldCarver<?>> context) {
        Map<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> vanilla = vanilla(context);
        COPIES.forEach((key, original) -> context.register(key, deep(vanilla.get(original))));
    }

    /** The same carver with its start height capped at {@link #MAX_START_Y} and its probability scaled to match. */
    static ConfiguredWorldCarver<?> deep(ConfiguredWorldCarver<?> original) {
        CarverConfiguration config = original.config();
        if (!(config.y instanceof UniformHeight)) {
            throw new IllegalStateException("vanilla carver without a uniform height: " + config.y);
        }
        VerticalAnchor lowest = field(HeightProvider.CODEC, config.y, "min_inclusive", VerticalAnchor.CODEC);
        VerticalAnchor highest = field(HeightProvider.CODEC, config.y, "max_inclusive", VerticalAnchor.CODEC);
        HeightProvider y = UniformHeight.of(lowest, VerticalAnchor.absolute(MAX_START_Y));
        float probability = config.probability * keptShare(resolve(lowest), resolve(highest));
        CarverConfiguration capped = new CarverConfiguration(probability, y, config.yScale, config.lavaLevel, config.debugSettings, config.replaceable);
        if (config instanceof CaveCarverConfiguration cave) {
            return new ConfiguredWorldCarver<>(net.minecraft.world.level.levelgen.carver.WorldCarver.CAVE,
                    new CaveCarverConfiguration(capped, cave.horizontalRadiusMultiplier, cave.verticalRadiusMultiplier, floorLevel(cave)));
        }
        if (config instanceof CanyonCarverConfiguration canyon) {
            return new ConfiguredWorldCarver<>(net.minecraft.world.level.levelgen.carver.WorldCarver.CANYON,
                    new CanyonCarverConfiguration(capped, canyon.verticalRotation, canyon.shape));
        }
        throw new IllegalStateException("unknown vanilla carver configuration: " + config);
    }

    /** Share of the start heights {@code min..max} (inclusive) that is at most {@link #MAX_START_Y}. */
    static float keptShare(int min, int max) {
        if (max <= MAX_START_Y) {
            return 1.0F;
        }
        return (float) (MAX_START_Y - min + 1) / (max - min + 1);
    }

    /** An anchor in the overworld (ring 2 only exists there). */
    private static int resolve(VerticalAnchor anchor) {
        var json = VerticalAnchor.CODEC.encodeStart(JsonOps.INSTANCE, anchor).getOrThrow().getAsJsonObject();
        if (json.has("absolute")) {
            return json.get("absolute").getAsInt();
        }
        if (json.has("above_bottom")) {
            return OVERWORLD_MIN_Y + json.get("above_bottom").getAsInt();
        }
        return OVERWORLD_MAX_Y - json.get("below_top").getAsInt();
    }

    /**
     * Vanilla's floor level of a cave carver. The field is package-private, and encoding the whole configuration would
     * dereference its block tag, which datagen cannot do yet; datagen only.
     */
    private static FloatProvider floorLevel(CaveCarverConfiguration cave) {
        return ObfuscationReflectionHelper.getPrivateValue(CaveCarverConfiguration.class, cave, "floorLevel");
    }

    /** One field of {@code value}, encoded with {@code codec} and decoded with {@code fieldCodec} (no registries needed). */
    private static <V, F> F field(Codec<V> codec, V value, String name, Codec<F> fieldCodec) {
        return codec.encodeStart(JsonOps.INSTANCE, value)
                .flatMap(json -> fieldCodec.parse(JsonOps.INSTANCE, json.getAsJsonObject().get(name)))
                .getOrThrow();
    }

    private static Map<ResourceKey<ConfiguredWorldCarver<?>>, ResourceKey<ConfiguredWorldCarver<?>>> copies() {
        Map<ResourceKey<ConfiguredWorldCarver<?>>, ResourceKey<ConfiguredWorldCarver<?>>> copies = new LinkedHashMap<>();
        copies.put(ModConfiguredCarvers.RING_2_CAVE, Carvers.CAVE);
        copies.put(ModConfiguredCarvers.RING_2_CAVE_EXTRA_UNDERGROUND, Carvers.CAVE_EXTRA_UNDERGROUND);
        copies.put(ModConfiguredCarvers.RING_2_CANYON, Carvers.CANYON);
        return java.util.Collections.unmodifiableMap(copies);
    }

    /** Vanilla's configured carvers, built on {@code context}'s lookups (so their block tags resolve there). */
    static Map<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> vanilla(BootstrapContext<ConfiguredWorldCarver<?>> context) {
        Map<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> recorded = new HashMap<>();
        Carvers.bootstrap(new BootstrapContext<>() {
            @Override
            public Holder.Reference<ConfiguredWorldCarver<?>> register(ResourceKey<ConfiguredWorldCarver<?>> key, ConfiguredWorldCarver<?> value, Lifecycle lifecycle) {
                recorded.put(key, value);
                // Carvers ignores the returned holder.
                return null;
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return context.lookup(registryKey);
            }

            @Override
            public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
                return context.registryLookup(registry);
            }
        });
        return recorded;
    }
}
