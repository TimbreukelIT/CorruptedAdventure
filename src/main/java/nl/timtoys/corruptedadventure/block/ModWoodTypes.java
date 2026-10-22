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
    public static final BlockSetType DRIPSHROOM_SET = BlockSetType.register(netherWoodSet("dripshroom"));
    /** Not registered, for the same reason as {@link #DEAD}; the fence gate takes its sounds from here. */
    public static final WoodType DRIPSHROOM = netherWood("dripshroom", DRIPSHROOM_SET);

    /**
     * {@code corruptedadventure:charred}: the burnt-out wood of ring 3. It does not burn (decision 2026-10-06), so it is
     * Nether wood like {@link #DRIPSHROOM_SET}: crimson's sounds and behaviour.
     */
    public static final BlockSetType CHARRED_SET = BlockSetType.register(netherWoodSet("charred"));
    /** Not registered, for the same reason as {@link #DEAD}. */
    public static final WoodType CHARRED = netherWood("charred", CHARRED_SET);

    /** {@code corruptedadventure:rimeshroom}: the frozen giant fungus of Nether ring 3, Nether wood like {@link #DRIPSHROOM_SET}. */
    public static final BlockSetType RIMESHROOM_SET = BlockSetType.register(netherWoodSet("rimeshroom"));
    /** Not registered, for the same reason as {@link #DEAD}. */
    public static final WoodType RIMESHROOM = netherWood("rimeshroom", RIMESHROOM_SET);

    /**
     * {@code corruptedadventure:moonpetal}: the blossom tree of ring 4, burnable like vanilla wood, with the sounds and
     * behaviour of vanilla's cherry set (the other blossom tree).
     */
    public static final BlockSetType MOONPETAL_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("moonpetal").toString(),
            true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.CHERRY_WOOD,
            SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN,
            SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN,
            SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON));
    /** Not registered, for the same reason as {@link #DEAD}; the fence gate takes cherry's sounds from here. */
    public static final WoodType MOONPETAL = new WoodType(CorruptedAdventure.id("moonpetal").toString(), MOONPETAL_SET,
            SoundType.CHERRY_WOOD, SoundType.CHERRY_WOOD_HANGING_SIGN,
            SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN);

    /** {@code corruptedadventure:verdant}: the tropical wood of {@code verdant_falls} (72b), oak sounds and behaviour. */
    public static final BlockSetType VERDANT_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("verdant").toString()));
    /** Not registered, for the same reason as {@link #DEAD}. */
    public static final WoodType VERDANT = new WoodType(CorruptedAdventure.id("verdant").toString(), VERDANT_SET);

    /** {@code corruptedadventure:corrupted}: the leached wood of ring 5 (88), oak sounds and behaviour. */
    public static final BlockSetType CORRUPTED_SET = BlockSetType.register(new BlockSetType(CorruptedAdventure.id("corrupted").toString()));
    /**
     * Registered, unlike the other wood types: the corrupted set has signs, and {@code WoodType.values()} is where the
     * client's {@code Sheets} builds the sign materials ({@code entity/signs/corrupted}, {@code entity/signs/hanging/corrupted}).
     */
    public static final WoodType CORRUPTED = WoodType.register(new WoodType(CorruptedAdventure.id("corrupted").toString(), CORRUPTED_SET));

    private ModWoodTypes() {
    }

    /** A block set type with the sounds and behaviour of vanilla's crimson (Nether wood) set. */
    private static BlockSetType netherWoodSet(String name) {
        return new BlockSetType(CorruptedAdventure.id(name).toString(),
                true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.NETHER_WOOD,
                SoundEvents.NETHER_WOOD_DOOR_CLOSE, SoundEvents.NETHER_WOOD_DOOR_OPEN,
                SoundEvents.NETHER_WOOD_TRAPDOOR_CLOSE, SoundEvents.NETHER_WOOD_TRAPDOOR_OPEN,
                SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON,
                SoundEvents.NETHER_WOOD_BUTTON_CLICK_OFF, SoundEvents.NETHER_WOOD_BUTTON_CLICK_ON);
    }

    /** A wood type on {@code set} with crimson's sounds (the fence gate takes its sounds from here). */
    private static WoodType netherWood(String name, BlockSetType set) {
        return new WoodType(CorruptedAdventure.id(name).toString(), set,
                SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN,
                SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN);
    }
}
