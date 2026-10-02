package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * The rule of {@link Condition#DEHYDRATION}: rises only in direct sun by day in ring 2 of the overworld
 * ({@link DehydrationExposure#SUN}); otherwise falls at the strongest applicable cooling rate (they do not stack).
 * Water and the oasis cool everywhere; shade and night only count in the zone, outside it the meter falls at the
 * outside rate. No multipliers here: the heat wave (change 28) is a {@link RateModifier} on the positive rate.
 */
public record DehydrationRule(Rates rates) implements ConditionRule {
    /** The live rates from {@code [conditions.dehydration]}. */
    public static DehydrationRule fromConfig() {
        ServerConfig.DehydrationCondition config = ServerConfig.DEHYDRATION;
        return new DehydrationRule(new Rates(config.riseRate.get().floatValue(),
                config.fallRateShade.get().floatValue(), config.fallRateNight.get().floatValue(),
                config.fallRateWater.get().floatValue(), config.fallRateOasis.get().floatValue(),
                config.fallRateOutside.get().floatValue()));
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context), rates).exposure().getSerializedName();
    }

    /** The zone: ring 2 of the overworld, switching at blend 0.5 like the biomes and the ring weather. */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_2;
    }

    static Input input(ConditionContext context) {
        boolean inZone = inZone(context.level().dimension(), context.position());
        // Sun and sky only matter in the zone; skip the light lookup elsewhere.
        boolean sunUp = inZone && context.isSunUp();
        boolean directSun = sunUp && context.canSeeSky();
        return new Input(inZone, sunUp, directSun, context.isSubmerged(), context.biome().is(ModBiomes.OASIS_FLATS));
    }

    /** The rate and its reason; on equal cooling rates the first in enum order is the label. */
    static Result evaluate(Input input, Rates rates) {
        Result best = null;
        if (input.inWater()) {
            best = stronger(best, DehydrationExposure.WATER, rates.water());
        }
        if (input.inOasis()) {
            best = stronger(best, DehydrationExposure.OASIS, rates.oasis());
        }
        if (!input.inZone()) {
            best = stronger(best, DehydrationExposure.OUTSIDE, rates.outside());
        } else if (!input.sunUp()) {
            best = stronger(best, DehydrationExposure.NIGHT, rates.night());
        } else if (!input.directSun()) {
            best = stronger(best, DehydrationExposure.SHADE, rates.shade());
        }
        return best != null ? best : new Result(rates.rise(), DehydrationExposure.SUN);
    }

    private static Result stronger(Result best, DehydrationExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        Result candidate = new Result(0 - fallRate, exposure);
        return best == null || candidate.rate() < best.rate() ? candidate : best;
    }

    /**
     * What the rule looks at, already decided (testable without a server).
     *
     * @param inZone    overworld and {@code ring() == RING_2}
     * @param sunUp     the sun is above the horizon ({@link Sun}); only meaningful in the zone
     * @param directSun full sky light at eye height while the sun is up
     * @param inWater   submerged above the waist
     * @param inOasis   the biome at the feet is {@code oasis_flats}
     */
    record Input(boolean inZone, boolean sunUp, boolean directSun, boolean inWater, boolean inOasis) {
    }

    /** Points per second: {@code rise} up in the sun, the others down. */
    public record Rates(float rise, float shade, float night, float water, float oasis, float outside) {
    }

    record Result(float rate, DehydrationExposure exposure) {
    }
}
