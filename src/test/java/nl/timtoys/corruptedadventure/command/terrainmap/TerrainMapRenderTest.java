package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing4Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainMapRenderTest {
    @Test
    void heightRampRunsFromTheBottomToTheTopColour() {
        assertEquals(0x0D0887, MapPalette.height(-64, -64, 319));
        assertEquals(0xFFFFFF, MapPalette.height(319, -64, 319));
        assertEquals(0xFFFFFF, MapPalette.height(400, -64, 319));
        assertNotEquals(MapPalette.height(100, -64, 319), MapPalette.height(140, -64, 319));
    }

    @Test
    void biomeColoursAreFixedForTheModAndStableForOthers() {
        assertEquals(0x6E1E14, MapPalette.biome("corruptedadventure:volcanic_peaks"));
        assertTrue(MapPalette.hasFixedColour("corruptedadventure:dormant_caldera"));
        assertFalse(MapPalette.hasFixedColour("minecraft:plains"));
        assertEquals(MapPalette.biome("minecraft:plains"), MapPalette.biome("minecraft:plains"));
        assertNotEquals(MapPalette.biome("minecraft:plains"), MapPalette.biome("minecraft:desert"));
    }

    @Test
    void everyRing4AndNether4BiomeHasAFixedColourOfItsOwn() {
        List<ResourceKey<Biome>> keys = new ArrayList<>(Ring4Biomes.WITH_TRANSITION);
        keys.addAll(NetherRing4Biomes.ALL);
        Set<Integer> colours = new HashSet<>();
        for (ResourceKey<Biome> key : keys) {
            String id = key.location().toString();
            assertTrue(MapPalette.hasFixedColour(id), id);
            assertTrue(colours.add(MapPalette.biome(id)), "colour of " + id + " is not unique among ring 4");
        }
    }

    @Test
    void deeperFluidIsDarker() {
        assertTrue((MapPalette.fluid(false, 30) & 0xFF) < (MapPalette.fluid(false, 1) & 0xFF));
    }

    @Test
    void contoursAndTheCloudLineFollowTheirLevels() {
        assertTrue(TopViewRenderer.contour(63, 64));
        assertFalse(TopViewRenderer.contour(64, 79));
        assertTrue(TopViewRenderer.contour(-1, 0));
        assertTrue(TopViewRenderer.crosses(191, 192, MapPalette.CLOUD_Y));
        assertFalse(TopViewRenderer.crosses(192, 230, MapPalette.CLOUD_Y));
    }

    @Test
    void heightPixelsShowFluidContoursCloudsAndRingBoundaries() {
        // 3 x 3: a slope from 60 to 200 west to east, water in the north-west corner, ring 2 only in the south-east corner.
        int[] ground = {60, 120, 200, 60, 120, 200, 60, 120, 200};
        int[] fluidTop = new int[9];
        Arrays.fill(fluidTop, TopView.DRY);
        fluidTop[0] = 62;
        byte[] fluid = new byte[9];
        fluid[0] = TerrainSampler.WATER;
        byte[] ring = {0, 0, 0, 0, 0, 0, 0, 0, 1};
        TopView view = new TopView(3, -64, 319, ground, fluidTop, fluid, new int[9], List.of("minecraft:plains"), ring);

        assertEquals(MapPalette.fluid(false, 2), TopViewRenderer.heightPixel(view, 0, 0));
        assertEquals(MapPalette.CLOUD_LINE, TopViewRenderer.heightPixel(view, 1, 1));
        assertEquals(MapPalette.RING_LINE_ON_HEIGHT, TopViewRenderer.heightPixel(view, 1, 2));
        assertEquals(MapPalette.height(200, -64, 319), TopViewRenderer.heightPixel(view, 2, 2));
        // Dry 60 next to dry 120: a contour.
        assertEquals(MapPalette.scale(MapPalette.height(60, -64, 319), 0.6), TopViewRenderer.heightPixel(view, 0, 2));
    }

    @Test
    void imagesHaveTheMapPlusTheLegendAndAreDeterministic() {
        int size = 16;
        int[] ground = new int[size * size];
        for (int i = 0; i < ground.length; i++) {
            ground[i] = 50 + (i % size) * 10;
        }
        int[] fluidTop = new int[ground.length];
        Arrays.fill(fluidTop, TopView.DRY);
        int[] biome = new int[ground.length];
        Arrays.fill(biome, ground.length / 2, ground.length, 1);
        byte[] ring = new byte[ground.length];
        TopView view = new TopView(size, -64, 319, ground, fluidTop, new byte[ground.length], biome,
                List.of("corruptedadventure:ashen_wastes", "minecraft:plains"), ring);

        BufferedImage height = TopViewRenderer.height(view, List.of("test"));
        BufferedImage biomes = TopViewRenderer.biomes(view, List.of("test"));
        assertEquals(size + LegendPanel.WIDTH, height.getWidth());
        assertTrue(height.getHeight() >= size);
        assertEquals(MapPalette.biome("corruptedadventure:ashen_wastes"), biomes.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(MapPalette.biome("minecraft:plains"), biomes.getRGB(0, size - 1) & 0xFFFFFF);
        assertArrayEquals(pixels(height), pixels(TopViewRenderer.height(view, List.of("test"))));
    }

    @Test
    void sectionClassifiesSkyCaveAndFluid() {
        // top first: 2 open, solid, open, solid
        boolean[] solid = {false, false, true, false, true};
        byte[] water = {TerrainSampler.NO_FLUID, TerrainSampler.WATER, TerrainSampler.NO_FLUID, TerrainSampler.WATER,
                TerrainSampler.NO_FLUID};
        assertArrayEquals(new byte[]{SectionView.SKY, SectionView.WATER, SectionView.STONE, SectionView.CAVE, SectionView.STONE},
                SectionView.classify(solid, water, true));
        // Without aquifers (Nether) the generator's fluid fills every opening below its level.
        assertArrayEquals(new byte[]{SectionView.SKY, SectionView.WATER, SectionView.STONE, SectionView.WATER, SectionView.STONE},
                SectionView.classify(solid, water, false));
        byte[] lava = {0, 0, 0, TerrainSampler.LAVA, 0};
        assertEquals(SectionView.LAVA, SectionView.classify(solid, lava, true)[3]);
    }

    @Test
    void sectionImageMarksTheRingBoundaryColumn() {
        int height = 10;
        byte[] cells = new byte[3 * height];
        SectionView view = new SectionView(3, 0, height - 1, 4, cells, new int[3], List.of("minecraft:plains"),
                new byte[]{1, 1, 2});
        BufferedImage image = SectionRenderer.render(view, List.of("test"));
        int top = SectionRenderer.STRIP + SectionRenderer.GAP;
        assertEquals(MapPalette.RING_LINE_ON_HEIGHT, image.getRGB(SectionRenderer.AXIS + 2, top + 1) & 0xFFFFFF);
        assertEquals(MapPalette.STONE, image.getRGB(SectionRenderer.AXIS + 1, top + 1) & 0xFFFFFF);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
