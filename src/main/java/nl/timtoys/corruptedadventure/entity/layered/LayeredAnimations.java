package nl.timtoys.corruptedadventure.entity.layered;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Layered's animations ({@code animations/entity/the_layered.animation.json}, written by
 * {@code tools/textures/layered_animations.py}). The body controller picks idle, walk (also the climb: the renderer turns
 * the model onto the wall), lying on its back or death from the synced state; the grip controller loops while it holds a
 * player; the heads controller never stops (the dead heads); the server triggers the attacks on the action controller.
 */
public final class LayeredAnimations {
    public static final String BODY = "body";
    public static final String GRIP = "grip";
    public static final String HEADS = "heads";
    public static final String ACTION = "action";
    public static final String GRAB = "grab";
    public static final String ROCKFALL = "rockfall";
    public static final String RELEASE = "release";

    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_layered.idle");
    static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.the_layered.walk");
    static final RawAnimation FALLEN = RawAnimation.begin().thenLoop("animation.the_layered.fallen");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.the_layered.death");
    static final RawAnimation HOLD = RawAnimation.begin().thenLoop("animation.the_layered.hold");
    static final RawAnimation DEAD_HEADS = RawAnimation.begin().thenLoop("animation.the_layered.heads");
    static final RawAnimation GRAB_ANIM = RawAnimation.begin().thenPlay("animation.the_layered.grab");
    static final RawAnimation ROCKFALL_ANIM = RawAnimation.begin().thenPlay("animation.the_layered.rockfall");
    static final RawAnimation RELEASE_ANIM = RawAnimation.begin().thenPlay("animation.the_layered.release");

    /** Ticks of the death animation: it sinks and curls its legs, then the body is removed. */
    static final int DEATH_TICKS = 60;
    /** Ticks the body controller takes to blend between states: the roll onto its back and back up. */
    static final int BODY_BLEND_TICKS = 8;

    private LayeredAnimations() {
    }
}
