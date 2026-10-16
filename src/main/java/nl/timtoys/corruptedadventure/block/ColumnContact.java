package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/**
 * Picks one block of a column for an entity that stands in several ({@code entityInside} runs once per block the entity
 * touches): the block in the column under the entity's centre that holds its feet, or the column's lowest block if the
 * feet are below it. So a push or drag that is not idempotent is applied exactly once per tick, and only while the
 * entity's centre is over the column.
 */
final class ColumnContact {
    private ColumnContact() {
    }

    static boolean isTheOne(BlockGetter level, BlockPos pos, Entity entity, Block block) {
        if (pos.getX() != Mth.floor(entity.getX()) || pos.getZ() != Mth.floor(entity.getZ())) {
            return false;
        }
        int feet = Mth.floor(entity.getBoundingBox().minY + 1.0E-7);
        return pos.getY() == feet || pos.getY() > feet && !level.getBlockState(pos.below()).is(block);
    }
}
