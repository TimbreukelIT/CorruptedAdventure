package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Glow lichen with its own item: vanilla's {@code GlowLichenBlock} only lets its item add faces to an existing block
 * ({@code canBeReplaced}) when that item is {@code Items.GLOW_LICHEN}, so a plain copy would replace the block instead.
 */
public class GlowMoldBlock extends GlowLichenBlock {
    public static final MapCodec<GlowLichenBlock> CODEC = simpleCodec(GlowMoldBlock::new);

    public GlowMoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GlowLichenBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(asItem()) || Arrays.stream(DIRECTIONS).anyMatch(direction -> !hasFace(state, direction));
    }
}
