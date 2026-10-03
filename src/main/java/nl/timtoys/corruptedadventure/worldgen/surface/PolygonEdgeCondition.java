package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeededHash;

/**
 * Surface rule condition {@code corruptedadventure:polygon_edge}: true on the edges of a pattern of slightly irregular
 * hexagons ({@link PolygonGrid}), {@code cell_size} across, edges {@code width} blocks wide. The salt flats use it for
 * the cracks between the salt polygons of the crust (change 30b).
 * <p>
 * A pure function of the seed (through the {@code hash} noise) and the column, so the pattern runs seamlessly across
 * chunks. The surface pass asks per block; the condition remembers its last column. One condition object per chunk's
 * surface pass, so per thread.
 *
 * @param cellSize distance between neighbouring polygon centres, in blocks
 * @param jitter   how far each centre moves from the lattice, as a fraction of the cell (0–0.45)
 * @param width    edge width along the axis that crosses the edge most steeply; 1 = a closed line of single blocks
 * @param hash     noise used as a seeded hash (firstOctave 0, as {@code ring_2_slot_cells})
 */
public record PolygonEdgeCondition(double cellSize, double jitter, double width, ResourceKey<NormalNoise.NoiseParameters> hash)
        implements SurfaceRules.ConditionSource {
    private static final long SALT = 7;

    public static final MapCodec<PolygonEdgeCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(4.0, 64.0).fieldOf("cell_size").forGetter(PolygonEdgeCondition::cellSize),
            Codec.doubleRange(0.0, 0.45).fieldOf("jitter").forGetter(PolygonEdgeCondition::jitter),
            Codec.doubleRange(0.1, 4.0).fieldOf("width").forGetter(PolygonEdgeCondition::width),
            ResourceKey.codec(Registries.NOISE).fieldOf("hash").forGetter(PolygonEdgeCondition::hash)
    ).apply(instance, PolygonEdgeCondition::new));

    public static final KeyDispatchDataCodec<PolygonEdgeCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        PolygonGrid grid = grid(context.randomState.getOrCreateNoise(hash));
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
                    result = grid.isEdge(x, z);
                }
                return result;
            }
        };
    }

    /** The pattern for one seeded hash noise. */
    PolygonGrid grid(NormalNoise noise) {
        return new PolygonGrid((i, row) -> SeededHash.hash(noise, i, row, SALT), SeededHash::unit, cellSize, jitter, width);
    }
}
