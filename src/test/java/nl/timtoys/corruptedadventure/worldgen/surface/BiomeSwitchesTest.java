package nl.timtoys.corruptedadventure.worldgen.surface;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class BiomeSwitchesTest {
    private static final List<ResourceKey<Biome>> KEYS = List.of(biome("a"), biome("b"), biome("c"), biome("d"), biome("e"));
    private static final List<BlockState> BLOCKS = List.of(Blocks.SAND.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
            Blocks.PODZOL.defaultBlockState(), Blocks.MUD.defaultBlockState());
    private static final HolderOwner<Biome> OWNER = new HolderOwner<>() {
    };
    private static final SurfaceRules.RuleSource STEEP = SurfaceRules.ifTrue(SurfaceRules.steep(), block(Blocks.STONE.defaultBlockState()));
    /** A rule that never gives a block. */
    private static final SurfaceRules.RuleSource NOTHING = new SurfaceRules.RuleSource() {
        @Override
        public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            return (x, y, z) -> null;
        }
    };

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("test", name));
    }

    private static SurfaceRules.RuleSource block(BlockState state) {
        return SurfaceRules.state(state);
    }

    @SafeVarargs
    private static SurfaceRules.RuleSource branch(SurfaceRules.RuleSource then, ResourceKey<Biome>... biomes) {
        return SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes), then);
    }

    @Test
    void runsOfThreeBiomeBranchesBecomeOneSwitchInOrder() {
        SurfaceRules.RuleSource sand = branch(block(BLOCKS.get(0)), KEYS.get(0));
        SurfaceRules.RuleSource nested = branch(SurfaceRules.sequence(branch(block(BLOCKS.get(1)), KEYS.get(2)),
                branch(block(BLOCKS.get(1)), KEYS.get(3)), branch(block(BLOCKS.get(2)), KEYS.get(4))), KEYS.get(1), KEYS.get(0));
        SurfaceRules.RuleSource grass = branch(block(BLOCKS.get(1)), KEYS.get(2));
        SurfaceRules.RuleSource swamp = branch(block(BLOCKS.get(3)), KEYS.get(3));
        SurfaceRules.RuleSource taiga = branch(block(BLOCKS.get(2)), KEYS.get(4));
        SurfaceRules.RuleSource stone = block(Blocks.STONE.defaultBlockState());
        SurfaceRules.SequenceRuleSource rule = (SurfaceRules.SequenceRuleSource) SurfaceRules.sequence(STEEP, sand, nested, grass, STEEP,
                swamp, taiga, stone);

        SurfaceRules.SequenceRuleSource out = assertInstanceOf(SurfaceRules.SequenceRuleSource.class, BiomeSwitches.of(rule));
        List<SurfaceRules.RuleSource> sequence = out.sequence();
        assertEquals(6, sequence.size());
        assertSame(STEEP, sequence.get(0));
        BiomeSwitchRule first = assertInstanceOf(BiomeSwitchRule.class, sequence.get(1));
        assertEquals(List.of(List.of(KEYS.get(0)), List.of(KEYS.get(1), KEYS.get(0)), List.of(KEYS.get(2))),
                first.cases().stream().map(BiomeSwitchRule.Case::biomes).toList());
        assertEquals(block(BLOCKS.get(0)), first.cases().get(0).rule());
        // The switch inside a case's rule as well.
        SurfaceRules.SequenceRuleSource inner = assertInstanceOf(SurfaceRules.SequenceRuleSource.class, first.cases().get(1).rule());
        assertInstanceOf(BiomeSwitchRule.class, inner.sequence().get(0));
        // A run of two stays as it was, the same instances.
        assertSame(STEEP, sequence.get(2));
        assertSame(swamp, sequence.get(3));
        assertSame(taiga, sequence.get(4));
        assertSame(stone, sequence.get(5));
        // The input is left alone.
        assertEquals(8, rule.sequence().size());
    }

    @Test
    void aRuleWithoutRunsComesBackTheSame() {
        SurfaceRules.RuleSource rule = SurfaceRules.sequence(STEEP, branch(block(BLOCKS.get(0)), KEYS.get(0)),
                SurfaceRules.ifTrue(SurfaceRules.hole(), SurfaceRules.sequence(branch(block(BLOCKS.get(1)), KEYS.get(1)), STEEP)),
                block(Blocks.STONE.defaultBlockState()));
        assertSame(rule, BiomeSwitches.of(rule));
        assertSame(STEEP, BiomeSwitches.of(STEEP));
    }

    /**
     * A switch gives the block of the branches it replaces for every biome: in their order, a case without a block
     * passes on to the next one holding the biome, a biome named twice in one case or in several cases, an unknown
     * biome and a biome without a key (a direct holder, which vanilla's biome test never matches).
     */
    @Test
    void aSwitchGivesTheBlockOfTheBranchesItReplaces() {
        Random random = new Random(20260930L);
        List<Holder<Biome>> biomes = new ArrayList<>();
        KEYS.forEach(key -> biomes.add(Holder.Reference.createStandAlone(OWNER, key)));
        biomes.add(Holder.Reference.createStandAlone(OWNER, biome("unknown")));
        biomes.add(Holder.direct(null));
        for (int round = 0; round < 500; round++) {
            List<SurfaceRules.RuleSource> branches = new ArrayList<>();
            List<BiomeSwitchRule.Case> cases = new ArrayList<>();
            int count = 1 + random.nextInt(8);
            for (int i = 0; i < count; i++) {
                List<ResourceKey<Biome>> keys = new ArrayList<>();
                int size = 1 + random.nextInt(3);
                for (int k = 0; k < size; k++) {
                    keys.add(KEYS.get(random.nextInt(KEYS.size())));
                }
                SurfaceRules.RuleSource then = switch (random.nextInt(3)) {
                    case 0 -> NOTHING;
                    case 1 -> block(BLOCKS.get(random.nextInt(BLOCKS.size())));
                    default -> SurfaceRules.sequence(NOTHING, block(BLOCKS.get(random.nextInt(BLOCKS.size()))));
                };
                @SuppressWarnings("unchecked")
                ResourceKey<Biome>[] array = keys.toArray(ResourceKey[]::new);
                branches.add(branch(then, array));
                cases.add(new BiomeSwitchRule.Case(keys, then));
            }
            SurfaceRules.RuleSource plain = SurfaceRules.sequence(branches.toArray(SurfaceRules.RuleSource[]::new));
            BiomeSwitchRule switched = new BiomeSwitchRule(cases);
            for (Holder<Biome> biome : biomes) {
                assertSame(evaluate(plain, biome), evaluate(switched, biome), "round " + round + ", " + biome);
            }
        }
    }

    /** The block a rule gives at a block of {@code biome}, on a fresh bare context (the biome rules read nothing else). */
    private static BlockState evaluate(SurfaceRules.RuleSource rule, Holder<Biome> biome) {
        SurfaceRules.Context context = new SurfaceRules.Context(null, null, null, null, pos -> biome, null, null);
        context.biome = () -> biome;
        return rule.apply(context).tryApply(0, 64, 0);
    }
}
