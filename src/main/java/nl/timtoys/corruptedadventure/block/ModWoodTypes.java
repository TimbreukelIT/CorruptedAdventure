package nl.timtoys.corruptedadventure.block;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/** Block set and wood types of our wood sets (sounds, door/button behaviour). */
public final class ModWoodTypes {
    /** {@code corruptedadventure:dead}: oak sounds and behaviour. */
    public static final BlockSetType DEAD_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("dead").toString()));

    // Deliberately not WoodType.register(...): that makes the client build sign materials, and we have no sign textures.
    // The fence gate only uses the wood type for its sounds.
    public static final WoodType DEAD = new WoodType(CorruptedAdventure.id("dead").toString(), DEAD_SET);

    /** {@code corruptedadventure:palm}: oak sounds and behaviour. */
    public static final BlockSetType PALM_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("palm").toString()));
    /** Not registered, for the same reason as {@link #DEAD}. */
    public static final WoodType PALM = new WoodType(CorruptedAdventure.id("palm").toString(), PALM_SET);

    /** {@code corruptedadventure:dripshroom}: Nether wood, with the sounds and behaviour of vanilla's crimson set. */
    public static final BlockSetType DRIPSHROOM_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("dripshroom").toString(),
            true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.NETHER_WOOD,
            SoundEvents.NETHER_WOOD_DOOR_CLOSE, SoundEvents.NETHER_WOOD_DOOR_OPEN,
            SoundEvents.NETHER_WOOD_TRAPDOOR_CLOSE, SoundEvents.NETHER_WOOD_TRAPDOOR_OPEN,
            SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.NETHER_WOOD_BUTTON_CLICK_OFF, SoundEvents.NETHER_WOOD_BUTTON_CLICK_ON));
    /** Not registered, for the same reason as {@link #DEAD}; the fence gate takes its sounds from here. */
    public static final WoodType DRIPSHROOM = new WoodType(CorruptedAdventure.id("dripshroom").toString(), DRIPSHROOM_SET,
            SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN,
            SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN);

    private ModWoodTypes() {
    }
}
