import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the item textures of phase 2 golf 5 (stream T, design §7.2-§7.4): the sky light draught, the echo shell,
 * the {@code anchored} and {@code resounding} effect icons, the glitch arrow (item and entity), the sky glass lens and its
 * scope overlay. Our own pixels in the palettes of the ring 4/5 materials (sky glass, purity crystal, fossil stone,
 * white core, glitch shard, tidesilver); the arrow entity keeps the vanilla 32 x 32 arrow layout (UVs of the arrow
 * model), the scope the vanilla spyglass scope layout, not their pixels.
 * Run from the repository root: {@code java tools/textures/Golf5ItemTextures.java}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent. Edit the
 * grids by hand and run the script again; the output is byte-for-byte reproducible. Checked before writing: size (16 x
 * 16, 16 x 16 per frame for animations, 18 x 18 for effect icons, 32 x 32 for arrow entities) and every character in
 * the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf5ItemTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    /** {@code frames}: null for a still texture, else the {@code .mcmeta} frame order as {index, ticks} pairs. */
    private record Texture(String name, Map<Character, Integer> palette, int[][] frames, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf5ItemTextures() {
    }

    public static void main(String[] args) throws IOException {
        // the round bottle of the dew draught: sky light inside with glowstone flecks, a purity crystal on the cork
        add("item/sky_light_draught",
                palette("O=1E2A30", "g=8EA2AE", "G=D2E2EA", "H=F6FCFF", "c=8A6A3C", "C=B08A52", "2=6E9ED8", "3=9CC6F0", "4=C8E2FA", "5=F0F8FF", "x=FFE88A", "X=F4EFDF", "p=C6B484"),
                ".........OXO....",
                ".......OOXpO....",
                "......OCcOOO....",
                "......OGgO......",
                "......OGgO......",
                "......OGgO......",
                ".....OGGgGO.....",
                "....OGgggggO....",
                "...OG5H5443gO...",
                "...OGH45x43gO...",
                "..OG545x5432gO..",
                "...OG4x54432gO..",
                "...OG4435432O...",
                "....Og3322gO....",
                ".....OggggO.....",
                "......OOOO......");
        // a spiral shell of fossil stone, the white core glowing in its mouth
        add("item/echo_shell",
                palette("O=3A352C", "1=5C564A", "2=70695B", "3=847C6C", "4=978E7C", "5=A9A08C", "6=BAB19C", "7=E4DCC6", "a=80859A", "b=C4C2D8", "w=EEF2FA", "W=FFFFFF"),
                "................",
                "................",
                "......OOOOO.....",
                "....OO66765OO...",
                "...O676OOO54O...",
                "..O675O543O43O..",
                "..O65O54O3O43O..",
                ".O664O4O.O32O3O.",
                ".O654O43OO3O32O.",
                ".O554O3433O332O.",
                ".O4543O333232OOO",
                "..O4433O2222OwbO",
                "..O34333221OWwaO",
                "...O3322211OObaO",
                "....OOO2111OOaO.",
                ".......OOOOO.OO.");
        // a pale gold anchor, the colour of the effect (18 x 18)
        add("mob_effect/anchored",
                palette("O=3A2E14", "2=8A7034", "3=B89A4C", "4=D8C47A", "5=F4E6A6", "6=FFF6D4", "7=FFFFFF"),
                "..................",
                "........OOO.......",
                ".......O676O......",
                ".......O5O5O......",
                ".......O545O......",
                "....OOOO545OOOO...",
                "....O766655443O...",
                "....OOOO554OOOO...",
                ".......O543O......",
                ".......O543O......",
                "..O....O543O....O.",
                ".O6O...O443O...O4O",
                ".O65O..O443O..O43O",
                "..O55OOO433OOO43O.",
                "...O55544333332O..",
                "....OO4433322OO...",
                "......OOOOOOO.....",
                "..................");
        // a small fossil shell sending out three lavender echo arcs, the colour of the effect (18 x 18)
        add("mob_effect/resounding",
                palette("O=2A2242", "3=6E5CB0", "4=8E7CD4", "5=B9A8F0", "6=DCD2FA", "7=FFFFFF", "f=978E7C", "F=BAB19C", "d=70695B"),
                "..................",
                "...........OO.....",
                "............O6O...",
                "......OO.....O5O..",
                ".......O6O....O5O.",
                "........O5O...O5O.",
                "..OOO....O5O...O5O",
                ".OFFfO...O5O...O4O",
                "OF77FdO..O4O...O4O",
                "OF7FfdO..O4O...O4O",
                ".OfffO...O4O...O4O",
                "..OOO....O4O...O3O",
                "........O3O...O3O.",
                ".......O3O....O3O.",
                "......OO.....O3O..",
                "............O3O...",
                "...........OO.....",
                "..................");
        // glitch shard head with a black facet, fletching split cyan and magenta
        add("item/glitch_arrow",
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "O=14001A", "0=000000", "3=5A0060", "4=B000B8", "5=F800F8", "6=FF70FF", "w=F4F4F4", "a=5A0060", "b=F800F8", "B=00E8F0"),
                "................",
                "............OO..",
                "...........O6wO.",
                "..........O6553O",
                "..........O5043O",
                "..........mn43O.",
                ".........mnOOO..",
                "........mn......",
                ".......mn.......",
                "......mn........",
                "..B..mn.........",
                "..BBmn..........",
                "...bkm..........",
                "..akab..........",
                ".aa..ab.........",
                "................");
        // vanilla arrow entity layout (side quad 16 x 5, back cross 5 x 5), glitch shard head, split fletching
        add("entity/projectiles/glitch_arrow",
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "0=000000", "3=5A0060", "4=B000B8", "5=F800F8", "6=FF70FF", "w=F4F4F4", "b=F800F8", "B=00E8F0"),
                "BBB.............................",
                ".BBB.........6w.................",
                "kkkmnmnnmnn04450................",
                ".bbb.........34.................",
                "bbb.............................",
                "..B.............................",
                "..B.............................",
                "BBnbb...........................",
                "..b.............................",
                "..b.............................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................");
        // a tidesilver spyglass with jade bands, the sky glass lens at the top
        add("item/sky_glass_lens",
                palette("O=1A2830", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "c=2E6A54", "d=44886C", "e=62A688", "A=DCEEFF", "B=BCDCFA", "C=9CC6F0", "W=F4FAFF"),
                "................",
                "...........OOO..",
                "..........O6WAO.",
                ".........O65ABCO",
                ".........O5ABC2O",
                "........O6dBC3O.",
                ".......O65e33O..",
                "......O654dO2O..",
                ".....O6543O.O...",
                "....Oded3O......",
                "...O65433O......",
                "..O65432O.......",
                ".O6c432O........",
                ".Oedc2O.........",
                "..OOOO..........",
                "................");
        List<BufferedImage> written = new ArrayList<>();
        for (Texture texture : TEXTURES_TO_WRITE) {
            written.add(write(texture));
        }
        writeScope();
        contactSheet(written);
    }

    private static void add(String name, Map<Character, Integer> palette, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, palette, null, rows));
    }

    private static void addAnimated(String name, Map<Character, Integer> palette, int[][] frames, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, palette, frames, rows));
    }

    /** {@code "c=RRGGBB"} pairs. */
    private static Map<Character, Integer> palette(String... entries) {
        Map<Character, Integer> palette = new HashMap<>();
        for (String entry : entries) {
            palette.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
        }
        return palette;
    }

    /** {@code index, ticks, index, ticks, ...} → {index, ticks} pairs. */
    private static int[][] frames(int... pairs) {
        int[][] frames = new int[pairs.length / 2][];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new int[]{pairs[2 * i], pairs[2 * i + 1]};
        }
        return frames;
    }

    private static BufferedImage write(Texture texture) throws IOException {
        String name = texture.name();
        int height = texture.rows().length;
        int width = texture.rows()[0].length();
        boolean entity = name.startsWith("entity/projectiles/");
        boolean icon = name.startsWith("mob_effect/");
        int frameCount = 1;
        if (texture.frames() != null) {
            for (int[] frame : texture.frames()) {
                frameCount = Math.max(frameCount, frame[0] + 1);
            }
        }
        int wantWidth = entity ? 32 : icon ? 18 : 16;
        int wantHeight = entity ? 32 : icon ? 18 : 16 * frameCount;
        if (width != wantWidth || height != wantHeight) {
            throw new IllegalStateException(name + ": " + width + " x " + height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            String row = texture.rows()[y];
            if (row.length() != width) {
                throw new IllegalStateException(name + ": row " + y + " has " + row.length() + " pixels");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = texture.palette().get(c);
                if (rgb == null) {
                    throw new IllegalStateException(name + ": '" + c + "' not in the palette");
                }
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        Path file = TEXTURES.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
        if (texture.frames() != null) {
            StringBuilder json = new StringBuilder("{\n  \"animation\": {\n    \"interpolate\": false,\n    \"frames\": [\n");
            for (int i = 0; i < texture.frames().length; i++) {
                int[] frame = texture.frames()[i];
                json.append("      {\"index\": ").append(frame[0]).append(", \"time\": ").append(frame[1]).append('}')
                        .append(i + 1 < texture.frames().length ? ",\n" : "\n");
            }
            json.append("    ]\n  }\n}\n");
            Path meta = TEXTURES.resolve(name + ".png.mcmeta");
            Files.writeString(meta, json.toString(), StandardCharsets.UTF_8);
            System.out.println("wrote " + meta);
        }
        return image;
    }

    /**
     * The scope overlay (256 x 256, the layout of vanilla's spyglass scope): an 8 px dark rim, an 8 px tidesilver rim
     * lit on the right and bottom with jade studs, a soft 4 px inner edge and faint sky glass in 64 px panes with two
     * glints. The only texture here with partial alpha: the glass is see-through.
     */
    private static void writeScope() throws IOException {
        int[] edgeAlpha = {238, 204, 120, 39};
        int[] panes = {0xF4FAFF, 0xDCEEFF, 0xBCDCFA, 0xF4FAFF};
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int edge = Math.min(Math.min(x, y), Math.min(255 - x, 255 - y));
                boolean lit = x > 255 - 16 || y > 255 - 16;
                int argb;
                if (edge < 8) {
                    argb = 0xFF1A2830;
                } else if (edge < 16) {
                    argb = lit && edge < 10 ? 0xFFA4B8BE : 0xFF5A6E78;
                    if ((x / 8 + y / 8) % 8 == 0 && edge >= 10) {
                        argb = 0xFF44886C;
                    }
                } else if (edge < 20) {
                    argb = edgeAlpha[edge - 16] << 24 | (edge < 18 ? 0x4A78B4 : 0xBCDCFA);
                } else {
                    argb = 13 << 24 | panes[(x / 64 + y / 64 * 3) % 4];
                    if (x >= 64 && x < 96 && y >= 64 && y < 96 || x >= 176 && x < 192 && y >= 176 && y < 192) {
                        argb = 27 << 24 | 0xFFFFFF;
                    }
                }
                image.setRGB(x, y, argb);
            }
        }
        Path file = TEXTURES.resolve("misc/sky_glass_lens_scope.png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
    }

    private static void contactSheet(List<BufferedImage> images) throws IOException {
        int width = SCALE;
        int height = 0;
        for (BufferedImage image : images) {
            width += (image.getWidth() + 1) * SCALE;
            height = Math.max(height, image.getHeight() * SCALE);
        }
        BufferedImage sheet = new BufferedImage(width, height + 2 * SCALE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                sheet.setRGB(x, y, 0xFF3C3C40);
            }
        }
        int left = SCALE;
        for (BufferedImage image : images) {
            for (int y = 0; y < image.getHeight() * SCALE; y++) {
                for (int x = 0; x < image.getWidth() * SCALE; x++) {
                    int argb = image.getRGB(x / SCALE, y / SCALE);
                    if (argb >>> 24 != 0) {
                        sheet.setRGB(left + x, SCALE + y, argb);
                    }
                }
            }
            left += (image.getWidth() + 1) * SCALE;
        }
        Path preview = Path.of("build/texture-preview/" + Golf5ItemTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
