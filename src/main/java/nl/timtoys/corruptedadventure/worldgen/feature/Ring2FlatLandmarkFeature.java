package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.tags.BlockTags;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * {@code corruptedadventure:ring_2_flat_landmark} (G2 ring 2, G2-R2-1): writes this chunk's part of every landmark of
 * {@code kind} ({@link Ring2FlatLandmarks}) that reaches into it. Placed once per chunk, last in its step. The plan is
 * the same in every chunk (pure in the seed and the terrain); each chunk writes only its own columns:
 * <ol>
 *   <li>{@link Part#SHAPE}: the feet (from the column's own frozen floor up to the planned block) and the blocks of
 *   the plan over whatever its own chunk placed before (the skeleton spares water, logs and leaves: a mini-oasis keeps its
 *   pool and palm); then plants and crystals, only into air and only when their support lies in this chunk;</li>
 *   <li>{@link Part#SPRING} (the giant palm): the spring pool, a configured {@code surface_pool} at the anchor, written
 *   whole by the anchor's chunk (the anchor sits in the middle of its chunk, so pool and rim stay in it).</li>
 * </ol>
 * Nothing from a neighbouring chunk writes into a landmark: the flat biomes' features stay in their chunk, the oasis
 * palms keep clear of the giant palm ({@link Ring2LandmarkClearanceFilter}) and the dune oases' palms stand inside their
 * chunk. So the blocks of a chunk depend only on the chunk, whatever was generated before it.
 */
public final class Ring2FlatLandmarkFeature extends Feature<Ring2FlatLandmarkFeature.Config> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    /** Which part of a landmark the placed feature writes. */
    public enum Part implements StringRepresentable {
        SHAPE("shape"),
        SPRING("spring");

        public static final Codec<Part> CODEC = StringRepresentable.fromEnum(Part::values);
        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** The kind and part to write; {@code pool} is the spring pool of {@link Part#SPRING}. */
    public record Config(Ring2FlatLandmarks.Kind kind, Part part, Optional<Holder<ConfiguredFeature<?, ?>>> pool)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ring2FlatLandmarks.Kind.CODEC.fieldOf("kind").forGetter(Config::kind),
                Part.CODEC.fieldOf("part").forGetter(Config::part),
                ConfiguredFeature.CODEC.optionalFieldOf("pool").forGetter(Config::pool)
        ).apply(instance, Config::new));
    }

    /** A foot whose real floor lies more than this under the planned one is skipped (carved away under the plan). */
    static final int MAX_FOOT_DROP = 4;

    public Ring2FlatLandmarkFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        if (config.part() == Part.SPRING) {
            for (Ring2FlatLandmarks.Site site : Ring2FlatLandmarks.reaching(level, config.kind(), chunk, 0)) {
                if (config.pool().isPresent()) {
                    placed |= config.pool().get().value().place(level, context.chunkGenerator(), Ring2FlatLandmarks.random(site, 3),
                            new BlockPos(site.x(), site.ground(), site.z()));
                }
            }
            return placed;
        }
        for (Ring2FlatLandmarks.Site site : Ring2FlatLandmarks.reaching(level, config.kind(), chunk, config.kind().reach())) {
            placed |= write(level, Ring2FlatLandmarks.plan(level, site), chunk, spares(config.kind()));
        }
        return placed;
    }

    /** What a kind's blocks never replace. */
    static Predicate<BlockState> spares(Ring2FlatLandmarks.Kind kind) {
        if (kind == Ring2FlatLandmarks.Kind.DUNE_SKELETON) {
            return state -> state.getFluidState().is(Fluids.WATER) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
        }
        return state -> false;
    }

    static boolean write(WorldGenLevel level, LandmarkPlan plan, ChunkPos chunk, Predicate<BlockState> spares) {
        Predicate<BlockPos> own = pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
        boolean placed = false;
        for (LandmarkPlan.Foot foot : plan.feet) {
            if (own.test(new BlockPos(foot.x(), 0, foot.z()))) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, foot.x(), foot.z());
                if (foot.bottom() - floor > MAX_FOOT_DROP) {
                    // The plan stands on the noise terrain; a carver (ring_2/canyon, caves) cut this column away:
                    // no column of salt, bone or log down to the ravine floor.
                    continue;
                }
                for (int y = floor - foot.sink(); y < foot.bottom(); y++) {
                    placed |= set(level, new BlockPos(foot.x(), y, foot.z()), foot.state(), spares);
                }
            }
        }
        for (var entry : plan.objects.entrySet()) {
            if (own.test(entry.getKey())) {
                placed |= set(level, entry.getKey(), entry.getValue(), spares);
            }
        }
        for (LandmarkPlan.Decor decor : plan.decor) {
            if (own.test(decor.pos()) && own.test(decor.support())) {
                GroundObjects.placeDecor(level, decor.pos(), decor.state());
            }
        }
        plan.groundDecor.forEach((column, state) -> {
            if (own.test(column) && GroundObjects.dry(level, column.getX(), column.getZ())) {
                GroundObjects.placeDecor(level, new BlockPos(column.getX(), GroundObjects.surface(level, column.getX(), column.getZ()),
                        column.getZ()), state);
            }
        });
        return placed;
    }

    private static boolean set(WorldGenLevel level, BlockPos pos, BlockState state, Predicate<BlockState> spares) {
        BlockState existing = level.getBlockState(pos);
        if (existing == state || spares.test(existing)) {
            return false;
        }
        level.setBlock(pos, state, FLAGS);
        return true;
    }
}
