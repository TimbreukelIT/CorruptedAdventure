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
 * Generates the block textures of phase 2 golf 3 + 4 (stream T, design §6.4, §6.7): the sulfur block, the dew crystal
 * block, the sulfur lamp, the pumice raft, and the inlaid building sets (fire ruby in pumice, jade in tidestone,
 * aquamarine in lagoon sandstone; side and top, stairs/slab/wall reuse them). Our own pixels; the inlays are drawn over
 * our own base textures ({@code '.'} keeps the base pixel), so run this after the ring 3/4 scripts. Run from the
 * repository root: {@code java tools/textures/Golf34BlockTextures.java}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf34BlockTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf34BlockTextures() {
    }

    public static void main(String[] args) throws IOException {
        // packed sulfur lumps with dark seams (like a coal block), tileable
        add("block/sulfur_block", null,
                palette("0=4A3A08", "1=7A6610", "2=AC961C", "3=D6BE30", "4=EDDA50", "5=F9EF8C", "6=FFFBD0"),
                "5444154443401221",
                "4443344433215506",
                "4433213330055441",
                "3433113114404430",
                "1220551644414332",
                "0105444144313322",
                "0554443033301222",
                "5144433200444011",
                "5503332154443315",
                "4441221144433215",
                "4533005513332054",
                "4332064450221514",
                "3320544433016440",
                "1221444332155444",
                "2010433310144443",
                "0554111444343332");
        // fused dew crystal facets with bright rims (like amethyst), tileable
        add("block/dew_crystal_block", null,
                palette("1=2E6F86", "2=5E9FB4", "3=7EB7C8", "4=9DCDDB", "5=C8E5EC", "6=E2F1F6"),
                "5554435516322226",
                "5544331644332225",
                "5443322554433225",
                "4433222555443325",
                "4332226515544314",
                "3322165441554166",
                "1221554433262222",
                "2115544332233222",
                "2656443322143322",
                "1544633222644332",
                "6443132211654433",
                "4433211622265441",
                "4332214332215546",
                "3322264433221514",
                "3222255443321663",
                "1166665544162211");
        // yellow-green sulfur glowing behind glass, dark sulfur-stone frame with studs
        add("block/sulfur_lamp", null,
                palette("2=9AB020", "3=C4D838", "4=E2F060", "5=F4FCA0", "6=FFFFE0", "f=3A3410", "F=6E6420", "G=A89A34"),
                "ffffffffffffffff",
                "fGGFFGFFGFFGFFGf",
                "fG333332433332Ff",
                "fF336442444322Ff",
                "fF364452554322Gf",
                "fG344542554332Ff",
                "fF345452654432Ff",
                "fF224226622222Gf",
                "fG442566625544Ff",
                "fF345266625543Ff",
                "fF344255455442Gf",
                "fG334254254462Ff",
                "fF332323244622Ff",
                "fF322223232222Gf",
                "fGFFGFFGFFGFFGGf",
                "ffffffffffffffff");
        // the pumice texture as four lashed slabs (opaque: the model is a full 16 x 1.5 x 16 slab and its sides use
        // the top rows), light rims, dark seams and a few deep pores
        add("block/pumice_raft", "block/pumice",
                palette("1=3E3934", "2=6A635C", "6=B5AEA2", "7=C8C2B6"),
                "7777777677777776",
                "6..............2",
                "6...1.......1..2",
                "6..............2",
                "6.....1........2",
                "6..1........1..2",
                "6..............2",
                "6222222122222221",
                "7777777677777776",
                "6...1..........2",
                "6.........1....2",
                "6..............2",
                "6.1.......1....2",
                "6..............2",
                "6.....1........2",
                "2222222122222221");
        // pumice with a carved band of faceted fire rubies
        add("block/fire_ruby_inlaid_pumice", "block/pumice",
                palette("g=6A635C", "k=4A453F", "K=938C82", "1=82121E", "2=B21D2A", "3=D5353F", "4=F07164"),
                "................",
                "................",
                "................",
                "................",
                "kkkkkkkkkkkkkkkk",
                "gggg44gggggg44gg",
                "ggg4432gggg4432g",
                "gg443321gg443321",
                "gg332211gg332211",
                "ggg2211gggg2211g",
                "gggg11gggggg11gg",
                "KKKKKKKKKKKKKKKK",
                "................",
                "................",
                "................",
                "................");
        // pumice top: carved frame, faceted fire ruby, corner studs
        add("block/fire_ruby_inlaid_pumice_top", "block/pumice",
                palette("g=6A635C", "k=4A453F", "K=938C82", "1=82121E", "2=B21D2A", "3=D5353F", "4=F07164", "5=F0B9AE"),
                ".k............K.",
                "kkkkkkkkkkkkkkkk",
                ".k............K.",
                ".k.2........2.K.",
                ".k.....gg.....K.",
                ".k....g44g....K.",
                ".k...g4543g...K.",
                ".k..g443332g..K.",
                ".k..g433221g..K.",
                ".k...g3221g...K.",
                ".k....g21g....K.",
                ".k.....gg.....K.",
                ".k.2........2.K.",
                ".k............K.",
                "KkKKKKKKKKKKKKKK",
                ".k............K.");
        // tidestone with a carved band of round jade cabochons
        add("block/jade_inlaid_tidestone", "block/tidestone",
                palette("g=2E3644", "k=1E242E", "K=646F7D", "1=1C4A3C", "2=2E6A54", "3=44886C", "4=62A688", "5=8CC4A6", "6=C4E6D2"),
                "................",
                "................",
                "................",
                "................",
                "kkkkkkkkkkkkkkkk",
                "ggg111ggggg111gg",
                "gg14561ggg14561g",
                "g1445531g1445531",
                "g1443321g1443321",
                "gg13321ggg13321g",
                "ggg111ggggg111gg",
                "KKKKKKKKKKKKKKKK",
                "................",
                "................",
                "................",
                "................");
        // tidestone top: carved frame, round jade cabochon, corner studs
        add("block/jade_inlaid_tidestone_top", "block/tidestone",
                palette("k=1E242E", "K=646F7D", "1=1C4A3C", "2=2E6A54", "3=44886C", "4=62A688", "5=8CC4A6", "6=C4E6D2"),
                ".k............K.",
                "kkkkkkkkkkkkkkkk",
                ".k............K.",
                ".k.2..1111..2.K.",
                ".k...145561...K.",
                ".k..14566541..K.",
                ".k..14555431..K.",
                ".k..14454331..K.",
                ".k..14443321..K.",
                ".k..13433221..K.",
                ".k...133221...K.",
                ".k....1111....K.",
                ".k.2........2.K.",
                ".k............K.",
                "KkKKKKKKKKKKKKKK",
                ".k............K.");
        // lagoon_sandstone with a carved band of aquamarine crystals
        add("block/aquamarine_inlaid_lagoon_sandstone", "block/lagoon_sandstone",
                palette("g=BAB6A4", "k=9C9886", "K=DBD9CA", "1=185066", "2=26758E", "3=3A9AB2", "4=5EBED0", "5=98DEE8"),
                "................",
                "................",
                "................",
                "................",
                "kkkkkkkkkkkkkkkk",
                "gggg5ggggggg5ggg",
                "ggg543ggggg543gg",
                "ggg432ggggg432gg",
                "ggg432ggggg432gg",
                "ggg321ggggg321gg",
                "gggg1ggggggg1ggg",
                "KKKKKKKKKKKKKKKK",
                "................",
                "................",
                "................",
                "................");
        // lagoon_sandstone_top top: carved frame, aquamarine crystal, corner studs
        add("block/aquamarine_inlaid_lagoon_sandstone_top", "block/lagoon_sandstone_top",
                palette("g=BAB6A4", "k=9C9886", "K=DBD9CA", "1=185066", "2=26758E", "3=3A9AB2", "4=5EBED0", "5=98DEE8"),
                ".k............K.",
                "kkkkkkkkkkkkkkkk",
                ".k............K.",
                ".k.2...g....2.K.",
                ".k....g5g.....K.",
                ".k...g553g....K.",
                ".k..g55433g...K.",
                ".k.g5543321g..K.",
                ".k..g54321g...K.",
                ".k...g4321g...K.",
                ".k....g21g....K.",
                ".k.....g......K.",
                ".k.2........2.K.",
                ".k............K.",
                "KkKKKKKKKKKKKKKK",
                ".k............K.");
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
        Path preview = Path.of("build/texture-preview/" + Golf34BlockTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
