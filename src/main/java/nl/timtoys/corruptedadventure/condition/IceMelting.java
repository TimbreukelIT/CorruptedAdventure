package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Package 55b: ice melts in the heat of ring 3, on its random tick (as gradually as vanilla's melting by block light),
 * where the heat meter would rise: the {@link HeatRule} evaluated for the ice instead of a player. So under the open
 * sky while the sun is up, or within {@code sourceRadius} of a lit heat source ({@link ConditionContext#isHeatSource};
 * magma, smouldering stone and the vents give too little light for vanilla), and never in a refuge biome or under
 * water. Weather does not count (it is a modifier on the player's rate). Asked by {@code IceBlockMeltMixin} before
 * vanilla's own light check; meltwater under the open sky then evaporates ({@link WaterEvaporation}).
 * <p>
 * Cost: only ice in the zone gets this far (the dimension and the ring come first), and in the sun, in a refuge or
 * under water no block is scanned. Otherwise {@link NearbyBlocks} searches a box of {@code sourceRadius} around the ice
 * and the block above it, nearest first, stopping at the first lit source: at most 810 block reads at the default
 * radius 4. Ring 3 has no natural ice, so only placed ice pays this, once per random tick (on average every ~68 s per
 * block at {@code randomTickSpeed} 3: a thousand blocks of ice cost ~15 scans per second). The radius stays the
 * player's {@code sourceRadius} (decision V1 = A).
 */
public final class IceMelting {
    private IceMelting() {
    }

    /** Whether the ice at {@code pos} melts on this random tick. */
    public static boolean meltsByHeat(ServerLevel level, BlockPos pos) {
        if (!HeatZone.contains(level, pos)) {
            return false;
        }
        HeatRule rule = HeatRule.fromConfig();
        return melts(input(level, pos, rule.sourceRadius()), rule.rates());
    }

    /** The heat rule's verdict: the ice melts where the heat meter would rise. */
    static boolean melts(HeatRule.Input input, HeatRule.Rates rates) {
        return HeatRule.evaluate(input, rates).rate() > 0.0F;
    }

    /**
     * The rule's input for a block in the zone. Only the sign of the rate matters, so the roof and the peak (which only
     * pick a fall rate or a stronger rise) are not looked up, and with direct sun the block scan is skipped. Water
     * resting on the ice counts as in water.
     */
    private static HeatRule.Input input(ServerLevel level, BlockPos pos, int sourceRadius) {
        boolean inWater = level.getFluidState(pos.above()).is(FluidTags.WATER);
        Holder<Biome> biome = level.getBiome(pos);
        boolean inRefuge = biome.is(ModTags.Biomes.HEAT_REFUGES);
        if (inWater || inRefuge) {
            return new HeatRule.Input(true, false, false, false, false, false, inWater, inRefuge);
        }
        boolean sunUp = Sun.isUp(level.dimensionType(), level.getDayTime());
        boolean directSun = sunUp && HeatZone.underOpenSky(level, pos);
        boolean nearSource = !directSun && NearbyBlocks.near(level, pos, sourceRadius, ConditionContext::isHeatSource);
        return new HeatRule.Input(true, sunUp, directSun, false, nearSource, false, false, false);
    }
}
