package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 3 templates of {@code tools/worldgen/Ring3Templates.java} (change 47), as committed: each holds only the
 * blocks its feature places, and fits where its feature puts it (a fossil in one chunk). The script itself writes
 * identical bytes on every run (checked when it is run, see the plan).
 */
class Ring3TemplatesTest {
    private static final String DIR = "src/main/resources/data/corruptedadventure/structure/ring_3";

    @Test
    void fossilsAreOnlyBonesAndFitInAChunkInEveryRotation() throws IOException {
        for (String name : List.of("sea_serpent_1", "sea_serpent_2", "sea_serpent_3", "sea_skull_1", "sea_skull_2")) {
            StructureTemplate template = load(name);
            assertEquals(Set.of(Blocks.BONE_BLOCK), blocks(template), name);
            for (Rotation rotation : Rotation.values()) {
                assertTrue(FossilLayout.fits(SurfaceFossilFeature.bones(template, rotation)), name + " " + rotation);
            }
        }
    }

    @Test
    void everyTemplateFileIsTested() throws IOException {
        try (Stream<Path> files = Files.list(root())) {
            assertEquals(5, files.filter(file -> file.toString().endsWith(".nbt")).count());
        }
    }

    private static Set<Block> blocks(StructureTemplate template) {
        Set<Block> found = new java.util.HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).isEmpty()) {
                found.add(block);
            }
        }
        assertFalse(found.isEmpty());
        return found;
    }

    static StructureTemplate load(String name) throws IOException {
        try (InputStream in = Files.newInputStream(root().resolve(name + ".nbt"))) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(BuiltInRegistries.BLOCK.asLookup(), tag);
            return template;
        }
    }

    private static Path root() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(DIR))) {
                return dir.resolve(DIR);
            }
        }
        throw new IllegalStateException("no " + DIR);
    }
}
