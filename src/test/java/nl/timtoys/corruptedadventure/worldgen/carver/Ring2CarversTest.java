package nl.timtoys.corruptedadventure.worldgen.carver;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring2CarversTest {
    private static final HolderLookup.Provider VANILLA = VanillaRegistries.createLookup();
    /** Kept share of vanilla's start heights (inclusive): cave −56…180, extra −56…47, canyon 10…67, capped at 40. */
    private static final Map<ResourceKey<ConfiguredWorldCarver<?>>, Float> SHARES = Map.of(
            Carvers.CAVE, 97.0F / 237.0F, Carvers.CAVE_EXTRA_UNDERGROUND, 97.0F / 104.0F, Carvers.CANYON, 31.0F / 58.0F);

    /** Each ring 2 carver is vanilla's, with only the upper bound of its start height lowered and the probability scaled. */
    @Test
    void vanillaCarversWithOnlyALowerStart() {
        Map<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> vanilla = Ring2Carvers.vanilla(context());
        assertEquals(java.util.List.of(Carvers.CAVE, Carvers.CAVE_EXTRA_UNDERGROUND, Carvers.CANYON), java.util.List.copyOf(Ring2Carvers.COPIES.values()));
        RegistryOps<com.google.gson.JsonElement> ops = VANILLA.createSerializationContext(JsonOps.INSTANCE);
        for (ResourceKey<ConfiguredWorldCarver<?>> original : Ring2Carvers.COPIES.values()) {
            var expected = ConfiguredWorldCarver.DIRECT_CODEC.encodeStart(ops, VANILLA.lookupOrThrow(Registries.CONFIGURED_CARVER).getOrThrow(original).value())
                    .getOrThrow().getAsJsonObject();
            var actual = ConfiguredWorldCarver.DIRECT_CODEC.encodeStart(ops, Ring2Carvers.deep(vanilla.get(original))).getOrThrow().getAsJsonObject();
            var expectedY = expected.getAsJsonObject("config").getAsJsonObject("y");
            var actualY = actual.getAsJsonObject("config").getAsJsonObject("y");
            assertEquals(expectedY.get("min_inclusive"), actualY.get("min_inclusive"), original.toString());
            assertEquals(Ring2Carvers.MAX_START_Y, actualY.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
            assertTrue(expectedY.getAsJsonObject("max_inclusive").get("absolute").getAsInt() > Ring2Carvers.MAX_START_Y);
            float share = SHARES.get(original);
            assertEquals(expected.getAsJsonObject("config").get("probability").getAsFloat() * share,
                    actual.getAsJsonObject("config").get("probability").getAsFloat(), 1e-6, original.toString());
            for (var json : java.util.List.of(expected, actual)) {
                json.getAsJsonObject("config").remove("y");
                json.getAsJsonObject("config").remove("probability");
            }
            assertEquals(expected, actual, original.toString());
        }
    }

    @Test
    void keptShare() {
        assertEquals(1.0F, Ring2Carvers.keptShare(-56, 30));
        assertEquals(97.0F / 237.0F, Ring2Carvers.keptShare(-56, 180), 1e-6);
    }

    private static BootstrapContext<ConfiguredWorldCarver<?>> context() {
        return new BootstrapContext<>() {
            @Override
            public Holder.Reference<ConfiguredWorldCarver<?>> register(ResourceKey<ConfiguredWorldCarver<?>> key, ConfiguredWorldCarver<?> value, Lifecycle lifecycle) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return VANILLA.lookupOrThrow(registryKey);
            }

            @Override
            public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
                return VANILLA.lookup(registry);
            }
        };
    }
}
