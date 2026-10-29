package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The target choice in a fight (fase-4-nightmares-concept.md "Leven-ronde", "Doelwit wisselen"): the damage each player
 * did to it, fading with a half-life, and a score per fighter: that damage, plus a bonus for prey (near and weak).
 * With nobody scoring it hunts the nearest; it switches only when another beats its target by a margin, and then not
 * again for a while. Server-side; {@link Nightmare} records and decays, {@link NightmareTargetGoal} chooses.
 */
public final class NightmareAggro {
    /** Below this the remembered damage is forgotten. */
    private static final float FORGET = 0.5F;

    private final Map<UUID, Float> damage = new HashMap<>();
    private int nextSwitchTick;

    void add(UUID player, float amount) {
        damage.merge(player, amount, Float::sum);
    }

    /** The remembered damage of a player (virtual health points, fading). */
    public float damageBy(UUID player) {
        return damage.getOrDefault(player, 0.0F);
    }

    /** One step of fading: every value times {@code factor}. */
    void decay(float factor) {
        for (Iterator<Map.Entry<UUID, Float>> it = damage.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Float> entry = it.next();
            float value = entry.getValue() * factor;
            if (value < FORGET) {
                it.remove();
            } else {
                entry.setValue(value);
            }
        }
    }

    /** The fading factor per {@code stepTicks} for a half-life of {@code halfLifeTicks}. */
    static float decayFactor(int stepTicks, int halfLifeTicks) {
        return (float) Math.pow(0.5, (double) stepTicks / Math.max(1, halfLifeTicks));
    }

    static double score(float damage, boolean prey, double preyBonus) {
        return damage + (prey ? preyBonus : 0.0);
    }

    /** Whether a challenger with {@code challenger} beats the target's {@code current} score. */
    static boolean beats(double challenger, double current, double margin) {
        return challenger > current * margin;
    }

    /**
     * The fighter it should hunt now: {@code current} if it is still one of {@code fighters} and nobody beats it (or a
     * switch is still cooling down), else the best by score, then by distance. Null without fighters.
     */
    @Nullable
    Player choose(Nightmare nightmare, List<Player> fighters, @Nullable Player current, int tick) {
        double preyRadius = nightmare.config().preyRadius.get();
        double preyShare = nightmare.config().preyHealthShare.get();
        double preyBonus = nightmare.config().preyBonus.get();
        Player best = null;
        double bestScore = 0.0;
        double bestDistance = 0.0;
        double currentScore = -1.0;
        for (Player player : fighters) {
            double distance = player.distanceToSqr(nightmare);
            boolean prey = distance <= preyRadius * preyRadius
                    && player.getHealth() <= player.getMaxHealth() * preyShare;
            double score = score(damageBy(player.getUUID()), prey, preyBonus);
            if (player == current) {
                currentScore = score;
            }
            if (best == null || score > bestScore || (score == bestScore && distance < bestDistance)) {
                best = player;
                bestScore = score;
                bestDistance = distance;
            }
        }
        if (currentScore < 0.0 || best == null) {
            // No current target among the fighters: the best at once.
            return best;
        }
        if (best == current || tick < nextSwitchTick
                || !beats(bestScore, currentScore, nightmare.config().targetSwitchMargin.get())) {
            return current;
        }
        nextSwitchTick = tick + nightmare.config().targetSwitchCooldownSeconds.get() * 20;
        return best;
    }

    void clear() {
        damage.clear();
    }
}
