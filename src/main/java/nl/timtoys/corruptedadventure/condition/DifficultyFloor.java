package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.Difficulty;

/** How far a {@link DamageThreshold} may hurt a player; each condition picks its own floor. */
public enum DifficultyFloor {
    /** Always hurts, on every difficulty (also peaceful). */
    NONE,
    /** Like vanilla starving: peaceful never, easy down to 10 HP, normal down to 1 HP, hard lethal. */
    VANILLA_STARVATION,
    /** Never below 1 HP (the damage is capped). */
    NEVER_LETHAL;

    /** The damage to deal at this health and difficulty; 0 = none. */
    public float damage(float amount, float health, Difficulty difficulty) {
        return switch (this) {
            case NONE -> amount;
            // Vanilla's starve check, plus peaceful (where vanilla never lets food run out).
            case VANILLA_STARVATION -> difficulty != Difficulty.PEACEFUL && (health > 10 || difficulty == Difficulty.HARD
                    || health > 1 && difficulty == Difficulty.NORMAL) ? amount : 0;
            case NEVER_LETHAL -> Math.max(0, Math.min(amount, health - 1));
        };
    }
}
