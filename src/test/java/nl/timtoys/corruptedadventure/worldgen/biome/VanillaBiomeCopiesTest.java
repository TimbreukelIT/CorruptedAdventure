package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaBiomeCopiesTest {
    @Test
    void copiesAreTheOverworldBiomesWithoutMushroomFields() {
        assertEquals(52, VanillaBiomeCopies.OVERWORLD_COPIES.size());
        assertFalse(VanillaBiomeCopies.OVERWORLD_COPIES.contains(Biomes.MUSHROOM_FIELDS));
        assertTrue(VanillaBiomeCopies.OVERWORLD_COPIES.containsAll(List.of(
                Biomes.PLAINS, Biomes.CHERRY_GROVE, Biomes.MANGROVE_SWAMP, Biomes.DEEP_DARK, Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES)));
    }

    @Test
    void netherCopiesAreWastesCrimsonAndWarped() {
        assertEquals(Set.of(Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST), VanillaBiomeCopies.NETHER_COPIES);
        assertEquals(55, VanillaBiomeCopies.COPIES.size());
        assertTrue(VanillaBiomeCopies.COPIES.containsAll(VanillaBiomeCopies.OVERWORLD_COPIES));
        assertTrue(VanillaBiomeCopies.COPIES.containsAll(VanillaBiomeCopies.NETHER_COPIES));
    }

    @Test
    void copyKeepsThePathInOurNamespace() {
        assertEquals(CorruptedAdventure.id("cherry_grove"), VanillaBiomeCopies.copyOf(Biomes.CHERRY_GROVE).location());
    }

    @Test
    void bootstrapRegistersExactlyTheCopies() {
        RecordingContext context = new RecordingContext();
        VanillaBiomeCopies.bootstrap(context);

        Set<ResourceKey<Biome>> expected = VanillaBiomeCopies.COPIES.stream()
                .map(VanillaBiomeCopies::copyOf)
                .collect(Collectors.toSet());
        assertEquals(expected, context.registered);
        assertTrue(context.registered.stream().allMatch(key -> key.location().getNamespace().equals(CorruptedAdventure.MOD_ID)));
        for (ResourceKey<Biome> excluded : List.of(Biomes.MUSHROOM_FIELDS, Biomes.THE_VOID, Biomes.SOUL_SAND_VALLEY,
                Biomes.BASALT_DELTAS, Biomes.THE_END)) {
            assertFalse(context.registered.contains(VanillaBiomeCopies.copyOf(excluded)), excluded.toString());
        }
    }

    @Test
    void wolfVariantsWithBiomeKeysGainTheCopies() {
        Map<ResourceKey<WolfVariant>, WolfVariant> registered = new HashMap<>();
        VanillaBiomeCopies.bootstrapWolfVariants(new BootstrapContext<>() {
            @Override
            public Holder.Reference<WolfVariant> register(ResourceKey<WolfVariant> key, WolfVariant value, Lifecycle lifecycle) {
                registered.put(key, value);
                return null;
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return new StandaloneHolderGetter<>(key -> true);
            }
        });

        Map<ResourceKey<WolfVariant>, ResourceKey<Biome>> byKey = Map.of(
                WolfVariants.PALE, Biomes.TAIGA, WolfVariants.SNOWY, Biomes.GROVE,
                WolfVariants.BLACK, Biomes.OLD_GROWTH_PINE_TAIGA, WolfVariants.ASHEN, Biomes.SNOWY_TAIGA,
                WolfVariants.WOODS, Biomes.FOREST, WolfVariants.CHESTNUT, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        assertEquals(byKey.keySet(), registered.keySet(), "tag-based variants (spotted, rusty, striped) stay vanilla");
        byKey.forEach((variant, biome) -> assertEquals(
                Set.of(biome, VanillaBiomeCopies.copyOf(biome)),
                registered.get(variant).biomes().stream().map(holder -> holder.unwrapKey().orElseThrow()).collect(Collectors.toSet()),
                variant.toString()));
        assertEquals(ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_snowy_angry.png"),
                registered.get(WolfVariants.SNOWY).angryTexture());
    }

    private static final class RecordingContext implements BootstrapContext<Biome> {
        private final Set<ResourceKey<Biome>> registered = new HashSet<>();
        private final StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);

        @Override
        public Holder.Reference<Biome> register(ResourceKey<Biome> key, Biome value, Lifecycle lifecycle) {
            assertTrue(registered.add(key), "registered twice: " + key);
            return Holder.Reference.createStandAlone(biomes.owner(), key);
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
            return new StandaloneHolderGetter<>(key -> true);
        }
    }
}
