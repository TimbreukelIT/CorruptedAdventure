package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.function.BooleanSupplier;

/**
 * Package 55b: ice melts in the heat of ring 3, on its random tick (so as gradually as vanilla's melting by block
 * light), where the heat meter would rise: under the open sky while the sun is up, or within {@code sourceRadius} of a
 * lit heat source ({@link ConditionContext#isHeatSource}; magma, smouldering stone and the vents give too little light
 * for vanilla). Weather does not count. Asked by {@code IceBlockMeltMixin} before vanilla's own light check; meltwater
 * under the open sky then evaporates ({@link WaterEvaporation}). Frosted ice has its own tick and is left alone.
 */
public final class IceMelting {
    private IceMelting() {
    }

    /** Whether the ice at {@code pos} melts on this random tick. Cheapest first: frozen oceans elsewhere tick a lot. */
    public static boolean meltsByHeat(ServerLevel level, BlockPos pos) {
        // The dimension first: no ring lookup for ice in the Nether or the End.
        boolean inZone = level.dimension() == Level.OVERWORLD && RingWorld.isActive(level.getServer())
                && HeatRule.inZone(level.dimension(), RingLayout.ringAt(level.dimension(), pos.getX() + 0.5, pos.getZ() + 0.5));
        return melts(inZone,
                () -> Sun.isUp(level.dimensionType(), level.getDayTime()) && underOpenSky(level, pos),
                () -> NearbyBlocks.near(level, pos, ServerConfig.HEAT.sourceRadius.get(), ConditionContext::isHeatSource));
    }

    /** The rule, with the lookups as suppliers: the sky and the block scan are only asked in the zone, the scan last. */
    static boolean melts(boolean inZone, BooleanSupplier inSun, BooleanSupplier nearSource) {
        return inZone && (inSun.getAsBoolean() || nearSource.getAsBoolean());
    }

    /**
     * Nothing solid above the ice (the heightmap, exact at once) and full sky light just above it (so leaves shade it),
     * like water that evaporates.
     */
    private static boolean underOpenSky(ServerLevel level, BlockPos pos) {
        return pos.getY() + 1 >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())
                && level.canSeeSky(pos.above());
    }
}
