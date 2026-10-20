package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import nl.timtoys.corruptedadventure.registry.ModTreeDecoratorTypes;

/**
 * Tree decorator {@code corruptedadventure:hanging_strand} (ring 4 decor rollout group A, 2026-10-19): strands of
 * {@code strand} (hanging swamp moss) from an aerial root under a tree's leaves, and a single {@code tip} (a glowing
 * root) under its roots, for the giant mangroves of {@code glowroot_swamp}. Part of the tree, so it grows with it in its {@link TreePlan}
 * ({@link IsolatedTreeFeature}): it sees the terrain and the tree's own blocks only. The strands rank as ground
 * ({@link TreeOverlap#strand}): another tree's leaf, propagule or moss takes their place, in either order. Moss only
 * under leaves and glowing roots only under roots, so two trees' strands seldom meet with different blocks.
 */
public final class HangingStrandDecorator extends TreeDecorator {
    public static final MapCodec<HangingStrandDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("hanger").forGetter(decorator -> decorator.hanger),
            BlockState.CODEC.fieldOf("strand").forGetter(decorator -> decorator.strand),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(decorator -> decorator.strandChance),
            Codec.intRange(1, 12).fieldOf("max_length").forGetter(decorator -> decorator.maxLength),
            BlockState.CODEC.fieldOf("tip").forGetter(decorator -> decorator.tip),
            Codec.floatRange(0.0F, 1.0F).fieldOf("tip_chance").forGetter(decorator -> decorator.tipChance)
    ).apply(instance, HangingStrandDecorator::new));

    private final BlockState hanger;
    private final BlockState strand;
    private final float strandChance;
    private final int maxLength;
    private final BlockState tip;
    private final float tipChance;

    /**
     * @param hanger       an aerial root under the leaf the strand hangs from (leaves give no hold: the moss hangs
     *                     only under a sturdy face or more moss)
     * @param strand       the moss, one block per step down
     * @param strandChance per leaf with air under it
     * @param maxLength    the longest strand
     * @param tip          a single block under a root over air
     * @param tipChance    per root with air under it
     */
    public HangingStrandDecorator(BlockState hanger, BlockState strand, float strandChance, int maxLength, BlockState tip,
                                  float tipChance) {
        this.hanger = hanger;
        this.strand = strand;
        this.strandChance = strandChance;
        this.maxLength = maxLength;
        this.tip = tip;
        this.tipChance = tipChance;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecoratorTypes.HANGING_STRAND.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        for (BlockPos leaf : context.leaves()) {
            // Always the same draws per leaf, so a strand never depends on the ones before it.
            float roll = random.nextFloat();
            int length = 1 + random.nextInt(maxLength);
            BlockPos below = leaf.below();
            if (!context.isAir(below)) {
                continue;
            }
            if (roll < strandChance) {
                context.setBlock(below, hanger);
                for (int i = 1; i <= length && context.isAir(below.below(i)); i++) {
                    context.setBlock(below.below(i), strand);
                }
            }
        }
        for (BlockPos root : context.roots()) {
            float roll = random.nextFloat();
            BlockPos below = root.below();
            if (roll < tipChance && context.isAir(below)) {
                context.setBlock(below, tip);
            }
        }
    }
}
