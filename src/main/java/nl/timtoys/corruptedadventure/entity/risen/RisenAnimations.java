package nl.timtoys.corruptedadventure.entity.risen;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Risen's animations ({@code animations/entity/the_risen.animation.json}, written by
 * {@code tools/textures/risen_animations.py}). The body controller picks the loop from the synced {@link RisenState};
 * the server triggers the attacks on the action controller.
 */
public final class RisenAnimations {
    public static final String BODY = "body";
    public static final String ACTION = "action";
    public static final String SCREAM = "scream";
    public static final String ASH_RAIN = "ash_rain";
    public static final String RELEASE = "release";
    public static final String PECK = "peck";
    public static final String BUFFET = "buffet";
    public static final String SWOOP_WINDUP = "swoop_windup";
    public static final String EMBER_RAIN = "ember_rain";

    static final RawAnimation PERCHED = RawAnimation.begin().thenLoop("animation.the_risen.perched");
    static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.the_risen.fly");
    static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.the_risen.hover");
    static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.the_risen.dive");
    static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.the_risen.stunned");
    static final RawAnimation RISE = RawAnimation.begin().thenPlayAndHold("animation.the_risen.rise");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.the_risen.death");
    static final RawAnimation SCREAM_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.scream");
    static final RawAnimation ASH_RAIN_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.ash_rain");
    static final RawAnimation RELEASE_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.release");
    static final RawAnimation PECK_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.peck");
    static final RawAnimation BUFFET_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.buffet");
    static final RawAnimation SWOOP_WINDUP_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.swoop_windup");
    static final RawAnimation EMBER_RAIN_ANIM = RawAnimation.begin().thenPlay("animation.the_risen.ember_rain");

    /** Ticks of the death animation: it falls and crumbles, then the body is removed. */
    static final int DEATH_TICKS = 60;
    /** Ticks the ground glows before the lava opens and it starts to rise (the rise animation's first 2 s). */
    static final int RISE_GLOW_TICKS = 40;
    /** Ticks it takes to rise out of the pool (the rise animation's last 3 s). */
    static final int RISE_TICKS = 60;
    /** The peck lands 9 ticks into its animation, the buffet 12. */
    static final int PECK_WINDUP_TICKS = 9;
    static final int BUFFET_WINDUP_TICKS = 12;

    private RisenAnimations() {
    }
}
