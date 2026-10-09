package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * Placement filter {@code corruptedadventure:in_ring} (change 47d): in the {@code biomes} (a transition biome), keeps
 * a position only on {@code ring}'s side of the ring switch ({@link RingLayout#ringProgress} present), so only in that
 * ring's half of the transition zone; in every other biome it keeps everything. Unlike {@link RingChanceFilter} it
 * draws nothing from the decoration random: added to an existing placed feature it leaves the positions of all its
 * other draws where they were.
 */
public final class InRingFilter extends PlacementFilter {
    public static final MapCodec<InRingFilter> CODEC = RecordCodecBuilder.<InRingFilter>mapCodec(instance -> instance.group(
            Ring.CODEC.fieldOf("ring").forGetter(filter -> filter.ring),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(filter -> filter.biomes)
    ).apply(instance, InRingFilter::new)).validate(InRingFilter::validate);

    private final Ring ring;
    private final HolderSet<Biome> biomes;

    public InRingFilter(Ring ring, HolderSet<Biome> biomes) {
        this.ring = ring;
        this.biomes = biomes;
    }

    /** Only on {@code ring}'s half, in the {@code biomes}. */
    public static InRingFilter of(Ring ring, HolderSet<Biome> biomes) {
        return new InRingFilter(ring, biomes);
    }

    private static DataResult<InRingFilter> validate(InRingFilter filter) {
        return filter.ring == Ring.VOID ? DataResult.error(() -> "in_ring: the void has no outer boundary") : DataResult.success(filter);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!biomes.contains(context.getLevel().getBiome(pos))) {
            return true;
        }
        // Only here, never in the codec: the layout exists once the server has started.
        ResourceKey<Level> dimension = context.getLevel().getLevel().dimension();
        int scale = RingLayout.scale(dimension);
        return RingLayout.current().ringProgress(ring, (double) pos.getX() * scale, (double) pos.getZ() * scale).isPresent();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.IN_RING.get();
    }
}
