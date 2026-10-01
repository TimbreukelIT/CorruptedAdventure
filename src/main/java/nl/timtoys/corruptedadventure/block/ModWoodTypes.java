package nl.timtoys.corruptedadventure.block;

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

    private ModWoodTypes() {
    }
}
