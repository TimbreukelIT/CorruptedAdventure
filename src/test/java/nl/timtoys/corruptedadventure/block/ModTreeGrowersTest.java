package nl.timtoys.corruptedadventure.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.block.grower.TreeGrower;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModTreeGrowersTest {
    @Test
    void palmGrowerHasANamespacedNameInTheCodec() {
        JsonElement json = TreeGrower.CODEC.encodeStart(JsonOps.INSTANCE, ModTreeGrowers.PALM).getOrThrow();
        assertEquals("corruptedadventure:palm", json.getAsString());
        assertSame(ModTreeGrowers.PALM, TreeGrower.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void palmSaplingCodecKeepsItsGrower() {
        PalmSaplingBlock sapling = ModBlocks.PALM_SAPLING.get();
        assertSame(PalmSaplingBlock.CODEC, sapling.codec());
        JsonElement json = PalmSaplingBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, sapling).getOrThrow();
        assertEquals("corruptedadventure:palm", json.getAsJsonObject().get("tree").getAsString());
    }
}
