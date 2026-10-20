package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code corruptedadventure:fungus_ring} (ring 4 decor B, 2026-10-19): a ring of mushrooms (a fairy ring) on dry
 * ground: every column about {@code radius} from the centre gets a {@code plant} with {@code density}. Its own random
 * and the frozen heightmaps decide, it stays in its own chunk and only fills air ({@link GroundObjects}).
 */
public final class FungusRingFeature extends Feature<FungusRingFeature.Config> {
    static final int MAX_RADIUS = 5;
    private static final long SALT = 0xF4_1121_6L;

    /**
     * @param plant   the mushrooms
     * @param radius  the ring's radius
     * @param density per column of the ring, the chance of a mushroom
     */
    public record Config(BlockStateProvider plant, IntProvider radius, float density) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("plant").forGetter(Config::plant),
                IntProvider.codec(1, MAX_RADIUS).fieldOf("radius").forGetter(Config::radius),
                Codec.floatRange(0.0F, 1.0F).fieldOf("density").forGetter(Config::density)
        ).apply(instance, Config::new));
    }

    public FungusRingFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        RandomSource random = GroundObjects.random(level.getSeed(), context.origin(), SALT);
        int radius = config.radius().sample(random);
        BlockPos centre = GroundObjects.fit(context.origin(), radius);
        boolean placed = false;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float roll = random.nextFloat();
                BlockState state = config.plant().getState(random, centre.offset(dx, 0, dz));
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (Math.abs(d - radius) > 0.5 || roll >= config.density() || !GroundObjects.dry(level, x, z)) {
                    continue;
                }
                placed |= GroundObjects.placeDecor(level, new BlockPos(x, GroundObjects.surface(level, x, z), z), state);
            }
        }
        return placed;
    }
}
