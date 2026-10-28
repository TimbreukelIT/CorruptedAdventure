package nl.timtoys.corruptedadventure.entity.hollow;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Hollow's animations ({@code animations/entity/the_hollow.animation.json}). The body controller picks idle, walk,
 * the out-of-phase loop or death from the synced state; the server triggers the attacks on the action controller.
 */
public final class HollowAnimations {
    public static final String BODY = "body";
    public static final String ACTION = "action";
    public static final String SLAM = "slam";
    public static final String RIFT_STEP = "rift_step";
    public static final String SWAP = "swap";

    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_hollow.idle");
    static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.the_hollow.walk");
    static final RawAnimation PHASE = RawAnimation.begin().thenLoop("animation.the_hollow.phase");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.the_hollow.death");
    static final RawAnimation SLAM_ANIM = RawAnimation.begin().thenPlay("animation.the_hollow.slam");
    static final RawAnimation RIFT_STEP_ANIM = RawAnimation.begin().thenPlay("animation.the_hollow.rift_step");
    static final RawAnimation SWAP_ANIM = RawAnimation.begin().thenPlay("animation.the_hollow.swap");

    /** Ticks of the death animation: it falls backwards, then the body is removed. */
    static final int DEATH_TICKS = 60;

    private HollowAnimations() {
    }
}
