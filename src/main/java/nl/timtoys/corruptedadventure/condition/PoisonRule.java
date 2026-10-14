package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.function.Predicate;

/**
 * The rule of {@link Condition#POISON} (package 69). In ring 4 of the overworld the meter rises only at a source: in or
 * next to a toxic pool (a block in {@code #poison_sources} within {@code poolRadius} of feet or head) or with the head
 * in swamp gas ({@code #poison_gases} at eye height). The sources do not stack: the stronger wins. Everywhere else it
 * falls; water and the blighted glowcaps are no source. Toxic rain is weather, so it is a {@link RateModifier}
 * ({@link ToxicRainPoisonModifier}), not part of this rule.
 */
public record PoisonRule(Rates rates, int poolRadius) implements ConditionRule {
    private static final Predicate<BlockState> POOL = state -> state.is(ModTags.Blocks.POISON_SOURCES);

    /** The live rates from {@code [conditions.poison]}. */
    public static PoisonRule fromConfig() {
        ServerConfig.PoisonCondition config = ServerConfig.POISON;
        return new PoisonRule(new Rates(config.poolRise.get().floatValue(), config.gasRise.get().floatValue(),
                config.fall.get().floatValue(), config.outsideFall.get().floatValue()), config.poolRadius.get());
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context, poolRadius), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context, poolRadius), rates).exposure().getSerializedName();
    }

    /**
     * The zone: ring 4 of the overworld, switching at blend 0.5 like the biomes and the ring weather. Both tests always
     * run ({@code &}, not {@code &&}), as in {@link HeatRule#inZone}.
     */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD & position.ring() == Ring.RING_4;
    }

    static Input input(ConditionContext context, int poolRadius) {
        if (!inZone(context.level().dimension(), context.position())) {
            return Input.OUTSIDE; // skip the block scan and the eye block
        }
        BlockPos eye = BlockPos.containing(context.player().getEyePosition());
        return new Input(true, NearbyBlocks.near(context.level(), context.player().blockPosition(), poolRadius, POOL),
                context.level().getBlockState(eye).is(ModTags.Blocks.POISON_GASES));
    }

    static Result evaluate(Input input, Rates rates) {
        if (!input.inZone()) {
            return fall(PoisonExposure.OUTSIDE, rates.outside());
        }
        Result best = null;
        if (input.nearPool()) {
            best = new Result(rates.pool(), PoisonExposure.POOL);
        }
        if (input.headInGas() && (best == null || rates.gas() > best.rate())) {
            best = new Result(rates.gas(), PoisonExposure.GAS);
        }
        return best != null ? best : fall(PoisonExposure.CLEAN, rates.fall());
    }

    private static Result fall(PoisonExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        return new Result(0 - fallRate, exposure);
    }

    /**
     * What the rule looks at, already decided (testable without a server).
     *
     * @param inZone    overworld and {@code ring() == RING_4}
     * @param nearPool  a block in {@code #poison_sources} within {@code poolRadius} of feet or head
     * @param headInGas the block at eye height is in {@code #poison_gases}
     */
    record Input(boolean inZone, boolean nearPool, boolean headInGas) {
        static final Input OUTSIDE = new Input(false, false, false);
    }

    /** Points per second: {@code pool} and {@code gas} up, {@code fall} and {@code outside} down. */
    public record Rates(float pool, float gas, float fall, float outside) {
    }

    record Result(float rate, PoisonExposure exposure) {
    }
}
