import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Review sheet: our ring 2 and Nether ring 2 textures next to the vanilla blocks and items they meet in the world and in
 * the inventory (decision 37b-6 A). Vanilla is only read, from the Minecraft resources jar ModDevGradle unpacks into
 * {@code build/moddev/artifacts/}, and only drawn into {@code build/texture-preview/ring2-vs-vanilla.png}: no vanilla
 * pixels ever reach {@code src/} or git (fase-1-wereld §1.4). Run from the repository root after
 * {@code java tools/textures/Ring2Textures.java}: {@code java tools/textures/VanillaCompare.java}.
 */
public final class VanillaCompare {
    private static final Path MOD = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path ARTIFACTS = Path.of("build/moddev/artifacts");
    private static final Path OUTPUT = Path.of("build/texture-preview/ring2-vs-vanilla.png");
    private static final int CELL = 300;
    private static final int ROW = 170;
    private static final int SLOT = 0x8B8B8B;
    private static final int INVENTORY = 0xC6C6C6;
    private static final int BACKDROP = 0x2B2B2B;

    /** A row of the sheet: references are {@code block/name} (ours) or {@code mc:block/name} (vanilla). */
    private record Row(String title, boolean items, List<String> refs) {
    }

    private static final List<Row> ROWS = List.of(
            new Row("Nether ring 2 ground and its vanilla neighbours", false, List.of("block/soaked_netherrack", "mc:block/netherrack",
                    "mc:block/nether_quartz_ore", "mc:block/nether_gold_ore", "mc:block/ancient_debris_side")),
            new Row("spore moss vs nylium", false, List.of("block/spore_moss", "block/spore_moss_side", "mc:block/warped_nylium",
                    "mc:block/warped_nylium_side", "mc:block/magma")),
            new Row("dripshroom vs crimson/warped", false, List.of("block/dripshroom_stem", "block/stripped_dripshroom_stem",
                    "mc:block/warped_stem", "mc:block/crimson_stem", "mc:block/stripped_warped_stem")),
            new Row("planks, cap, lamp", false, List.of("block/dripshroom_planks", "mc:block/warped_planks", "block/dripshroom_cap",
                    "mc:block/warped_wart_block", "block/dripshroom_lamp")),
            new Row("doors, stalactite, dew", false, List.of("block/dripshroom_door_top", "block/dripshroom_trapdoor",
                    "mc:block/shroomlight", "block/slime_stalactite_down_middle", "mc:block/pointed_dripstone_down_middle")),
            new Row("canyon stone vs vanilla", false, List.of("block/red_canyon_stone", "block/orange_canyon_stone",
                    "block/cream_canyon_stone", "mc:block/red_sandstone", "mc:block/red_sand")),
            new Row("cobbles, bricks, clay", false, List.of("block/canyon_cobblestone", "mc:block/cobblestone",
                    "block/canyon_stone_bricks", "mc:block/stone_bricks", "block/cracked_clay")),
            new Row("scorched sandstone, palm", false, List.of("block/scorched_sandstone", "block/scorched_sandstone_top",
                    "mc:block/sandstone", "block/palm_log", "block/palm_door_top")),
            new Row("raw ores and gems", true, List.of("item/raw_sunstone", "item/raw_turquoise", "mc:item/raw_gold",
                    "mc:item/raw_copper", "mc:item/amethyst_shard")),
            new Row("crystals and dusts", true, List.of("item/dew_crystal", "item/salt", "mc:item/quartz", "mc:item/sugar",
                    "item/slime_stalactite")),
            new Row("doors", true, List.of("item/palm_door", "item/dripshroom_door", "mc:item/jungle_door", "mc:item/warped_door",
                    "mc:item/pointed_dripstone")),
            new Row("flat block items ring 2", true, List.of("block/dry_shrub", "block/desert_flower", "block/palm_sapling",
                    "mc:block/dead_bush", "mc:block/poppy")),
            new Row("crystal stages and sprouts", true, List.of("block/salt_crystal", "block/large_dew_crystal_bud",
                    "block/dew_crystal_cluster", "mc:block/amethyst_cluster", "block/spore_sprouts")));

    /** The ore wall: vanilla ores in our rock (left) and in vanilla netherrack (right), 7×5 blocks. */
    private static final String[] WALL_HOSTS = {"block/soaked_netherrack", "mc:block/netherrack"};
    private static final String[] WALL_ORES = {"mc:block/nether_quartz_ore", "mc:block/nether_gold_ore",
            "mc:block/ancient_debris_side", "mc:block/magma", "mc:block/gravel", "mc:block/blackstone"};

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Path jar = findJar();
        try (ZipFile vanilla = new ZipFile(jar.toFile())) {
            int wallScale = 4;
            int block = 16 * wallScale;
            int wallHeight = 5 * block + 40;
            BufferedImage sheet = new BufferedImage(5 * CELL + 20, wallHeight + ROWS.size() * ROW + 10, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = sheet.createGraphics();
            graphics.setColor(new Color(BACKDROP));
            graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            drawWalls(graphics, vanilla, wallScale);
            int top = wallHeight;
            for (Row row : ROWS) {
                int left = 10;
                for (String ref : row.refs()) {
                    BufferedImage image = load(vanilla, ref);
                    label(graphics, ref.replace("mc:", "MC ").replace("block/", "").replace("item/", ""), left, top + 11);
                    if (row.items()) {
                        drawSlot(graphics, image, left, top + 16);
                    } else {
                        draw(graphics, image, left, top + 16, 8);
                        for (int ty = 0; ty < 3; ty++) {
                            for (int tx = 0; tx < 3; tx++) {
                                draw(graphics, image, left + 134 + tx * 48, top + 16 + ty * 48, 3);
                            }
                        }
                    }
                    left += CELL;
                }
                graphics.setColor(new Color(0xFFD080));
                graphics.drawString(row.title(), 5 * CELL - 280, top + 160);
                top += ROW;
            }
            graphics.dispose();
            Files.createDirectories(OUTPUT.getParent());
            ImageIO.write(sheet, "png", OUTPUT.toFile());
            System.out.println("wrote " + OUTPUT + " (vanilla from " + jar.getFileName() + ")");
        }
    }

    /** The one Minecraft resources jar; with several (an old NeoForge version left behind) it refuses to guess. */
    private static Path findJar() throws IOException {
        List<Path> found = new ArrayList<>();
        if (Files.isDirectory(ARTIFACTS)) {
            try (DirectoryStream<Path> jars = Files.newDirectoryStream(ARTIFACTS, "*client-extra*.jar")) {
                jars.forEach(found::add);
            }
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("No Minecraft resources jar in " + ARTIFACTS + ": run ./gradlew build first");
        }
        if (found.size() > 1) {
            throw new IllegalStateException("Several Minecraft resources jars in " + ARTIFACTS + ", remove the old ones: " + found);
        }
        return found.get(0);
    }

    /** The rest frame (top square) of our texture or of a vanilla one inside the jar. */
    private static BufferedImage load(ZipFile vanilla, String ref) throws IOException {
        BufferedImage full;
        if (ref.startsWith("mc:")) {
            ZipEntry entry = vanilla.getEntry("assets/minecraft/textures/" + ref.substring(3) + ".png");
            if (entry == null) {
                throw new IllegalStateException("not in the jar: " + ref);
            }
            try (InputStream in = vanilla.getInputStream(entry)) {
                full = ImageIO.read(in);
            }
        } else {
            full = ImageIO.read(MOD.resolve(ref + ".png").toFile());
        }
        return full.getSubimage(0, 0, full.getWidth(), full.getWidth());
    }

    private static void drawWalls(Graphics2D graphics, ZipFile vanilla, int scale) throws IOException {
        int block = 16 * scale;
        int[][] spots = {{1, 1}, {3, 1}, {5, 1}, {1, 3}, {3, 3}, {5, 3}};
        for (int h = 0; h < WALL_HOSTS.length; h++) {
            int left = 10 + h * (7 * block + 20);
            BufferedImage host = load(vanilla, WALL_HOSTS[h]);
            for (int by = 0; by < 5; by++) {
                for (int bx = 0; bx < 7; bx++) {
                    draw(graphics, host, left + bx * block, 30 + by * block, scale);
                }
            }
            for (int i = 0; i < WALL_ORES.length; i++) {
                draw(graphics, load(vanilla, WALL_ORES[i]), left + spots[i][0] * block, 30 + spots[i][1] * block, scale);
            }
            label(graphics, WALL_HOSTS[h] + " with quartz, gold, debris / magma, gravel, blackstone", left, 20);
        }
    }

    /** An item as the inventory shows it: 8× on slot grey, then 2× and 1× in a slot, then 5× on a light backdrop. */
    private static void drawSlot(Graphics2D graphics, BufferedImage image, int left, int top) {
        fill(graphics, SLOT, left, top, 128, 128);
        draw(graphics, image, left, top, 8);
        fill(graphics, INVENTORY, left + 134, top, 40, 40);
        fill(graphics, SLOT, left + 138, top + 4, 32, 32);
        draw(graphics, image, left + 138, top + 4, 2);
        fill(graphics, SLOT, left + 134, top + 46, 24, 24);
        draw(graphics, image, left + 138, top + 50, 1);
        fill(graphics, 0x7A9EC2, left + 180, top, 100, 100);
        draw(graphics, image, left + 186, top + 6, 5);
    }

    private static void draw(Graphics2D graphics, BufferedImage image, int left, int top, int scale) {
        int size = image.getWidth();
        int step = scale * 16 / size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = image.getRGB(x, y);
                if (argb >>> 24 == 0) {
                    continue;
                }
                fill(graphics, argb & 0xFFFFFF, left + x * step, top + y * step, step, step);
            }
        }
    }

    private static void fill(Graphics2D graphics, int rgb, int left, int top, int width, int height) {
        graphics.setColor(new Color(rgb));
        graphics.fillRect(left, top, width, height);
    }

    private static void label(Graphics2D graphics, String text, int left, int baseline) {
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, left, baseline);
    }

    private VanillaCompare() {
    }
}
