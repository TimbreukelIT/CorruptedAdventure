package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

/**
 * Placement filter {@code corruptedadventure:ring5_filter} (94/95): keeps a position on ring 5's own ground
 * ({@link Ring5World#core}, never the zone 4→5) that lies more than {@code void_clearance} blocks from the void
 * ({@link Ring5World#voidDistance}: cracks, holes, missing chunks, the void between the floes), and, with
 * {@code max_step} &gt; 0, where the frozen surface within {@link #STEP_REACH} blocks differs by at most that much (off the
 * hard walls of the corrupt spots and the stairs), and with {@code memory_clearance} &gt; 0 more than that many blocks
 * outside every memory ({@link Ring5Memories}); with {@code on_castle} only on a castle site's ground. Pure in the seed and
 * the terrain.
 */
public final class Ring5Filter extends PlacementFilter {
    /** How far (blocks, each axis) the step test looks; inside the neighbouring chunks a feature may read. */
    static final int STEP_REACH = 2;

    public static final MapCodec<Ring5Filter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, Ring5World.MAX_REACH).fieldOf("void_clearance").forGetter(filter -> filter.voidClearance),
            Codec.intRange(0, 64).optionalFieldOf("max_step", 0).forGetter(filter -> filter.maxStep),
            Codec.intRange(0, 16).optionalFieldOf("memory_clearance", 0).forGetter(filter -> filter.memoryClearance),
            Codec.BOOL.optionalFieldOf("on_castle", false).forGetter(filter -> filter.onCastle)
    ).apply(instance, Ring5Filter::new));

    private final int voidClearance;
    private final int maxStep;
    private final int memoryClearance;
    private final boolean onCastle;

    public Ring5Filter(int voidClearance, int maxStep, int memoryClearance, boolean onCastle) {
        this.voidClearance = voidClearance;
        this.maxStep = maxStep;
        this.memoryClearance = memoryClearance;
        this.onCastle = onCastle;
    }

    public static Ring5Filter of(int voidClearance, int maxStep, int memoryClearance) {
        return new Ring5Filter(voidClearance, maxStep, memoryClearance, false);
    }

    /** On a castle site's ground only (the plateau and its ruined rim; V9-B). */
    public static Ring5Filter onCastle(int voidClearance) {
        return new Ring5Filter(voidClearance, 0, 0, true);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        int x = pos.getX();
        int z = pos.getZ();
        Ring5World world = Ring5Terrain.world(level);
        if (world == null || !world.core(x, z) || world.voidDistance(x, z, voidClearance) <= voidClearance) {
            return false;
        }
        if (onCastle && world.field().column(x, z).castle() <= 0.0) {
            return false;
        }
        if (maxStep > 0) {
            int here = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            for (int dx = -STEP_REACH; dx <= STEP_REACH; dx += STEP_REACH) {
                for (int dz = -STEP_REACH; dz <= STEP_REACH; dz += STEP_REACH) {
                    if (Math.abs(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + dx, z + dz) - here) > maxStep) {
                        return false;
                    }
                }
            }
        }
        return memoryClearance == 0 || !Ring5Memories.near(world, x, z, memoryClearance);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.RING5_FILTER.get();
    }
}
