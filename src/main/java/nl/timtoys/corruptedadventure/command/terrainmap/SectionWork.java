package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/** A vertical section: every block of every column along the line; one unit per column. */
final class SectionWork implements MapWork {
    private final TerrainSource source;
    private final SectionLine line;
    private final String name;
    private final List<String> header;
    private final byte[] cells;
    private final Holder<Biome>[] biomes;
    private final byte[] ring;

    @SuppressWarnings("unchecked")
    SectionWork(TerrainSource source, SectionLine line, String name, List<String> header) {
        this.source = source;
        this.line = line;
        this.name = name;
        this.header = header;
        this.cells = new byte[line.samples() * heightBlocks()];
        this.biomes = (Holder<Biome>[]) new Holder[line.samples()];
        this.ring = new byte[line.samples()];
    }

    private int heightBlocks() {
        return source.maxY() - source.minY() + 1;
    }

    @Override
    public int units() {
        return line.samples();
    }

    @Override
    public int columnsPerUnit() {
        return 1;
    }

    @Override
    public void sample(TerrainSampler sampler, int sample) {
        int x = line.blockX(sample);
        int z = line.blockZ(sample);
        int height = heightBlocks();
        boolean[] solid = new boolean[height];
        byte[] fluid = new byte[height];
        for (int i = 0; i < height; i++) {
            int y = source.maxY() - i;
            solid[i] = sampler.solid(x, y, z);
            fluid[i] = solid[i] ? TerrainSampler.NO_FLUID : sampler.fluid(x, y, z);
        }
        System.arraycopy(SectionView.classify(solid, fluid, source.aquifers()), 0, cells, sample * height, height);
        int ground = TerrainSampler.ground(source, y -> solid[source.maxY() - y], 1);
        biomes[sample] = sampler.biome(x, Math.clamp(ground, source.minY(), source.maxY()), z);
        ring[sample] = sampler.ringCode(x, z);
    }

    @Override
    public Map<String, BufferedImage> render() {
        BiomeIds.Indexed indexed = BiomeIds.index(biomes);
        SectionView view = new SectionView(line.samples(), source.minY(), source.maxY(), source.seaLevel(), cells,
                indexed.indices(), indexed.ids(), ring);
        return Map.of(name + ".png", SectionRenderer.render(view, header));
    }
}
