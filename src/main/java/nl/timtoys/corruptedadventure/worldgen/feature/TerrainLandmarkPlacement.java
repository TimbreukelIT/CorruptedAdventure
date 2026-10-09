package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Placement modifier {@code corruptedadventure:terrain_landmark} (39f accessors): the key points of a landmark of the
 * pure ring 3 terrain that lie in this chunk, e.g. the top of a fumarole mound, the floor of a sinkhole or the feet and
 * vertebrae of a titan's ribcage. Each point is kept with {@code chance}, moved up to {@code spread} blocks, and only
 * placed when it then lies at least {@code margin} blocks inside the chunk.
 * <p>
 * The landmarks are pure functions of the seed ({@link VolcanicField} accessors), and so are the keep and the move of a
 * point (a hash of the seed, the point and {@code salt}; features on the same landmark use different salts). So every
 * kept point lands in exactly one chunk, which places it, whatever the order of generation.
 */
public final class TerrainLandmarkPlacement extends PlacementModifier {
    /** The landmarks the terrain exposes. */
    public enum Landmark implements StringRepresentable {
        /** A fumarole mound of the sulfur fields: its centre (the top). */
        MOUND("mound"),
        /** A sinkhole of the sulfur fields: its centre (the floor). */
        SINKHOLE("sinkhole"),
        /** A collapse (39f): its centre. */
        COLLAPSE("collapse"),
        /** A titan's ribcage: the feet of its ribs and the vertebrae of its spine. */
        RIBCAGE("ribcage");

        public static final Codec<Landmark> CODEC = StringRepresentable.fromEnum(Landmark::values);

        private final String name;

        Landmark(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<TerrainLandmarkPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Landmark.CODEC.fieldOf("landmark").forGetter(placement -> placement.landmark),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(placement -> placement.chance),
            Codec.intRange(0, 16).optionalFieldOf("spread", 0).forGetter(placement -> placement.spread),
            Codec.intRange(0, 7).optionalFieldOf("margin", 0).forGetter(placement -> placement.margin),
            Codec.INT.optionalFieldOf("salt", 0).forGetter(placement -> placement.salt)
    ).apply(instance, TerrainLandmarkPlacement::new));

    /** Corners of the chunk: every landmark cell (≥ 36 blocks) overlapping the chunk contains one of them. */
    private static final int[][] CORNERS = {{0, 0}, {15, 0}, {0, 15}, {15, 15}};
    private static final long SALT = 0x4F1BBCDCBFA53E0BL;

    private final Landmark landmark;
    private final float chance;
    private final int spread;
    private final int margin;
    private final int salt;

    public TerrainLandmarkPlacement(Landmark landmark, float chance, int spread, int margin, int salt) {
        this.landmark = landmark;
        this.chance = chance;
        this.spread = spread;
        this.margin = margin;
        this.salt = salt;
    }

    public static TerrainLandmarkPlacement of(Landmark landmark, float chance, int spread, int margin, int salt) {
        return new TerrainLandmarkPlacement(landmark, chance, spread, margin, salt);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        VolcanicField field = Ring3Terrain.field(context.getLevel());
        if (field == null) {
            return Stream.empty();
        }
        int minX = pos.getX() & ~15;
        int minZ = pos.getZ() & ~15;
        return place(points(field, landmark, minX, minZ), minX, pos.getY(), minZ, context.getLevel().getSeed()).stream();
    }

    /** The kept points of {@code points} that land at least {@code margin} inside the chunk at {@code minX, minZ}. */
    List<BlockPos> place(List<VolcanicField.Point> points, int minX, int y, int minZ, long seed) {
        List<BlockPos> positions = new ArrayList<>();
        for (VolcanicField.Point point : points) {
            int px = Mth.floor(point.x());
            int pz = Mth.floor(point.z());
            long h = mix(seed ^ SALT ^ mix(salt * 0xD1B54A32D192ED03L + px * 0x9E3779B97F4A7C15L + pz * 0xC2B2AE3D27D4EB4FL));
            boolean kept = unit(h, 0) < chance;
            int x = px + (int) (unit(h, 1) * (2 * spread + 1)) - spread;
            int z = pz + (int) (unit(h, 2) * (2 * spread + 1)) - spread;
            if (kept && x >= minX + margin && x < minX + 16 - margin && z >= minZ + margin && z < minZ + 16 - margin) {
                positions.add(new BlockPos(x, y, z));
            }
        }
        return positions;
    }

    /** The key points of the landmarks whose cells overlap the chunk (or, for ribcages, nearest to it), in a fixed order. */
    static List<VolcanicField.Point> points(VolcanicField field, Landmark landmark, int minX, int minZ) {
        Set<VolcanicField.Point> points = new LinkedHashSet<>();
        for (int[] corner : CORNERS) {
            int x = minX + corner[0];
            int z = minZ + corner[1];
            switch (landmark) {
                case MOUND -> {
                    VolcanicField.Mound mound = field.moundAt(x, z);
                    if (mound != null) {
                        points.add(new VolcanicField.Point(mound.x(), mound.z()));
                    }
                }
                case SINKHOLE -> {
                    VolcanicField.Sinkhole sinkhole = field.sinkholeAt(x, z);
                    if (sinkhole != null) {
                        points.add(new VolcanicField.Point(sinkhole.x(), sinkhole.z()));
                    }
                }
                case COLLAPSE -> {
                    VolcanicField.Sinkhole collapse = field.collapseAt(x, z);
                    if (collapse != null) {
                        points.add(new VolcanicField.Point(collapse.x(), collapse.z()));
                    }
                }
                case RIBCAGE -> {
                    VolcanicField.Ribcage cage = field.ribcageNear(x, z);
                    if (cage != null) {
                        points.addAll(cage.ribFeet());
                        points.addAll(cage.vertebrae());
                    }
                }
            }
        }
        return List.copyOf(points);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long hash, int n) {
        return (mix(hash + n * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.TERRAIN_LANDMARK.get();
    }
}
