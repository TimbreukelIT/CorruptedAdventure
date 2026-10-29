package nl.timtoys.corruptedadventure.entity.lostkeeper;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Lost Keeper's GeckoLib animations ({@code animations/entity/the_lost_keeper.animation.json}, written by
 * {@code tools/textures/lost_keeper_animations.py}). The loops follow its synced {@link TheLostKeeper.Mode}; the attacks are
 * triggered by the server on the {@link #ACTION} controller. Only the head's parts, wings and tail fin are animated: the
 * body segments are laid along the body chain by the client.
 */
public final class KeeperAnimations {
    public static final String BODY = "body";
    public static final String ACTION = "action";

    public static final String BREATH = "breath";
    public static final String GRAB = "grab";
    public static final String TAIL_WHIP = "tail_whip";
    public static final String ROAR = "roar";
    public static final String BUBBLE_CALL = "bubble_call";
    public static final String DIVE = "dive";
    public static final String BURST = "burst";
    public static final String SQUEEZE = "squeeze";

    public static final RawAnimation HOVER = loop("hover");
    public static final RawAnimation SWIM = loop("swim");
    public static final RawAnimation LURE = loop("lure");
    public static final RawAnimation HOLD = loop("hold");
    public static final RawAnimation FALL = loop("fall");
    public static final RawAnimation STUNNED = loop("stunned");
    public static final RawAnimation CRAWL = loop("crawl");
    public static final RawAnimation COIL = loop("coil");
    public static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.the_lost_keeper.death");

    public static final RawAnimation BREATH_ANIM = once(BREATH);
    public static final RawAnimation GRAB_ANIM = once(GRAB);
    public static final RawAnimation TAIL_WHIP_ANIM = once(TAIL_WHIP);
    public static final RawAnimation ROAR_ANIM = once(ROAR);
    public static final RawAnimation BUBBLE_CALL_ANIM = once(BUBBLE_CALL);
    public static final RawAnimation DIVE_ANIM = once(DIVE);
    public static final RawAnimation BURST_ANIM = once(BURST);
    public static final RawAnimation SQUEEZE_ANIM = once(SQUEEZE);

    private KeeperAnimations() {
    }

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop("animation.the_lost_keeper." + name);
    }

    private static RawAnimation once(String name) {
        return RawAnimation.begin().thenPlay("animation.the_lost_keeper." + name);
    }
}
