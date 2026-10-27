package nl.timtoys.corruptedadventure.worldgen.surface;

import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts {@link BiomeSwitchRule}s into a surface rule (opt/surface): every run of at least {@link #MIN_CASES} consecutive
 * {@code if biome[...] then rule} rules in a sequence becomes one switch, at any depth. The rule gives the same block
 * everywhere (see the switch); only the number of tests per block drops. Before this, a block above the preliminary
 * surface tested up to ten biomes in a row (one branch per ring 2 biome, then rings 3, 4 and 5), and a ring 4 block
 * twelve more inside the ring 4 branch. Done on the loaded rule objects, not through the codec: every other node stays
 * the instance the datapack load made (an encode without registry ops would inline the density functions of
 * {@code ring4_threshold}), and the surface JSON and the scripts that write it keep their plain branches.
 */
public final class BiomeSwitches {
    /** Two tests cost about what one switch does. */
    static final int MIN_CASES = 3;

    private BiomeSwitches() {
    }

    /** {@code rule} with its runs of biome branches as switches; {@code rule} itself if it has none. */
    public static SurfaceRules.RuleSource of(SurfaceRules.RuleSource rule) {
        if (rule instanceof SurfaceRules.TestRuleSource test) {
            SurfaceRules.RuleSource thenRun = of(test.thenRun());
            return thenRun == test.thenRun() ? rule : SurfaceRules.ifTrue(test.ifTrue(), thenRun);
        }
        if (!(rule instanceof SurfaceRules.SequenceRuleSource sequence)) {
            return rule;
        }
        List<SurfaceRules.RuleSource> out = new ArrayList<>();
        List<SurfaceRules.TestRuleSource> run = new ArrayList<>();
        boolean changed = false;
        for (SurfaceRules.RuleSource child : sequence.sequence()) {
            SurfaceRules.RuleSource inner = of(child);
            changed |= inner != child;
            if (inner instanceof SurfaceRules.TestRuleSource test && test.ifTrue() instanceof SurfaceRules.BiomeConditionSource) {
                run.add(test);
            } else {
                changed |= flush(run, out);
                out.add(inner);
            }
        }
        changed |= flush(run, out);
        return changed ? SurfaceRules.sequence(out.toArray(SurfaceRules.RuleSource[]::new)) : rule;
    }

    /** Moves the run into {@code out}: as one switch if it is long enough (true), else as it was. */
    private static boolean flush(List<SurfaceRules.TestRuleSource> run, List<SurfaceRules.RuleSource> out) {
        boolean switched = run.size() >= MIN_CASES;
        if (switched) {
            out.add(new BiomeSwitchRule(run.stream()
                    .map(branch -> new BiomeSwitchRule.Case(((SurfaceRules.BiomeConditionSource) branch.ifTrue()).biomes, branch.thenRun()))
                    .toList()));
        } else {
            out.addAll(run);
        }
        run.clear();
        return switched;
    }
}
