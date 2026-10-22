package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.NetherCathedrals;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing5Terrain;

/**
 * Surface rule condition {@code corruptedadventure:nether_ring_5_pillar} (87): true in the columns of Nether ring 5 (the
 * grid's square columns and a cathedral's giant one, {@link NetherCathedrals#pillarAt}), so the surface paints them as
 * columns and the walls and ceiling around them as plain stone. Reads the {@code nether_cathedrals} terrain of the world's
 * noise router ({@link NetherRing5Terrain}); without it (another dimension) the condition is false.
 * <p>
 * The surface pass asks per block, column by column: the condition remembers its last column and reads through its own
 * {@link NetherCathedrals.Reader} (one condition object per chunk's surface pass, so per thread).
 */
public final class NetherRing5PillarCondition implements SurfaceRules.ConditionSource {
    public static final NetherRing5PillarCondition INSTANCE = new NetherRing5PillarCondition();
    public static final MapCodec<NetherRing5PillarCondition> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<NetherRing5PillarCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private NetherRing5PillarCondition() {
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        NetherCathedrals cathedrals = NetherRing5Terrain.cathedrals(context.randomState);
        if (cathedrals == null) {
            return () -> false;
        }
        NetherCathedrals.Reader reader = cathedrals.reader();
        return new SurfaceRules.Condition() {
            private boolean known;
            private int lastX;
            private int lastZ;
            private boolean result;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (!known || x != lastX || z != lastZ) {
                    known = true;
                    lastX = x;
                    lastZ = z;
                    result = reader.pillarAt(x, z);
                }
                return result;
            }
        };
    }
}
