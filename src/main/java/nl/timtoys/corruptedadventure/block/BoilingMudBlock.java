package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Boiling mud: vanilla mud's shape (you sink in two pixels) with the slowness of honey (speed and jump factor in its
 * properties, so the server moves entities and the client predicts the same). Bubbles are client-side
 * ({@code animateTick}); the heat comes from the conditions ({@code #heat_sources}). The coloured sulfur muds are the
 * same block with a {@code steam} particle rising now and then (46b); plain boiling mud has none (its steam comes from
 * the biome).
 */
public class BoilingMudBlock extends MudBlock {
    private static final Codec<SimpleParticleType> STEAM_CODEC = BuiltInRegistries.PARTICLE_TYPE.byNameCodec()
            .comapFlatMap(type -> type instanceof SimpleParticleType simple ? DataResult.success(simple)
                    : DataResult.error(() -> "Not a simple particle type: " + type), type -> type);
    public static final MapCodec<MudBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STEAM_CODEC.optionalFieldOf("steam").forGetter(block -> ((BoilingMudBlock) block).steamType()),
            propertiesCodec()).apply(instance, (steam, properties) ->
            new BoilingMudBlock(properties, steam.<Supplier<SimpleParticleType>>map(type -> () -> type).orElse(null))));
    /** One bubble sound per this many ticks per client, nearer blocks first: a mud pool has hundreds of blocks (tuning value). */
    private static final AmbientSoundLimiter BUBBLE_SOUND = new AmbientSoundLimiter(5);
    /** One in this many animate ticks puffs steam: a pool has hundreds of blocks, the client's particle setting thins it. */
    private static final int STEAM_CHANCE = 6;

    private final @Nullable Supplier<? extends SimpleParticleType> steam;

    public BoilingMudBlock(Properties properties) {
        this(properties, null);
    }

    /** @param steam the particle that rises from the mud now and then (a supplier: particle types register after blocks) */
    public BoilingMudBlock(Properties properties, @Nullable Supplier<? extends SimpleParticleType> steam) {
        super(properties);
        this.steam = steam;
    }

    private Optional<SimpleParticleType> steamType() {
        return Optional.ofNullable(steam).map(Supplier::get);
    }

    @Override
    public MapCodec<MudBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        if (steam != null && random.nextInt(STEAM_CHANCE) == 0) {
            level.addParticle(steam.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03 + random.nextDouble() * 0.03, 0.0);
        }
        if (random.nextInt(60) == 0 && BUBBLE_SOUND.tryAcquire(level, pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ModSoundEvents.BOILING_MUD_BUBBLE.get(),
                    SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
