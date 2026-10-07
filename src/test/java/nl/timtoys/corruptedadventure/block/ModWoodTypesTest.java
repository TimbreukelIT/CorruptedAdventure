package nl.timtoys.corruptedadventure.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModWoodTypesTest {
    @Test
    void dripshroomSetIsRegisteredUnderItsNamespacedName() {
        JsonElement json = BlockSetType.CODEC.encodeStart(JsonOps.INSTANCE, ModWoodTypes.DRIPSHROOM_SET).getOrThrow();
        assertEquals("corruptedadventure:dripshroom", json.getAsString());
        assertSame(ModWoodTypes.DRIPSHROOM_SET, BlockSetType.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void dripshroomSetBehavesAndSoundsLikeCrimson() {
        BlockSetType crimson = BlockSetType.CRIMSON;
        BlockSetType set = ModWoodTypes.DRIPSHROOM_SET;
        assertEquals(crimson.canOpenByHand(), set.canOpenByHand());
        assertEquals(crimson.canOpenByWindCharge(), set.canOpenByWindCharge());
        assertEquals(crimson.canButtonBeActivatedByArrows(), set.canButtonBeActivatedByArrows());
        assertEquals(crimson.pressurePlateSensitivity(), set.pressurePlateSensitivity());
        assertEquals(crimson.soundType(), set.soundType());
        assertEquals(crimson.doorClose(), set.doorClose());
        assertEquals(crimson.doorOpen(), set.doorOpen());
        assertEquals(crimson.trapdoorClose(), set.trapdoorClose());
        assertEquals(crimson.trapdoorOpen(), set.trapdoorOpen());
        assertEquals(crimson.pressurePlateClickOff(), set.pressurePlateClickOff());
        assertEquals(crimson.pressurePlateClickOn(), set.pressurePlateClickOn());
        assertEquals(crimson.buttonClickOff(), set.buttonClickOff());
        assertEquals(crimson.buttonClickOn(), set.buttonClickOn());
    }

    @Test
    void dripshroomWoodTypeHasCrimsonGateSoundsAndIsNotRegistered() {
        assertNetherWoodType(ModWoodTypes.DRIPSHROOM, ModWoodTypes.DRIPSHROOM_SET);
    }

    @Test
    void charredSetIsNetherWoodUnderItsNamespacedName() {
        JsonElement json = BlockSetType.CODEC.encodeStart(JsonOps.INSTANCE, ModWoodTypes.CHARRED_SET).getOrThrow();
        assertEquals("corruptedadventure:charred", json.getAsString());
        assertSame(ModWoodTypes.CHARRED_SET, BlockSetType.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(BlockSetType.CRIMSON.soundType(), ModWoodTypes.CHARRED_SET.soundType());
        assertEquals(BlockSetType.CRIMSON.doorOpen(), ModWoodTypes.CHARRED_SET.doorOpen());
        assertEquals(BlockSetType.CRIMSON.buttonClickOn(), ModWoodTypes.CHARRED_SET.buttonClickOn());
        assertNetherWoodType(ModWoodTypes.CHARRED, ModWoodTypes.CHARRED_SET);
    }

    @Test
    void rimeshroomSetIsNetherWoodUnderItsNamespacedName() {
        JsonElement json = BlockSetType.CODEC.encodeStart(JsonOps.INSTANCE, ModWoodTypes.RIMESHROOM_SET).getOrThrow();
        assertEquals("corruptedadventure:rimeshroom", json.getAsString());
        assertSame(ModWoodTypes.RIMESHROOM_SET, BlockSetType.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(BlockSetType.CRIMSON.soundType(), ModWoodTypes.RIMESHROOM_SET.soundType());
        assertNetherWoodType(ModWoodTypes.RIMESHROOM, ModWoodTypes.RIMESHROOM_SET);
    }

    private static void assertNetherWoodType(WoodType wood, BlockSetType set) {
        assertSame(set, wood.setType());
        assertEquals(WoodType.CRIMSON.fenceGateClose(), wood.fenceGateClose());
        assertEquals(WoodType.CRIMSON.fenceGateOpen(), wood.fenceGateOpen());
        assertEquals(WoodType.CRIMSON.soundType(), wood.soundType());
        // No sign textures exist, so it stays out of WoodType.values() (the client would build sign materials).
        assertFalse(WoodType.values().anyMatch(type -> type.name().equals(wood.name())));
    }
}
