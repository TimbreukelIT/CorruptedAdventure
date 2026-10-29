package nl.timtoys.corruptedadventure.entity.hollow;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Which extra attack The Hollow uses next, so the fight changes: the sweep when its target is at arm's length, the
 * ground slam when it is near (or two fighters are within the wave), the rift rain when it keeps its distance. Never
 * the same attack twice in a row when another one fits. Pure, so it can be unit tested.
 */
final class HollowAttackPicker {
    /**
     * @param sweep          up to how far (blocks, from its centre) the sweep reaches
     * @param groundSlam     up to how far it picks the ground slam
     * @param rainMin        from how far it picks the rift rain
     * @param rainMax        up to how far it picks the rift rain
     */
    record Ranges(double sweep, double groundSlam, double rainMin, double rainMax) {
    }

    private HollowAttackPicker() {
    }

    /**
     * @param distance      horizontal blocks to its target
     * @param fightersInWave fighters within the ground slam's radius
     * @param last          the attack before, or null
     * @param roll          any non-negative random number
     * @return the attack, or null when none fits (its target is too far)
     */
    @Nullable
    static HollowAttack pick(Ranges ranges, double distance, int fightersInWave, @Nullable HollowAttack last, int roll) {
        List<HollowAttack> fitting = new ArrayList<>(3);
        if (distance <= ranges.sweep()) {
            fitting.add(HollowAttack.SWEEP);
        }
        if (distance <= ranges.groundSlam() || fightersInWave >= 2) {
            fitting.add(HollowAttack.GROUND_SLAM);
        }
        if (distance >= ranges.rainMin() && distance <= ranges.rainMax()) {
            fitting.add(HollowAttack.RIFT_RAIN);
        }
        if (fitting.size() > 1) {
            fitting.remove(last);
        }
        return fitting.isEmpty() ? null : fitting.get(Math.floorMod(roll, fitting.size()));
    }
}
