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
 * The surface pass asks per block, column by column: the condition remembers the dressing of its last column.
 */
public record NetherRing5DressingCondition(NetherRing5Dressing.Part part) implements SurfaceRules.ConditionSource {
    public static final MapCodec<NetherRing5DressingCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NetherRing5Dressing.Part.CODEC.fieldOf("part").forGetter(NetherRing5DressingCondition::part)
    ).apply(instance, NetherRing5DressingCondition::new));
    public static final KeyDispatchDataCodec<NetherRing5DressingCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

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
        return new SurfaceRules.Condition() {
            private NetherRing5Dressing.Column column;
            private int lastX;
            private int lastZ;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (column == null || x != lastX || z != lastZ) {
                    lastX = x;
                    lastZ = z;
                    column = cathedrals.dressing(x, z);
                }
                return column.has(part, context.blockY);
            }
        };
    }
}
