package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.jetbrains.annotations.Nullable;

/**
 * Where The Layered can move under the ground ({@link LayeredBurrowGoal}): only through sand and gravel (the block tag
 * {@code corruptedadventure:layered_burrow}), never through water, and only where its body has room to burst up again.
 * It moves along the surface at the height of its feet (hidden, without collision), one block up or down a step at
 * most; nothing in the world changes, so it leaves no hole.
 */
public final class LayeredBurrow {
    private static final int[] STEPS = {0, 1, -1};

    private LayeredBurrow() {
    }

    /** Whether the feet at {@code feet} stand on sand or gravel, out of the water. */
    public static boolean isBurrowGround(Level level, Vec3 feet) {
        BlockPos ground = BlockPos.containing(feet.x, feet.y - 0.5, feet.z);
        BlockPos at = BlockPos.containing(feet);
        return level.getBlockState(ground).is(ModTags.Blocks.LAYERED_BURROW) && level.getFluidState(at).isEmpty();
    }

    /**
     * Where its feet go from {@code from} toward {@code wanted} (same height or one block up or down): on sand or gravel,
     * with room for its hitbox there. Null where it cannot go on: it bursts up where it is.
     */
    @Nullable
    public static Vec3 step(Level level, Entity layered, Vec3 from, Vec3 wanted) {
        for (int dy : STEPS) {
            Vec3 feet = new Vec3(wanted.x, Math.floor(from.y + 0.5) + dy, wanted.z);
            if (isBurrowGround(level, feet) && hasRoom(level, layered, feet)) {
                return feet;
            }
        }
        return null;
    }

    /** Room for its hitbox with its feet at {@code feet}. */
    public static boolean hasRoom(Level level, Entity layered, Vec3 feet) {
        AABB box = layered.getDimensions(layered.getPose()).makeBoundingBox(feet).deflate(0.05);
        return level.noBlockCollision(layered, box);
    }
}
