package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.WorldTreePart;

import java.util.Locale;

/**
 * Surface rule condition {@code corruptedadventure:world_tree} (74, Q7 = A: the world tree is terrain): true in the
 * blocks of a world tree of ring 4 that are {@code part} ({@code log}: the trunk, {@code wood}: branches and roots,
 * {@code leaves}: the crown; {@code any}: any of them), so the surface paints them {@code moonpetal_log},
 * {@code moonpetal_wood} or persistent {@code moonpetal_leaves}. Reads the noise chunk's own ring 4 field
 * ({@link RingChunkGenerator#surfaceIslandField}), so it sees exactly the blocks the terrain made. Without that field
 * the condition is false.
 * <p>
 * The surface pass asks per block, column by column: the condition remembers whether its last column holds a tree, so
 * a block outside the trees costs one comparison.
 *
 * @param part the part, or {@link WorldTreePart#NONE} for {@code any}
 */
public record WorldTreeCondition(WorldTreePart part) implements SurfaceRules.ConditionSource {
    private static final String ANY = "any";
    private static final Codec<WorldTreePart> PART = Codec.STRING.comapFlatMap(WorldTreeCondition::parse,
            part -> part == WorldTreePart.NONE ? ANY : part.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<WorldTreeCondition> MAP_CODEC = PART.fieldOf("part").xmap(WorldTreeCondition::new, WorldTreeCondition::part);

    public static final KeyDispatchDataCodec<WorldTreeCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static DataResult<WorldTreePart> parse(String name) {
        if (name.equals(ANY)) {
            return DataResult.success(WorldTreePart.NONE);
        }
        for (WorldTreePart part : WorldTreePart.values()) {
            if (part != WorldTreePart.NONE && part.name().toLowerCase(Locale.ROOT).equals(name)) {
                return DataResult.success(part);
            }
        }
        return DataResult.error(() -> "world_tree part must be log, wood, leaves or any, got " + name);
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        IslandField field = RingChunkGenerator.surfaceIslandField();
        if (field == null) {
            return () -> false;
        }
        return new SurfaceRules.Condition() {
            private boolean known;
            private int lastX;
            private int lastZ;
            private boolean tree;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (!known || x != lastX || z != lastZ) {
                    known = true;
                    lastX = x;
                    lastZ = z;
                    tree = field.hasTree(x, z);
                }
                if (!tree) {
                    return false;
                }
                WorldTreePart found = field.treePart(x, context.blockY, z);
                return part == WorldTreePart.NONE ? found != WorldTreePart.NONE : found == part;
            }
        };
    }
}
