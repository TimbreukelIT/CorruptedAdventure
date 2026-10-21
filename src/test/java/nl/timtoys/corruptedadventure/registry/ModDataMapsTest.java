package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.RegistryManager;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModDataMapsTest {
    @Test
    void healthyBlockIsABlockToBlockMapRegisteredUnderItsId() {
        assertEquals(CorruptedAdventure.id("healthy_block"), ModDataMaps.HEALTHY_BLOCK.id());
        assertEquals(Registries.BLOCK, ModDataMaps.HEALTHY_BLOCK.registryKey());
        // Registered through RegisterDataMapTypesEvent while the mod loaded (the tests run in FML).
        assertSame(ModDataMaps.HEALTHY_BLOCK, RegistryManager.getDataMap(Registries.BLOCK, CorruptedAdventure.id("healthy_block")));
        // Synced so the client knows the contents too, but not mandatory.
        assertNotNull(ModDataMaps.HEALTHY_BLOCK.networkCodec());
        assertFalse(ModDataMaps.HEALTHY_BLOCK.mandatorySync());
    }

    @Test
    void valueCodecRoundTripsABlockId() {
        JsonElement json = ModDataMaps.HEALTHY_BLOCK.codec().encodeStart(JsonOps.INSTANCE, Blocks.GRASS_BLOCK).getOrThrow();
        assertEquals("minecraft:grass_block", json.getAsString());
        Block back = ModDataMaps.HEALTHY_BLOCK.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertSame(Blocks.GRASS_BLOCK, back);
    }
}
