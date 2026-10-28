package nl.timtoys.corruptedadventure.entity.unfinished;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Unfinished's animations ({@code animations/entity/the_unfinished.animation.json}, written by
 * tools/textures/unfinished_animations.py). The body controller picks idle, the stride or death from the synced state;
 * the server triggers the attacks on the action controller. The flicker of the unloaded parts is the renderer's.
 */
public final class UnfinishedAnimations {
    public static final String BODY = "body";
    public static final String ACTION = "action";
    public static final String SLAM = "slam";
    public static final String RIFT_SINK = "rift_sink";
    public static final String RIFT_RISE = "rift_rise";
    public static final String GRAB = "grab";
    public static final String ASH_RAIN = "ash_rain";
    public static final String LURE = "lure";
    public static final String VOID_PULL = "void_pull";
    public static final String FLINCH = "flinch";

    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_unfinished.idle");
    static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.the_unfinished.walk");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.the_unfinished.death");
    static final RawAnimation SLAM_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.slam");
    static final RawAnimation RIFT_SINK_ANIM = RawAnimation.begin().thenPlayAndHold("animation.the_unfinished.rift_sink");
    static final RawAnimation RIFT_RISE_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.rift_rise");
    static final RawAnimation GRAB_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.grab");
    static final RawAnimation ASH_RAIN_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.ash_rain");
    static final RawAnimation LURE_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.lure");
    static final RawAnimation VOID_PULL_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.void_pull");
    static final RawAnimation FLINCH_ANIM = RawAnimation.begin().thenPlay("animation.the_unfinished.flinch");

    /** Ticks of the death animation: he sinks back into his void, then the body is removed. */
    static final int DEATH_TICKS = 100;
    /** Ticks of the slam animation (the fist lands at {@code slamWindupTicks}). */
    static final int SLAM_TICKS = 44;
    /** Ticks of the ash rain animation; the cloud starts at {@link #ASH_CLOUD_TICK}. */
    static final int ASH_RAIN_TICKS = 64;
    static final int ASH_CLOUD_TICK = 20;
    /** Ticks the grab animation adds after the claw lets go (the leg coming back). */
    static final int GRAB_RECOVER_TICKS = 12;

    private UnfinishedAnimations() {
    }
}
