package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaClimateBiomeSourceTest {
    private static final String OVERWORLD = """
            {"preset": "minecraft:overworld", "replace": {"minecraft:mushroom_fields": "corruptedadventure:plains"}}""";
    private static final ResourceKey<Biome> PLAINS_COPY = VanillaBiomeCopies.copyOf(Biomes.PLAINS);
    private static final Climate.ParameterList<ResourceKey<Biome>> VANILLA =
            MultiNoiseBiomeSourceParameterList.knownPresets().get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);

    @Test
    void everyVanillaPointGetsItsCopyOrTheReplacement() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> ours = decode(OVERWORLD, allCopies()).getOrThrow().parameters().values();
        assertEquals(VANILLA.values().size(), ours.size());
        for (int i = 0; i < ours.size(); i++) {
            ResourceKey<Biome> vanilla = VANILLA.values().get(i).getSecond();
            ResourceKey<Biome> expected = vanilla.equals(Biomes.MUSHROOM_FIELDS) ? PLAINS_COPY : VanillaBiomeCopies.copyOf(vanilla);
            assertEquals(VANILLA.values().get(i).getFirst(), ours.get(i).getFirst());
            assertEquals(expected, key(ours.get(i).getSecond()), "point " + i + " (" + vanilla.location() + ")");
        }
    }

    @Test
    void lookupMatchesVanillaAtRandomClimates() {
        VanillaClimateBiomeSource source = decode(OVERWORLD, allCopies()).getOrThrow();
        Random random = new Random(20260930L);
        for (int i = 0; i < 2000; i++) {
            Climate.TargetPoint target = Climate.target(coord(random), coord(random), coord(random), coord(random), coord(random), coord(random));
            ResourceKey<Biome> vanilla = VANILLA.findValue(target);
            ResourceKey<Biome> expected = vanilla.equals(Biomes.MUSHROOM_FIELDS) ? PLAINS_COPY : VanillaBiomeCopies.copyOf(vanilla);
            assertEquals(expected, key(source.parameters().findValue(target)), target.toString());
        }
    }

    @Test
    void possibleBiomesAreTheCopiesWithoutMushroomFields() {
        Set<ResourceKey<Biome>> possible = decode(OVERWORLD, allCopies()).getOrThrow().possibleBiomes().stream()
                .map(VanillaClimateBiomeSourceTest::key)
                .collect(Collectors.toSet());
        assertEquals(VanillaBiomeCopies.OVERWORLD_COPIES.stream().map(VanillaBiomeCopies::copyOf).collect(Collectors.toSet()), possible);
        assertEquals(52, possible.size());
    }

    @Test
    void missingCopyIsACodecErrorNamingTheKey() {
        ResourceKey<Biome> desert = VanillaBiomeCopies.copyOf(Biomes.DESERT);
        DataResult<VanillaClimateBiomeSource> result = decode(OVERWORLD, key -> isCopy(key) && !key.equals(desert));
        assertTrue(result.isError());
        assertTrue(result.error().orElseThrow().message().contains(desert.location().toString()), result.error().orElseThrow().message());
    }

    @Test
    void replacingABiomeThePresetDoesNotUseIsAnError() {
        DataResult<VanillaClimateBiomeSource> result = decode("""
                {"preset": "minecraft:overworld", "replace": {"minecraft:nether_wastes": "corruptedadventure:plains"}}""", allCopies());
        assertTrue(result.isError());
    }

    @Test
    void encodesOnlyPresetAndReplace() {
        RegistryOps<JsonElement> ops = ops(allCopies());
        VanillaClimateBiomeSource source = decode(OVERWORLD, allCopies()).getOrThrow();
        JsonElement encoded = VanillaClimateBiomeSource.CODEC.codec().encodeStart(ops, source).getOrThrow();
        assertEquals(JsonParser.parseString(OVERWORLD), encoded);
    }

    private static DataResult<VanillaClimateBiomeSource> decode(String json, Predicate<ResourceKey<Biome>> known) {
        return VanillaClimateBiomeSource.CODEC.codec().parse(ops(known), JsonParser.parseString(json));
    }

    private static RegistryOps<JsonElement> ops(Predicate<ResourceKey<Biome>> known) {
        return StandaloneHolderGetter.ops(Registries.BIOME, known);
    }

    private static Predicate<ResourceKey<Biome>> allCopies() {
        return VanillaClimateBiomeSourceTest::isCopy;
    }

    private static boolean isCopy(ResourceKey<Biome> key) {
        return VanillaBiomeCopies.OVERWORLD_COPIES.stream().map(VanillaBiomeCopies::copyOf).anyMatch(key::equals);
    }

    private static ResourceKey<Biome> key(Holder<Biome> holder) {
        return holder.unwrapKey().orElseThrow();
    }

    private static float coord(Random random) {
        return random.nextFloat() * 4.0F - 2.0F;
    }
}
