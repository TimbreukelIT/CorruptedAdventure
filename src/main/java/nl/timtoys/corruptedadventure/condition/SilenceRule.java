package nl.timtoys.corruptedadventure.condition;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.function.Predicate;

/**
 * The rule of {@link Condition#SILENCE} (package 97, {@code docs/plans/ring-5/contract.md} §5, §11). In ring 5 of the
 * Nether the meter rises everywhere; faster in open space (the hollow cathedral, standing on a white plateau), and
 * slowed when another player is close by (the two factors multiply). A sound heard a moment ago ({@link SilenceSounds})
 * makes it fall instead, whatever else holds. Outside the zone it falls (fast in Nether ring 4 through
 * {@link PurifyingModifier}).
 */
public record SilenceRule(Rates rates, int companionRadius, int reliefTicks) implements ConditionRule {
    /** The top block of the white plateaus of {@code white_halls} (package 87). */
    private static final Predicate<BlockState> PLATEAU = state -> state.is(ModBlocks.POLISHED_WHITE_STONE.get());

    /** The live rates from {@code [conditions.silence]}. */
    public static SilenceRule fromConfig() {
        ServerConfig.SilenceCondition config = ServerConfig.SILENCE;
        return new SilenceRule(new Rates(config.rise.get().floatValue(), config.openMultiplier.get().floatValue(),
                config.companionMultiplier.get().floatValue(), config.soundFall.get().floatValue(),
                config.outsideFall.get().floatValue()), config.companionRadius.get(),
                config.soundReliefSeconds.get() * SharedConstants.TICKS_PER_SECOND);
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context), rates).exposure().getSerializedName();
    }

    /**
     * The zone: ring 5 of the Nether, switching at blend 0.5 like the biomes. Both tests always run ({@code &}), as in
     * {@link ColdRule#inZone}.
     */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.NETHER & position.ring() == Ring.RING_5;
    }

    /** Cheapest first: the zone (already sampled), the sound (one lookup), open space, the other players last. */
    Input input(ConditionContext context) {
        if (!inZone(context.level().dimension(), context.position())) {
            return Input.OUTSIDE;
        }
        ServerPlayer player = context.player();
        if (SilenceSounds.relieved(player, player.server.getTickCount(), reliefTicks)) {
            return Input.HEARD;
        }
        boolean open = context.biome().is(ModBiomes.HOLLOW_CATHEDRAL) || context.standsOn(PLATEAU);
        return new Input(true, false, open, hasCompanion(player, companionRadius));
    }

    /** Another living player (not a spectator; creative counts) in the same level within {@code radius} blocks. */
    static boolean hasCompanion(ServerPlayer player, int radius) {
        double radiusSquared = (double) radius * radius;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.isAlive() && !other.isSpectator()
                    && other.distanceToSqr(player) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    static Result evaluate(Input input, Rates rates) {
        if (!input.inZone()) {
            return fall(SilenceExposure.OUTSIDE, rates.outside());
        }
        if (input.heard()) {
            return fall(SilenceExposure.SOUND, rates.sound());
        }
        float rate = rates.rise() * (input.open() ? rates.openMultiplier() : 1.0F);
        if (input.together()) {
            return new Result(rate * rates.companionMultiplier(), SilenceExposure.TOGETHER);
        }
        return new Result(rate, input.open() ? SilenceExposure.OPEN : SilenceExposure.ZONE);
    }

    private static Result fall(SilenceExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        return new Result(0 - fallRate, exposure);
    }

    /**
     * What the rule looks at, already decided (testable without a server). After a sound the last two fields are not
     * looked up.
     *
     * @param inZone   Nether and {@code ring() == RING_5}
     * @param heard    a sound within {@code soundReliefRadius}, at most {@code soundReliefSeconds} ago
     * @param open     in {@code hollow_cathedral}, or standing on polished white stone
     * @param together another player within {@code companionRadius}
     */
    record Input(boolean inZone, boolean heard, boolean open, boolean together) {
        static final Input OUTSIDE = new Input(false, false, false, false);
        static final Input HEARD = new Input(true, true, false, false);
    }

    /** Points per second: {@code rise} (times the factors) up, {@code sound} and {@code outside} down. */
    public record Rates(float rise, float openMultiplier, float companionMultiplier, float sound, float outside) {
    }

    record Result(float rate, SilenceExposure exposure) {
    }
}
