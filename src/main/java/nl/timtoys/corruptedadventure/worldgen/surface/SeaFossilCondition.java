package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossilShape;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Surface rule condition {@code corruptedadventure:sea_fossil} (78): true in the bones of the sea-creature fossils of
 * Nether ring 4 ({@link SeaFossilShape}), so the surface paints them {@code bone_block}. Reads the {@code sea_fossils}
 * density function of the world's noise router (found once per random state); its cells are decided once per world, in
 * the map the terrain shares. Without that function (another dimension) the condition is false.
 * <p>
 * The surface pass asks per block, column by column: the condition remembers the cell (a chunk lies in one) and the bone
 * mask of its last column, so a block costs one bit test.
 */
public final class SeaFossilCondition implements SurfaceRules.ConditionSource {
    public static final SeaFossilCondition INSTANCE = new SeaFossilCondition();
    public static final MapCodec<SeaFossilCondition> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<SeaFossilCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final Map<RandomState, Optional<SeaFossils>> FOUND = Collections.synchronizedMap(new WeakHashMap<>());

    private SeaFossilCondition() {
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        SeaFossils fossils = FOUND.computeIfAbsent(context.randomState,
                random -> Optional.ofNullable(SeaFossils.find(random.router().finalDensity()))).orElse(null);
        if (fossils == null) {
            return () -> false;
        }
        int cellSize = fossils.settings().cellSize();
        return new SurfaceRules.Condition() {
            private boolean cellKnown;
            private int cellX;
            private int cellZ;
            private SeaFossilShape.Fossil fossil;
            private boolean columnKnown;
            private int lastX;
            private int lastZ;
            private long mask;
            private int base;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (!columnKnown || x != lastX || z != lastZ) {
                    columnKnown = true;
                    lastX = x;
                    lastZ = z;
                    int cx = Math.floorDiv(x, cellSize);
                    int cz = Math.floorDiv(z, cellSize);
                    if (!cellKnown || cx != cellX || cz != cellZ) {
                        cellKnown = true;
                        cellX = cx;
                        cellZ = cz;
                        fossil = fossils.fossilOfCell(cx, cz);
                    }
                    mask = fossil != null && fossil.touches(x, z) ? fossil.boneMask(x, z) : 0L;
                    base = fossil != null ? fossil.maskBase() : 0;
                }
                if (mask == 0L) {
                    return false;
                }
                int i = context.blockY - base;
                return i >= 0 && i < 64 && (mask >>> i & 1L) != 0L;
            }
        };
    }
}
