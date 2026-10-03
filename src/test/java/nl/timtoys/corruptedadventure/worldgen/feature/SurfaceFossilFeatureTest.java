package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement core of {@code surface_fossil} ({@link FossilLayout}) with vanilla's own fossil templates (read from
 * the game's data): in the chunk, half buried, never on foreign ground, never floating.
 */
class SurfaceFossilFeatureTest {
    private static final List<String> TEMPLATES = List.of("spine_1", "spine_2", "spine_3", "spine_4", "skull_1", "skull_2", "skull_3", "skull_4");
    private static final List<StructureTemplate> LOADED = new ArrayList<>();

    /**
     * Vanilla's data (structure templates) is not on the test class path: ModDevGradle puts it in a separate artifact,
     * {@code build/moddev/artifacts/*client-extra*.jar} (see CODEBASE.md, vanilla worldgen JSON).
     */
    @BeforeAll
    static void load() throws IOException {
        // Tests run in a sub-directory of the project: search upwards.
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("build/moddev/artifacts"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IOException("no build/moddev/artifacts above " + Path.of("").toAbsolutePath());
        }
        Path jar;
        try (Stream<Path> artifacts = Files.list(dir.resolve("build/moddev/artifacts"))) {
            jar = artifacts.filter(path -> path.getFileName().toString().contains("client-extra")).findFirst()
                    .orElseThrow(() -> new IOException("no client-extra artifact in build/moddev/artifacts"));
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String name : TEMPLATES) {
                ZipEntry entry = zip.getEntry("data/minecraft/structure/fossil/" + name + ".nbt");
                assertNotNull(entry, name);
                try (InputStream in = zip.getInputStream(entry)) {
                    StructureTemplate template = new StructureTemplate();
                    template.load(BuiltInRegistries.BLOCK.asLookup(), NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
                    LOADED.add(template);
                }
            }
        }
    }

    @Test
    void vanillaTemplatesAreOnlyBonesAndFitInAChunkInEveryRotation() {
        for (int i = 0; i < LOADED.size(); i++) {
            StructureTemplate template = LOADED.get(i);
            int all = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(),
                    Blocks.BONE_BLOCK).size();
            assertTrue(all > 0, TEMPLATES.get(i));
            for (Rotation rotation : Rotation.values()) {
                List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(template, rotation);
                assertEquals(all, bones.size(), TEMPLATES.get(i) + " " + rotation);
                assertTrue(FossilLayout.fits(bones), TEMPLATES.get(i) + " " + rotation);
                assertEquals(0, bones.stream().mapToInt(FossilLayout.Bone::x).min().orElseThrow());
                assertEquals(0, bones.stream().mapToInt(FossilLayout.Bone::z).min().orElseThrow());
            }
            // Only bone blocks: the template's size is exactly the bounding box of its bones.
            List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(template, Rotation.NONE);
            assertEquals(template.getSize().getX(), bones.stream().mapToInt(FossilLayout.Bone::x).max().orElseThrow() + 1);
            assertEquals(template.getSize().getY(), bones.stream().mapToInt(FossilLayout.Bone::y).max().orElseThrow() + 1);
            assertEquals(template.getSize().getZ(), bones.stream().mapToInt(FossilLayout.Bone::z).max().orElseThrow() + 1);
        }
    }

    @Test
    void inTheChunkAndHalfBuriedOnFlatSand() {
        for (int n = 0; n < 2000; n++) {
            RandomSource random = RandomSource.create(n);
            List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(LOADED.get(random.nextInt(LOADED.size())), Rotation.values()[random.nextInt(4)]);
            int height = bones.stream().mapToInt(FossilLayout.Bone::y).max().orElseThrow() + 1;
            int cx = random.nextInt(200) - 100;
            int cz = random.nextInt(200) - 100;
            int originX = cx * 16 + random.nextInt(16);
            int originZ = cz * 16 + random.nextInt(16);
            float buried = 0.4F + 0.3F * random.nextFloat();
            List<FossilLayout.Placed> placed = FossilLayout.place(bones, originX, originZ, buried, 1.0F, random, new Sand(65, null));
            assertEquals(bones.size(), placed.size(), "unweathered: every bone, seed " + n);
            int buriedLayers = FossilLayout.buriedLayers(height, buried);
            assertTrue(buriedLayers >= 1 && buriedLayers < height);
            assertTrue(Math.abs(buriedLayers - buried * height) <= 0.5F + 1e-4F, "about the buried share of the height");
            int minY = Integer.MAX_VALUE;
            for (FossilLayout.Placed bone : placed) {
                assertEquals(cx, bone.pos().getX() >> 4, "seed " + n);
                assertEquals(cz, bone.pos().getZ() >> 4, "seed " + n);
                int localX = bone.pos().getX() & 15;
                int localZ = bone.pos().getZ() & 15;
                assertTrue(localX >= FossilLayout.MARGIN && localX <= 15 - FossilLayout.MARGIN, "one block from the chunk edge");
                assertTrue(localZ >= FossilLayout.MARGIN && localZ <= 15 - FossilLayout.MARGIN, "one block from the chunk edge");
                assertTrue(bone.state().is(Blocks.BONE_BLOCK));
                minY = Math.min(minY, bone.pos().getY());
            }
            assertEquals(66 - buriedLayers, minY, "the lowest layers below the ground (top block 65)");
            long below = placed.stream().filter(bone -> bone.pos().getY() <= 65).map(bone -> bone.pos().getY()).distinct().count();
            assertEquals(buriedLayers, below, "seed " + n);
        }
    }

    @Test
    void sinksBelowTheLowestGroundOfItsFootprint() {
        // A slope: the fossil goes below the lowest column, so it is buried deeper uphill and never hangs downhill.
        RandomSource random = RandomSource.create(7);
        List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(LOADED.get(TEMPLATES.indexOf("spine_4")), Rotation.NONE);
        Sand slope = new Sand(65, null) {
            @Override
            public int groundTop(int x, int z) {
                return 65 + (x & 15) / 2;
            }
        };
        List<FossilLayout.Placed> placed = FossilLayout.place(bones, 8, 8, 0.5F, 1.0F, random, slope);
        assertFalse(placed.isEmpty());
        int lowest = placed.stream().mapToInt(bone -> slope.groundTop(bone.pos().getX(), bone.pos().getZ())).min().orElseThrow();
        int height = bones.stream().mapToInt(FossilLayout.Bone::y).max().orElseThrow() + 1;
        assertEquals(lowest + 1 - FossilLayout.buriedLayers(height, 0.5F), placed.stream().mapToInt(bone -> bone.pos().getY()).min().orElseThrow());
    }

    @Test
    void nothingWhenAColumnOfTheFootprintIsNotGround() {
        List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(LOADED.get(TEMPLATES.indexOf("skull_1")), Rotation.NONE);
        for (int n = 0; n < 200; n++) {
            RandomSource random = RandomSource.create(n);
            // One column in the middle of the chunk is salt (or water): the fossil there would lie half on it.
            Sand withSalt = new Sand(65, new BlockPos(8, 65, 8));
            List<FossilLayout.Placed> placed = FossilLayout.place(bones, 8, 8, 0.5F, 1.0F, random, withSalt);
            assertTrue(placed.isEmpty() || placed.stream().noneMatch(bone -> bone.pos().getX() == 8 && bone.pos().getZ() == 8));
        }
        // Centred on the salt column the footprint (6 × 7, every column has a bone) always covers it.
        assertTrue(FossilLayout.place(bones, 8, 8, 0.5F, 1.0F, RandomSource.create(1), new Sand(65, new BlockPos(8, 65, 8))).isEmpty());
    }

    @Test
    void weatheredBonesNeverFloat() {
        for (int n = 0; n < 1000; n++) {
            RandomSource random = RandomSource.create(n);
            List<FossilLayout.Bone> bones = SurfaceFossilFeature.bones(LOADED.get(random.nextInt(LOADED.size())), Rotation.values()[random.nextInt(4)]);
            Sand sand = new Sand(65, null);
            List<FossilLayout.Placed> placed = FossilLayout.place(bones, random.nextInt(16), random.nextInt(16), 0.4F, 0.6F, random, sand);
            Set<BlockPos> all = new HashSet<>();
            placed.forEach(bone -> all.add(bone.pos()));
            // Every bone connects (26-neighbourhood) to one in or on the ground.
            Set<BlockPos> reached = new HashSet<>();
            List<BlockPos> queue = new ArrayList<>(all.stream().filter(pos -> pos.getY() <= 66).toList());
            reached.addAll(queue);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeLast();
                BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1)).forEach(next -> {
                    if (all.contains(next) && reached.add(next.immutable())) {
                        queue.add(next.immutable());
                    }
                });
            }
            assertEquals(all, reached, "seed " + n);
            assertTrue(placed.size() <= bones.size());
        }
    }

    @Test
    void aTemplateWiderThanAChunkMinusTheMarginsNeverFits() {
        List<FossilLayout.Bone> line = new ArrayList<>();
        for (int x = 0; x <= FossilLayout.MAX_SPAN; x++) {
            line.add(new FossilLayout.Bone(x, 0, 0, Blocks.BONE_BLOCK.defaultBlockState()));
        }
        assertFalse(FossilLayout.fits(line));
        assertTrue(FossilLayout.place(line, 8, 8, 0.5F, 1.0F, RandomSource.create(0), new Sand(65, null)).isEmpty());
        assertTrue(FossilLayout.fits(line.subList(0, FossilLayout.MAX_SPAN)));
    }

    /** Flat sand with its top block at {@code top}; {@code salt} (if set) is a column whose top is not ground. */
    private static class Sand implements FossilLayout.Terrain {
        private final int top;
        private final BlockPos salt;

        Sand(int top, BlockPos salt) {
            this.top = top;
            this.salt = salt;
        }

        @Override
        public int groundTop(int x, int z) {
            return top;
        }

        @Override
        public boolean ground(int x, int y, int z) {
            return y <= groundTop(x, z) && (salt == null || ((x & 15) != salt.getX() || (z & 15) != salt.getZ()));
        }

        @Override
        public boolean free(int x, int y, int z) {
            return true;
        }
    }
}
