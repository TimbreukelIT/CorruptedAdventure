package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.function.Predicate;

/**
 * The rule of {@link Condition#VOID} (package 91, {@code docs/plans/ring-5/contract.md} §5, §11). In ring 5 of the
 * overworld and on the void rim ({@link VoidZone}) the meter creeps up everywhere: slowly in the inner band, faster in
 * the outer band and within {@code sourceRadius} of a block in {@code #void_sources}. These do not stack: the strongest
 * wins. Block light of at least {@code lightLevel} at eye height makes it fall instead ("light keeps the void away"),
 * also at a source and in the outer band; daylight does not count. Outside the zone it falls (fast in Nether ring 4
 * through {@link PurifyingModifier}). Night and ring weather are {@link VoidAmplifierModifier}, not this rule.
 */
public record VoidRule(Rates rates, int sourceRadius, int lightLevel) implements ConditionRule {
    private static final Predicate<BlockState> SOURCE = state -> state.is(ModTags.Blocks.VOID_SOURCES);

    /** The live rates from {@code [conditions.void]}. */
    public static VoidRule fromConfig() {
        ServerConfig.VoidCondition config = ServerConfig.VOID;
        return new VoidRule(new Rates(config.innerRise.get().floatValue(), config.outerRise.get().floatValue(),
                config.sourceRise.get().floatValue(), config.lightFall.get().floatValue(),
                config.outsideFall.get().floatValue()), config.sourceRadius.get(), config.lightLevel.get());
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context), rates).exposure().getSerializedName();
    }

    /** Cheapest first: the zone (already sampled), the light (one lookup), then the band (noise), the source scan last. */
    Input input(ConditionContext context) {
        if (!VoidZone.inZone(context.level().dimension(), context.position())) {
            return Input.OUTSIDE;
        }
        BlockPos eye = BlockPos.containing(context.player().getEyePosition());
        if (context.level().getBrightness(LightLayer.BLOCK, eye) >= lightLevel) {
            return Input.LIT; // light wins over everything: no band, no scan
        }
        VoidZone zone = VoidZone.of(context.level().dimension(), context.position(), context.player().getX(),
                context.player().getZ());
        return new Input(zone, NearbyBlocks.near(context.level(), context.player().blockPosition(), sourceRadius, SOURCE),
                false);
    }

    static Result evaluate(Input input, Rates rates) {
        if (input.zone() == VoidZone.OUTSIDE) {
            return fall(VoidExposure.OUTSIDE, rates.outside());
        }
        if (input.lit()) {
            return fall(VoidExposure.LIGHT, rates.light());
        }
        Result best = input.zone() == VoidZone.OUTER
                ? new Result(rates.outer(), VoidExposure.OUTER)
                : new Result(rates.inner(), VoidExposure.INNER);
        if (input.nearSource() && rates.source() > best.rate()) {
            best = new Result(rates.source(), VoidExposure.SOURCE);
        }
        return best;
    }

    private static Result fall(VoidExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        return new Result(0 - fallRate, exposure);
    }

    /**
     * What the rule looks at, already decided (testable without a server).
     *
     * @param zone       inner band, outer band (or void rim), or outside the zone
     * @param nearSource a block in {@code #void_sources} within {@code sourceRadius} of feet or head
     * @param lit        in the zone, block light at eye height of at least {@code lightLevel}
     */
    record Input(VoidZone zone, boolean nearSource, boolean lit) {
        static final Input OUTSIDE = new Input(VoidZone.OUTSIDE, false, false);
        /** In the zone and lit; the band and the source no longer matter, so they are not looked up. */
        static final Input LIT = new Input(VoidZone.INNER, false, true);
    }

    /** Points per second: {@code inner}, {@code outer} and {@code source} up, {@code light} and {@code outside} down. */
    public record Rates(float inner, float outer, float source, float light, float outside) {
    }

    record Result(float rate, VoidExposure exposure) {
    }
}
