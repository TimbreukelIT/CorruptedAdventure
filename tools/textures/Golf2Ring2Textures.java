import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the ring 2 and Nether-1 textures of phase 2 golf 2 (stream T, design §5.3-§5.6): the canteen (empty and
 * filled), cactus juice, salted jerky, the {@code cooled} effect icon, the soul crystal block, the turquoise inlaid
 * sandstone (side and top; stairs, slab and wall reuse them) and the salt lamp. Our own pixels and our own textures
 * (scorched sandstone as the base of the inlay, the salt block's pattern for the lamp); no vanilla pixels read or
 * copied. Run from the repository root: {@code java tools/textures/Golf2Ring2Textures.java}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf2Ring2Textures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf2Ring2Textures() {
    }

    public static void main(String[] args) throws IOException {
        // dry, paler leather flask; open neck, the cork hangs on its string
        add("item/canteen_empty", null,
                palette("O=24140A", "1=4F2F18", "2=6E4524", "3=8E5E34", "s=E2C08A", "r=9C8452", "R=C4AE78", "c=8A6A3C", "C=B08A52", "k=14100C", "e=7E6650", "f=9C8468", "h=5E4A38"),
                "....ORrrrrrO....",
                "...ORO2kk1OrO...",
                "...ORO2211OrO...",
                "....OR2211rOO...",
                "....Of3211fOrOO.",
                "...OfffsfsseOCcO",
                "..OfsfffffeeeccO",
                "..OffffffeessOO.",
                ".OfsffffeeeehhO.",
                ".OfsfffeeeehshO.",
                ".OffffeeeehhshO.",
                "..OsseeeehhhhO..",
                "..OfeeeehhhshO..",
                "...OesshshhhO...",
                "....OehhhhhO....",
                ".....OOOOOO.....");
        // leather flask with a stitched seam, corked, a cold bead on the neck
        add("item/canteen", null,
                palette("O=24140A", "1=4F2F18", "2=6E4524", "3=8E5E34", "4=AE7A48", "5=C99A66", "s=E2C08A", "r=9C8452", "R=C4AE78", "c=8A6A3C", "C=B08A52", "W=A8D0FF"),
                "....ORrCcrrO....",
                "...OROCCccOrO...",
                "...ORO2211OrO...",
                "....OR2211rO....",
                "....O532114O....",
                "...O555s4Ws3O...",
                "..O5s55444333O..",
                "..O55544433ssO..",
                ".O5s5444333322O.",
                ".O5s44433332s2O.",
                ".O5444333322s2O.",
                "..Oss33332222O..",
                "..O43333222s2O..",
                "...O3ss2s222O...",
                "....O322222O....",
                ".....OOOOOO.....");
        // round glass bottle, green juice, cork with a cactus flower
        add("item/cactus_juice", null,
                palette("O=1E2A30", "g=8EA2AE", "G=D2E2EA", "H=F6FCFF", "2=3E7A2A", "3=5E9E3A", "4=86C25A", "5=B4E07E", "c=8A6A3C", "C=B08A52", "p=E86FA0", "P=FFB0CC"),
                ".......OOpPO....",
                "......OCcOpO....",
                "......OCcOO.....",
                "......OGgO......",
                "......OGgO......",
                "......OGgO......",
                ".....OGGgGO.....",
                "....OGgggggO....",
                "...OG5H4443gO...",
                "...OGH44433gO...",
                "..OG54443322gO..",
                "...OG443322gO...",
                "...OG433222gO...",
                "....Og3222gO....",
                ".....OggggO.....",
                "......OOOO......");
        // three strips of dried meat with salt crystals
        add("item/salted_jerky", null,
                palette("O=220A08", "2=6A2618", "3=8A3A24", "4=A8503A", "5=C46E52", "s=F2EEE6", "S=CFC8B8"),
                "................",
                "................",
                "................",
                "................",
                "..........O.....",
                ".........O5O....",
                ".......OO4SOO...",
                "......O553OOsOOO",
                "....OO5s2OO42O55",
                "...O543OOS53O433",
                "..O5S2OO523s52OO",
                ".O53OOs43O533O..",
                ".O2OO523S42OO...",
                "..OO53O533O.....",
                "...O3O52OO......",
                "....OO3O........");
        // a cool drop with a frost sparkle (18 x 18 like the vanilla effect icons)
        add("mob_effect/cooled", null,
                palette("O=0F2A4A", "1=1F4F86", "2=3A7FC4", "3=6FB4EA", "4=B0E0FF", "5=EAF8FF"),
                "..................",
                "..............O...",
                "........OO...O5O..",
                ".......O44O.O555O.",
                ".......O44O..O5O..",
                "......O4443O..O...",
                ".....O444333O.....",
                "....O44433332O....",
                "...O4443333222O...",
                "...O4444332222O...",
                "...O4453322221O...",
                "...O4353322221O...",
                "...O3343222211O...",
                "...O3332222111O...",
                "....O32222111O....",
                ".....O222211O.....",
                "......OOOOOO......",
                "..................");
        // fused crystal facets, bright rim on the lit side, tileable
        add("block/soul_crystal_block", null,
                palette("0=092937", "1=125B6D", "2=23909C", "3=4AB1B3", "4=83C1BE", "5=BCD1D1", "6=DDF9F9"),
                "0000001533322221",
                "6443331533222220",
                "4433331532222210",
                "4333332022222116",
                "3333322042221116",
                "3333210601211115",
                "1332105546111060",
                "0121655444404443",
                "3100554444404433",
                "3050644444164333",
                "1034064443163333",
                "0322214433033333",
                "5222221331033331",
                "2222211131033320",
                "2222111105500114",
                "2221111533332414");
        // scorched sandstone with a carved band of turquoise diamonds
        add("block/turquoise_inlaid_sandstone", "block/scorched_sandstone",
                palette("g=8A6B45", "k=4A3A2A", "K=6B5236", "1=1C6B66", "2=2E9C92", "3=4FC6B5", "4=8FE6D6"),
                "................",
                "................",
                "................",
                "................",
                "kkkkkkkkkkkkkkkk",
                "ggg43gggggg43ggg",
                "gg4332gggg4332gg",
                "g443321gg443321g",
                "g433211gg433211g",
                "gg3211gggg3211gg",
                "ggg21gggggg21ggg",
                "KKKKKKKKKKKKKKKK",
                "................",
                "................",
                "................",
                "................");
        // scorched sandstone top: carved frame, turquoise diamond, corner studs
        add("block/turquoise_inlaid_sandstone_top", "block/scorched_sandstone_top",
                palette("g=8A6B45", "k=4A3A2A", "K=6B5236", "1=1C6B66", "2=2E9C92", "3=4FC6B5", "4=8FE6D6"),
                ".k............K.",
                "kkkkkkkkkkkkkkkk",
                ".k............K.",
                ".k.2...gg...2.K.",
                ".k....g33g....K.",
                ".k...g3332g...K.",
                ".k..g333222g..K.",
                ".k.g33344221g.K.",
                ".k.g33244211g.K.",
                ".k..g222211g..K.",
                ".k...g2211g...K.",
                ".k....g11g....K.",
                ".k.2...gg...2.K.",
                ".k............K.",
                "KkKKKKKKKKKKKKKK",
                ".k............K.");
        // warm salt lit from within (salt block pattern), wooden foot
        add("block/salt_lamp", null,
                palette("0=7A3420", "1=9E4C2C", "2=C2673A", "3=DC8A55", "4=F0AE78", "5=FFD3A6", "w=3E2716", "W=5C3B22", "v=7A5232", "V=94673E"),
                "0202222111121000",
                "0012112212122100",
                "0211111121221210",
                "1222112332222221",
                "2212122323323322",
                "1211123333323221",
                "1222222354433222",
                "1121233242343221",
                "3111223333333224",
                "1212233443423223",
                "0121222242222221",
                "1222222322111211",
                "0000000000000000",
                "vVVVVvVVVVvVVVVv",
                "WvvvvvvWvvvvvvWv",
                "wwwwwwwwwwwwwwww");
        // warm salt, brightest in the middle
        add("block/salt_lamp_top", null,
                palette("0=7A3420", "1=9E4C2C", "2=C2673A", "3=DC8A55", "4=F0AE78", "5=FFD3A6", "6=FFEFD8"),
                "0201222111120000",
                "0012112212122100",
                "0211112232221210",
                "1222223333332221",
                "2213222323334322",
                "1212224444334221",
                "1233233465534322",
                "1132244353444321",
                "3122334444444324",
                "1223344554534323",
                "1122334354333222",
                "2223333433222212",
                "2221333333321120",
                "0221113334111210",
                "0011222111221101",
                "1321112111221001");
        // the wooden foot from below
        add("block/salt_lamp_bottom", null,
                palette("w=3E2716", "W=5C3B22", "v=7A5232", "V=94673E"),
                "wwwwwwwwwwwwwwww",
                "wvvvvvVvvvvvVvvw",
                "wvvvvvVvvvvvVvvw",
                "wWWWWWWWWWWWWWWw",
                "wvvVvvvvvVvvvvvw",
                "wvvVvvvvvVvvvvvw",
                "wvvVvvvvvVvvvvvw",
                "wWWWWWWWWWWWWWWw",
                "wvvvvvVvvvvvVvvw",
                "wvvvvvVvvvvvVvvw",
                "wvvvvvVvvvvvVvvw",
                "wWWWWWWWWWWWWWWw",
                "wvvVvvvvvVvvvvvw",
                "wvvVvvvvvVvvvvvw",
                "wvvVvvvvvVvvvvvw",
                "wwwwwwwwwwwwwwww");
        List<BufferedImage> written = new ArrayList<>();
        for (Texture texture : TEXTURES_TO_WRITE) {
            written.add(write(texture));
        }
        contactSheet(written);
    }

    private static void add(String name, String base, Map<Character, Integer> palette, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, base, palette, rows));
    }

    /** {@code "c=RRGGBB"} pairs. */
    private static Map<Character, Integer> palette(String... entries) {
        Map<Character, Integer> palette = new HashMap<>();
        for (String entry : entries) {
            palette.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
        }
        return palette;
    }

    private static BufferedImage write(Texture texture) throws IOException {
        int height = texture.rows().length;
        int width = texture.rows()[0].length();
        boolean icon = texture.name().startsWith("mob_effect/");
        boolean layer = texture.name().startsWith("models/armor/");
        int wantWidth = layer ? 64 : icon ? 18 : 16;
        int wantHeight = layer ? 32 : icon ? 18 : 16;
        if (width != wantWidth || height != wantHeight) {
            throw new IllegalStateException(texture.name() + ": " + width + " x " + height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (texture.base() != null) {
            BufferedImage base = ImageIO.read(TEXTURES.resolve(texture.base() + ".png").toFile());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, 0xFF000000 | base.getRGB(x, y));
                }
            }
        }
        for (int y = 0; y < height; y++) {
            String row = texture.rows()[y];
            if (row.length() != width) {
                throw new IllegalStateException(texture.name() + ": row " + y + " has " + row.length() + " pixels");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = texture.palette().get(c);
                if (rgb == null) {
                    throw new IllegalStateException(texture.name() + ": '" + c + "' not in the palette");
                }
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(texture.name() + ": alpha " + alpha + " at " + x + "," + y);
                }
            }
        }
        Path file = TEXTURES.resolve(texture.name() + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
        return image;
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
        Path preview = Path.of("build/texture-preview/" + Golf2Ring2Textures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
