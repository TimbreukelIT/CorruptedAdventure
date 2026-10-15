package nl.timtoys.corruptedadventure.worldgen.surface;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ring4_threshold} (verdant_falls, Q7 = A): reads a constant stub or one island output, always on the surface
 * pass's own field; the ring 4 surface uses it on existing 2D keys only.
 */
class Ring4ThresholdConditionTest {
    private static final IslandField FIELD = IslandFieldTest.field();

    @Test
    void aConstantStubReadsItsValue() {
        DensityFunction reader = Ring4ThresholdCondition.reader(DensityFunctions.constant(0.0), FIELD);
        assertEquals(0.0, reader.compute(new DensityFunction.SinglePointContext(6000, 0, 0)));
    }

    /** The key's island output is rebuilt on the surface pass's field, behind a reference and a flat cache. */
    @Test
    void anIslandOutputIsReadOnTheSurfaceField() {
        // Another instance (as the registry's unwired one is): the reader must not use it.
        IslandField other = IslandFieldTest.field();
        DensityFunction key = new DensityFunctions.HolderHolder(Holder.direct(DensityFunctions.flatCache(
                new IslandOutput(other, IslandOutput.Output.CLIFF))));
        DensityFunction reader = Ring4ThresholdCondition.reader(key, FIELD);
        assertTrue(reader instanceof IslandOutput output && output.field() == FIELD && output.output() == IslandOutput.Output.CLIFF);
        for (int x = 5800; x <= 6300; x += 50) {
            assertEquals(IslandOutput.columnValue(FIELD.column(x, 0), IslandOutput.Output.CLIFF),
                    reader.compute(new DensityFunction.SinglePointContext(x, 0, 0)), "x " + x);
        }
    }

    @Test
    void threeDimensionalOutputsAndOtherFunctionsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Ring4ThresholdCondition.reader(new IslandOutput(FIELD, IslandOutput.Output.THIN), FIELD));
        assertThrows(IllegalArgumentException.class,
                () -> Ring4ThresholdCondition.reader(DensityFunctions.add(new IslandOutput(FIELD, IslandOutput.Output.CLIFF),
                        DensityFunctions.constant(1.0)), FIELD));
    }

    /** Every use in the overworld surface rules names an existing density function and a real range. */
    @Test
    void everyUseInTheSurfaceRulesNamesAnExistingKey() {
        List<JsonObject> uses = new ArrayList<>();
        collect(WorldgenData.read("noise_settings/overworld.json").get("surface_rule"), uses);
        assertFalse(uses.isEmpty());
        for (JsonObject use : uses) {
            String key = use.get("density_function").getAsString();
            assertTrue(key.startsWith("corruptedadventure:ring_4/overworld/"), key);
            assertTrue(Files.exists(WorldgenData.root().resolve("density_function/" + key.substring(key.indexOf(':') + 1) + ".json")), key);
            assertTrue(use.get("min").getAsDouble() < use.get("max").getAsDouble(), use.toString());
        }
        assertFalse(WorldgenData.read("noise_settings/nether.json").toString().contains("ring4_threshold"));
    }

    private static void collect(JsonElement element, List<JsonObject> out) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals("corruptedadventure:ring4_threshold")) {
                out.add(object);
            }
            object.entrySet().forEach(entry -> collect(entry.getValue(), out));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, out));
        }
    }
}
