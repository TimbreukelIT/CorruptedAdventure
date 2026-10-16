package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sucking mud (G2 = A): the motion that {@code LivingEntity.travel} makes of the velocity the block sets, the breath
 * and the damage rhythm with the head under.
 */
class SuckingMudBlockTest {
    private static final double GRAVITY = 0.08;
    private static final double TRAVEL_DRAG = 0.98;

    /** What vanilla's travel moves the next tick: (velocity − gravity) × 0.98. */
    private static double nextMove(boolean jumping, double gravity) {
        return (SuckingMudBlock.nextVerticalSpeed(jumping, gravity) - gravity) * TRAVEL_DRAG;
    }

    @Test
    void sinksAboutOneBlockPerTwoSecondsAndRisesWhileJumping() {
        assertEquals(-0.025, nextMove(false, GRAVITY), 1.0E-9);
        assertEquals(40 * 0.025, -40 * nextMove(false, GRAVITY), 1.0E-9);
        assertTrue(nextMove(true, GRAVITY) > 0.0);
        // The same for a mob with other gravity, and without gravity.
        assertEquals(-0.025, nextMove(false, 0.04), 1.0E-9);
        assertEquals(-0.025, nextMove(false, 0.0), 1.0E-9);
    }

    @Test
    void horizontalSpeedSettlesNearFourTenthsOfWalking() {
        double speed = 0.0;
        for (int i = 0; i < 200; i++) {
            // Air control 0.02 per tick, then the mud's drag, then vanilla's air friction 0.91.
            speed = (speed + 0.02) * SuckingMudBlock.HORIZONTAL_DRAG * 0.91;
        }
        double walking = 0.216;
        assertTrue(speed > 0.3 * walking && speed < 0.5 * walking, "settles at " + speed);
    }

    @Test
    void solidForItemsWithoutAFloorForNothingElse() {
        // The empty context (no entity) is what items, falling blocks and the pathfinder see: a full block.
        assertFalse(ModBlocks.SUCKING_MUD.get().defaultBlockState()
                .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).isEmpty());
    }

    @Test
    void theBreathLastsAFewSecondsThenItHurtsOncePerSecond() {
        int air = 300;
        int ticks = 0;
        while (air > 0) {
            // Vanilla gives 4 back every tick outside water, then the mud takes its share.
            air = SuckingMudSuffocation.nextAir(Math.min(300, air + 4));
            ticks++;
        }
        assertTrue(ticks >= 50 && ticks <= 70, "out of breath after " + ticks + " ticks");
        assertFalse(SuckingMudSuffocation.hurtsNow(10, 40));
        assertTrue(SuckingMudSuffocation.hurtsNow(0, 40));
        assertFalse(SuckingMudSuffocation.hurtsNow(0, 41));
        assertEquals(0, SuckingMudSuffocation.nextAir(3));
    }

    @Test
    void theUpdraftLiftsAgainstGravityUpToItsTopSpeed() {
        double move = 0.0;
        for (int i = 0; i < 40; i++) {
            move = (UpdraftBlock.nextVerticalSpeed(move, GRAVITY) - GRAVITY) * TRAVEL_DRAG;
        }
        assertEquals(UpdraftBlock.MAX_RISE * TRAVEL_DRAG, move, 1.0E-9);
        assertTrue((UpdraftBlock.nextVerticalSpeed(0.0, GRAVITY) - GRAVITY) * TRAVEL_DRAG > 0.0);
    }
}
