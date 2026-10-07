package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A top-view map: per pixel the ground, the fluid above it, the biome at the ground and the ring; one unit per row. */
final class TopViewWork implements MapWork {
    private final TerrainSource source;
    private final MapArea area;
    private final String name;
    private final List<String> header;
    private final int[] ground;
    private final int[] fluidTop;
    private final byte[] fluid;
    private final Holder<Biome>[] biomes;
    private final byte[] ring;

    @SuppressWarnings("unchecked")
    TopViewWork(TerrainSource source, MapArea area, String name, List<String> header) {
        this.source = source;
        this.area = area;
        this.name = name;
        this.header = header;
        int columns = area.columns();
        this.ground = new int[columns];
        this.fluidTop = new int[columns];
        this.fluid = new byte[columns];
        this.biomes = (Holder<Biome>[]) new Holder[columns];
        this.ring = new byte[columns];
    }

    @Override
    public int units() {
        return area.size();
    }

    @Override
    public int columnsPerUnit() {
        return area.size();
    }

    @Override
    public void sample(TerrainSampler sampler, int row) {
        int z = area.blockZ(row);
        for (int column = 0; column < area.size(); column++) {
            int x = area.blockX(column);
            int index = row * area.size() + column;
            int groundY = sampler.ground(x, z);
            ground[index] = groundY;
            fluidTop[index] = TopView.DRY;
            if (groundY < source.maxY() && sampler.fluid(x, groundY + 1, z) != TerrainSampler.NO_FLUID) {
                int top = groundY + 1;
                while (top < source.maxY() && sampler.fluid(x, top + 1, z) != TerrainSampler.NO_FLUID) {
                    top++;
                }
                fluidTop[index] = top;
                // The surface decides the colour: water above the overworld's lava layer is water.
                fluid[index] = sampler.fluid(x, top, z);
            }
            biomes[index] = sampler.biome(x, Math.max(groundY, source.minY()), z);
            ring[index] = sampler.ringCode(x, z);
        }
    }

    @Override
    public Map<String, BufferedImage> render() {
        BiomeIds.Indexed indexed = BiomeIds.index(biomes);
        Arrays.fill(biomes, null);
        TopView view = new TopView(area.size(), source.minY(), source.maxY(), ground, fluidTop, fluid,
                indexed.indices(), indexed.ids(), ring);
        Map<String, BufferedImage> images = new LinkedHashMap<>();
        images.put(name + "_height.png", TopViewRenderer.height(view, header));
        images.put(name + "_biome.png", TopViewRenderer.biomes(view, header));
        return images;
    }
}
