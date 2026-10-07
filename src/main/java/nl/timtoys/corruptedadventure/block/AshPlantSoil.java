package nl.timtoys.corruptedadventure.block;

import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

/** The ground rule of the ring 3 fire flora: {@code #ash_plant_soil}, where an ash layer only counts when it is full. */
public final class AshPlantSoil {
    private AshPlantSoil() {
    }

    /**
     * Whether a fire plant may stand on {@code ground}. A partial ash layer is not a full face: a plant on it would
     * float above the ash, so only a full (8-layer) ash layer counts.
     */
    public static boolean supports(BlockState ground) {
        if (!ground.is(ModTags.Blocks.ASH_PLANT_SOIL)) {
            return false;
        }
        return !ground.is(ModBlocks.ASH_LAYER) || ground.getValue(SnowLayerBlock.LAYERS) == SnowLayerBlock.MAX_HEIGHT;
    }
}
