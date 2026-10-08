package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * The rule of {@link Condition#COLD} (package 55). In ring 3 of the Nether the meter rises everywhere, faster on ice
 * and in the exposed biomes (the factors do not stack: the strongest wins). Within {@code warmthRadius} of a lit warmth
 * source it falls, wherever the player stands; outside the zone it falls at the outside rate. Leather and other gear
 * are {@link RateModifier}s on the positive rate ({@link LeatherColdModifier}); fire resistance does nothing against
 * the cold (as in vanilla).
 */
public record ColdRule(Rates rates, int warmthRadius) implements ConditionRule {
    /** The live rates from {@code [conditions.cold]}. */
    public static ColdRule fromConfig() {
        ServerConfig.ColdCondition config = ServerConfig.COLD;
        return new ColdRule(new Rates(config.riseInZone.get().floatValue(), config.iceMultiplier.get().floatValue(),
                config.exposedMultiplier.get().floatValue(), config.warmthFall.get().floatValue(),
                config.outsideFall.get().floatValue()), config.warmthRadius.get());
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context, warmthRadius), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context, warmthRadius), rates).exposure().getSerializedName();
    }

    /** What the cold modifiers (gear) change: a rising cold rate, never a falling one. */
    static boolean isRisingCold(Condition condition, float rate) {
        return condition == Condition.COLD && rate > 0.0F;
    }

    /**
     * The zone: ring 3 of the Nether, switching at blend 0.5 like the biomes. Both tests always run ({@code &}, not
     * {@code &&}): no branch whose profile flips when the first players reach ring 3 (backlog r. 203).
     */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.NETHER & position.ring() == Ring.RING_3;
    }

    static Input input(ConditionContext context, int warmthRadius) {
        if (!inZone(context.level().dimension(), context.position())) {
            return Input.OUTSIDE; // skip the block scan and the block underfoot
        }
        if (context.isNearWarmth(warmthRadius)) {
            return new Input(true, true, false, false);
        }
        return new Input(true, false, context.standsOn(BlockTags.ICE),
                context.biome().is(ModTags.Biomes.COLD_EXPOSED));
    }

    static Result evaluate(Input input, Rates rates) {
        if (!input.inZone()) {
            return fall(ColdExposure.OUTSIDE, rates.outside());
        }
        if (input.nearWarmth()) {
            return fall(ColdExposure.WARMTH, rates.warmth());
        }
        float factor = 1.0F;
        ColdExposure exposure = ColdExposure.ZONE;
        if (input.onIce() && rates.iceMultiplier() > factor) {
            factor = rates.iceMultiplier();
            exposure = ColdExposure.ICE;
        }
        if (input.inExposed() && rates.exposedMultiplier() > factor) {
            factor = rates.exposedMultiplier();
            exposure = ColdExposure.EXPOSED;
        }
        return new Result(rates.rise() * factor, exposure);
    }

    private static Result fall(ColdExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        return new Result(0 - fallRate, exposure);
    }

    /**
     * What the rule looks at, already decided (testable without a server). Near warmth the last two fields are not
     * looked up.
     *
     * @param inZone     Nether and {@code ring() == RING_3}
     * @param nearWarmth a lit block in {@code #warmth_sources} within {@code warmthRadius}
     * @param onIce      the block underfoot is in {@code #minecraft:ice}
     * @param inExposed  the biome at the feet is in {@code #cold_exposed}
     */
    record Input(boolean inZone, boolean nearWarmth, boolean onIce, boolean inExposed) {
        static final Input OUTSIDE = new Input(false, false, false, false);
    }

    /** Points per second: {@code rise} (times a factor) up, the others down. */
    public record Rates(float rise, float iceMultiplier, float exposedMultiplier, float warmth, float outside) {
    }

    record Result(float rate, ColdExposure exposure) {
    }
}
