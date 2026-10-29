package nl.timtoys.corruptedadventure.entity.hollow;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of The Hollow's extra attacks: which one it picks, and where the wave and the sweep hit. */
class HollowAttackLogicTest {
    private static final HollowAttackPicker.Ranges RANGES = new HollowAttackPicker.Ranges(5.75, 7.0, 5.0, 24.0);

    @Test
    void itPicksByDistance() {
        assertEquals(EnumSet.of(HollowAttack.SWEEP, HollowAttack.GROUND_SLAM), picks(3.0, 1, null));
        assertEquals(EnumSet.of(HollowAttack.GROUND_SLAM, HollowAttack.RIFT_RAIN), picks(6.5, 1, null));
        assertEquals(EnumSet.of(HollowAttack.RIFT_RAIN), picks(15.0, 1, null));
        assertNull(HollowAttackPicker.pick(RANGES, 30.0, 1, null, 0), "too far for anything");
    }

    @Test
    void twoFightersInTheWaveMakeTheGroundSlamFitFromAfar() {
        assertTrue(picks(15.0, 2, null).contains(HollowAttack.GROUND_SLAM));
    }

    @Test
    void neverTheSameTwiceInARowWhenAnotherFits() {
        for (int roll = 0; roll < 8; roll++) {
            assertNotEquals(HollowAttack.SWEEP, HollowAttackPicker.pick(RANGES, 3.0, 1, HollowAttack.SWEEP, roll));
        }
        // Only one fits: then it may repeat.
        assertEquals(HollowAttack.RIFT_RAIN, HollowAttackPicker.pick(RANGES, 15.0, 1, HollowAttack.RIFT_RAIN, 3));
    }

    @Test
    void theWaveHitsOnlyOnTheGroundWhereItPassesThisTick() {
        assertTrue(HollowStrikeShapes.waveHits(0.0, 0.45, 0.0, 0.0, true), "right next to it on the first tick");
        assertTrue(HollowStrikeShapes.waveHits(4.0, 4.45, 4.2, 0.5, true));
        assertFalse(HollowStrikeShapes.waveHits(4.0, 4.45, 4.2, 0.5, false), "a jump lets it pass");
        assertFalse(HollowStrikeShapes.waveHits(4.0, 4.45, 3.9, 0.0, true), "already behind the front");
        assertFalse(HollowStrikeShapes.waveHits(4.0, 4.45, 5.0, 0.0, true), "not there yet");
        assertFalse(HollowStrikeShapes.waveHits(4.0, 4.45, 4.2, 2.0, true), "on a ledge above it");
    }

    @Test
    void theSweepHitsInFrontNotBehind() {
        // Yaw 0 faces south (+z).
        assertTrue(HollowStrikeShapes.inArc(0.0F, 0.0, 3.0, 150.0));
        assertTrue(HollowStrikeShapes.inArc(0.0F, 3.0, 1.0, 150.0), "to the side, a little ahead");
        assertFalse(HollowStrikeShapes.inArc(0.0F, 0.0, -3.0, 150.0), "behind");
        assertFalse(HollowStrikeShapes.inArc(0.0F, 3.0, -1.0, 150.0), "to the side, a little behind");
        // Yaw 90 faces west (-x).
        assertTrue(HollowStrikeShapes.inArc(90.0F, -3.0, 0.0, 150.0));
        assertFalse(HollowStrikeShapes.inArc(90.0F, 3.0, 0.0, 150.0));
    }

    private static Set<HollowAttack> picks(double distance, int inWave, HollowAttack last) {
        Set<HollowAttack> seen = EnumSet.noneOf(HollowAttack.class);
        for (int roll = 0; roll < 12; roll++) {
            HollowAttack attack = HollowAttackPicker.pick(RANGES, distance, inWave, last, roll);
            if (attack != null) {
                seen.add(attack);
            }
        }
        return seen;
    }
}
