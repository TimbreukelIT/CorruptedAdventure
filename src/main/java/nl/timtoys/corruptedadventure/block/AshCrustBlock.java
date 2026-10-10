package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crust of ash that hangs until a player walks under it (38c). Unlike sand it never schedules its own fall: placing
 * it, a neighbour update, worldgen post-processing, chunk loading or a piston leave it hanging, so worldgen can put it
 * on thin roofs without the world depending on when a chunk was updated. Only {@link LooseSandTrigger} starts a
 * collapse ({@link AshCrustCollapse}); the scheduled tick then drops it through vanilla's falling-block logic. A landed
 * crust hangs again where it lands.
 */
public class AshCrustBlock extends FallingBlock {
    public static final MapCodec<AshCrustBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(block -> block.dustColor), propertiesCodec())
            .apply(instance, AshCrustBlock::new));

    private final ColorRGBA dustColor;

    public AshCrustBlock(ColorRGBA dustColor, Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    @Override
    protected MapCodec<AshCrustBlock> codec() {
        return CODEC;
    }

    /** No fall on placement: a crust placed by worldgen, a player or a landing falling block stays put. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    }

    /** No fall on a neighbour update (the vanilla falling block schedules one here). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return dustColor.rgba();
    }
}
