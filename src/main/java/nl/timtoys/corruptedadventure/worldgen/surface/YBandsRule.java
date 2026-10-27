package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.List;
import java.util.Objects;

/**
 * Surface rule {@code corruptedadventure:y_bands}: blocks in horizontal bands by height, one table lookup per block
 * (opt/surface). A block at {@code y} gets the block of the first band in the list whose {@code from} it reaches, and
 * {@code below} if it reaches none. It reaches {@code from} when {@code y >= from + surface depth ×
 * surface_depth_multiplier}: exactly vanilla's {@code y_above} with an absolute anchor and {@code add_stone_depth}
 * false. So a sequence of such {@code y_above → block} rules that ends in a plain block gives the same block as this
 * one rule (the rock strata of ring 2: 47 bands, up to 15 tests per block before; {@code tools/worldgen/surface_bands.py}
 * converts them).
 */
public final class YBandsRule implements SurfaceRules.RuleSource {
    /** One {@code y_above → block} rule: {@code block} from {@code from} (absolute) up. */
    public record Band(int from, BlockState block) {
        static final Codec<Band> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("from").forGetter(Band::from),
                BlockState.CODEC.fieldOf("block").forGetter(Band::block)
        ).apply(instance, Band::new));
    }

    public static final MapCodec<YBandsRule> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            // The range of y_above's multiplier.
            Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(YBandsRule::surfaceDepthMultiplier),
            Codec.list(Band.CODEC, 1, Integer.MAX_VALUE).fieldOf("bands").forGetter(YBandsRule::bands),
            BlockState.CODEC.fieldOf("below").forGetter(YBandsRule::below)
    ).apply(instance, YBandsRule::new));

    public static final KeyDispatchDataCodec<YBandsRule> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final int surfaceDepthMultiplier;
    private final List<Band> bands;
    private final BlockState below;
    // Built once: the surface pass applies the rule per chunk, the carvers per grass block they cut.
    private final Table table;

    /**
     * @param surfaceDepthMultiplier as in {@code y_above}
     * @param bands                  in the order the sequence tested them
     * @param below                  the block when no band is reached
     */
    public YBandsRule(int surfaceDepthMultiplier, List<Band> bands, BlockState below) {
        this.surfaceDepthMultiplier = surfaceDepthMultiplier;
        this.bands = List.copyOf(bands);
        this.below = below;
        this.table = table();
    }

    public int surfaceDepthMultiplier() {
        return surfaceDepthMultiplier;
    }

    public List<Band> bands() {
        return bands;
    }

    public BlockState below() {
        return below;
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof YBandsRule rule && rule.surfaceDepthMultiplier == surfaceDepthMultiplier && rule.bands.equals(bands)
                && rule.below == below;
    }

    @Override
    public int hashCode() {
        return Objects.hash(surfaceDepthMultiplier, bands, below);
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        Table bandTable = table;
        int multiplier = surfaceDepthMultiplier;
        // The same height y_above compares (the context's block, as the surface pass and the carvers set it).
        return (x, y, z) -> bandTable.get(context.blockY - context.surfaceDepth * multiplier);
    }

    /**
     * The block for every reduced height from the lowest band's {@code from} to the highest one's. Above the highest
     * every band is reached, so the first band in the list wins there as at the top entry.
     */
    Table table() {
        int lowest = bands.stream().mapToInt(Band::from).min().orElseThrow();
        int highest = bands.stream().mapToInt(Band::from).max().orElseThrow();
        BlockState[] states = new BlockState[highest - lowest + 1];
        for (int i = 0; i < states.length; i++) {
            states[i] = at(lowest + i);
        }
        return new Table(lowest, states, below);
    }

    /** The block at reduced height {@code y} (y − surface depth × multiplier) by the sequence's own rule. */
    BlockState at(int y) {
        for (Band band : bands) {
            if (y >= band.from()) {
                return band.block();
            }
        }
        return below;
    }

    record Table(int lowest, BlockState[] states, BlockState below) {
        BlockState get(int y) {
            int index = y - lowest;
            return index < 0 ? below : states[Math.min(index, states.length - 1)];
        }
    }
}
