package nl.timtoys.corruptedadventure.entity.hollow;

/**
 * The Hollow's extra attacks (life round, fase-4-nightmares-concept.md "Leven-ronde"), beside its slam, rift step and
 * swap. Each has a windup with a clear warning (animation, sound, particles), a strike and a recovery; the name is its
 * triggered animation in {@link HollowAnimations}.
 */
enum HollowAttack {
    /** Both arms come down on the ground: a shock wave runs out in a growing ring. Jump it or keep away. */
    GROUND_SLAM(HollowAnimations.GROUND_SLAM),
    /** The giant hand claws into the ground: purple cracks open round its target, then void claws shoot up. */
    RIFT_RAIN(HollowAnimations.RIFT_RAIN),
    /** A wide backhand of its knuckles across its front that knocks everyone in the arc away. */
    SWEEP(HollowAnimations.SWEEP);

    final String animation;

    HollowAttack(String animation) {
        this.animation = animation;
    }
}
