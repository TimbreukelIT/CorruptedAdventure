package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * Placement filter {@code corruptedadventure:in_ring} (change 47d): keeps a position on {@code ring}'s side of the ring
 * switch ({@link RingLayout#ringProgress} present), so in a transition zone only on that ring's half. Unlike
 * {@link RingChanceFilter} it draws nothing from the decoration random: added to an existing placed feature it leaves
 * the positions of all its other draws where they were.
 */
public final class InRingFilter extends PlacementFilter {
    public static final MapCodec<InRingFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ring.CODEC.fieldOf("ring").forGetter(filter -> filter.ring)
    ).apply(instance, InRingFilter::new));

    private final Ring ring;

    public InRingFilter(Ring ring) {
        if (ring == Ring.VOID) {
            throw new IllegalArgumentException("in_ring: the void has no outer boundary");
        }
        this.ring = ring;
    }

    public static InRingFilter of(Ring ring) {
        return new InRingFilter(ring);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
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
