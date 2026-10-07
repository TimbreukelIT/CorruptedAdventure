package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RingFillGateTest {
    @Test
    void outsideAChunkFillItIsTheArgumentEverywhere() {
        // Terrain reads its flat-cached inputs at other columns too; those reads must never be gated (review 39e).
        RingFillGate gate = new RingFillGate(Level.OVERWORLD, Ring.RING_3, DensityFunctions.constant(-3.5));
        for (int x : new int[]{0, 2500, 100_000}) {
            assertEquals(-3.5, gate.compute(new DensityFunction.SinglePointContext(x, 64, -x)));
        }
        assertEquals(-3.5, gate.minValue());
        assertEquals(0.0, gate.maxValue());
    }

    @Test
    void codecRoundTrip() {
        RingFillGate gate = new RingFillGate(Level.NETHER, Ring.RING_2, DensityFunctions.constant(2.0));
        JsonElement json = RingFillGate.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, gate).getOrThrow();
        RingFillGate parsed = RingFillGate.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(Level.NETHER, parsed.dimension());
        assertEquals(Ring.RING_2, parsed.ring());
        assertEquals(json, RingFillGate.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, parsed).getOrThrow());
    }
}
