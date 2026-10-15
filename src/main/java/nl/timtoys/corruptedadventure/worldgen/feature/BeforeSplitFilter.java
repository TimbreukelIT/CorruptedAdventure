package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * Placement filter {@code corruptedadventure:before_split} (verdant_falls polish, 2026-10-15): keeps a position only
 * if every column within {@code reach} blocks lies before the share {@code at} of the transition zone after
 * {@code ring} (its blend below {@code at}), so a feature that reaches {@code reach} blocks never touches what lies past
 * the split. The zone 3→4 splits at {@code IslandField.VERDANT_EDGE_SHARE}: the steam_fens pools, geysers and vents stay
 * on steam_fens' own ground, off the descent of verdant_falls (its top terrace, rim lakes and source pools). The
 * {@code BiomeFilter} only sees the feature's origin.
 * <p>
 * Draws nothing from the decoration random, so the placed feature's other draws stay where they were. Reads the ring
 * layout, not the biomes: the terrain's descent begins at the same blend, per block.
 */
public final class BeforeSplitFilter extends PlacementFilter {
    public static final MapCodec<BeforeSplitFilter> CODEC = RecordCodecBuilder.<BeforeSplitFilter>mapCodec(instance -> instance.group(
            Ring.CODEC.fieldOf("ring").forGetter(filter -> filter.ring),
            Codec.DOUBLE.fieldOf("at").forGetter(filter -> filter.at),
            Codec.intRange(0, 16).fieldOf("reach").forGetter(filter -> filter.reach)
    ).apply(instance, BeforeSplitFilter::new)).validate(BeforeSplitFilter::validate);

    /**
     * The boundary noise bends the zone's axis by less than this many blocks per block (as {@code VerdantGorges} reads
     * it): a position whose margin to the split is larger than this times the reach needs no further look.
     */
    static final double MAX_BEND = 4.0;

    private final Ring ring;
    private final double at;
    private final int reach;

    public BeforeSplitFilter(Ring ring, double at, int reach) {
        this.ring = ring;
        this.at = at;
        this.reach = reach;
    }

    public static BeforeSplitFilter of(Ring ring, double at, int reach) {
        return new BeforeSplitFilter(ring, at, reach);
    }

    private static DataResult<BeforeSplitFilter> validate(BeforeSplitFilter filter) {
        if (filter.ring == Ring.VOID) {
            return DataResult.error(() -> "before_split: there is no transition beyond the void");
        }
        return filter.at > 0.0 && filter.at < 1.0
                ? DataResult.success(filter)
                : DataResult.error(() -> "before_split: the split must lie inside its zone, 0 < at < 1: " + filter.at);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        // Only here, never in the codec: the layout exists once the server has started.
        ResourceKey<Level> dimension = context.getLevel().getLevel().dimension();
        return clear(RingLayout.current(), dimension, pos.getX(), pos.getZ());
    }

    /** Whether every column within the reach of x, z lies before the split. */
    boolean clear(RingLayout layout, ResourceKey<Level> dimension, int x, int z) {
        RingPosition origin = layout.sample(dimension, x, z);
        if (past(origin)) {
            return false;
        }
        if (origin.inTransition() && origin.inner() == ring
                && (at - origin.blend()) * layout.settings().transitionWidth(ring.ordinal()) > MAX_BEND * reach) {
            return true;
        }
        int reach2 = reach * reach;
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                if (dx * dx + dz * dz <= reach2 && past(layout.sample(dimension, x + dx, z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** At or past the split: in the zone after {@code ring} from {@code at} on, or beyond the zone in the next ring. */
    private boolean past(RingPosition position) {
        if (position.inTransition()) {
            return position.inner() == ring ? position.blend() >= at : position.inner().ordinal() > ring.ordinal();
        }
        return position.inner().ordinal() > ring.ordinal();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.BEFORE_SPLIT.get();
    }
}
