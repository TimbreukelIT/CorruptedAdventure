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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Review sheet: our textures next to the vanilla blocks and items they meet in the world and in the inventory (decision
 * 37b-6 A). Vanilla is only read, from the Minecraft resources jar ModDevGradle unpacks into
 * {@code build/moddev/artifacts/}, and only drawn into {@code build/texture-preview/}: no vanilla pixels ever reach
 * {@code src/} or git (fase-1-wereld §1.4). Run from the repository root after the texture script of that ring:
 * {@code java tools/textures/VanillaCompare.java} (ring 2, {@code ring2-vs-vanilla.png}) or
 * {@code java tools/textures/VanillaCompare.java ring3} ({@code ring3-vs-vanilla.png}).
 *
 * <p>References: {@code block/name} (ours), {@code mc:block/name} (vanilla), {@code overlay@base} (an ore overlay drawn
 * over its host rock, as the two-layer ore model shows it, e.g. {@code block/sulfur_ore_overlay@mc:block/stone}).
 */
public final class VanillaCompare {
    private static final Path MOD = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path ARTIFACTS = Path.of("build/moddev/artifacts");
    private static final int CELL = 300;
    private static final int ROW = 170;
    private static final int SLOT = 0x8B8B8B;
    private static final int INVENTORY = 0xC6C6C6;
    private static final int BACKDROP = 0x2B2B2B;

    /** A row of the sheet. */
    private record Row(String title, boolean items, List<String> refs) {
    }

    /**
     * One sheet: its output, the ore wall (vanilla ores in our rock on the left, in vanilla's own rock on the right; 7×5
     * blocks) and the rows.
     */
    private record Sheet(Path output, String[] wallHosts, String[] wallOres, List<Row> rows) {
    }

    private static final Sheet RING_2 = new Sheet(Path.of("build/texture-preview/ring2-vs-vanilla.png"),
            new String[] {"block/soaked_netherrack", "mc:block/netherrack"},
            new String[] {"mc:block/nether_quartz_ore", "mc:block/nether_gold_ore", "mc:block/ancient_debris_side",
                    "mc:block/magma", "mc:block/gravel", "mc:block/blackstone"},
            List.of(
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
                            "block/dew_crystal_cluster", "mc:block/amethyst_cluster", "block/spore_sprouts"))));

    /** Ring 3 (57a): the Nether ring 3 ground with vanilla's nether ores, then the ores over their vanilla hosts. */
    private static final Sheet RING_3 = new Sheet(Path.of("build/texture-preview/ring3-vs-vanilla.png"),
            new String[] {"block/rime_netherrack", "mc:block/netherrack"},
            new String[] {"mc:block/nether_quartz_ore", "mc:block/nether_gold_ore", "mc:block/ancient_debris_side",
                    "mc:block/magma", "mc:block/blackstone", "mc:block/packed_ice"},
            List.of(
                    new Row("ash ground and the vanilla rock of ring 3", false, List.of("block/ash_block", "block/ash_crust",
                            "mc:block/basalt_side", "mc:block/blackstone", "mc:block/gravel")),
                    new Row("pumice vs calcite/tuff", false, List.of("block/pumice", "block/polished_pumice", "block/pumice_bricks",
                            "mc:block/calcite", "mc:block/tuff")),
                    new Row("glowing ground", false, List.of("block/smoldering_stone", "block/smoldering_ash", "mc:block/magma",
                            "mc:block/basalt_top", "mc:block/smooth_basalt")),
                    new Row("charred wood vs dark oak", false, List.of("block/charred_log", "block/charred_log_top",
                            "block/stripped_charred_log", "block/charred_planks", "mc:block/dark_oak_log")),
                    new Row("ember woods", false, List.of("block/ember_leaves", "mc:block/dark_oak_planks", "block/charred_door_top",
                            "block/charred_trapdoor", "mc:block/crimson_stem")),
                    new Row("volcanite over stone, deepslate, basalt side/top", false, List.of(
                            "block/volcanite_ore_overlay@mc:block/stone", "block/volcanite_ore_overlay@mc:block/deepslate",
                            "block/volcanite_ore_overlay@mc:block/basalt_side", "block/volcanite_ore_overlay@mc:block/basalt_top",
                            "mc:block/deepslate_iron_ore")),
                    new Row("sulfur over stone, basalt; fire ruby over stone, deepslate", false, List.of(
                            "block/sulfur_ore_overlay@mc:block/stone", "block/sulfur_ore_overlay@mc:block/basalt_side",
                            "block/fire_ruby_ore_overlay@mc:block/stone", "block/fire_ruby_ore_overlay@mc:block/deepslate",
                            "mc:block/redstone_ore")),
                    new Row("fire crystal vs amethyst, sulfur crust, fumarole", false, List.of("block/fire_crystal_block",
                            "mc:block/amethyst_block", "block/sulfur_crust", "block/fumarole_top", "block/fumarole_side")),
                    new Row("steam fens", false, List.of("block/boiling_mud", "block/geyser_top", "block/geyser_side",
                            "mc:block/mud", "mc:block/calcite")),
                    new Row("Nether 3: cold lava, ice", false, List.of("block/cold_lava", "block/polished_cold_lava",
                            "block/cold_lava_bricks", "mc:block/basalt_side", "mc:block/polished_basalt_side")),
                    new Row("Nether 3: ices vs vanilla ice", false, List.of("block/nether_ice", "block/packed_nether_ice",
                            "block/primordial_ice", "mc:block/packed_ice", "mc:block/blue_ice")),
                    new Row("Nether 3: rimeshroom vs warped", false, List.of("block/rimeshroom_stem", "block/rimeshroom_cap",
                            "block/rimeshroom_planks", "mc:block/warped_stem", "mc:block/warped_wart_block")),
                    new Row("raw ores and gems", true, List.of("item/raw_volcanite", "item/sulfur", "item/fire_ruby",
                            "mc:item/raw_iron", "mc:item/emerald")),
                    new Row("crystals", true, List.of("item/fire_crystal", "block/fire_crystal_cluster", "block/large_fire_crystal_bud",
                            "mc:item/amethyst_shard", "mc:block/amethyst_cluster")),
                    new Row("doors and stalactites", true, List.of("item/charred_door", "item/rimeshroom_door", "item/ice_stalactite",
                            "mc:item/dark_oak_door", "mc:item/pointed_dripstone")),
                    new Row("flora", true, List.of("block/fire_bloom", "block/ash_sprouts", "block/ember_berry_bush_stage3",
                            "mc:block/torchflower", "mc:block/sweet_berry_bush_stage3"))));

    private static final Map<String, Sheet> SHEETS = Map.of("ring2", RING_2, "ring3", RING_3);

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        String ring = args.length > 0 ? args[0] : "ring2";
        Sheet sheetSpec = SHEETS.get(ring);
        if (sheetSpec == null) {
            throw new IllegalArgumentException("Unknown sheet " + ring + ", expected one of " + SHEETS.keySet());
        }
        Path jar = findJar();
        try (ZipFile vanilla = new ZipFile(jar.toFile())) {
            int wallScale = 4;
            int block = 16 * wallScale;
            int wallHeight = 5 * block + 40;
            List<Row> rows = sheetSpec.rows();
            BufferedImage sheet = new BufferedImage(5 * CELL + 20, wallHeight + rows.size() * ROW + 10, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = sheet.createGraphics();
            graphics.setColor(new Color(BACKDROP));
            graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            drawWalls(graphics, vanilla, sheetSpec, wallScale);
            int top = wallHeight;
            for (Row row : rows) {
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
            Files.createDirectories(sheetSpec.output().getParent());
            ImageIO.write(sheet, "png", sheetSpec.output().toFile());
            System.out.println("wrote " + sheetSpec.output() + " (vanilla from " + jar.getFileName() + ")");
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

    /**
     * One frame of our texture, of a vanilla one inside the jar, or an overlay drawn over its base. For an animation
     * strip the frame a quarter into the cycle: the first frame of a pulse leans towards the dim tone, so it would show
     * the material darker than it mostly looks.
     */
    private static BufferedImage load(ZipFile vanilla, String ref) throws IOException {
        int at = ref.indexOf('@');
        if (at >= 0) {
            BufferedImage overlay = load(vanilla, ref.substring(0, at));
            BufferedImage base = load(vanilla, ref.substring(at + 1));
            BufferedImage composite = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    int argb = overlay.getRGB(x, y);
                    composite.setRGB(x, y, argb >>> 24 != 0 ? argb : base.getRGB(x, y));
                }
            }
            return composite;
        }
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
        int frames = full.getHeight() / full.getWidth();
        return full.getSubimage(0, frames / 4 * full.getWidth(), full.getWidth(), full.getWidth());
    }

    private static void drawWalls(Graphics2D graphics, ZipFile vanilla, Sheet sheet, int scale) throws IOException {
        int block = 16 * scale;
        int[][] spots = {{1, 1}, {3, 1}, {5, 1}, {1, 3}, {3, 3}, {5, 3}};
        String[] hosts = sheet.wallHosts();
        String[] ores = sheet.wallOres();
        for (int h = 0; h < hosts.length; h++) {
            int left = 10 + h * (7 * block + 20);
            BufferedImage host = load(vanilla, hosts[h]);
            for (int by = 0; by < 5; by++) {
                for (int bx = 0; bx < 7; bx++) {
                    draw(graphics, host, left + bx * block, 30 + by * block, scale);
                }
            }
            for (int i = 0; i < ores.length; i++) {
                draw(graphics, load(vanilla, ores[i]), left + spots[i][0] * block, 30 + spots[i][1] * block, scale);
            }
            StringBuilder names = new StringBuilder();
            for (String ore : ores) {
                names.append(names.isEmpty() ? "" : ", ").append(ore.substring(ore.lastIndexOf('/') + 1));
            }
            label(graphics, hosts[h] + " with " + names, left, 20);
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
