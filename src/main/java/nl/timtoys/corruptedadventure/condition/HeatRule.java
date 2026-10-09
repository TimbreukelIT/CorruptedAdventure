package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * The rule of {@link Condition#HEAT} (change 43). In ring 3 of the overworld the meter rises when the player is
 * exposed: in direct sun by day, or within {@code sourceRadius} of a heat source (also at night and underground: lava
 * in a cave is hot). The rise is {@code sunRisePerSecond} times the strongest applicable factor (source, peak); the
 * factors do not stack. Water and the refuge biomes cool everywhere, also in the sun and next to a source. Unexposed
 * the meter falls at the strongest of night, roof and shade; outside the zone at the outside rate. No weather or
 * fire resistance here: those are {@link RateModifier}s on the positive rate.
 */
public record HeatRule(Rates rates, int sourceRadius) implements ConditionRule {
    /** The live rates from {@code [conditions.heat]}. */
    public static HeatRule fromConfig() {
        ServerConfig.HeatCondition config = ServerConfig.HEAT;
        return new HeatRule(new Rates(config.sunRisePerSecond.get().floatValue(),
                config.sourceMultiplier.get().floatValue(), config.peakMultiplier.get().floatValue(),
                config.nightFall.get().floatValue(), config.shadeFall.get().floatValue(),
                config.undergroundFall.get().floatValue(), config.waterFall.get().floatValue(),
                config.refugeFall.get().floatValue(), config.outsideFall.get().floatValue()),
                config.sourceRadius.get());
    }

    @Override
    public float ratePerSecond(ConditionContext context) {
        return evaluate(input(context, sourceRadius), rates).rate();
    }

    @Override
    public String debugLabel(ConditionContext context) {
        return evaluate(input(context, sourceRadius), rates).exposure().getSerializedName();
    }

    /** What the heat modifiers (weather, fire resistance) change: a rising heat rate, never a falling one. */
    static boolean isRisingHeat(Condition condition, float rate) {
        return condition == Condition.HEAT && rate > 0.0F;
    }

    /**
     * The zone: ring 3 of the overworld, switching at blend 0.5 like the biomes and the ring weather. Both tests always
     * run ({@code &}, not {@code &&}): no branch whose profile flips once the first players or water updates reach
     * ring 3 after the JIT compiled this on rings 1 and 2 (backlog r. 203, deopts seen in integration 1). The ring is
     * looked up once per player per second ({@link ConditionContext}) or once per water update, never per block.
     */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD & position.ring() == Ring.RING_3;
    }

    static Input input(ConditionContext context, int sourceRadius) {
        boolean inZone = inZone(context.level().dimension(), context.position());
        boolean inWater = context.isSubmerged();
        boolean inRefuge = context.biome().is(ModTags.Biomes.HEAT_REFUGES);
        if (!inZone || inWater || inRefuge) {
            // Cooling wins regardless of sun and sources: skip the light, heightmap and block lookups.
            return new Input(inZone, false, false, false, false, false, inWater, inRefuge);
        }
        boolean sunUp = context.isSunUp();
        boolean directSun = sunUp && context.canSeeSky();
        boolean underRoof = !directSun && context.isUnderRoof();
        return new Input(true, sunUp, directSun, underRoof, context.isNearHeatSource(sourceRadius),
                context.biome().is(ModTags.Biomes.HEAT_PEAKS), false, false);
    }

    /** The rate and its reason; on equal cooling rates the first in enum order is the label. */
    static Result evaluate(Input input, Rates rates) {
        Result cooling = null;
        if (input.inWater()) {
            cooling = stronger(cooling, HeatExposure.WATER, rates.water());
        }
        if (input.inRefuge()) {
            cooling = stronger(cooling, HeatExposure.REFUGE, rates.refuge());
        }
        if (!input.inZone()) {
            cooling = stronger(cooling, HeatExposure.OUTSIDE, rates.outside());
        }
        if (cooling != null) {
            return cooling;
        }
        if (input.directSun() || input.nearSource()) {
            return rise(input, rates);
        }
        Result best = null;
        if (!input.sunUp()) {
            best = stronger(best, HeatExposure.NIGHT, rates.night());
        }
        if (input.underRoof()) {
            best = stronger(best, HeatExposure.ROOF, rates.roof());
        } else if (input.sunUp()) {
            best = stronger(best, HeatExposure.SHADE, rates.shade());
        }
        return best;
    }

    /** {@code rise} times the strongest factor; on equal factors sun before source before peak. */
    private static Result rise(Input input, Rates rates) {
        float factor = 1.0F;
        HeatExposure exposure = input.directSun() ? HeatExposure.SUN : HeatExposure.SOURCE;
        if (input.nearSource() && rates.sourceMultiplier() > factor) {
            factor = rates.sourceMultiplier();
            exposure = HeatExposure.SOURCE;
        }
        if (input.inPeak() && rates.peakMultiplier() > factor) {
            factor = rates.peakMultiplier();
            exposure = HeatExposure.PEAK;
        }
        return new Result(rates.rise() * factor, exposure);
    }

    private static Result stronger(Result best, HeatExposure exposure, float fallRate) {
        // 0 - fallRate, so a fall rate of 0 gives +0.0, not -0.0.
        Result candidate = new Result(0 - fallRate, exposure);
        return best == null || candidate.rate() < best.rate() ? candidate : best;
    }

    /**
     * What the rule looks at, already decided (testable without a server). Outside the zone, in water or in a refuge
     * only the first and the last two fields are meaningful.
     *
     * @param inZone     overworld and {@code ring() == RING_3}
     * @param sunUp      the sun is above the horizon ({@link Sun})
     * @param directSun  full sky light at eye height while the sun is up
     * @param underRoof  without direct sun: a solid block somewhere above the head ({@link ConditionContext#isUnderRoof})
     * @param nearSource a block in {@code #heat_sources} within {@code sourceRadius}
     * @param inPeak     the biome at the feet is in {@code #heat_peaks}
     * @param inWater    submerged above the waist
     * @param inRefuge   the biome at the feet is in {@code #heat_refuges}
     */
    record Input(boolean inZone, boolean sunUp, boolean directSun, boolean underRoof, boolean nearSource,
                 boolean inPeak, boolean inWater, boolean inRefuge) {
    }

    /** Points per second: {@code rise} (times a factor) up, the others down. */
    public record Rates(float rise, float sourceMultiplier, float peakMultiplier, float night, float shade, float roof,
                        float water, float refuge, float outside) {
    }

    record Result(float rate, HeatExposure exposure) {
    }
}
