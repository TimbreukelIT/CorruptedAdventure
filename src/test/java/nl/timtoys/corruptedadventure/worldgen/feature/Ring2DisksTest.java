package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring2DisksTest {
    /** A disk writes up to its radius around the origin; the origin is at least DISK_MARGIN inside the chunk. */
    @Test
    void vanillaDisksFitInsideTheirChunk() {
        HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>> features = VanillaRegistries.createLookup().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        assertEquals(3, Ring2Features.RING_2_DISKS.size());
        Ring2Features.RING_2_DISKS.values().forEach(disk -> {
            DiskConfiguration config = (DiskConfiguration) features.getOrThrow(disk.getKey()).value().config();
            assertTrue(config.radius().getMaxValue() <= Ring2Features.DISK_MARGIN, disk.getKey() + " radius " + config.radius().getMaxValue());
        });
        // jittered_grid keeps the origin in [margin, 15 - margin]: margin + radius stays inside 0..15.
        assertTrue(2 * Ring2Features.DISK_MARGIN < 16);
    }
}
