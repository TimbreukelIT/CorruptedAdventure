package nl.timtoys.corruptedadventure.worldgen.chunk;

import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vanilla's sky (opt/aquifer) against vanilla's own noise aquifer on the overworld's terrain, alone and in {@link RingAquifer}. */
class VanillaSkyTest {
    private static final HolderLookup.Provider VANILLA = VanillaRegistries.createLookup();
    private static final long SEED = 20260930L;
    /** No structure pieces: the terrain as the noise makes it. */
    private static final Beardifier NO_STRUCTURES = new Beardifier(ObjectLists.<Beardifier.Rigid>emptyList().iterator(),
            ObjectLists.<JigsawJunction>emptyList().iterator());

    @Test
    void vanillasNoiseAquiferAnswersAirInTheSky() {
        NoiseGeneratorSettings settings = VANILLA.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        RandomState random = RandomState.create(settings, VANILLA.lookupOrThrow(Registries.NOISE), SEED);
        // The overworld's global picker as NoiseBasedChunkGenerator makes it.
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        Aquifer.FluidPicker picker = (x, y, z) -> y < Math.min(-54, settings.seaLevel()) ? lava : sea;
        NoiseSettings heights = settings.noiseSettings();
        int top = heights.minY() + heights.height() - 1;
        long skyBlocks = 0;
        int lowSky = 0;
        int[][] chunks = {{0, 0}, {100, 3}, {-210, 47}, {37, -400}, {311, 290}, {-512, -505}, {640, -20}, {-75, 800}};
        for (int[] at : chunks) {
            ChunkPos chunk = new ChunkPos(at[0], at[1]);
            NoiseChunk noise = new NoiseChunk(16 / heights.getCellWidth(), random, chunk.getMinBlockX(), chunk.getMinBlockZ(), heights,
                    NO_STRUCTURES, settings, picker, Blender.empty());
            Aquifer.NoiseBasedAquifer aquifer = (Aquifer.NoiseBasedAquifer) noise.aquifer;
            VanillaSky sky = new VanillaSky(aquifer, (x, z) -> -54, chunk, heights.minY(), heights.height());
            assertEquals(VanillaSky.NONE, sky.from(chunk.getMinBlockX() - 1, chunk.getMinBlockZ(), Integer.MAX_VALUE), "outside the chunk");
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int from = sky.from(x, z, Integer.MAX_VALUE);
                    if (from == VanillaSky.NONE) {
                        continue;
                    }
                    lowSky += from < 100 ? 1 : 0;
                    for (int y = from; y <= top; y++) {
                        for (double substance : new double[]{0.0, -0.05, -0.6}) {
                            BlockState state = aquifer.computeSubstance(new DensityFunction.SinglePointContext(x, y, z), substance);
                            assertSame(Blocks.AIR.defaultBlockState(), state, x + " " + y + " " + z + " (sky from " + from + ")");
                        }
                        skyBlocks++;
                    }
                }
            }
        }
        // Not vacuous: the sky reaches down to the hills and the sea in most columns.
        assertTrue(lowSky > chunks.length * 256 / 2, lowSky + " columns with the sky below Y 100");
        assertTrue(skyBlocks > chunks.length * 256 * 150L, skyBlocks + " sky blocks");
    }

    /**
     * The sky in {@link RingAquifer}'s profile against a ring aquifer that asks vanilla everywhere: the same state at every
     * block (and the same fluid update flag where it places a fluid), in rings 1–3 and the zones between them; the cells
     * {@link VanillaSky} places are where vanilla places them; and both of vanilla's aquifers place nothing in rock.
     */
    @Test
    void theAquifersSkyGivesWhatAskingVanillaGives() {
        NoiseGeneratorSettings settings = VANILLA.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        RandomState random = RandomState.create(settings, VANILLA.lookupOrThrow(Registries.NOISE), SEED);
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        Aquifer.FluidPicker picker = (x, y, z) -> y < Math.min(-54, settings.seaLevel()) ? lava : sea;
        NoiseSettings heights = settings.noiseSettings();
        RingPosition[] positions = {RingPosition.within(Ring.RING_1), RingPosition.within(Ring.RING_2), RingPosition.within(Ring.RING_3),
                new RingPosition(Ring.RING_1, Ring.RING_2, 0.4), new RingPosition(Ring.RING_2, Ring.RING_3, 0.6)};
        RingAquifer.RingLookup rings = (x, z) -> positions[Math.floorMod(Math.floorDiv(x, 3) + Math.floorDiv(z, 5), positions.length)];
        RingFluidPicker.Ring5Bands bands = (x, z) -> 0.0;
        long compared = 0;
        int cells = 0;
        for (int[] at : new int[][]{{0, 0}, {-210, 47}, {311, 290}}) {
            ChunkPos chunk = new ChunkPos(at[0], at[1]);
            Aquifer.NoiseBasedAquifer withSky = (Aquifer.NoiseBasedAquifer) new NoiseChunk(16 / heights.getCellWidth(), random,
                    chunk.getMinBlockX(), chunk.getMinBlockZ(), heights, NO_STRUCTURES, settings, picker, Blender.empty()).aquifer;
            Aquifer.NoiseBasedAquifer alone = (Aquifer.NoiseBasedAquifer) new NoiseChunk(16 / heights.getCellWidth(), random,
                    chunk.getMinBlockX(), chunk.getMinBlockZ(), heights, NO_STRUCTURES, settings, picker, Blender.empty()).aquifer;
            VanillaSky sky = new VanillaSky(withSky, (x, z) -> -54, chunk, heights.minY(), heights.height());
            RingAquifer profiled = new RingAquifer(Level.OVERWORLD, withSky, null, null, rings, bands, sky, null, heights.minY(), heights.height());
            RingAquifer asking = new RingAquifer(Level.OVERWORLD, alone, null, null, rings, bands, null, null, heights.minY(), heights.height());
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    for (int y = heights.minY(); y < heights.minY() + heights.height(); y++) {
                        for (double substance : new double[]{0.0, -0.3}) {
                            DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, y, z);
                            BlockState expected = asking.computeSubstance(context, substance);
                            boolean flag = asking.shouldScheduleFluidUpdate();
                            BlockState actual = profiled.computeSubstance(context, substance);
                            assertSame(expected, actual, x + " " + y + " " + z + " " + rings.at(x, z));
                            if (expected != null && !expected.getFluidState().isEmpty()) {
                                assertEquals(flag, profiled.shouldScheduleFluidUpdate(), "flag at " + x + " " + y + " " + z);
                            }
                            compared++;
                        }
                    }
                }
            }
            for (int i = 0; i < withSky.aquiferLocationCache.length; i++) {
                if (withSky.aquiferLocationCache[i] != Long.MAX_VALUE && alone.aquiferLocationCache[i] != Long.MAX_VALUE) {
                    assertEquals(alone.aquiferLocationCache[i], withSky.aquiferLocationCache[i], "cell " + i);
                    cells++;
                }
            }
            DensityFunction.FunctionContext rock = new DensityFunction.SinglePointContext(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
            assertNull(withSky.computeSubstance(rock, 0.5));
            assertFalse(withSky.shouldScheduleFluidUpdate());
            assertNull(Aquifer.createDisabled(picker).computeSubstance(rock, 0.5));
        }
        assertTrue(cells > 100, cells + " cells compared");
        System.out.println("sky in the profile: " + compared + " blocks, " + cells + " cells compared");
    }

    @Test
    void theSkyStaysAboveTheGlobalFluidsLava() {
        NoiseGeneratorSettings settings = VANILLA.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        RandomState random = RandomState.create(settings, VANILLA.lookupOrThrow(Registries.NOISE), SEED);
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        NoiseSettings heights = settings.noiseSettings();
        ChunkPos chunk = new ChunkPos(0, 0);
        NoiseChunk noise = new NoiseChunk(16 / heights.getCellWidth(), random, 0, 0, heights,
                NO_STRUCTURES, settings, (x, y, z) -> sea, Blender.empty());
        VanillaSky sky = new VanillaSky((Aquifer.NoiseBasedAquifer) noise.aquifer, (x, z) -> 150, chunk, heights.minY(), heights.height());
        int from = sky.from(3, 3, Integer.MAX_VALUE);
        assertTrue(from >= 150, "sky from " + from);
        // Where the global lava reaches the top of what is asked, the cells are not looked at.
        assertEquals(VanillaSky.NONE, sky.from(3, 3, 150));
    }
}
