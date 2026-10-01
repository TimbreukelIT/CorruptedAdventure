package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.jetbrains.annotations.Nullable;

/**
 * Barrel cactus: a small spiny block that hurts like a cactus on contact, never grows and is avoided by mobs.
 * Grows on {@code #corruptedadventure:desert_plant_soil}. The shape is final for this block (textures draw inside it).
 */
public class BarrelCactusBlock extends BushBlock {
    public static final MapCodec<BarrelCactusBlock> CODEC = simpleCodec(BarrelCactusBlock::new);
    /** Outline and collision: 10 × 10 px, 10 px high. The block model uses the same box. */
    static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);
    /** Distance at which an entity counts as touching the spines (collision stops it exactly at the shape). */
    private static final double TOUCH_MARGIN = 0.05;

    public BarrelCactusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BarrelCactusBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.DESERT_PLANT_SOIL);
    }

    /**
     * Vanilla calls this for every block cell the entity's box overlaps, whatever the block's shape; the cactus is
     * smaller than its cell, so only entities that actually touch it get hurt.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (touches(entity.getBoundingBox(), pos)) {
            entity.hurt(level.damageSources().cactus(), 1.0F);
        }
    }

    static boolean touches(AABB entityBox, BlockPos pos) {
        return entityBox.intersects(SHAPE.bounds().move(pos).inflate(TOUCH_MARGIN));
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.DAMAGE_OTHER;
    }

    @Override
    public @Nullable PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob,
                                                       PathType originalType) {
        return PathType.DANGER_OTHER;
    }
}
