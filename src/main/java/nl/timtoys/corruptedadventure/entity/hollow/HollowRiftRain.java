package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scheurregen: where the void claws of the rift rain come up round its target. One right under its feet, the rest in a
 * ring round it, each a few ticks after the one before, so it rains claws. Each sits on the ground there; a spot without
 * ground (a drop, a wall) gets none.
 */
public final class HollowRiftRain {
    /** Extra ticks of crack per claw after the first, so they come up one after the other. */
    private static final int STAGGER_TICKS = 2;
    /** Blocks above and below its target's feet where a claw looks for ground. */
    private static final int GROUND_UP = 2;
    private static final int GROUND_DOWN = 4;

    private HollowRiftRain() {
    }

    /** Opens the cracks round {@code target}; the claws are returned so a broken-off attack can cancel them. */
    public static List<HollowVoidClaw> open(ServerLevel level, TheHollow hollow, LivingEntity target) {
        HollowConfig config = ServerConfig.HOLLOW;
        int count = config.riftRainClaws.get();
        double radius = config.riftRainRadius.get();
        int warning = config.riftRainWarningTicks.get();
        float damage = config.riftRainDamage.get().floatValue();
        RandomSource random = hollow.getRandom();
        List<HollowVoidClaw> claws = new ArrayList<>(count);
        float turn = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            double x = target.getX();
            double z = target.getZ();
            if (i > 0) {
                double angle = turn + (i - 1) * Mth.TWO_PI / (count - 1) + (random.nextFloat() - 0.5F) * 0.5F;
                double r = radius * (0.75 + random.nextFloat() * 0.5);
                x += Math.cos(angle) * r;
                z += Math.sin(angle) * r;
            }
            Optional<Vec3> ground = ground(level, x, target.getBlockY(), z);
            if (ground.isPresent()) {
                // The hand hooks its fingers toward its target (the model's fingers curl against its yaw); the one under
                // its feet any way.
                float yaw = i == 0 ? random.nextFloat() * 360.0F
                        : HollowRiftStepGoal.yawToward(ground.get(), target.position()) + 180.0F;
                claws.add(HollowVoidClaw.spawn(level, hollow, ground.get(), yaw, warning + i * STAGGER_TICKS, damage));
            }
        }
        HollowEffects.riftRainCracks(level, target);
        return claws;
    }

    /** The feet height of the first standing spot at {@code x, z} from a little above {@code y} downward. */
    static Optional<Vec3> ground(ServerLevel level, double x, int y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = GROUND_UP; dy >= -GROUND_DOWN; dy--) {
            pos.set(Mth.floor(x), y + dy, Mth.floor(z));
            BlockPos below = pos.below();
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return Optional.of(new Vec3(x, pos.getY(), z));
            }
        }
        return Optional.empty();
    }
}
