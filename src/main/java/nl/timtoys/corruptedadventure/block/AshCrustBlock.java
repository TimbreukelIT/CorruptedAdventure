package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crust of ash that hangs until a player walks under it (38c). Unlike sand it never schedules its own fall: placing
 * it, a neighbour update, worldgen post-processing, chunk loading or a piston leave it hanging, so worldgen can put it
 * on thin roofs without the world depending on when a chunk was updated. Only {@link LooseSandTrigger} starts a
 * collapse ({@link AshCrustCollapse}); the scheduled tick then drops it through vanilla's falling-block logic (only
 * with room below at that moment) and the crust resting on it follows. A landed crust hangs again where it lands.
 */
public class AshCrustBlock extends FallingBlock implements CollapsingBlock {
    public static final MapCodec<AshCrustBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(block -> block.dustColor), propertiesCodec())
            .apply(instance, AshCrustBlock::new));
    /**
     * One in this many animation ticks drops a speck of dust under a hanging crust: now and then, ten times rarer than
     * sand (user, 38c review); the clear warning comes from {@link LooseSandTrigger} just before a collapse.
     */
    static final int DUST_CHANCE = 160;

    private final ColorRGBA dustColor;

    public AshCrustBlock(ColorRGBA dustColor, Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    @Override
    protected MapCodec<AshCrustBlock> codec() {
        return CODEC;
    }

    @Override
    public void startCollapse(ServerLevel level, BlockPos pos, int delay) {
        AshCrustCollapse.start(level, pos, this, delay);
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
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (!level.getBlockState(pos).is(this)) {
            AshCrustCollapse.fell(level, pos, this);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(DUST_CHANCE) == 0 && isFree(level.getBlockState(pos.below()))) {
            ParticleUtils.spawnParticleBelow(level, pos, random, new BlockParticleOption(ParticleTypes.FALLING_DUST, state));
        }
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return dustColor.rgba();
    }
}
