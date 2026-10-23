package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.NetherCathedrals;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.NetherRing5Dressing;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing5Terrain;

/**
 * Surface rule condition {@code corruptedadventure:nether_ring_5_dressing} (96, field {@code part}): true where the
 * dressing of Nether ring 5 puts {@code part} ({@link NetherCathedrals#dressing}): the light band and the ore inside the
 * columns, the imprint in the floor round a giant column's foot. The rule that holds it narrows it down (the column
 * rule for {@code light}/{@code ore}, the floor's top block for the imprint). Reads the {@code nether_cathedrals}
 * terrain of the world's noise router ({@link NetherRing5Terrain}); without it (another dimension) the condition is false.
 * <p>
 * The surface pass asks per block, column by column, and the four parts ask about the same column one after another:
 * the dressing of the last column is shared between them per thread ({@link #LAST}), so it is worked out once.
 */
public record NetherRing5DressingCondition(NetherRing5Dressing.Part part) implements SurfaceRules.ConditionSource {
    public static final MapCodec<NetherRing5DressingCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NetherRing5Dressing.Part.CODEC.fieldOf("part").forGetter(NetherRing5DressingCondition::part)
    ).apply(instance, NetherRing5DressingCondition::new));
    public static final KeyDispatchDataCodec<NetherRing5DressingCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);
    /** Per thread (one chunk's surface pass at a time): the last column asked, by any part, of any world's field. */
    private static final ThreadLocal<Last> LAST = ThreadLocal.withInitial(Last::new);

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
        Last last = LAST.get();
        return () -> last.of(cathedrals, context.blockX, context.blockZ).has(part, context.blockY);
    }

    /** The last column's dressing; a new field (another world) or column replaces it. */
    private static final class Last {
        private NetherCathedrals field;
        private int x;
        private int z;
        private NetherRing5Dressing.Column column;

        NetherRing5Dressing.Column of(NetherCathedrals cathedrals, int x, int z) {
            if (field != cathedrals || this.x != x || this.z != z) {
                field = cathedrals;
                this.x = x;
                this.z = z;
                column = cathedrals.dressing(x, z);
            }
            return column;
        }
    }
}
