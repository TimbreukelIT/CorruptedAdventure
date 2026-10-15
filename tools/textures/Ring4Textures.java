import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the ring 4 (mythical water ring) and Nether ring 4 (dry crystal and bone desert) block, item and particle
 * textures from our own pixels: no vanilla pixels are read or copied. Run from the repository root:
 * {@code java tools/textures/Ring4Textures.java}.
 *
 * <p>Good first versions in the 37b style (fase-1-wereld §1.4, package 71a): light from the top left, 5–7 tones per
 * material with a hue shift, selective outlines for items and crystals, a flatness lint, and a contact sheet
 * ({@code build/texture-preview/ring4.png}) with 6×6 walls, night views, items in a slot and small scenes.
 *
 * <p>Glow (contract §7): the glowing flora has two textures, {@code <name>} (the whole plant, glow painted in) and
 * {@code <name>_glow} (a cutout layer with only the glowing pixels, pulsing). B's model draws the glow layer emissive over
 * the plant. {@link #checkGlowLayers} checks that a glow layer matches its plant pixel for pixel.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs and
 * {@code .mcmeta}s. Every texture is checked before it is written. The shared helpers are copied from
 * {@code Ring4Textures.java}: a single-file script cannot load a second source file (deduplication: backlog, an
 * {@code infra} choice).
 */
public final class Ring4Textures {
    private static final int SIZE = 16;
    /** Vanilla's flowing fluids use 32×32 frames: the renderer samples half of the texture on a side face. */
    private static final int FLOW_SIZE = 32;
    private static final int PARTICLE_SIZE = 8;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/ring4.png");

    /** Retouched by hand after generation; the script skips these and the preview reads them from disk. */
    private static final Set<String> HAND_EDITED = Set.of();
    /** Textures on 32×32 frames. */
    private static final Set<String> LARGE = Set.of("block/toxic_pool_flow");
    /**
     * Sprites that may be half-transparent: particles drawn on the translucent particle sheet (blended, and the particle
     * atlas has no mipmaps), so a soft alpha edge shows as intended instead of being cut.
     */
    private static final String SOFT_ALPHA = "particle/waterfall_mist_";

    // ---- Palettes, dark to light (RGB): cool shadows, warmer lights (hue shift) ---------------------------------

    // Tidestone: blue-grey layered rock of the cliffs, clods and tunnels; a little darker than stone (cliffs read heavy).
    private static final int[] TIDESTONE = {0x2E3644, 0x3A4351, 0x47515F, 0x55606E, 0x646F7D, 0x757F8D, 0x87919E};
    private static final int TIDESTONE_MORTAR = 0x222833;
    // Moonpetal: white-silver bark with lilac shadows, pale pink-cream wood, pink-lilac blossom.
    private static final int[] MOONPETAL_BARK = {0x8A8496, 0xA29DAE, 0xBAB6C4, 0xCFCCD8, 0xE2E0EA, 0xF2F1F7};
    private static final int[] MOONPETAL_LENTICEL = {0x5E5868, 0x7A7484};
    private static final int MOONPETAL_SHEEN = 0xFBFAFF;
    private static final int[] MOONPETAL_WOOD = {0x9C7F86, 0xB4969A, 0xC9ACAD, 0xDBC2BF, 0xEAD6D0, 0xF5E7E1};
    private static final int[] BLOSSOM = {0x6E4472, 0x8E5A90, 0xB078AE, 0xCE96C8, 0xE6B6DE, 0xF6D8EE};
    private static final int BLOSSOM_HEART = 0xFFF2DC;
    private static final int[] SAPLING_LEAF = {0x4E6A40, 0x6E8A5A};
    // Withered moonpetal: dead grey bark, grey-beige wood.
    private static final int[] WITHERED_BARK = {0x3A3A3C, 0x4A494B, 0x5B5A5B, 0x6D6C6C, 0x807E7D, 0x93918F};
    private static final int[] WITHERED_WOOD = {0x5E5852, 0x6F6861, 0x817971, 0x938A80, 0xA59B90};
    // Reeds, peat, hanging moss, water flora.
    private static final int[] REED = {0x3E4A22, 0x52602C, 0x687936, 0x809142, 0x99A852, 0xB4BE6E};
    private static final int[] REED_PLUME = {0x5A4434, 0x7A604A, 0x9A7E62, 0xBCA184, 0xD8C2A4};
    private static final int[] PEAT = {0x1E1612, 0x2A1F18, 0x37291F, 0x453427, 0x544030, 0x654D3A};
    private static final int[] PEAT_ROOT = {0x7A5E40, 0x97794F};
    private static final int[] SWAMP_MOSS = {0x3C4634, 0x505C44, 0x667356, 0x7E8A6A, 0x97A280, 0xB3BC9C};
    private static final int[] LILY = {0x1A421C, 0x255A24, 0x32722E, 0x438A3A, 0x5AA24A, 0x78BA5E};
    private static final int LILY_OUTLINE_LIGHT = 0x24502A;
    private static final int LILY_OUTLINE = 0x0E2610;
    private static final int[] LOTUS = {0xB0507A, 0xD06E96, 0xE892B2, 0xF6BCD0, 0xFFE6EE, 0xFFFFFF};
    private static final int LOTUS_HEART = 0xF2D04A;
    // Glow flora: purple caps and violet petals, the glow blue ([0] is the dim tone a pulse leans towards).
    private static final int[] GLOWCAP_CAP = {0x241C46, 0x33285E, 0x45387A, 0x584A96, 0x6E5CB2};
    private static final int[] GLOWCAP_STEM = {0x56607A, 0x76829A, 0x9AA6BC, 0xBCC6D6};
    private static final int[] GLOW = {0x2A4FB0, 0x3E78E0, 0x5C9EF4, 0x86C2FF, 0xB8E0FF, 0xE6F6FF};
    private static final int GLOW_COLOUR = 0xCFEFFF;
    private static final int[] BLOOM_PETAL = {0x3A2266, 0x50308A, 0x6A44AA, 0x8862C6, 0xA886DC};
    private static final int[] BLOOM_STEM = {0x1E4A40, 0x2E6656, 0x44846E, 0x62A288};
    private static final int[] ROOT = {0x2E241E, 0x3E3128, 0x524034, 0x685242, 0x806652};
    // Blighted glowcap: the same fungus gone sickly green-grey, a weak yellow-green glow.
    private static final int[] BLIGHT_CAP = {0x30352A, 0x3F4636, 0x505842, 0x636B50, 0x777F62};
    private static final int[] BLIGHT_STEM = {0x55584E, 0x6D7064, 0x878A7C, 0xA0A294};
    private static final int[] BLIGHT_GLOW = {0x4E6424, 0x6A8430, 0x86A040, 0xA2B85A, 0xBACD7A};
    private static final int BLIGHT_GLOW_COLOUR = 0xC8DC8A;
    // Bubble spring: a pale mineral crust (travertine) around vents of dark water.
    private static final int[] MINERAL = {0x5E7480, 0x748A96, 0x8CA2AC, 0xA6BAC2, 0xC0D2D8, 0xDCE8EC};
    private static final int[] SPRING_WATER = {0x0C1E2A, 0x173446, 0x245068, 0x3C7896};
    private static final int BUBBLE = 0xE8FAFF;
    // Ores and crystals (V-T1 A, V-T2 A: toxic and purity crystal glow and pulse, aquamarine and jade matte).
    private static final int[] TIDESILVER = {0x16222C, 0x2C3E4C, 0x4E6676, 0x7890A0, 0xA2BCC6, 0xC8E2E4, 0xF2FFFF};
    /** Weak glow of the tidesilver ore: a cool sea-white the pulse leans towards, from the mid silver. */
    private static final int TIDESILVER_GLOW = 0xD8FFF8;
    private static final int TIDESILVER_DIM = TIDESILVER[2];
    private static final int[] RAW_TIDESILVER = {0x2A3A46, 0x48606E, 0x6E8896, 0x96B2BC, 0xBCD8DC, 0xEEFFFF};
    private static final int[] JADE = {0x0E2E26, 0x1C4A3C, 0x2E6A54, 0x44886C, 0x62A688, 0x8CC4A6, 0xC4E6D2};
    private static final int[] TOXIC_CRYSTAL = {0x14300A, 0x285A0E, 0x3E8414, 0x5AAC1C, 0x84D02A, 0xB4EC52, 0xEEFFB4};
    private static final int[] AQUAMARINE = {0x0E2E3C, 0x185066, 0x26758E, 0x3A9AB2, 0x5EBED0, 0x98DEE8, 0xE4FBFC};
    private static final int[] PURITY = {0x7A6A48, 0x9E8C62, 0xC6B484, 0xE2D6AE, 0xF0EAD2, 0xF9F6EA, 0xFFFFFC};
    // Gameplay blocks (72).
    private static final int[] SUCKING_MUD = {0x1A1C14, 0x24271B, 0x2F3322, 0x3B402A, 0x484E33, 0x575E3E};
    private static final int SUCKING_SHEEN = 0x737C55;
    private static final int[] TOXIC_POOL = {0x2E5A0C, 0x3E7610, 0x509214, 0x66AE1C, 0x80C82A, 0xA2E040};
    private static final int TOXIC_SHEEN = 0xD4F870;
    private static final int[] BUCKET = {0x3A3E44, 0x5A6068, 0x7E868E, 0xA8B0B6, 0xD0D6DA};
    private static final int[] UPDRAFT = {0x8CC8DC, 0xB8E2EE, 0xE6F8FC};
    private static final int[] SLIME = {0x4E6A48, 0x6A8A5E, 0x8AAA78, 0xAECA9A, 0xD6EAC4};
    private static final int[] NEST = {0x1E1712, 0x2E241A, 0x423426, 0x584634, 0x705A44, 0x8A7256};
    private static final int[] NEST_MOSS = {0x4E6A3A, 0x6A8A4A};
    private static final int[] BONE = {0xA8A090, 0xDCD4BE, 0xF6F2E6};

    // Nether ring 4.
    private static final int[] BONE_SAND = {0x9C9078, 0xB0A48A, 0xC2B79C, 0xD2C8AE, 0xE0D8C0, 0xECE6D2};
    private static final int[] BLEACHED = {0x8A6E68, 0x9C807A, 0xAE948C, 0xC0A8A0, 0xD0BCB4, 0xDFCFC8};
    private static final int BLEACHED_RED = 0x9A645E;
    private static final int[] FOSSIL_STONE = {0x6A6250, 0x7E7562, 0x928874, 0xA59B86, 0xB8AE98, 0xCAC1AC};
    private static final int FOSSIL_MORTAR = 0x48422F;
    private static final int[] FOSSIL_BONE = {0x554D3C, 0xE4DCC6, 0xF4F0E2};

    /** A crystal material in the amethyst pattern: palette [0..6], selective outline, pulsing or still. */
    private record Crystal(int[] palette, int outlineLight, int outline, boolean glowing, int glow, int dim, boolean prism) {
    }

    private static final Crystal TOXIC = new Crystal(TOXIC_CRYSTAL, 0x2C5A10, 0x10240A, true, 0xE8FFB8, TOXIC_CRYSTAL[1], false);
    private static final Crystal AQUA = new Crystal(AQUAMARINE, 0x1E5A70, 0x0A2430, false, 0, 0, true);
    private static final Crystal PURE = new Crystal(PURITY, 0x8A7A54, 0x4A4030, true, 0xFFF4D0, PURITY[2], false);

    /** A glowing fungus: palettes, the number of glowing spots on the caps and whether the whole gill rim glows. */
    private record Fungus(int[] cap, int[] stem, int[] glow, int spots, boolean rimGlow) {
    }

    private static final Fungus GLOWCAP = new Fungus(GLOWCAP_CAP, GLOWCAP_STEM, GLOW, 3, true);
    private static final Fungus BLIGHTED = new Fungus(BLIGHT_CAP, BLIGHT_STEM, BLIGHT_GLOW, 1, false);

    /**
     * How a texture moves: {@code frames} frames of {@code frametime} ticks, optionally interpolated; {@code glow} is the
     * colour bright frames mix towards and {@code dim} the tone dim frames lean towards. {@code cycling}: painted per
     * phase; {@code opaque}: a cycling texture without transparency (cutout cycling textures may change their alpha
     * mask per frame because they are not interpolated).
     */
    private record Animation(int frames, int frametime, boolean interpolate, int glow, int dim, boolean cycling, boolean opaque) {
        static final Animation STILL = new Animation(1, 1, false, 0, 0, false, false);

        boolean animated() {
            return frames > 1;
        }
    }

    private record Texture(Painter painter, CyclingPainter cyclingPainter, Animation animation) {
    }

    @FunctionalInterface
    private interface Painter {
        void paint(BufferedImage image, Random random);
    }

    /** Paints one frame of a moving texture; {@code phase} runs from 0 (inclusive) to 1 (exclusive) over a cycle. */
    @FunctionalInterface
    private interface CyclingPainter {
        void paint(BufferedImage image, Random random, double phase);
    }

    /** Pulse used by glowing crystals and flora, as in ring 1–3: 8 frames, frame time 4, interpolated. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();
    private static final Map<String, String> GLOW_LAYERS = new LinkedHashMap<>();
    private static final Map<String, Set<Integer>> GLOW_TONES = new LinkedHashMap<>();
    private static final Map<BufferedImage, boolean[][]> GLOW_MASKS = new IdentityHashMap<>();

    /**
     * Tones that stay still in a pulsing texture although they are lighter than its dim tone: the fossil stone frame of
     * the purity crystal lamp (its light bevel would otherwise pulse with the panes).
     */
    private static final Map<String, Set<Integer>> STILL_TONES = Map.of(
            "block/purity_crystal_lamp", toneSet(FOSSIL_STONE));

    /** First texture of the Nether ring 4 and the particle sections of the contact sheet. */
    private static final String NETHER_FIRST = "block/bone_sand";
    private static final String PARTICLES_FIRST = "particle/will_o_wisp_0";

    /** The four parts of the 2×2 giant lily pad (B's {@code part} values), with their quadrant in the 32×32 pad. */
    private static final String[] LILY_PARTS = {"north_west", "north_east", "south_west", "south_east"};

    static {
        // ---- Ring 4 (66): tidestone, moonpetal, withered moonpetal ----
        still("block/tidestone", Ring4Textures::tidestone);
        still("block/cobbled_tidestone", Ring4Textures::cobbledTidestone);
        still("block/polished_tidestone", Ring4Textures::polishedTidestone);
        still("block/tidestone_bricks", (image, random) -> stoneBricks(image, random, TIDESTONE, TIDESTONE_MORTAR));
        still("block/moonpetal_log", Ring4Textures::moonpetalLog);
        still("block/moonpetal_log_top", (image, random) -> logTop(image, random, MOONPETAL_WOOD, MOONPETAL_BARK, false));
        still("block/stripped_moonpetal_log", (image, random) -> strippedLog(image, random, MOONPETAL_WOOD));
        still("block/stripped_moonpetal_log_top", (image, random) -> strippedLogTop(image, random, "block/moonpetal_log_top", MOONPETAL_WOOD, MOONPETAL_BARK));
        still("block/moonpetal_planks", (image, random) -> planks(image, random, MOONPETAL_WOOD));
        still("block/moonpetal_door_top", (image, random) -> doorTop(image, random, MOONPETAL_WOOD, MOONPETAL_BARK));
        still("block/moonpetal_door_bottom", (image, random) -> doorBottom(image, random, MOONPETAL_WOOD, MOONPETAL_BARK));
        still("block/moonpetal_trapdoor", (image, random) -> trapdoor(image, random, MOONPETAL_WOOD, MOONPETAL_BARK));
        still("item/moonpetal_door", (image, random) -> doorItem(image, random, MOONPETAL_WOOD, MOONPETAL_BARK));
        still("block/moonpetal_leaves", Ring4Textures::moonpetalLeaves);
        still("block/moonpetal_sapling", Ring4Textures::moonpetalSapling);
        still("block/withered_moonpetal_log", Ring4Textures::witheredLog);
        still("block/withered_moonpetal_log_top", (image, random) -> logTop(image, random, WITHERED_WOOD, WITHERED_BARK, true));
        still("block/stripped_withered_moonpetal_log", (image, random) -> strippedLog(image, random, WITHERED_WOOD));
        still("block/stripped_withered_moonpetal_log_top", (image, random) -> strippedLogTop(image, random, "block/withered_moonpetal_log_top", WITHERED_WOOD, WITHERED_BARK));
        // Reeds, peat, moss, water flora.
        still("block/reeds", (image, random) -> reeds(image, false));
        still("block/reeds_top", (image, random) -> reeds(image, true));
        still("block/peat", Ring4Textures::peat);
        still("block/hanging_swamp_moss", Ring4Textures::hangingSwampMoss);
        for (int part = 0; part < LILY_PARTS.length; part++) {
            int p = part;
            still("block/giant_lily_pad_" + LILY_PARTS[part], (image, random) -> lilyPadPart(image, p));
        }
        still("item/giant_lily_pad", Ring4Textures::lilyPadItem);
        still("block/lotus", Ring4Textures::lotus);
        // Glow flora: the plant and its glow layer.
        still("block/glowcap", (image, random) -> fungi(image, random, GLOWCAP));
        glowLayer("block/glowcap", GLOW, GLOW_COLOUR, GLOW[0]);
        still("block/glow_bloom", Ring4Textures::glowBloom);
        glowLayer("block/glow_bloom", GLOW, GLOW_COLOUR, GLOW[0]);
        still("block/glowing_hanging_roots", Ring4Textures::glowingHangingRoots);
        glowLayer("block/glowing_hanging_roots", GLOW, GLOW_COLOUR, GLOW[0]);
        still("block/blighted_glowcap", (image, random) -> fungi(image, random, BLIGHTED));
        glowLayer("block/blighted_glowcap", BLIGHT_GLOW, BLIGHT_GLOW_COLOUR, BLIGHT_GLOW[0]);
        still("block/bubble_spring_top", Ring4Textures::bubbleSpringTop);
        still("block/bubble_spring_side", Ring4Textures::bubbleSpringSide);

        // ---- Ores and crystals (67) ----
        // B (67): tidesilver ore has a weak glow (light 3, emissive overlay like the fire ruby); the raw metal stays matte.
        pulsing("block/tidesilver_ore_overlay", TIDESILVER_GLOW, TIDESILVER_DIM, (image, random) -> chunks(image, random, new double[] {1.7, 1.3, 1.1, 1.0, 0.9}, TIDESILVER));
        still("item/raw_tidesilver", Ring4Textures::rawTidesilver);
        still("block/jade_ore_overlay", Ring4Textures::jadeOreOverlay);
        still("item/jade", Ring4Textures::jadeItem);
        crystalSet("toxic_crystal_block", "budding_toxic_crystal", "toxic_crystal", "toxic_crystal_cluster", "toxic_crystal_shard", TOXIC);
        crystalSet("aquamarine_block", "budding_aquamarine", "aquamarine", "aquamarine_cluster", "aquamarine_shard", AQUA);

        // ---- Gameplay blocks (72, registered by B in golf 2) ----
        cycling("block/sucking_mud", 16, 5, Ring4Textures::suckingMud);
        cycling("block/toxic_pool_still", 32, 3, Ring4Textures::toxicPoolStill);
        cycling("block/toxic_pool_flow", 32, 2, Ring4Textures::toxicPoolFlow);
        still("item/toxic_pool_bucket", Ring4Textures::toxicPoolBucket);
        rising("block/updraft", 16, 2, Ring4Textures::updraft);
        still("block/clawed_tidestone", Ring4Textures::clawedTidestone);
        still("block/slime_trail", Ring4Textures::slimeTrail);
        still("block/creature_nest", Ring4Textures::creatureNest);

        // ---- Nether ring 4 (68) ----
        still("block/bone_sand", Ring4Textures::boneSand);
        still("block/bleached_netherrack", Ring4Textures::bleachedNetherrack);
        still("block/fossil_stone", Ring4Textures::fossilStone);
        still("block/polished_fossil_stone", Ring4Textures::polishedFossilStone);
        still("block/fossil_stone_bricks", (image, random) -> stoneBricks(image, random, FOSSIL_STONE, FOSSIL_MORTAR));
        crystal("block/purity_crystal_block", PURE, (image, random) -> crystalBlock(image, random, PURITY));
        crystal("block/purity_crystal_cluster", PURE, (image, random) -> crystalStage(image, random, 4, PURE));
        crystal("item/purity_crystal_shard", PURE, (image, random) -> crystalShard(image, PURE));
        crystal("block/purity_crystal_lamp", PURE, Ring4Textures::purityCrystalLamp);

        // ---- Particles (contract §6, sprites for A): 8×8 ----
        for (int i = 0; i < 4; i++) {
            int n = i;
            still("particle/will_o_wisp_" + i, (image, random) -> particle(image, Ring4Textures.WILL_O_WISPS[n], Ring4Textures.GREYS));
            still("particle/blossom_" + i, (image, random) -> particle(image, Ring4Textures.BLOSSOMS[n], Ring4Textures.BLOSSOM_SPRITE));
            still("particle/firefly_" + i, (image, random) -> particle(image, Ring4Textures.FIREFLIES[n], Ring4Textures.GREYS));
            still("particle/swamp_gas_" + i, (image, random) -> particle(image, Ring4Textures.SWAMP_GAS[n], Ring4Textures.GREYS));
            still("particle/toxic_bubble_" + i, (image, random) -> particle(image, Ring4Textures.TOXIC_BUBBLES[n], Ring4Textures.GREYS));
            still("particle/updraft_bubble_" + i, (image, random) -> particle(image, Ring4Textures.UPDRAFT_BUBBLES[n], Ring4Textures.GREYS));
            still("particle/glitch_speck_" + i, (image, random) -> particle(image, Ring4Textures.GLITCH_SPECKS[n], Ring4Textures.GLITCH));
            still("particle/bone_dust_" + i, (image, random) -> particle(image, Ring4Textures.BONE_DUST[n], Ring4Textures.GREYS));
            // verdant_falls (Q8 A): waterfall spray, soft white-blue blobs with an alpha fall-off (polish round "mist").
            still("particle/waterfall_mist_" + i, (image, random) -> softParticle(image, Ring4Textures.WATERFALL_MIST[n], Ring4Textures.MIST_CORE, Ring4Textures.MIST_EDGE));
        }
    }

    private static void still(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, Animation.STILL));
    }

    /** An 8-frame pulse towards {@code glow}, the glow colour of this material, and back towards its {@code dim} tone. */
    private static void pulsing(String name, int glow, int dim, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, new Animation(PULSE_FRAMES, PULSE_FRAMETIME, true, glow, dim, false, false)));
    }

    /** A moving, opaque pattern (mud, the toxic pool), looping seamlessly because the painter is periodic in its phase. */
    private static void cycling(String name, int frames, int frametime, CyclingPainter painter) {
        TEXTURES.put(name, new Texture(null, painter, new Animation(frames, frametime, false, 0, 0, true, true)));
    }

    /** A moving cutout pattern (the updraft): not interpolated, so its alpha mask may change from frame to frame. */
    private static void rising(String name, int frames, int frametime, CyclingPainter painter) {
        TEXTURES.put(name, new Texture(null, painter, new Animation(frames, frametime, false, 0, 0, true, false)));
    }

    private static void crystal(String name, Crystal crystal, Painter painter) {
        if (crystal.glowing()) {
            pulsing(name, crystal.glow(), crystal.dim(), painter);
        } else {
            still(name, painter);
        }
    }

    /** The amethyst pattern (contract §3, 67): block, budding block, small/medium/large bud, cluster and the shard item. */
    private static void crystalSet(String block, String budding, String bud, String cluster, String shard, Crystal crystal) {
        crystal("block/" + block, crystal, (image, random) -> crystalBlock(image, random, crystal.palette()));
        crystal("block/" + budding, crystal, (image, random) -> buddingCrystal(image, random, "block/" + block, crystal.palette()));
        String[] sizes = {"small", "medium", "large"};
        for (int stage = 1; stage <= 3; stage++) {
            int s = stage;
            crystal("block/" + sizes[stage - 1] + "_" + bud + "_bud", crystal, (image, random) -> crystalStage(image, random, s, crystal));
        }
        crystal("block/" + cluster, crystal, (image, random) -> crystalStage(image, random, 4, crystal));
        crystal("item/" + shard, crystal, (image, random) -> crystalShard(image, crystal));
    }

    /**
     * {@code <block>_glow}: the pixels the block's painter marked as glowing ({@link #glow}), everything else
     * transparent; pulsing towards {@code glow}. {@code tones[1..]} are the colours a glowing pixel may have.
     */
    private static void glowLayer(String block, int[] tones, int glow, int dim) {
        String name = block + "_glow";
        GLOW_LAYERS.put(name, block);
        Set<Integer> glowing = new HashSet<>();
        for (int i = 1; i < tones.length; i++) {
            glowing.add(tones[i]);
        }
        GLOW_TONES.put(block, glowing);
        pulsing(name, glow, dim, (image, random) -> {
            BufferedImage source = renderUnchecked(block);
            boolean[][] mask = glowMask(source);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (mask[x][y]) {
                        image.setRGB(x, y, source.getRGB(x, y));
                    }
                }
            }
        });
    }

    /** Paints a glowing pixel: the colour, and the mark the glow layer reads. */
    private static void glow(BufferedImage image, int x, int y, int rgb) {
        set(image, x, y, rgb);
        GLOW_MASKS.computeIfAbsent(image, key -> new boolean[SIZE][SIZE])[x][y] = true;
    }

    private static boolean[][] glowMask(BufferedImage image) {
        return GLOW_MASKS.getOrDefault(image, new boolean[SIZE][SIZE]);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        // Checked before anything is written (rendering is pure), so a failed check leaves no broken texture behind.
        checkGlowLayers();
        checkSideRules();
        checkSlimeTrailEdges();
        Map<String, BufferedImage> outputs = new LinkedHashMap<>();
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            if (HAND_EDITED.contains(name)) {
                continue;
            }
            Animation animation = entry.getValue().animation();
            BufferedImage output = animation.cycling() ? cyclingStrip(name, animation)
                    : animation.animated() ? pulseStrip(render(name), animation, STILL_TONES.getOrDefault(name, Set.of())) : render(name);
            check(name, output, animation);
            outputs.put(name, output);
        }
        for (Map.Entry<String, BufferedImage> entry : outputs.entrySet()) {
            String name = entry.getKey();
            Path file = OUTPUT.resolve(name + ".png");
            Files.createDirectories(file.getParent());
            ImageIO.write(entry.getValue(), "png", file.toFile());
            System.out.println("wrote " + file);
            Animation animation = TEXTURES.get(name).animation();
            if (animation.animated()) {
                Path meta = OUTPUT.resolve(name + ".png.mcmeta");
                Files.writeString(meta, "{\n  \"animation\": {\n    \"frametime\": " + animation.frametime()
                        + ",\n    \"interpolate\": " + animation.interpolate() + "\n  }\n}\n", StandardCharsets.UTF_8);
                System.out.println("wrote " + meta);
            }
        }
        System.out.println(outputs.size() + " textures");
        lint();
        writePreview();
    }

    // ---- Checks -------------------------------------------------------------------------------------------------

    /** Textures that are calm or sparse on purpose, with the reason (flatness lint). */
    private static final Map<String, String> FLAT_OK = Map.of(
            "block/updraft", "a see-through column: a few streaks and bubbles in three pale tones",
            "block/slime_trail", "a decal: one streak of slime on a transparent tile");

    private static final List<String> LINT_WARNINGS = new ArrayList<>();

    /**
     * Warns (does not fail) about textures that read flat or half drawn: an opaque block with fewer than 5 colours or one
     * colour on more than half the pixels (vanilla: 6-9 colours, the commonest on 20-37 %), a sprite with fewer than 5
     * colours, an item whose outline is a single colour all round. Glow layers, particles and 32×32 textures are skipped;
     * exceptions with their reason in {@link #FLAT_OK}.
     */
    private static void lint() throws IOException {
        for (String name : TEXTURES.keySet()) {
            if (name.startsWith("particle/") || GLOW_LAYERS.containsKey(name) || FLAT_OK.containsKey(name) || LARGE.contains(name)) {
                continue;
            }
            BufferedImage image = render(name);
            Map<Integer, Integer> colours = new HashMap<>();
            Set<Integer> outline = new HashSet<>();
            int opaque = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!isOpaque(image, x, y)) {
                        continue;
                    }
                    int rgb = image.getRGB(x, y) & 0xFFFFFF;
                    colours.merge(rgb, 1, Integer::sum);
                    opaque++;
                    if (!isOpaque(image, x - 1, y) || !isOpaque(image, x + 1, y) || !isOpaque(image, x, y - 1) || !isOpaque(image, x, y + 1)) {
                        outline.add(rgb);
                    }
                }
            }
            int commonest = colours.values().stream().max(Integer::compare).orElse(0);
            boolean block = opaque == SIZE * SIZE;
            if (colours.size() < 5) {
                LINT_WARNINGS.add(name + ": " + colours.size() + " colours");
            }
            if (block && commonest * 2 > opaque) {
                LINT_WARNINGS.add(name + ": one colour on " + 100 * commonest / opaque + " %");
            }
            if (name.startsWith("item/") && outline.size() < 2) {
                LINT_WARNINGS.add(name + ": single-colour outline");
            }
        }
        LINT_WARNINGS.forEach(warning -> System.out.println("lint: " + warning));
        System.out.println("lint: " + LINT_WARNINGS.size() + " warning(s)");
    }

    /** The texture as the game shows it at rest (first frame); hand-edited textures come from disk. */
    private static BufferedImage render(String name) throws IOException {
        int size = size(name);
        if (HAND_EDITED.contains(name)) {
            BufferedImage file = ImageIO.read(OUTPUT.resolve(name + ".png").toFile());
            return file.getSubimage(0, 0, size, size);
        }
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        try {
            Texture texture = TEXTURES.get(name);
            if (texture.cyclingPainter() != null) {
                texture.cyclingPainter().paint(image, seeded(name), 0.0);
            } else {
                texture.painter().paint(image, seeded(name));
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return image;
    }

    /** {@link #render} for painters, which cannot throw checked exceptions. */
    private static BufferedImage renderUnchecked(String name) {
        try {
            return render(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int size(String name) {
        return name.startsWith("particle/") ? PARTICLE_SIZE : LARGE.contains(name) ? FLOW_SIZE : SIZE;
    }

    /**
     * Fails the run on a texture the game would show wrongly: wrong size (a strip of square frames: 16×16, the flowing
     * fluid 32×32, particles 8×8), half-transparent pixels (cutout and mipmaps need alpha 0 or 255), interpolated frames
     * whose alpha mask differs (interpolation would blend alpha), or an opaque cycling texture with a hole.
     */
    private static void check(String name, BufferedImage image, Animation animation) {
        int width = size(name);
        if (image.getWidth() != width || image.getHeight() != width * animation.frames()) {
            throw new IllegalStateException(name + ": " + image.getWidth() + "×" + image.getHeight() + ", expected "
                    + width + "×" + width * animation.frames());
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255 && !name.startsWith(SOFT_ALPHA)) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
                boolean opaque = alpha != 0;
                if (animation.opaque() && !opaque) {
                    throw new IllegalStateException(name + ": a cycling texture must be opaque (transparent at " + x + "," + y + ")");
                }
                if (animation.interpolate() && opaque != ((image.getRGB(x, y % width) >>> 24) != 0)) {
                    throw new IllegalStateException(name + ": frame " + y / width + " has another alpha mask at " + x + "," + y % width);
                }
            }
        }
    }

    /**
     * Every glowing plant agrees with its glow layer: each pixel its painter marked as glowing has a glow tone, no other
     * pixel uses a glow tone, and the layer is not empty.
     */
    private static void checkGlowLayers() throws IOException {
        for (Map.Entry<String, String> entry : GLOW_LAYERS.entrySet()) {
            String block = entry.getValue();
            BufferedImage image = render(block);
            boolean[][] mask = glowMask(image);
            Set<Integer> tones = GLOW_TONES.get(block);
            int pixels = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!isOpaque(image, x, y)) {
                        if (mask[x][y]) {
                            throw new IllegalStateException(block + " at " + x + "," + y + ": glowing pixel is transparent");
                        }
                        continue;
                    }
                    boolean glowTone = tones.contains(image.getRGB(x, y) & 0xFFFFFF);
                    if (mask[x][y] != glowTone) {
                        throw new IllegalStateException(block + " at " + x + "," + y + ": "
                                + (mask[x][y] ? "glowing pixel without a glow tone" : "glow tone on a pixel that does not glow"));
                    }
                    pixels += mask[x][y] ? 1 : 0;
                }
            }
            if (pixels == 0) {
                throw new IllegalStateException(entry.getKey() + " is empty");
            }
        }
    }

    /**
     * Side textures that show their ground below a crust must carry exactly the pixels of that ground from the given
     * row down: {side, ground, first row that is ground}; and variants that only add marks to a block ({copy, block, -1}:
     * checked only for the same mean tone family, so not here).
     */
    private static final String[][] SIDE_RULES = {
            {"block/bubble_spring_side", "block/tidestone", "4"}};

    private static void checkSideRules() throws IOException {
        for (String[] rule : SIDE_RULES) {
            BufferedImage side = render(rule[0]);
            BufferedImage ground = render(rule[1]);
            for (int y = Integer.parseInt(rule[2]); y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (side.getRGB(x, y) != ground.getRGB(x, y)) {
                        throw new IllegalStateException(rule[0] + " differs from " + rule[1] + " at " + x + "," + y);
                    }
                }
            }
        }
    }

    /**
     * Throws unless the slime trail's left and right edge columns are identical (71a review), so a trail runs on
     * seamlessly into a neighbouring trail block.
     */
    private static void checkSlimeTrailEdges() throws IOException {
        BufferedImage image = render("block/slime_trail");
        for (int y = 0; y < SIZE; y++) {
            if (image.getRGB(0, y) != image.getRGB(SIZE - 1, y)) {
                throw new IllegalStateException("slime_trail: edge columns differ at row " + y + ", the trail would not join");
            }
        }
    }

    // ---- Tidestone ----------------------------------------------------------------------------------------------

    /**
     * Tidestone (66): blue-grey layered rock. Two wavy layers per block (the waves are periodic across the tile, so a
     * cliff shows continuous strata), each lit on its upper part and darker towards a broken seam below; horizontally
     * stretched grain and a few pale silt grains with a shadow pixel. Layout picked by {@link #bestOf}.
     */
    private static void tidestone(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::tidestoneLayout);
    }

    private static void tidestoneLayout(BufferedImage image, Random random) {
        double phase = random.nextDouble() * 2 * Math.PI;
        double phase2 = random.nextDouble() * 2 * Math.PI;
        double[][] streaks = valueNoise(random, 8, 2);
        double[][] fine = valueNoise(random, 4, 1);
        double[][] seamGaps = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double wave = y + 1.1 * Math.sin(2 * Math.PI * x / SIZE + phase) + 0.5 * Math.sin(4 * Math.PI * x / SIZE + phase2);
                double inLayer = frac(wave / 8.0);
                double t = 0.62 - 0.32 * inLayer + 0.3 * (streaks[x][y] - 0.5) + 0.25 * (fine[x][y] - 0.5) + 0.14 * (random.nextDouble() - 0.5);
                if (inLayer > 0.88 && seamGaps[x][y] > 0.3) {
                    t = 0.08 + 0.1 * random.nextDouble();
                }
                set(image, x, y, shade(TIDESTONE, t));
            }
        }
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, TIDESTONE[5]);
            setWrapped(image, x + 1, y + 1, TIDESTONE[2]);
        }
    }

    /**
     * Cobbled tidestone: rounded stones (wrapping cells) set in a dark joint, each lit on its upper left and shadowed on
     * its lower right, with a faint layering inside each stone (it is broken tidestone).
     */
    private static void cobbledTidestone(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::cobbledTidestoneLayout);
    }

    private static void cobbledTidestoneLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 9);
        double[] base = new double[cells.count()];
        double[] layer = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.4 + 0.3 * random.nextDouble();
            layer[i] = random.nextDouble() * 2 * Math.PI;
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.55) {
                    set(image, x, y, gap < 0.3 ? TIDESTONE_MORTAR : TIDESTONE[0]);
                    continue;
                }
                double light = base[nearest.index()] - 0.4 * slope + 0.1 * Math.sin(y * 1.6 + layer[nearest.index()])
                        + 0.12 * (random.nextDouble() - 0.5);
                if (gap < 1.2) {
                    light += slope < 0 ? 0.16 : -0.14;
                }
                set(image, x, y, shade(TIDESTONE, light));
            }
        }
    }

    /** Polished tidestone: a smooth mottled face with two faint, broken layer lines and vanilla's bevel. */
    private static void polishedTidestone(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        double[][] broad = valueNoise(random, 8, 8);
        int[] lines = {4 + random.nextInt(2), 10 + random.nextInt(2)};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.25 * (noise[x][y] - 0.5) + 0.25 * (broad[x][y] - 0.5) + 0.15 * (random.nextDouble() - 0.5);
                if ((y == lines[0] || y == lines[1]) && random.nextInt(4) != 0) {
                    t -= 0.18;
                }
                set(image, x, y, shade(TIDESTONE, t));
            }
        }
        bevel(image, TIDESTONE);
    }

    /**
     * Clawed tidestone (72, a creature trace): the tidestone pixels with three parallel claw gouges slashing down to the
     * left: a dark groove, its upper left wall in shadow and its lower right wall lit (light from the top left), the
     * ends shallow. The marks stay inside the tile (71a review: no wrap, so no stray claw pixels on the far edge): a claw
     * slants at most 6 px to the left, the third starts 6 px right of the first, so {@code start} 7–8 keeps x in 0–15.
     */
    private static void clawedTidestone(BufferedImage image, Random random) {
        copy(renderUnchecked("block/tidestone"), image);
        int start = 7 + random.nextInt(2);
        for (int claw = 0; claw < 3; claw++) {
            int length = 9 + random.nextInt(3) - (claw == 2 ? 2 : 0);
            int y0 = 2 + random.nextInt(2) + (claw == 2 ? 1 : 0);
            for (int j = 0; j < length; j++) {
                int x = start + 3 * claw - (int) Math.round(j * 0.55);
                int y = y0 + j;
                boolean end = j == 0 || j == length - 1;
                set(image, x, y, end ? TIDESTONE[1] : TIDESTONE[0]);
                set(image, x + 1, y, end ? TIDESTONE[3] : TIDESTONE[6]);
                if (!end) {
                    set(image, x - 1, y, TIDESTONE[1]);
                }
            }
        }
    }

    // ---- Moonpetal and withered moonpetal -----------------------------------------------------------------------

    /**
     * Moonpetal log side (66): smooth white-silver bark with lilac shadows, vertical fibre, horizontal lenticels like a
     * birch's (but grey-lilac, not black) each with a lit pixel above, a few silver glints and two spots where the bark
     * peeled to the pink wood.
     */
    private static void moonpetalLog(BufferedImage image, Random random) {
        double[][] fibre = valueNoise(random, 2, 8);
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.56 + 0.4 * (fibre[x][y] - 0.5) + 0.25 * (broad[x][y] - 0.5) + 0.12 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(MOONPETAL_BARK, t));
            }
        }
        for (int i = 0; i < 8; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 2 + random.nextInt(3);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x + j, y, MOONPETAL_LENTICEL[j == 0 || j == length - 1 ? 1 : 0]);
                setWrapped(image, x + j, y - 1, MOONPETAL_BARK[5]);
            }
        }
        for (int i = 0; i < 3; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), MOONPETAL_SHEEN);
        }
        for (int i = 0; i < 2; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, MOONPETAL_WOOD[3]);
            setWrapped(image, x, y + 1, MOONPETAL_WOOD[2]);
            setWrapped(image, x + 1, y + 1, MOONPETAL_BARK[0]);
        }
    }

    /**
     * Log end grain: a bark rim (two pixels, as a ring of {@code bark}), growth rings in squarish rounds lit to the top
     * left, a darker pith; {@code cracked} adds three radial drying cracks (the dead withered moonpetal).
     */
    private static void logTop(BufferedImage image, Random random, int[] wood, int[] bark, boolean cracked) {
        double start = random.nextDouble() * 2 * Math.PI;
        double[] cracks = {start, start + 2.1 + 0.3 * random.nextDouble(), start + 4.2 + 0.3 * random.nextDouble()};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double r = 0.55 * Math.max(Math.abs(dx), Math.abs(dy)) + 0.45 * Math.hypot(dx, dy);
                if (r > 6.6) {
                    set(image, x, y, shade(bark, 0.35 + 0.55 * random.nextDouble()));
                    continue;
                }
                if (r > 5.8) {
                    set(image, x, y, shade(bark, 0.05 + 0.3 * random.nextDouble()));
                    continue;
                }
                boolean crack = false;
                if (cracked) {
                    double angle = Math.atan2(dy, dx);
                    for (double c : cracks) {
                        double diff = Math.abs(Math.atan2(Math.sin(angle - c), Math.cos(angle - c)));
                        crack |= diff * r < 0.6 && r > 1.5 && r < 5.4;
                    }
                }
                if (crack) {
                    set(image, x, y, wood[0]);
                } else if (r < 1.2) {
                    set(image, x, y, wood[1]);
                } else {
                    double light = 0.5 + 0.22 * Math.cos(r * 2.0) - 0.025 * (dx + dy) + 0.14 * (random.nextDouble() - 0.5);
                    set(image, x, y, shade(wood, light));
                }
            }
        }
    }

    /**
     * Withered moonpetal log side (66): dead grey bark split by deep vertical cracks into long plates (columns of 3–5 px,
     * plates of 4–8 rows), each plate lit on its top row and left column; a few plates have peeled off and show the
     * grey-beige wood.
     */
    private static void witheredLog(BufferedImage image, Random random) {
        int x0 = 0;
        while (x0 < SIZE) {
            int width = Math.min(SIZE - x0, 3 + random.nextInt(3));
            if (SIZE - x0 - width > 0 && SIZE - x0 - width < 3) {
                width = SIZE - x0;
            }
            int y0 = random.nextInt(SIZE);
            int covered = 0;
            while (covered < SIZE) {
                int height = Math.min(SIZE - covered, 4 + random.nextInt(5));
                if (SIZE - covered - height < 4 && SIZE - covered - height > 0) {
                    height = SIZE - covered;
                }
                double tone = 0.3 + 0.4 * random.nextDouble();
                boolean peeled = random.nextInt(7) == 0;
                for (int dy = 0; dy < height; dy++) {
                    int y = Math.floorMod(y0 + covered + dy, SIZE);
                    for (int dx = 0; dx < width; dx++) {
                        int x = x0 + dx;
                        int color;
                        if (dx == width - 1) {
                            color = WITHERED_BARK[0];
                        } else if (peeled && dx > 0 && dy > 0 && dy < height - 1) {
                            color = shade(WITHERED_WOOD, 0.3 + 0.4 * random.nextDouble());
                        } else if (dy == 0 || dx == 0) {
                            color = shade(WITHERED_BARK, tone + 0.32);
                        } else {
                            color = shade(WITHERED_BARK, tone + 0.16 * (random.nextDouble() - 0.5));
                        }
                        set(image, x, y, color);
                    }
                }
                covered += height;
            }
            x0 += width;
        }
    }

    /**
     * Moonpetal leaves (66): a blossom crown in pink-lilac (fixed colour, no tint): a depth of darker lilac with holes
     * (cutout_mipped, like vanilla leaves) and small five-pixel florets on top, lit petals on the upper left, a shadowed
     * petal on the lower right and a cream heart on some.
     */
    private static void moonpetalLeaves(BufferedImage image, Random random) {
        double[][] clumps = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (clumps[x][y] < 0.22 || random.nextInt(9) == 0) {
                    continue;
                }
                set(image, x, y, shade(BLOSSOM, 0.05 + 0.35 * clumps[x][y] + 0.1 * random.nextDouble()));
            }
        }
        int[][] petals = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        for (int i = 0; i < 26; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if (clumps[x][y] < 0.3) {
                continue;
            }
            double tone = 0.55 + 0.25 * clumps[x][y];
            for (int[] petal : petals) {
                double light = tone + (petal[0] + petal[1] < 0 ? 0.15 : petal[0] + petal[1] > 0 ? -0.2 : 0);
                setWrapped(image, x + petal[0], y + petal[1], shade(BLOSSOM, light));
            }
            setWrapped(image, x, y, random.nextInt(3) == 0 ? BLOSSOM_HEART : BLOSSOM[5]);
        }
    }

    /** Moonpetal sapling: a little silver stem with two leaves and a crown of pink-lilac blossom with white hearts (cross). */
    private static void moonpetalSapling(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                "................",
                "......5.4.......",
                "....45545.4.....",
                "...4W5435W4.....",
                "..354454534.3...",
                "...4354W5443....",
                "....3443433.2...",
                ".....23.S32.....",
                ".......Ss.......",
                "......LSs.......",
                ".......Ss.l.....",
                ".......SsL......",
                ".......Ss.......",
                ".......Ss.......",
                ".......Ss......."}, Map.of(
                '2', BLOSSOM[1], '3', BLOSSOM[2], '4', BLOSSOM[3], '5', BLOSSOM[4], 'W', BLOSSOM_HEART,
                'S', MOONPETAL_BARK[4], 's', MOONPETAL_BARK[1], 'L', SAPLING_LEAF[1], 'l', SAPLING_LEAF[0]));
    }

    // ---- Reeds, peat, hanging moss ------------------------------------------------------------------------------

    /**
     * Reeds (66, cross): five thin stalks that sway a pixel (periodic over the height, so stacked blocks join) with nodes
     * and narrow blades that leave the stalk and droop. {@code top}: the stalks end at different heights in feathery
     * brown-beige plumes; the stalk positions are shared with the lower part ({@code block/reeds#stalks}), so they join.
     */
    private static void reeds(BufferedImage image, boolean top) {
        Random stalks = seeded("block/reeds#stalks");
        Random random = seeded(top ? "block/reeds_top" : "block/reeds");
        int[] columns = {2, 5, 8, 11, 13};
        for (int column : columns) {
            int x0 = column + stalks.nextInt(2) - 1;
            double sway = stalks.nextDouble() * 2 * Math.PI;
            int end = top ? 3 + random.nextInt(6) : 0;
            for (int y = SIZE - 1; y >= end; y--) {
                int x = x0 + (int) Math.round(0.8 * Math.sin(2 * Math.PI * y / SIZE + sway));
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int tone = Math.floorMod(y + column, 5) == 0 ? 1 : 3 - (y > 10 ? 1 : 0) + (top && y - end < 3 ? 1 : 0);
                set(image, x, y, REED[tone]);
            }
            if (top) {
                int x = x0 + (int) Math.round(0.8 * Math.sin(2 * Math.PI * end / SIZE + sway));
                int lean = random.nextBoolean() ? 1 : -1;
                for (int j = 0; j < 5; j++) {
                    int py = end - 1 - j + (j > 2 ? 1 : 0);
                    int px = x + (j > 2 ? lean : 0);
                    if (py < 0) {
                        break;
                    }
                    set(image, Math.max(0, Math.min(SIZE - 1, px)), py, REED_PLUME[j == 4 ? 4 : 2 + j % 2]);
                    int side = px + (j % 2 == 0 ? -1 : 1);
                    if (side >= 0 && side < SIZE && j < 4) {
                        set(image, side, py, REED_PLUME[j % 2 == 0 ? 3 : 1]);
                    }
                }
                set(image, Math.max(0, Math.min(SIZE - 1, x)), end, REED_PLUME[0]);
            }
            // one blade per stalk: leaves the stalk, rises one pixel, then droops
            int by = top ? 9 + random.nextInt(4) : 3 + random.nextInt(10);
            int dir = random.nextBoolean() ? 1 : -1;
            int bx = x0 + (int) Math.round(0.8 * Math.sin(2 * Math.PI * by / SIZE + sway));
            for (int j = 1; j <= 4; j++) {
                int px = bx + dir * j;
                int py = by - (j == 1 ? 1 : 0) + (j > 2 ? j - 2 : 0);
                if (px >= 0 && px < SIZE && py >= 0 && py < SIZE) {
                    set(image, px, py, REED[j == 1 ? 5 : j == 4 ? 2 : 4]);
                }
            }
        }
    }

    /**
     * Peat (66): dark brown fibrous bog soil in horizontal layers (stretched grain), pale root threads and dark wet
     * spots. Layout picked by {@link #bestOf}.
     */
    private static void peat(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::peatLayout);
    }

    private static void peatLayout(BufferedImage image, Random random) {
        double[][] fibre = valueNoise(random, 8, 2);
        double[][] fine = valueNoise(random, 4, 1);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.4 * (fibre[x][y] - 0.5) + 0.3 * (fine[x][y] - 0.5) + 0.22 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(PEAT, t));
            }
        }
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 2 + random.nextInt(4);
            for (int j = 0; j < length; j++) {
                if (j > 0 && random.nextInt(3) == 0) {
                    y += random.nextBoolean() ? 1 : -1;
                }
                setWrapped(image, x + j, y, PEAT_ROOT[j == 0 ? 1 : random.nextInt(2)]);
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, PEAT[0]);
            setWrapped(image, x + 1, y, PEAT[0]);
            setWrapped(image, x, y - 1, PEAT[4]);
        }
    }

    /**
     * Hanging swamp moss (66): grey-green strands hanging from the top edge (cutout), each swaying a pixel, lighter at
     * the top where the light falls, a few side wisps, ending at different lengths.
     */
    private static void hangingSwampMoss(BufferedImage image, Random random) {
        hangingStrands(image, random, SWAMP_MOSS, 0.65, false);
    }

    /**
     * Strands hanging from the top edge: about every second column a strand of 5–16 rows that sways a pixel, its tone
     * falling from {@code topLight} towards the tip; {@code thick} strands are two pixels wide (roots), lit on the left.
     * Returns the strands, so a caller can put nodes exactly on them ({@link Strand#x}).
     */
    private static List<Strand> hangingStrands(BufferedImage image, Random random, int[] palette, double topLight, boolean thick) {
        List<Strand> strands = new ArrayList<>();
        int x0 = random.nextInt(2);
        while (x0 < SIZE) {
            Strand strand = new Strand(x0, 5 + random.nextInt(12), random.nextDouble() * 2 * Math.PI);
            int length = strand.length();
            boolean wide = thick && random.nextInt(3) == 0 && x0 < SIZE - 1;
            for (int y = 0; y < length; y++) {
                int x = strand.x(y);
                double t = topLight - 0.45 * y / 16.0 + 0.18 * (random.nextDouble() - 0.5);
                if (x >= 0 && x < SIZE) {
                    set(image, x, y, shade(palette, t));
                }
                if (wide && x + 1 < SIZE && x + 1 >= 0) {
                    set(image, x + 1, y, shade(palette, t - 0.3));
                }
                if (!thick && y > 2 && random.nextInt(6) == 0) {
                    int side = x + (random.nextBoolean() ? 1 : -1);
                    if (side >= 0 && side < SIZE) {
                        set(image, side, y, shade(palette, t - 0.15));
                    }
                }
            }
            strands.add(strand);
            x0 += (wide ? 3 : 2) + random.nextInt(2);
        }
        return strands;
    }

    /** One hanging strand: from column {@code x0} at the top edge, {@code length} rows, swaying a pixel by {@code sway}. */
    private record Strand(int x0, int length, double sway) {
        /** The strand's column on row {@code y}: the exact pixel the strand was painted on. */
        int x(int y) {
            return x0 + (int) Math.round(0.7 * Math.sin(y * 0.6 + sway));
        }
    }

    // ---- Giant lily pad -----------------------------------------------------------------------------------------

    /** The whole 2×2 pad (32×32), painted once; each part block shows one quadrant, so the parts always join. */
    private static BufferedImage lilyPad;

    private static BufferedImage lilyPad() {
        if (lilyPad == null) {
            lilyPad = new BufferedImage(2 * SIZE, 2 * SIZE, BufferedImage.TYPE_INT_ARGB);
            paintLilyPad(lilyPad, seeded("block/giant_lily_pad"), 2 * SIZE, 15.4);
        }
        return lilyPad;
    }

    /**
     * Giant lily pad (66, multiblock 2×2) seen from above, north up: one round leaf over the four parts with a notch to
     * the south-east, light veins radiating from the centre, a slightly raised rim that catches the light on the far
     * (lower right) side and is shaded on the near side, a dark edge; transparent outside the leaf.
     */
    private static void paintLilyPad(BufferedImage image, Random random, int size, double radius) {
        double c = size / 2.0;
        double wobble = random.nextDouble() * 2 * Math.PI;
        double notch = Math.PI / 4 + 0.3 * (random.nextDouble() - 0.5);
        double[][] grid = new double[9][9];
        for (double[] row : grid) {
            for (int j = 0; j < row.length; j++) {
                row[j] = random.nextDouble();
            }
        }
        double cell = size / 8.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - c;
                double dy = y + 0.5 - c;
                double r = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                double edge = radius * (1 + 0.025 * Math.sin(6 * angle + wobble));
                double fromNotch = Math.abs(Math.atan2(Math.sin(angle - notch), Math.cos(angle - notch)));
                if (r > edge || (fromNotch * r < 0.9 + 0.05 * r && r > 1.2)) {
                    continue;
                }
                double gx = x / cell;
                double gy = y / cell;
                int ix = (int) gx;
                int iy = (int) gy;
                double noise = lerp(lerp(grid[ix][iy], grid[ix + 1][iy], smoothstep(gx - ix)),
                        lerp(grid[ix][iy + 1], grid[ix + 1][iy + 1], smoothstep(gx - ix)), smoothstep(gy - iy));
                double t = 0.48 + 0.2 * (noise - 0.5) - 0.12 * (dx + dy) / radius + 0.1 * (random.nextDouble() - 0.5);
                boolean vein = false;
                for (int k = 0; k < 9; k++) {
                    double va = notch + 2 * Math.PI * (k + 0.5) / 9;
                    double diff = Math.abs(Math.atan2(Math.sin(angle - va), Math.cos(angle - va)));
                    vein |= diff * r < 0.5 && r > 1.5 && r < edge - 1.5;
                }
                int color;
                if (r > edge - 0.9 || fromNotch * r < 1.6 + 0.05 * r) {
                    color = LILY[0];
                } else if (r > edge - 2.0) {
                    color = dx + dy > 0 ? LILY[5] : LILY[2];
                } else if (r < 1.4) {
                    color = LILY[1];
                } else if (vein) {
                    color = shade(LILY, t + 0.3);
                } else {
                    color = shade(LILY, t);
                }
                set(image, x, y, color);
            }
        }
    }

    private static void lilyPadPart(BufferedImage image, int part) {
        BufferedImage pad = lilyPad();
        int ox = part % 2 == 0 ? 0 : SIZE;
        int oy = part < 2 ? 0 : SIZE;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, pad.getRGB(ox + x, oy + y));
            }
        }
    }

    /** The giant lily pad in the inventory: the same leaf at 16×16 with a selective outline. */
    private static void lilyPadItem(BufferedImage image, Random random) {
        paintLilyPad(image, random, SIZE, 6.6);
        outlineSelective(image, LILY_OUTLINE_LIGHT, LILY_OUTLINE);
    }

    /**
     * Lotus (66): floats flat on the water like a lily pad (B: a flat model at lily pad height, fixed colour, cutout),
     * seen from above: a small round leaf with its notch, and on it an open flower: eight pointed outer petals in pink
     * (lit on the upper left, deeper pink on the lower right), a ring of white inner petals and a golden heart.
     */
    private static void lotus(BufferedImage image, Random random) {
        paintLilyPad(image, random, SIZE, 7.4);
        double cx = 7.5 + random.nextInt(2) - 0.5;
        double cy = 7.5 + random.nextInt(2) - 0.5;
        double turn = random.nextDouble() * Math.PI / 4;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double r = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx) - turn;
                // pointed petals: the radius reaches out towards eight petal tips
                double petal = Math.abs(Math.cos(4 * angle));
                double outer = 2.6 + 2.0 * Math.pow(petal, 3);
                double inner = 1.6 + 0.9 * Math.pow(Math.abs(Math.cos(4 * angle + Math.PI / 4)), 3);
                if (r < 0.9) {
                    set(image, x, y, LOTUS_HEART);
                } else if (r < inner) {
                    set(image, x, y, dx + dy < 0 ? LOTUS[5] : LOTUS[4]);
                } else if (r < outer) {
                    double light = 0.55 - 0.12 * (dx + dy) - 0.25 * (r / outer) + 0.1 * (random.nextDouble() - 0.5);
                    set(image, x, y, petal < 0.35 ? LOTUS[0] : shade(LOTUS, light));
                }
            }
        }
    }

    // ---- Glow flora ---------------------------------------------------------------------------------------------

    /**
     * Glowcaps (66, cross): three mushrooms back to front, a domed cap lit on the upper left, a pale stem shadowed on the
     * right; the gill rim under the cap glows (all of it for the glowcap, two pixels for the blighted one) and a few
     * glowing spots sit on the caps. The blighted glowcap is the same fungus in sickly green-grey with one weak spot.
     */
    private static void fungi(BufferedImage image, Random random, Fungus fungus) {
        double[][] caps = {{10.5, 8, 3.6, 4, 2}, {4.5, 10, 3.0, 3, 2}, {8.0, 12, 2.0, 2, 1}};
        for (double[] cap : caps) {
            mushroom(image, random, fungus, cap[0] + random.nextInt(2) - 0.5, (int) cap[1], cap[2], (int) cap[3], (int) cap[4]);
        }
    }

    private static void mushroom(BufferedImage image, Random random, Fungus fungus, double cx, int top, double half, int height, int stemWidth) {
        int[] stem = fungus.stem();
        int[] cap = fungus.cap();
        int[] glow = fungus.glow();
        int left = (int) Math.round(cx - stemWidth / 2.0);
        for (int y = top + 1; y < SIZE; y++) {
            for (int i = 0; i < stemWidth; i++) {
                int x = left + i;
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int tone = y == SIZE - 1 ? 0 : stemWidth == 1 ? 2 : i == 0 ? 3 : 1;
                set(image, x, y, stem[tone]);
            }
        }
        List<int[]> capPixels = new ArrayList<>();
        for (int dy = 0; dy <= height; dy++) {
            int y = top - dy;
            double w = half * Math.sqrt(Math.max(0, 1 - Math.pow(dy / (height + 0.7), 2)));
            int x0 = (int) Math.round(cx - w);
            int x1 = (int) Math.round(cx + w) - 1;
            for (int x = x0; x <= x1; x++) {
                if (x < 0 || x >= SIZE || y < 0) {
                    continue;
                }
                if (dy == 0) {
                    boolean rim = x == x0 || x == x1;
                    if (fungus.rimGlow()) {
                        glow(image, x, y, glow[rim ? 3 : 1 + Math.floorMod(x, 2)]);
                    } else if (rim) {
                        glow(image, x, y, glow[1]);
                    } else {
                        set(image, x, y, cap[0]);
                    }
                    continue;
                }
                double light = -0.6 * (x + 0.5 - cx) / half + 0.5 * dy / height + 0.15 * (random.nextDouble() - 0.5);
                int tone = (int) Math.round(1.6 + 2.2 * light);
                set(image, x, y, cap[Math.max(1, Math.min(cap.length - 1, tone))]);
                if (dy < height) {
                    capPixels.add(new int[] {x, y});
                }
            }
        }
        for (int i = 0; i < fungus.spots() && !capPixels.isEmpty(); i++) {
            int[] spot = capPixels.remove(random.nextInt(capPixels.size()));
            glow(image, spot[0], spot[1], glow[fungus.rimGlow() ? 4 : 2]);
        }
    }

    /**
     * Glow bloom (66, cross): a violet flower of five petals on a blue-green stem with two leaves; the heart and the
     * petal tips glow blue-white (two layers).
     */
    private static void glowBloom(BufferedImage image, Random random) {
        double cx = 7.5 + random.nextInt(2);
        double cy = 5.5;
        for (int y = 8; y < SIZE; y++) {
            set(image, (int) cx, y, BLOOM_STEM[y == SIZE - 1 ? 0 : y % 3 == 0 ? 3 : 2]);
        }
        int stemX = (int) cx;
        int[][] leaves = {{-1, 12, 3}, {-2, 11, 3}, {-3, 11, 2}, {-4, 10, 1}, {1, 13, 3}, {2, 12, 3}, {3, 12, 2}, {4, 11, 1}};
        for (int[] leaf : leaves) {
            set(image, stemX + leaf[0], leaf[1], BLOOM_STEM[leaf[2]]);
        }
        double turn = -Math.PI / 2 + 0.3 * (random.nextDouble() - 0.5);
        for (int y = 0; y < 11; y++) {
            for (int x = 1; x < SIZE - 1; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double r = Math.hypot(dx, dy);
                if (r < 1.2) {
                    glow(image, x, y, dx + dy < 0 ? GLOW[5] : GLOW[4]);
                    continue;
                }
                for (int k = 0; k < 5; k++) {
                    double pa = turn + k * 2 * Math.PI / 5;
                    double along = dx * Math.cos(pa) + dy * Math.sin(pa);
                    double across = -dx * Math.sin(pa) + dy * Math.cos(pa);
                    double e = Math.pow((along - 2.3) / 2.5, 2) + Math.pow(across / 1.35, 2);
                    if (along > 0.4 && e < 1) {
                        if (along > 3.9) {
                            glow(image, x, y, GLOW[2]);
                        } else if (r < 1.9) {
                            glow(image, x, y, GLOW[3]);
                        } else {
                            double light = 0.55 - 0.14 * (dx + dy) + 0.15 * (random.nextDouble() - 0.5) - (Math.abs(across) > 0.9 ? 0.2 : 0);
                            set(image, x, y, shade(BLOOM_PETAL, light));
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Glowing hanging roots (66): brown-grey roots hanging from the top edge (some two pixels wide), a few glowing nodes
     * along them and glowing tips, as the light of the island undersides.
     */
    private static void glowingHangingRoots(BufferedImage image, Random random) {
        List<Strand> strands = hangingStrands(image, random, ROOT, 0.75, true);
        for (Strand strand : strands) {
            int length = strand.length();
            int tipY = length - 1;
            int tipX = strand.x(tipY);
            if (tipX >= 0 && tipX < SIZE && random.nextInt(3) != 0) {
                glow(image, tipX, tipY, GLOW[random.nextInt(3) == 0 ? 5 : 4]);
                if (tipY > 0) {
                    int aboveX = strand.x(tipY - 1);
                    if (aboveX >= 0 && aboveX < SIZE) {
                        glow(image, aboveX, tipY - 1, GLOW[2]);
                    }
                }
            }
            if (length > 8 && random.nextBoolean()) {
                int ny = 3 + random.nextInt(length - 6);
                int nx = strand.x(ny);
                if (nx >= 0 && nx < SIZE) {
                    glow(image, nx, ny, GLOW[3]);
                }
            }
        }
    }

    // ---- Bubble spring ------------------------------------------------------------------------------------------

    /**
     * Bubble spring top (66): a pale mineral crust (travertine, wrapping cells lit on the upper left) around two or three
     * vents of dark water, each with a lit lower right inner wall (light falls in from the top left) and a pale bubble
     * ring beside it.
     */
    private static void bubbleSpringTop(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 11);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.25 + 0.3 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double light = gap < 0.35 ? 0.0 : base[nearest.index()] - 0.3 * (nearest.dx() + nearest.dy()) + 0.12 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(MINERAL, light));
            }
        }
        double[][] vents = {{5.0 + random.nextInt(2), 5.5 + random.nextInt(2), 1.9}, {11.0 + random.nextInt(2), 10.0 + random.nextInt(2), 1.5},
                {4.5, 12.0, 1.0}};
        for (double[] vent : vents) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int x = (int) Math.floor(vent[0]) + dx;
                    int y = (int) Math.floor(vent[1]) + dy;
                    double ox = x + 0.5 - vent[0];
                    double oy = y + 0.5 - vent[1];
                    double d = Math.hypot(ox, oy);
                    if (d < vent[2]) {
                        setWrapped(image, x, y, d < vent[2] - 0.8 ? SPRING_WATER[0] : ox + oy > 0.4 ? SPRING_WATER[3] : SPRING_WATER[1]);
                    } else if (d < vent[2] + 0.9 && ox + oy < -0.5) {
                        setWrapped(image, x, y, MINERAL[1]);
                    }
                }
            }
            int bx = (int) Math.floor(vent[0] + vent[2] + 0.5);
            int by = (int) Math.floor(vent[1] - vent[2]);
            setWrapped(image, bx, by, BUBBLE);
            setWrapped(image, bx + 1, by, MINERAL[5]);
            setWrapped(image, bx, by - 1, MINERAL[5]);
        }
    }

    /**
     * Bubble spring side (66): tidestone (the same pixels from row 4 down, {@link #SIDE_RULES}) under a ragged mineral
     * crust of 1–3 rows with short drips.
     */
    private static void bubbleSpringSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/tidestone"), image);
        for (int x = 0; x < SIZE; x++) {
            int depth = 1 + random.nextInt(3);
            for (int y = 0; y < depth; y++) {
                set(image, x, y, y == depth - 1 ? MINERAL[1] : shade(MINERAL, 0.75 - 0.25 * y + 0.25 * random.nextDouble()));
            }
            if (depth < 3 && random.nextInt(4) == 0) {
                set(image, x, depth, MINERAL[2]);
                set(image, x, depth + 1, MINERAL[0]);
            }
        }
    }

    // ---- Ores (67) ----------------------------------------------------------------------------------------------

    /** Raw tidesilver: three angular nuggets of silver metal with a teal sheen, back to front, selective outline. */
    private static void rawTidesilver(BufferedImage image, Random random) {
        double[][] nuggets = {{10.2, 5.6, 3.4, 0.9, 1.1}, {5.6, 7.6, 4.2, 1.0, 0.85}, {9.6, 10.8, 3.6, 1.15, 0.9}};
        for (double[] nugget : nuggets) {
            nugget(image, nugget[0], nugget[1], nugget[2], nugget[3], nugget[4], RAW_TIDESILVER);
        }
        outlineSelective(image, TIDESILVER[1], TIDESILVER[0]);
    }

    /**
     * Jade overlay (stone, tidestone, deepslate): three or four milky, rounded nodules (smooth shading from the top left,
     * a cloudy lighter swirl, one gloss pixel) with the selective outline. Matte: no pulse, not emissive.
     */
    private static void jadeOreOverlay(BufferedImage image, Random random) {
        int target = 3 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < target; attempt++) {
            double rx = (placed == 0 ? 2.2 : 1.5) + 0.4 * random.nextDouble();
            double ry = rx * (0.7 + 0.3 * random.nextDouble());
            double cx = 2 + rx + random.nextDouble() * (SIZE - 4 - 2 * rx);
            double cy = 2 + ry + random.nextDouble() * (SIZE - 4 - 2 * ry);
            BufferedImage nodule = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            pebble(nodule, random, cx, cy, rx, ry, JADE);
            outlineSelective(nodule, JADE[1], JADE[0]);
            if (merge(image, nodule)) {
                placed++;
            }
        }
    }

    /** A smooth rounded stone (ellipse) of {@code palette} [2..6]: lit upper left, a milky swirl, one gloss pixel. */
    private static void pebble(BufferedImage image, Random random, double cx, double cy, double rx, double ry, int[] palette) {
        double swirl = random.nextDouble() * 2 * Math.PI;
        int glossX = -1;
        int glossY = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = (x + 0.5 - cx) / rx;
                double dy = (y + 0.5 - cy) / ry;
                double d = dx * dx + dy * dy;
                if (d > 1) {
                    continue;
                }
                double light = -0.55 * (dx + dy) - 0.35 * d + 0.25 * Math.sin(3 * dx + 2 * dy + swirl);
                int tone = light > 0.45 ? 5 : light > 0.05 ? 4 : light > -0.4 ? 3 : 2;
                set(image, x, y, palette[tone]);
                if (glossX < 0 || (x - cx) + (y - cy) < (glossX - cx) + (glossY - cy)) {
                    glossX = x;
                    glossY = y;
                }
            }
        }
        if (glossX >= 0) {
            set(image, Math.min(SIZE - 1, glossX + 1), Math.min(SIZE - 1, glossY + 1), palette[6]);
        }
    }

    /** Jade: a polished oval cabochon, milky blue-green with a cloudy band and a gloss, selective outline. */
    private static void jadeItem(BufferedImage image, Random random) {
        pebble(image, random, 8.0, 8.5, 5.6, 4.6, JADE);
        set(image, 6, 6, JADE[6]);
        outlineSelective(image, JADE[1], JADE[0]);
    }

    // ---- Crystals (67, 68): the amethyst pattern ----------------------------------------------------------------

    /**
     * Crystal block (as the amethyst block): flat crystal faces (wrapping cells), each with its own tone by the way it
     * faces the light, edges lit on the upper left of a face and dark on the lower right, a few glints.
     */
    private static void crystalBlock(BufferedImage image, Random random, int[] palette) {
        Cells cells = Cells.random(random, 8);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.3 + 0.45 * random.nextDouble();
        }
        int[] faces = {palette[1], palette[2], palette[3], palette[4], palette[5]};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.5) {
                    set(image, x, y, slope < 0 ? palette[5] : palette[1]);
                    continue;
                }
                double light = base[nearest.index()] - 0.25 * slope + 0.08 * random.nextDouble();
                set(image, x, y, shade(faces, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), palette[6]);
        }
    }

    /**
     * Budding crystal (as budding amethyst): the crystal block with five dark pits, each with a bright nub of a new
     * crystal in it.
     */
    private static void buddingCrystal(BufferedImage image, Random random, String block, int[] palette) {
        copy(renderUnchecked(block), image);
        List<int[]> placed = new ArrayList<>();
        for (int attempt = 0; attempt < 300 && placed.size() < 5; attempt++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            boolean free = true;
            for (int[] other : placed) {
                int dx = Math.abs(x - other[0]);
                int dy = Math.abs(y - other[1]);
                dx = Math.min(dx, SIZE - dx);
                dy = Math.min(dy, SIZE - dy);
                free &= dx + dy >= 6;
            }
            if (!free) {
                continue;
            }
            placed.add(new int[] {x, y});
            setWrapped(image, x, y, palette[6]);
            setWrapped(image, x + 1, y, palette[4]);
            setWrapped(image, x, y + 1, palette[3]);
            setWrapped(image, x + 1, y + 1, palette[1]);
            setWrapped(image, x - 1, y, palette[0]);
            setWrapped(image, x, y - 1, palette[0]);
            setWrapped(image, x + 2, y + 1, palette[0]);
            setWrapped(image, x + 1, y + 2, palette[0]);
        }
    }

    /**
     * Crystal stage 1 to 4 (small, medium, large bud, cluster): 1, 2, 3 and 5 crystals from the bottom edge, as the
     * dew and fire crystal stages; pointed spikes, or for a {@code prism} crystal (aquamarine) hexagonal prisms with a
     * short pyramid tip.
     */
    private static void crystalStage(BufferedImage image, Random random, int stage, Crystal crystal) {
        int[] p = crystal.palette();
        boolean prism = crystal.prism();
        switch (stage) {
            case 1 -> crystalSpike(image, p, prism, 7.5, 1, 5, 0);
            case 2 -> {
                crystalSpike(image, p, prism, 4.5, 1, 6, -1);
                crystalSpike(image, p, prism, 10.5, 1, 8 + random.nextInt(2), 1);
            }
            case 3 -> {
                crystalSpike(image, p, prism, 3.5, 1, 7, -1);
                crystalSpike(image, p, prism, 11.5, 1, 8, 1);
                crystalSpike(image, p, prism, 7.5, 2, 11 + random.nextInt(2), 0);
            }
            default -> {
                crystalSpike(image, p, prism, 2.5, 1, 6 + random.nextInt(2), -2);
                crystalSpike(image, p, prism, 12.5, 1, 7 + random.nextInt(2), 2);
                crystalSpike(image, p, prism, 5.5, 2, 10, -1);
                crystalSpike(image, p, prism, 9.5, 2, 11, 1);
                crystalSpike(image, p, prism, 7.5, 2, 14, 0);
            }
        }
        outlineSelective(image, crystal.outlineLight(), crystal.outline());
    }

    /**
     * One crystal from the bottom row up: light left edge, lit left facet, a bright ridge, shaded right facet, dark right
     * edge; one tone darker in the lower third, the tip pale. A spike tapers over its whole height, a prism keeps its
     * width and tapers over its top {@code halfWidth + 1} rows.
     */
    private static void crystalSpike(BufferedImage image, int[] palette, boolean prism, double centre, int halfWidth, int height, int lean) {
        int tip = halfWidth + 1;
        for (int row = 0; row < height; row++) {
            int y = SIZE - 1 - row;
            int ridge = (int) Math.round(centre + lean * row / (double) height);
            int fromTop = height - 1 - row;
            int half = prism ? (fromTop < tip ? fromTop * halfWidth / tip : halfWidth)
                    : Math.min(halfWidth, fromTop * halfWidth / (halfWidth + 1));
            int foot = row < height / 3 ? 1 : 0;
            for (int x = ridge - half; x <= ridge + half; x++) {
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int tone = x == ridge - half && half > 0 ? 4 : x < ridge ? 3 : x == ridge ? 5 : x == ridge + half ? 1 : 2;
                if (row == 0) {
                    tone = x <= ridge ? 2 : 1;
                }
                set(image, x, y, fromTop == 0 ? palette[6] : palette[Math.max(1, tone - foot)]);
            }
        }
    }

    /**
     * A shard: one leaning crystal (a piece broken off a cluster), 14 rows tall and lifted one pixel, so it spans rows
     * 1–14 and the selective outline closes both under the foot and above the tip.
     */
    private static void crystalShard(BufferedImage image, Crystal crystal) {
        BufferedImage spike = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        crystalSpike(spike, crystal.palette(), crystal.prism(), 6.0, 2, 14, 4);
        for (int y = 1; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y - 1, spike.getRGB(x, y));
            }
        }
        outlineSelective(image, crystal.outlineLight(), crystal.outline());
    }

    /**
     * Purity crystal lamp (68): four crystal panes (milky white with gold sheen, lit on their upper left, a glint each)
     * in a frame of polished fossil stone: 2-px bars (border and cross), each bar lit on its own first row/column and
     * shaded on its second, the junctions lit, shaded or mid by both; the panes pulse (B makes the lamp emissive), the
     * stone frame stays still ({@link #STILL_TONES}).
     */
    private static void purityCrystalLamp(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int column = LAMP_BARS.indexOf(x);
                int row = LAMP_BARS.indexOf(y);
                if (column >= 0 || row >= 0) {
                    // per bar: 0 = its first (lit) line, 1 = its second (shaded) line; -1 = not in a bar this way
                    int vertical = column < 0 ? -1 : column % 2;
                    int horizontal = row < 0 ? -1 : row % 2;
                    boolean lit = (vertical == 0 || horizontal == 0) && vertical != 1 && horizontal != 1;
                    boolean shaded = (vertical == 1 || horizontal == 1) && vertical != 0 && horizontal != 0;
                    set(image, x, y, lit ? FOSSIL_STONE[5] : shaded ? FOSSIL_STONE[1] : FOSSIL_STONE[3]);
                    continue;
                }
                int px = x < 7 ? x - 2 : x - 9;
                int py = y < 7 ? y - 2 : y - 9;
                double light = 0.75 - 0.08 * (px + py) + 0.12 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(new int[] {PURITY[2], PURITY[3], PURITY[4], PURITY[5]}, light));
            }
        }
        for (int[] pane : new int[][] {{2, 2}, {9, 2}, {2, 9}, {9, 9}}) {
            set(image, pane[0] + 1, pane[1] + 1, PURITY[6]);
            set(image, pane[0] + 4, pane[1] + 4, PURITY[1]);
        }
    }

    /** Columns and rows of the lamp's 2-px frame bars (border and cross), in pairs: first line lit, second shaded. */
    private static final List<Integer> LAMP_BARS = List.of(0, 1, 7, 8, 14, 15);

    // ---- Gameplay blocks (72) -----------------------------------------------------------------------------------

    /**
     * Sucking mud (72): wet green-black mud with a soft sheen; three slow dimples each on its own beat, a dark ring that
     * draws in (the mud sucking down) with a lit lip, then a small bubble at the centre. Periodic in {@code phase}; opaque.
     */
    private static void suckingMud(BufferedImage image, Random random, double phase) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.45 + 0.4 * (broad[x][y] - 0.5) + 0.3 * (mid[x][y] - 0.5) + 0.2 * (random.nextDouble() - 0.5);
                set(image, x, y, t > 0.9 ? SUCKING_SHEEN : shade(SUCKING_MUD, t));
            }
        }
        for (int i = 0; i < 3; i++) {
            double cx = random.nextDouble() * SIZE;
            double cy = random.nextDouble() * SIZE;
            double offset = i / 3.0 + 0.1 * random.nextDouble();
            double maxRadius = 2.0 + random.nextDouble();
            double local = (phase + offset) % 1.0;
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    int x = (int) Math.floor(cx) + dx;
                    int y = (int) Math.floor(cy) + dy;
                    double ox = x + 0.5 - cx;
                    double oy = y + 0.5 - cy;
                    double d = Math.hypot(ox, oy);
                    if (local < 0.7) {
                        double r = maxRadius * (1 - local / 0.7) + 0.4;
                        if (Math.abs(d - r) < 0.55) {
                            setWrapped(image, x, y, ox + oy < 0 ? SUCKING_MUD[0] : SUCKING_MUD[4]);
                        } else if (d < r - 0.55) {
                            setWrapped(image, x, y, SUCKING_MUD[1]);
                        }
                    } else if (local < 0.85 && d < 0.8) {
                        setWrapped(image, x, y, SUCKING_SHEEN);
                    }
                }
            }
        }
    }

    /** {@code field} sampled at (x, y) with wrapping and bilinear interpolation, for drifting cycling textures. */
    private static double sample(double[][] field, double x, double y) {
        int size = field.length;
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double a = lerp(field[Math.floorMod(x0, size)][Math.floorMod(y0, size)], field[Math.floorMod(x0 + 1, size)][Math.floorMod(y0, size)], fx);
        double b = lerp(field[Math.floorMod(x0, size)][Math.floorMod(y0 + 1, size)], field[Math.floorMod(x0 + 1, size)][Math.floorMod(y0 + 1, size)], fx);
        return lerp(a, b, fy);
    }

    /**
     * Toxic pool, still (72, V-T3 A: opaque like lava): a thick, bright poison green that drifts slowly (two
     * noise layers moving in different directions, each one tile per cycle) with five bubbles on their own beat that
     * swell (lit top left) and pop into a dark ring with a few bright drops. Periodic in {@code phase}; opaque.
     */
    private static void toxicPoolStill(BufferedImage image, Random random, double phase) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        double shift = phase * SIZE;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.55 * (sample(broad, x + shift, y) - 0.5) + 0.45 * (sample(mid, x, y - shift) - 0.5);
                set(image, x, y, shade(TOXIC_POOL, t));
            }
        }
        for (int i = 0; i < 5; i++) {
            double cx = random.nextDouble() * SIZE;
            double cy = random.nextDouble() * SIZE;
            double offset = i / 5.0 + 0.1 * random.nextDouble();
            double maxRadius = 1.0 + 1.0 * random.nextDouble();
            double local = (phase + offset) % 1.0;
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int x = (int) Math.floor(cx) + dx;
                    int y = (int) Math.floor(cy) + dy;
                    double ox = x + 0.5 - cx;
                    double oy = y + 0.5 - cy;
                    double d = Math.hypot(ox, oy);
                    if (local < 0.5) {
                        double r = 0.5 + maxRadius * local / 0.5;
                        if (d < r) {
                            double light = -(ox + oy) / r;
                            setWrapped(image, x, y, light > 0.5 ? TOXIC_SHEEN : d > r - 0.7 && light < -0.3 ? TOXIC_POOL[1] : TOXIC_POOL[5]);
                        }
                    } else if (local < 0.62) {
                        if (Math.abs(d - maxRadius) < 0.55) {
                            setWrapped(image, x, y, TOXIC_POOL[0]);
                        } else if (Math.abs(d - maxRadius - 1.1) < 0.45 && Math.floorMod(dx * 5 + dy * 3, 4) == 0) {
                            setWrapped(image, x, y, TOXIC_SHEEN);
                        }
                    }
                }
            }
        }
    }

    /**
     * Toxic pool, flowing (72): 32×32 frames as vanilla's flowing fluids; lengthwise streaks of the poison green that
     * run down the texture (the flow direction) one tile per cycle, a few pale foam flecks riding along.
     */
    private static void toxicPoolFlow(BufferedImage image, Random random, double phase) {
        int size = image.getWidth();
        double[][] streaks = valueNoiseSized(random, size, 4, 16);
        double[][] fine = valueNoiseSized(random, size, 2, 8);
        double shift = phase * size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double t = 0.5 + 0.6 * (sample(streaks, x, y - shift) - 0.5) + 0.35 * (sample(fine, x, y - 2 * shift) - 0.5);
                set(image, x, y, shade(TOXIC_POOL, t));
            }
        }
        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(size);
            int y = Math.floorMod(random.nextInt(size) + (int) Math.round(2 * shift), size);
            image.setRGB(x, y, 0xFF000000 | TOXIC_SHEEN);
            image.setRGB(x, Math.floorMod(y - 1, size), 0xFF000000 | TOXIC_POOL[5]);
        }
    }

    /**
     * Toxic pool bucket (72, placeholder item): our own metal bucket (lit on the left, two hoops) brimful with the poison
     * green and a drip running over the rim; selective outline.
     */
    private static void toxicPoolBucket(BufferedImage image, Random random) {
        for (int x = 3; x <= 12; x++) {
            set(image, x, 3, x == 3 ? BUCKET[4] : x == 12 ? BUCKET[1] : BUCKET[3]);
            set(image, x, 5, x == 3 ? BUCKET[4] : x == 12 ? BUCKET[1] : BUCKET[2]);
            set(image, x, 4, x == 3 ? BUCKET[4] : x == 12 ? BUCKET[1] : x == 5 ? TOXIC_SHEEN : TOXIC_POOL[x % 3 == 0 ? 4 : 5]);
        }
        for (int y = 6; y <= 14; y++) {
            int left = 3 + (y - 6) / 4;
            int right = 12 - (y - 6) / 4;
            for (int x = left; x <= right; x++) {
                double t = (x - left) / (double) (right - left);
                int tone = x == left ? 4 : x == right ? 0 : t < 0.3 ? 3 : t < 0.7 ? 2 : 1;
                if ((y == 8 || y == 12) && x != left && x != right) {
                    tone = Math.min(4, tone + 1);
                }
                set(image, x, y, BUCKET[tone]);
            }
        }
        set(image, 9, 6, TOXIC_POOL[4]);
        set(image, 9, 7, TOXIC_POOL[3]);
        set(image, 9, 8, TOXIC_POOL[2]);
        outlineSelective(image, 0x2A2E34, 0x16181C);
    }

    /**
     * Updraft (72, the lift): a see-through column (cutout) with streaks of pale water spray and small bubbles rising
     * through it, the streaks one tile per cycle, the bubbles twice as fast. Not interpolated, so the alpha mask moves.
     */
    private static void updraft(BufferedImage image, Random random, double phase) {
        int[] columns = {1, 4, 7, 10, 13};
        for (int column : columns) {
            int x = column + random.nextInt(2);
            int start = random.nextInt(SIZE);
            int length = 3 + random.nextInt(4);
            for (int j = 0; j < length; j++) {
                int y = Math.floorMod(start + j - (int) Math.round(phase * SIZE), SIZE);
                set(image, x, y, UPDRAFT[j == 0 ? 2 : j < length / 2 ? 1 : 0]);
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE - 1);
            int y = Math.floorMod(random.nextInt(SIZE) - (int) Math.round(phase * 2 * SIZE), SIZE);
            set(image, x, y, UPDRAFT[2]);
            set(image, x + 1, Math.floorMod(y + 1, SIZE), UPDRAFT[0]);
        }
    }

    /**
     * Slime trail (72, a creature trace): a flat decal (cutout) of glossy grey-green slime winding from the middle of the
     * left edge to the middle of the right edge, so trails of neighbouring blocks join; lit along its upper edge, dark on
     * its lower edge, a few glints and two droplets beside it.
     */
    private static void slimeTrail(BufferedImage image, Random random) {
        // 71a review: the path and width are mirror-symmetric about the tile's middle column (odd multiples of π over
        // the tile), so columns 0 and 15 cover the same rows and the trail joins its neighbours seamlessly
        double bend = (1.6 + random.nextDouble()) * (random.nextBoolean() ? 1 : -1);
        double wiggle = 0.5 + 0.4 * random.nextDouble();
        for (int x = 0; x < SIZE; x++) {
            // the mirrored column gives bit-identical values on both halves (sin(π - a) is not exact in floating point)
            double u = (Math.min(x, SIZE - 1 - x) + 0.5) / SIZE;
            double path = SIZE / 2.0 + bend * Math.sin(Math.PI * u) - wiggle * bend / Math.abs(bend) * Math.sin(3 * Math.PI * u);
            double width = 1.4 + 1.0 * Math.pow(Math.sin(Math.PI * u), 2);
            boolean edge = x == 0 || x == SIZE - 1;
            for (int y = 0; y < SIZE; y++) {
                double v = y + 0.5 - path;
                if (Math.abs(v) > width) {
                    continue;
                }
                int grain = edge || random.nextInt(4) != 0 ? 0 : random.nextBoolean() ? 1 : -1;
                int tone = v < -width + 0.9 ? 4 : v > width - 0.9 ? 0 : 2 + grain;
                set(image, x, y, SLIME[tone]);
            }
        }
        for (int i = 0; i < 2; i++) {
            int x = 3 + random.nextInt(10);
            int y = random.nextBoolean() ? 2 + random.nextInt(2) : 12 + random.nextInt(2);
            set(image, x, y, SLIME[3]);
            set(image, x + 1, y, SLIME[1]);
        }
    }

    /**
     * Creature nest (72): a woven mass of twigs and roots (strokes in many directions, lit at one end and dark at the
     * other) over a dark depth, tufts of moss and two pale bone splinters.
     */
    private static void creatureNest(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::creatureNestLayout);
    }

    private static void creatureNestLayout(BufferedImage image, Random random) {
        double[][] depth = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, NEST[depth[x][y] > 0.6 ? 1 : 0]);
            }
        }
        // 71a-r1: long twigs in three woven directions (flat, rising, falling), each lit along its upper side with a
        // shadow pixel below, so the mass reads as wickerwork instead of speckle
        double[] directions = {0.0, -0.55, 0.55};
        for (int i = 0; i < 30; i++) {
            double angle = directions[i % 3] + 0.25 * (random.nextDouble() - 0.5);
            int length = 6 + random.nextInt(6);
            double x = random.nextDouble() * SIZE;
            double y = random.nextDouble() * SIZE;
            double tone = 0.45 + 0.35 * random.nextDouble();
            for (int j = 0; j < length; j++) {
                int px = (int) Math.floor(x + j * Math.cos(angle));
                int py = (int) Math.floor(y + j * Math.sin(angle));
                double light = tone + (j == 0 ? 0.2 : j == length - 1 ? -0.2 : 0);
                setWrapped(image, px, py, shade(NEST, light));
                setWrapped(image, px, py + 1, shade(NEST, light - 0.35));
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, NEST_MOSS[1]);
            setWrapped(image, x + 1, y, NEST_MOSS[0]);
            setWrapped(image, x, y + 1, NEST_MOSS[0]);
        }
        for (int i = 0; i < 2; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, BONE[2]);
            setWrapped(image, x + 1, y, BONE[1]);
            setWrapped(image, x + 1, y + 1, BONE[0]);
        }
    }

    // ---- Nether ring 4 (68) -------------------------------------------------------------------------------------

    /**
     * Bone sand (68): fine beige-white grains (falls like sand), soft drifts, dark grains and splinters of bleached bone
     * with a shadow pixel. Layout picked by {@link #bestOf}.
     */
    private static void boneSand(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::boneSandLayout);
    }

    private static void boneSandLayout(BufferedImage image, Random random) {
        double[][] mid = valueNoise(random, 4, 4);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.3 * (mid[x][y] - 0.5) + 0.25 * (fine[x][y] - 0.5) + 0.3 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(BONE_SAND, t));
            }
        }
        for (int i = 0; i < 6; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), BONE_SAND[0]);
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, BONE[2]);
            if (random.nextBoolean()) {
                setWrapped(image, x + 1, y, BONE[2]);
                setWrapped(image, x + 1, y + 1, BONE_SAND[1]);
            } else {
                setWrapped(image, x, y + 1, BONE_SAND[1]);
            }
        }
    }

    /**
     * Bleached netherrack (68): netherrack's lumpy build (wrapping cells lit on the upper left, dark crevices) bleached to
     * a pale pinkish bone colour, a little of the old red left in some crevices.
     */
    private static void bleachedNetherrack(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::bleachedNetherrackLayout);
    }

    private static void bleachedNetherrackLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 16);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.4 + 0.35 * random.nextDouble();
        }
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.45) {
                    set(image, x, y, random.nextInt(5) == 0 ? BLEACHED_RED : BLEACHED[0]);
                    continue;
                }
                double light = base[nearest.index()] + 0.2 * (broad[x][y] - 0.5) - 0.3 * slope + 0.08 * random.nextDouble();
                if (gap < 1.1 && slope > 0.15) {
                    light -= 0.12;
                }
                set(image, x, y, shade(BLEACHED, light));
            }
        }
    }

    /**
     * Fossil stone (68): beige-grey sedimentary stone (soft grain, a few pale grains) with small fossil fragments: two rib
     * arcs and a tiny spiral shell in bone white, each with a dark shadow edge below right. Fragments stay small so a wall
     * does not repeat one motif; layout picked by {@link #bestOf}.
     */
    private static void fossilStone(BufferedImage image, Random random) {
        bestOf(image, random, Ring4Textures::fossilStoneLayout);
    }

    private static void fossilStoneLayout(BufferedImage image, Random random) {
        double[][] grain = valueNoise(random, 8, 2);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.45 * (grain[x][y] - 0.5) + 0.4 * (mid[x][y] - 0.5) + 0.3 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(FOSSIL_STONE, t));
            }
        }
        // 71a-r1: one short rib arc and a tiny shell in a pale bone just above the stone's lightest tone, with a soft
        // shadow: two arcs and a white shell read as the same bright motif in every tile of a wall
        double cx = random.nextDouble() * SIZE;
        double cy = random.nextDouble() * SIZE;
        double radius = 3.5 + 2 * random.nextDouble();
        double start = random.nextDouble() * 2 * Math.PI;
        for (int j = 0; j < 6; j++) {
            double a = start + j * 0.2;
            int x = (int) Math.floor(cx + radius * Math.cos(a));
            int y = (int) Math.floor(cy + radius * Math.sin(a));
            setWrapped(image, x, y, j == 0 || j == 5 ? FOSSIL_STONE[5] : FOSSIL_BONE[1]);
            setWrapped(image, x + 1, y + 1, FOSSIL_STONE[1]);
        }
        int sx = random.nextInt(SIZE);
        int sy = random.nextInt(SIZE);
        int[][] shell = {{0, -1, 1}, {1, -1, 1}, {-1, 0, 1}, {1, 0, 5}, {0, 1, 5}, {0, 0, 0}};
        for (int[] s : shell) {
            setWrapped(image, sx + s[0], sy + s[1], s[2] == 1 ? FOSSIL_BONE[1] : FOSSIL_STONE[s[2]]);
        }
        setWrapped(image, sx + 1, sy + 1, FOSSIL_STONE[1]);
    }

    /** Polished fossil stone: a smooth mottled beige face with vanilla's bevel. */
    private static void polishedFossilStone(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(FOSSIL_STONE, 0.52 + 0.25 * (noise[x][y] - 0.5) + 0.25 * (broad[x][y] - 0.5) + 0.15 * (random.nextDouble() - 0.5)));
            }
        }
        bevel(image, FOSSIL_STONE);
    }

    // ---- Particles ----------------------------------------------------------------------------------------------

    /** Light grey particle tones, tinted by A's particle class (V-T4 A): '#' light, 'o' mid, 'x' shadow/fringe. */
    private static final int[] GREYS = {0xF2F2F2, 0xCFCFCF, 0xA8A8A8};
    /** Own colours (not tinted): blossom petals white-pink to deep pink; glitch specks magenta, cyan, near black. */
    private static final int[] BLOSSOM_SPRITE = {0xFFEAF2, 0xF2B2D2, 0xC97AAE};
    private static final int[] GLITCH = {0xE040FF, 0x40F0FF, 0x101018};

    /** Will-o'-wisps: soft round orbs with a bright core, small to large. */
    private static final String[][] WILL_O_WISPS = {
            {"........", "........", "........", "...#o...", "...ox...", "........", "........", "........"},
            {"........", "........", "...oo...", "..o##o..", "..o##x..", "...xx...", "........", "........"},
            {"........", "...xx...", "..o##o..", ".o####x.", ".o####x.", "..o##x..", "...xx...", "........"},
            {"...xx...", "..o##o..", ".o####o.", "x######x", "x######x", ".o####x.", "..x##x..", "...xx..."}};
    /** Blossom petals, curled, small to large. */
    private static final String[][] BLOSSOMS = {
            {"........", "........", "...#o...", "...ox...", "........", "........", "........", "........"},
            {"........", "........", "..#o....", "..#oo...", "...ox...", "........", "........", "........"},
            {"........", "..##....", ".#ooo...", "..oooo..", "...oxx..", "....x...", "........", "........"},
            {"........", ".##o....", ".#ooo...", "..#oooo.", "...ooox.", "....oxx.", ".....x..", "........"}};
    /** Fireflies: a tiny core with a faint fringe. */
    private static final String[][] FIREFLIES = {
            {"........", "........", "........", "...#....", "........", "........", "........", "........"},
            {"........", "........", "........", "...#o...", "...o....", "........", "........", "........"},
            {"........", "........", "...x....", "..x#o...", "...ox...", "........", "........", "........"},
            {"........", "...x....", "..xox...", ".xo#ox..", "..xox...", "...x....", "........", "........"}};
    /** Swamp gas: thin, broken wisps of haze. */
    private static final String[][] SWAMP_GAS = {
            {"........", "........", "...o....", "..o#o...", "...ox...", "........", "........", "........"},
            {"........", "...o.o..", "..o#oo..", "..##o...", "...oxo..", "....x...", "........", "........"},
            {"........", "..oo.o..", ".o##oo..", ".o#o#o..", "..ooxo..", "...x.x..", "........", "........"},
            {"..o.o...", ".o##oo..", "o####o..", ".##oo#o.", ".o#ooox.", "..oxox..", "...x.x..", "........"}};
    /** Toxic bubbles: rings with a highlight on the upper left. */
    private static final String[][] TOXIC_BUBBLES = {
            {"........", "........", "...oo...", "..o#.o..", "..o..x..", "...xx...", "........", "........"},
            {"........", "..ooo...", ".o#..o..", ".o...x..", ".o...x..", "..xxx...", "........", "........"},
            {"........", "..ooo...", ".o#.oo..", "o#...ox.", "o....ox.", ".o..xx..", "..xxx...", "........"},
            {"..oooo..", ".o##..o.", "o#....ox", "o.....ox", "o......x", "o......x", ".x....x.", "..xxxx.."}};
    /** Updraft bubbles: small bubbles and droplets. */
    private static final String[][] UPDRAFT_BUBBLES = {
            {"........", "........", "........", "...#....", "........", "........", "........", "........"},
            {"........", "........", "...o....", "..#.o...", "...x....", "........", "........", "........"},
            {"........", "........", "...oo...", "..#..o..", "..o..x..", "...xx...", "........", "........"},
            {"........", "........", "..ooo...", ".#...o..", ".o...x..", ".o...x..", "..xxx...", "........"}};
    /** Glitch specks: broken bars in magenta, cyan and black. */
    private static final String[][] GLITCH_SPECKS = {
            {"........", "........", "..##o...", "........", "...xx...", "........", "........", "........"},
            {"........", "..####..", "........", ".oo.xx..", "........", "...##...", "........", "........"},
            {"........", ".###.o..", ".xxx....", "....ooo.", "........", "..##....", "........", "........"},
            {"#####...", "........", "..oooooo", "........", "xxx.....", "....###.", "........", "..oo...."}};
    /** Bone dust: tiny specks, alone or in a small drift. */
    private static final String[][] BONE_DUST = {
            {"........", "........", "........", "...#....", "........", "........", "........", "........"},
            {"........", "........", "...#o...", "....x...", "........", "........", "........", "........"},
            {"........", "..#.....", "...o....", "......#.", "....#o..", "........", "........", "........"},
            {"........", ".#o.....", "..x..#..", "........", "...#o...", "....x...", ".o......", "........"}};

    /**
     * Waterfall mist: fine, soft spray, a droplet up to a thin wisp. The old checkered cutout sprites read as big blocky
     * squares in game; these are round blobs whose alpha falls off to the rim (allowed: {@link #SOFT_ALPHA}). Each blob is
     * {@code {x, y, radius, peak alpha}} in pixels; overlapping blobs add up like light.
     */
    private static final double[][][] WATERFALL_MIST = {
            {{3.5, 3.5, 1.8, 1.00}},
            {{3.5, 3.6, 2.7, 0.90}},
            {{3.2, 3.9, 2.2, 0.90}, {5.7, 2.2, 1.3, 0.75}, {1.7, 1.9, 1.1, 0.65}},
            {{3.5, 3.6, 3.4, 0.45}, {3.0, 4.0, 1.8, 0.75}, {5.8, 5.6, 1.1, 0.65}}};
    /** White at the heart, a light blue hint towards the thin rim. */
    private static final int MIST_CORE = 0xF8FCFF;
    private static final int MIST_EDGE = 0xCFE6F7;

    // ---- Animation ----------------------------------------------------------------------------------------------

    /** The frames of a cycling texture under each other: the same painter and seed, only the phase moves. */
    private static BufferedImage cyclingStrip(String name, Animation animation) {
        int size = size(name);
        BufferedImage strip = new BufferedImage(size, size * animation.frames(), BufferedImage.TYPE_INT_ARGB);
        CyclingPainter painter = TEXTURES.get(name).cyclingPainter();
        for (int i = 0; i < animation.frames(); i++) {
            BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            painter.paint(frame, seeded(name), i / (double) animation.frames());
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    strip.setRGB(x, i * size + y, frame.getRGB(x, y));
                }
            }
        }
        return strip;
    }

    private static BufferedImage animationStrip(String name, BufferedImage rest, Animation animation) {
        return animation.cycling() ? cyclingStrip(name, animation) : pulseStrip(rest, animation, STILL_TONES.getOrDefault(name, Set.of()));
    }

    // ---- Preview ------------------------------------------------------------------------------------------------

    private static final int CELL_WIDTH = 350;
    private static final int CELL_HEIGHT = 150;
    private static final int COLUMNS = 6;
    private static final int SHEET_WIDTH = COLUMNS * CELL_WIDTH + 20;

    /** 6×6 walls (2×) to judge repetition; own textures only. */
    private static final String[] WALLS = {"block/tidestone", "block/cobbled_tidestone", "block/polished_tidestone",
            "block/tidestone_bricks", "block/clawed_tidestone", "block/bubble_spring_top", "block/moonpetal_log",
            "block/moonpetal_planks", "block/moonpetal_leaves", "block/withered_moonpetal_log", "block/stripped_moonpetal_log",
            "block/peat", "block/sucking_mud", "block/toxic_pool_still", "block/creature_nest", "block/toxic_crystal_block",
            "block/aquamarine_block", "block/budding_toxic_crystal", "block/bone_sand", "block/bleached_netherrack",
            "block/fossil_stone", "block/polished_fossil_stone", "block/fossil_stone_bricks", "block/purity_crystal_block"};

    /** Drawn at night: the block darkened as by a dark sky, its glow layer (or the whole emissive crystal) at full brightness. */
    private static final String[] NIGHT = {"block/glowcap", "block/blighted_glowcap", "block/glow_bloom",
            "block/glowing_hanging_roots", "block/toxic_crystal_cluster", "block/purity_crystal_lamp", "block/purity_crystal_cluster"};

    private static final String[] SLOT_SPRITES = {"item/raw_tidesilver", "item/jade", "item/toxic_crystal_shard",
            "item/aquamarine_shard", "item/purity_crystal_shard", "item/moonpetal_door", "item/giant_lily_pad",
            "item/toxic_pool_bucket", "block/moonpetal_sapling", "block/reeds_top", "block/glowcap", "block/blighted_glowcap",
            "block/glow_bloom", "block/glowing_hanging_roots", "block/hanging_swamp_moss", "block/toxic_crystal_cluster",
            "block/large_toxic_crystal_bud", "block/medium_toxic_crystal_bud", "block/small_toxic_crystal_bud",
            "block/aquamarine_cluster", "block/large_aquamarine_bud", "block/medium_aquamarine_bud",
            "block/small_aquamarine_bud", "block/purity_crystal_cluster", "block/slime_trail"};

    /** Ores as B's two-layer model shows them: the overlay over its host (tidestone, and flat grey for stone/deepslate). */
    private static final String[] ORES = {"block/tidesilver_ore_overlay", "block/jade_ore_overlay"};

    private static void writePreview() throws IOException {
        BufferedImage sheet = new BufferedImage(SHEET_WIDTH, 12000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x2B2B2B));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int top = drawExtras(graphics, 10);
        int index = 0;
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            if (index == 0 || name.equals(NETHER_FIRST) || name.equals(PARTICLES_FIRST)) {
                if (index % COLUMNS != 0) {
                    index += COLUMNS - index % COLUMNS;
                }
                graphics.setColor(new Color(0xFFC857));
                String title = index == 0 ? "== RING 4 ==" : name.equals(NETHER_FIRST) ? "== NETHER RING 4 ==" : "== PARTICLES (8x8) ==";
                graphics.drawString(title, 10, top + (index / COLUMNS) * CELL_HEIGHT + 12);
                index += COLUMNS;
            }
            int left = 10 + (index % COLUMNS) * CELL_WIDTH;
            int cellTop = top + (index / COLUMNS) * CELL_HEIGHT;
            BufferedImage frame = render(name);
            graphics.setColor(Color.WHITE);
            graphics.drawString(name.replace("block/", "").replace("item/", "item: ").replace("particle/", "particle: ")
                    + (HAND_EDITED.contains(name) ? " (hand)" : ""), left, cellTop + 12);
            int scale = 96 / frame.getWidth();
            draw(graphics, frame, left, cellTop + 16, scale);
            int tileScale = Math.max(1, 2 * SIZE / frame.getWidth());
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    draw(graphics, frame, left + 104 + tx * frame.getWidth() * tileScale, cellTop + 16 + ty * frame.getWidth() * tileScale, tileScale);
                }
            }
            Animation animation = entry.getValue().animation();
            if (animation.animated()) {
                BufferedImage strip = animationStrip(name, frame, animation);
                int size = frame.getWidth();
                int shown = Math.min(animation.frames(), 8);
                for (int i = 0; i < shown; i++) {
                    int frameIndex = i * animation.frames() / shown;
                    BufferedImage single = strip.getSubimage(0, frameIndex * size, size, size);
                    draw(graphics, single, left + 208 + (i % 4) * 34, cellTop + 16 + (i / 4) * 34, 32 / size);
                }
            }
            index++;
        }
        int bottom = top + ((index + COLUMNS - 1) / COLUMNS) * CELL_HEIGHT + 10;
        graphics.dispose();
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet.getSubimage(0, 0, sheet.getWidth(), bottom), "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    /** Walls, the night view, ores, items in a slot and scenes; returns the top of the per-texture cells. */
    private static int drawExtras(Graphics2D graphics, int top) throws IOException {
        int block = SIZE * 2;
        int wallSize = 6 * block;
        graphics.setColor(Color.WHITE);
        graphics.drawString("6x6 walls at 2x (repetition) | lint: " + LINT_WARNINGS.size() + " warning(s), see the console", 10, top + 12);
        top += 20;
        for (int i = 0; i < WALLS.length; i++) {
            int left = 10 + (i % 6) * (wallSize + 24);
            int wallTop = top + (i / 6) * (wallSize + 30);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString(WALLS[i].replace("block/", ""), left, wallTop + 10);
            BufferedImage tile = render(WALLS[i]);
            for (int ty = 0; ty < 6; ty++) {
                for (int tx = 0; tx < 6; tx++) {
                    drawOn(graphics, tile, left + tx * block, wallTop + 14 + ty * block, 2, 0x3E5A66);
                }
            }
        }
        top += ((WALLS.length + 5) / 6) * (wallSize + 30) + 10;

        graphics.setColor(Color.WHITE);
        graphics.drawString("night: plant at 20 % light, glow layer / whole crystal at full brightness (emissive), 3x3 at 4x", 10, top + 12);
        top += 20;
        for (int i = 0; i < NIGHT.length; i++) {
            BufferedImage night = TEXTURES.containsKey(NIGHT[i] + "_glow") ? atNight(NIGHT[i]) : render(NIGHT[i]);
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    drawOn(graphics, night, 10 + i * (3 * SIZE * 4 + 16) + tx * SIZE * 4, top + ty * SIZE * 4, 4, 0x0A0A12);
                }
            }
        }
        top += 3 * SIZE * 4 + 20;

        graphics.setColor(Color.WHITE);
        graphics.drawString("ores (two-layer model): overlay over tidestone and over flat grey (stone/deepslate stand-in), 3x3 at 4x", 10, top + 12);
        top += 20;
        BufferedImage tidestone = render("block/tidestone");
        BufferedImage grey = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(grey, x, y, (x * 7 + y * 3) % 5 == 0 ? 0x6E6E6E : 0x7E7E7E);
            }
        }
        int slot = 0;
        for (String ore : ORES) {
            BufferedImage overlay = render(ore);
            for (BufferedImage host : new BufferedImage[] {tidestone, grey}) {
                for (int ty = 0; ty < 3; ty++) {
                    for (int tx = 0; tx < 3; tx++) {
                        int left = 10 + slot * (3 * SIZE * 4 + 16) + tx * SIZE * 4;
                        drawOn(graphics, host, left, top + ty * SIZE * 4, 4, -1);
                        drawOn(graphics, overlay, left, top + ty * SIZE * 4, 4, -1);
                    }
                }
                slot++;
            }
        }
        top += 3 * SIZE * 4 + 20;

        graphics.setColor(Color.WHITE);
        graphics.drawString("items and plants in a slot at 1x, 2x and 3x (inventory grey)", 10, top + 12);
        top += 20;
        for (int i = 0; i < SLOT_SPRITES.length; i++) {
            int left = 10 + (i % 9) * 128;
            int rowTop = top + (i / 9) * 64;
            BufferedImage sprite = render(SLOT_SPRITES[i]);
            int x = left;
            for (int scale = 1; scale <= 3; scale++) {
                int size = SIZE * scale;
                graphics.setColor(new Color(0xC6C6C6));
                graphics.fillRect(x, rowTop, size + 4, size + 4);
                graphics.setColor(new Color(0x8B8B8B));
                graphics.fillRect(x + 2, rowTop + 2, size, size);
                drawOn(graphics, sprite, x + 2, rowTop + 2, scale, -1);
                x += size + 8;
            }
        }
        top += ((SLOT_SPRITES.length + 8) / 9) * 64 + 20;

        graphics.setColor(Color.WHITE);
        graphics.drawString("cascade vale (day, side view) | lotus shallows (top view, 2x2 giant lily pads) | glowroot swamp (night)", 10, top + 12);
        top += 20;
        drawScene(graphics, CASCADE, 10, top, 0x9FDCE6, false);
        drawScene(graphics, LILY_POND, 10 + 11 * SIZE * 4, top, 0x3D9CC0, false);
        drawScene(graphics, SWAMP, 10 + 22 * SIZE * 4, top, 0x0A0E14, true);
        top += CASCADE.length * SIZE * 4 + 20;
        graphics.setColor(Color.WHITE);
        graphics.drawString("Nether ring 4 floor (top view, day) | withered garden (side view)", 10, top + 12);
        top += 20;
        drawScene(graphics, NETHER_FLOOR, 10, top, 0xE6DCC8, false);
        drawScene(graphics, WITHERED, 10 + 11 * SIZE * 4, top, 0xA8B4A0, false);
        top += NETHER_FLOOR.length * SIZE * 4 + 20;
        return top;
    }

    private static final String LE = "block/moonpetal_leaves";
    private static final String LO = "block/moonpetal_log";
    private static final String TS = "block/tidestone";
    private static final String[][] CASCADE = {
            {"", LE, LE, LE, "", "", "", LE, LE, ""},
            {LE, LE, LE, LE, LE, "", LE, LE, LE, LE},
            {"", LE, LO, LE, "", "", "", LE, LO, LE},
            {"", "", LO, "", "", "", "", "", LO, ""},
            {"block/glow_bloom", "block/reeds_top", LO, "block/moonpetal_sapling", "", "block/glow_bloom", "", "block/reeds_top", LO, "block/reeds_top"},
            {TS, TS, TS, "block/bubble_spring_side", TS, TS, "block/cobbled_tidestone", TS, TS, "block/reeds"},
            {TS, "block/clawed_tidestone", TS, TS, TS, "block/tidestone_bricks", TS, TS, TS, TS}};
    private static final String[][] LILY_POND = {
            {"", "", "", "", "", "", "", "", "", ""},
            {"", "block/giant_lily_pad_north_west", "block/giant_lily_pad_north_east", "", "", "", "", "", "", ""},
            {"", "block/giant_lily_pad_south_west", "block/giant_lily_pad_south_east", "", "", "block/giant_lily_pad_north_west", "block/giant_lily_pad_north_east", "", "", ""},
            {"", "", "", "", "", "block/giant_lily_pad_south_west", "block/giant_lily_pad_south_east", "", "", ""},
            {"", "", "", "block/giant_lily_pad_north_west", "block/giant_lily_pad_north_east", "", "", "", "block/giant_lily_pad_north_west", "block/giant_lily_pad_north_east"},
            {"", "", "", "block/giant_lily_pad_south_west", "block/giant_lily_pad_south_east", "", "", "", "block/giant_lily_pad_south_west", "block/giant_lily_pad_south_east"},
            {"", "", "", "", "", "", "", "", "", ""}};
    private static final String[][] SWAMP = {
            {"block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat"},
            {"block/glowing_hanging_roots", "block/hanging_swamp_moss", "", "block/glowing_hanging_roots", "", "block/hanging_swamp_moss", "block/glowing_hanging_roots", "", "block/hanging_swamp_moss", "block/glowing_hanging_roots"},
            {"", "", "", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", "", "", ""},
            {"block/glowcap", "", "block/glow_bloom", "block/glowcap", "", "block/blighted_glowcap", "", "block/blighted_glowcap", "block/glowcap", ""},
            {"block/peat", "block/peat", "block/peat", "block/sucking_mud", "block/sucking_mud", "block/peat", "block/peat", "block/toxic_pool_still", "block/peat", "block/peat"},
            {"block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat"}};
    private static final String BS = "block/bone_sand";
    private static final String[][] NETHER_FLOOR = {
            {BS, BS, BS, "block/bleached_netherrack", "block/bleached_netherrack", BS, BS, BS, BS, BS},
            {BS, "block/fossil_stone", BS, BS, "block/bleached_netherrack", BS, BS, "block/purity_crystal_block", BS, BS},
            {BS, "block/fossil_stone", "block/fossil_stone", BS, BS, BS, BS, BS, BS, "block/bleached_netherrack"},
            {BS, BS, BS, BS, BS, "block/fossil_stone_bricks", "block/purity_crystal_lamp", "block/fossil_stone_bricks", BS, "block/bleached_netherrack"},
            {"block/bleached_netherrack", BS, BS, BS, BS, BS, BS, BS, BS, BS},
            {"block/bleached_netherrack", "block/bleached_netherrack", BS, "block/polished_fossil_stone", BS, BS, "block/fossil_stone", BS, BS, BS},
            {BS, BS, BS, BS, BS, BS, BS, BS, BS, BS}};
    private static final String WL = "block/withered_moonpetal_log";
    private static final String[][] WITHERED = {
            {"", "", "", "", "", "", "", "", "", ""},
            {"", WL, "", WL, "", "", "", WL, "", ""},
            {"", "", WL, "", "", "", "", WL, "", WL},
            {"", "", WL, "", "", "block/hanging_swamp_moss", "", WL, WL, ""},
            {"block/reeds_top", "", WL, "block/blighted_glowcap", "", "", "block/reeds_top", WL, "block/blighted_glowcap", "block/reeds_top"},
            {"block/reeds", "block/peat", "block/peat", "block/peat", "block/sucking_mud", "block/sucking_mud", "block/reeds", "block/peat", "block/peat", "block/reeds"},
            {"block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat", "block/peat"}};

    /** Draws a grid of textures at 4x; at {@code night} blocks are darkened and glow layers drawn on top at full brightness. */
    private static void drawScene(Graphics2D graphics, String[][] scene, int left0, int top, int backdrop, boolean night) throws IOException {
        for (int ty = 0; ty < scene.length; ty++) {
            for (int tx = 0; tx < scene[ty].length; tx++) {
                int left = left0 + tx * SIZE * 4;
                int rowTop = top + ty * SIZE * 4;
                graphics.setColor(new Color(backdrop));
                graphics.fillRect(left, rowTop, SIZE * 4, SIZE * 4);
                if (!scene[ty][tx].isEmpty()) {
                    drawOn(graphics, night ? atNight(scene[ty][tx]) : render(scene[ty][tx]), left, rowTop, 4, -1);
                }
            }
        }
    }

    // ---- Helpers ------------------------------------------------------------------------------------------------

    /** Tileable value noise in [0, 1) over a 16×16 tile: random values on a grid of {@code cellX} × {@code cellY} pixels. */
    private static double[][] valueNoise(Random random, int cellX, int cellY) {
        return valueNoiseSized(random, SIZE, cellX, cellY);
    }

    /** Tileable value noise in [0, 1) over a {@code size} × {@code size} tile, smoothly interpolated. */
    private static double[][] valueNoiseSized(Random random, int size, int cellX, int cellY) {
        int pointsX = size / cellX;
        int pointsY = size / cellY;
        double[][] grid = new double[pointsX][pointsY];
        for (int i = 0; i < pointsX; i++) {
            for (int j = 0; j < pointsY; j++) {
                grid[i][j] = random.nextDouble();
            }
        }
        double[][] noise = new double[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int gx = x / cellX;
                int gy = y / cellY;
                double fx = smoothstep((x % cellX) / (double) cellX);
                double fy = smoothstep((y % cellY) / (double) cellY);
                double top = lerp(grid[gx][gy], grid[(gx + 1) % pointsX][gy], fx);
                double bottom = lerp(grid[gx][(gy + 1) % pointsY], grid[(gx + 1) % pointsX][(gy + 1) % pointsY], fx);
                noise[x][y] = lerp(top, bottom, fy);
            }
        }
        return noise;
    }

    private static Set<Integer> toneSet(int[] palette) {
        Set<Integer> tones = new HashSet<>();
        for (int tone : palette) {
            tones.add(tone);
        }
        return tones;
    }

    private static double frac(double value) {
        return value - Math.floor(value);
    }

    /**
     * Opaque pixel; every texture uses alpha 0 or 255 only (cutout-safe). Overpainting a glowing pixel clears its glow
     * mark, so a glow layer never keeps a pixel that a later stroke covered.
     */
    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
        boolean[][] mask = GLOW_MASKS.get(image);
        if (mask != null) {
            mask[x][y] = false;
        }
    }

    /** {@link #set} wrapping around the tile edges, for seamless textures. */
    private static void setWrapped(BufferedImage image, int x, int y, int rgb) {
        set(image, Math.floorMod(x, SIZE), Math.floorMod(y, SIZE), rgb);
    }

    // ---- Copied from Ring3Textures.java (below) -----------------------------------------------------------------
    /** Vanilla's polished bevel: top row and left column lit, bottom row and right column dark, a softer second ring. */
    private static void bevel(BufferedImage image, int[] palette) {
        int last = palette.length - 1;
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, palette[last]);
            set(image, 0, i, palette[last]);
        }
        for (int i = 1; i < SIZE - 1; i++) {
            set(image, i, 1, palette[last - 1]);
            set(image, 1, i, palette[last - 1]);
            set(image, i, SIZE - 2, palette[1]);
            set(image, SIZE - 2, i, palette[1]);
        }
        for (int i = 0; i < SIZE; i++) {
            set(image, i, SIZE - 1, palette[0]);
            set(image, SIZE - 1, i, palette[0]);
        }
    }

    /**
     * Vanilla's stone brick layout (as the canyon stone bricks, 37b-5 A): two rows of 16×8 bricks offset by half a brick,
     * a dark mortar, each brick bevelled (lit top row and left column, shadowed bottom row and right column) over grain,
     * with a few chips on the lit edges.
     */
    private static void stoneBricks(BufferedImage image, Random random, int[] palette, int mortar) {
        double[][] grain = valueNoise(random, 4, 2);
        for (int row = 0; row < 2; row++) {
            int offset = row == 0 ? 0 : 8;
            double brickShade = 0.38 + 0.22 * random.nextDouble();
            for (int dy = 0; dy < 8; dy++) {
                int y = row * 8 + dy;
                for (int x = 0; x < SIZE; x++) {
                    int inBrick = Math.floorMod(x - offset, SIZE);
                    double light = brickShade + 0.35 * (grain[x][y] - 0.5) + 0.14 * random.nextDouble();
                    if (dy == 7 || inBrick == 15) {
                        set(image, x, y, mortar);
                        continue;
                    }
                    if (dy == 0 || inBrick == 0) {
                        light += 0.3;
                    } else if (dy == 6 || inBrick == 14) {
                        light -= 0.3;
                    }
                    set(image, x, y, shade(palette, light));
                }
            }
            for (int i = 0; i < 2; i++) {
                int x = Math.floorMod(offset + 2 + random.nextInt(11), SIZE);
                set(image, x, row * 8, palette[1]);
            }
        }
    }
    /**
     * Paints eight layouts of a cell pattern from the seed and keeps the one that repeats least visibly on a wall
     * ({@link #stripeScore} plus {@link #motifScore}): random cell centres sometimes line up into diagonal bands, or put
     * one bright spot in the tile that a wall repeats as a motif.
     */
    private static void bestOf(BufferedImage image, Random random, Painter layout) {
        BufferedImage best = null;
        double bestScore = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 8; attempt++) {
            BufferedImage candidate = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            layout.paint(candidate, new Random(random.nextLong()));
            double score = stripeScore(candidate) + motifScore(candidate);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        copy(best, image);
    }

    /**
     * How strongly one spot of a tile stands out, so that a wall shows the same motif in every tile: the largest
     * difference in mean brightness between the sixteen 4x4 blocks of the tile.
     */
    private static double motifScore(BufferedImage image) {
        double[] blocks = new double[16];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                blocks[(y / 4) * 4 + x / 4] += luminance(image.getRGB(x, y) & 0xFFFFFF) / 16;
            }
        }
        double min = Double.MAX_VALUE;
        double max = 0;
        for (double block : blocks) {
            min = Math.min(min, block);
            max = Math.max(max, block);
        }
        return max - min;
    }
    // ---- Wood helpers (copied from Ring2Textures.java, 37b) ----------------------------------------------------

    /** Stripped wood: lengthwise fibre streaks in five tones, a few long dark grain lines and pale glints. */
    private static void strippedLog(BufferedImage image, Random random, int[] wood) {
        double[][] streaks = valueNoise(random, 2, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double edge = streaks[x][y] - streaks[Math.floorMod(x - 1, SIZE)][y];
                set(image, x, y, shade(wood, 0.15 + 0.7 * streaks[x][y] + 0.6 * edge + 0.12 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = 1 + 4 * i + random.nextInt(2);
            int start = random.nextInt(SIZE);
            int length = 5 + random.nextInt(8);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, wood[0]);
            }
            setWrapped(image, x + 1, start + random.nextInt(length), wood[wood.length - 1]);
        }
    }

    /**
     * The log's end grain {@code topName} (same pixels) with every pixel in a {@code bark} tone turned into wood (bark
     * that reaches deeper than the rim, such as the charred log's charcoal ring and corners) and a wood rim.
     */
    private static void strippedLogTop(BufferedImage image, Random random, String topName, int[] wood, int[] bark) {
        copy(renderUnchecked(topName), image);
        Set<Integer> barkTones = new HashSet<>();
        for (int tone : bark) {
            barkTones.add(tone);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (barkTones.contains(image.getRGB(x, y) & 0xFFFFFF)) {
                    set(image, x, y, shade(wood, 0.25 + 0.3 * random.nextDouble()));
                }
            }
        }
        rim(image, random, wood, 0.2, 0.55);
    }

    /** Four planks of four rows: dark seam row, lit top row, staggered joints, grain streaks. */
    private static void planks(BufferedImage image, Random random, int[] wood) {
        int[] joints = {5, 12, 2, 9};
        for (int plank = 0; plank < 4; plank++) {
            double plankShade = 0.35 + random.nextDouble() * 0.3;
            for (int row = 0; row < 4; row++) {
                int y = plank * 4 + row;
                for (int x = 0; x < SIZE; x++) {
                    if (row == 3 || x == joints[plank]) {
                        set(image, x, y, wood[0]);
                    } else {
                        double lit = row == 0 ? 0.2 : 0.0;
                        set(image, x, y, shade(wood, plankShade + lit + (random.nextDouble() - 0.5) * 0.25));
                    }
                }
            }
            for (int i = 0; i < 2; i++) {
                int y = plank * 4 + 1 + random.nextInt(2);
                int x = random.nextInt(SIZE);
                int length = 3 + random.nextInt(4);
                for (int j = 0; j < length; j++) {
                    if ((x + j) % SIZE != joints[plank]) {
                        setWrapped(image, x + j, y, wood[1]);
                    }
                }
            }
        }
    }

    /** Upper door half: bevelled boards in a frame lit on its top and left, two window holes, one rail, a hinge. */
    private static void doorTop(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, true, false);
        rail(image, random, bark, 11);
        window(image, wood, bark, 3, 3, 3, 5);
        window(image, wood, bark, 10, 3, 3, 5);
        hinge(image, bark, 2);
    }

    /** Lower door half: boards, two rails with a diagonal brace between them, a hinge and the handle. */
    private static void doorBottom(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, false, true);
        rail(image, random, bark, 2);
        rail(image, random, bark, 12);
        for (int x = 2; x < SIZE - 2; x++) {
            int y = 10 - (x - 2) * 6 / 11;
            set(image, x, y, wood[wood.length - 1]);
            set(image, x, y + 1, wood[1]);
        }
        hinge(image, bark, 13);
        set(image, 12, 0, bark[bark.length - 1]);
        set(image, 12, 1, bark[0]);
        set(image, 13, 1, bark[1]);
    }

    /** Trapdoor as a lattice: slats lit along their top row with see-through gaps, a centre brace, a frame, nails. */
    private static void trapdoor(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        for (int slat : new int[] {0, 4, 8, 12}) {
            for (int x = 1; x < SIZE - 1; x++) {
                set(image, x, slat, wood[wood.length - 2]);
                set(image, x, slat + 2, wood[1]);
            }
        }
        frame(image, random, bark, true, true);
        for (int gap : new int[] {3, 7, 11}) {
            for (int x = 2; x < SIZE - 2; x++) {
                if (x != 7 && x != 8) {
                    image.setRGB(x, gap, TRANSPARENT);
                }
            }
        }
        for (int y = 1; y < SIZE - 1; y++) {
            set(image, 7, y, bark[bark.length - 2]);
            set(image, 8, y, bark[1]);
        }
        for (int y : new int[] {1, 5, 9, 13}) {
            set(image, 2, y, bark[0]);
            set(image, 13, y, bark[0]);
        }
    }

    /** The door in the inventory (8 wide, columns 4–11): frame, boards, two window holes with a sill, rails, handle. */
    private static void doorItem(BufferedImage image, Random random, int[] wood, int[] bark) {
        int light = bark[bark.length - 2];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 4; x < 12; x++) {
                if (x == 4 || y == 0) {
                    set(image, x, y, x == 4 && y == SIZE - 1 ? bark[1] : light);
                } else if (x == 11 || y == SIZE - 1) {
                    set(image, x, y, bark[0]);
                } else if (x == 8) {
                    set(image, x, y, wood[0]);
                } else {
                    set(image, x, y, shade(wood, (x == 5 || x == 9 ? 0.6 : 0.3) + random.nextDouble() * 0.3));
                }
            }
        }
        for (int y = 2; y <= 4; y++) {
            image.setRGB(6, y, TRANSPARENT);
            image.setRGB(9, y, TRANSPARENT);
        }
        set(image, 6, 5, wood[wood.length - 1]);
        set(image, 9, 5, wood[wood.length - 1]);
        for (int y : new int[] {7, 13}) {
            for (int x = 5; x < 11; x++) {
                set(image, x, y - 1, bark[bark.length - 2]);
                set(image, x, y, bark[1]);
            }
        }
        set(image, 10, 9, bark[bark.length - 1]);
        set(image, 10, 10, bark[0]);
    }

    /** Three vertical boards (seams at x 5 and 10), each with its own tone, a lit bevel right of each seam and grain. */
    private static void boards(BufferedImage image, Random random, int[] wood) {
        double[] boards = {0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double board = boards[x < 5 ? 0 : x < 10 ? 1 : 2];
                if (x == 5 || x == 10) {
                    set(image, x, y, wood[0]);
                } else if (x == 1 || x == 6 || x == 11) {
                    set(image, x, y, shade(wood, board + 0.25));
                } else {
                    set(image, x, y, shade(wood, board + (random.nextDouble() - 0.5) * 0.2));
                }
            }
        }
        int[] grain = {2, 3, 7, 8, 12, 13};
        for (int i = 0; i < 5; i++) {
            int x = grain[random.nextInt(grain.length)];
            int start = random.nextInt(SIZE);
            int length = 3 + random.nextInt(5);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, wood[1]);
            }
        }
    }

    /** Frame edges: lit on the left and top, dark on the right and bottom (light from the top left). */
    private static void frame(BufferedImage image, Random random, int[] bark, boolean top, boolean bottom) {
        int lit = bark.length - 2;
        for (int i = 0; i < SIZE; i++) {
            set(image, 0, i, bark[lit - random.nextInt(2)]);
            set(image, SIZE - 1, i, bark[random.nextInt(2)]);
            if (top) {
                set(image, i, 0, bark[i > 0 && i < SIZE - 1 ? lit + random.nextInt(2) : lit]);
            }
            if (bottom) {
                set(image, i, SIZE - 1, bark[random.nextInt(2)]);
            }
        }
    }

    /** A two-row rail: lit top row, shadowed bottom row, a nail near each end. */
    private static void rail(BufferedImage image, Random random, int[] bark, int y) {
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, y, bark[bark.length - 2 + random.nextInt(2)]);
            set(image, x, y + 1, bark[1 + random.nextInt(2)]);
        }
        set(image, 2, y, bark[0]);
        set(image, SIZE - 3, y, bark[0]);
    }

    /** Two dark hinge pixels on the left frame from row {@code y}, a lit pixel above them. */
    private static void hinge(BufferedImage image, int[] bark, int y) {
        set(image, 1, y - 1, bark[bark.length - 1]);
        set(image, 1, y, bark[0]);
        set(image, 1, y + 1, bark[0]);
    }

    /** A see-through hole sunk into the door: rim above and left in shadow, sill below and right lit. */
    private static void window(BufferedImage image, int[] wood, int[] bark, int x0, int y0, int width, int height) {
        for (int y = y0 - 1; y <= y0 + height; y++) {
            for (int x = x0 - 1; x <= x0 + width; x++) {
                boolean inside = x >= x0 && x < x0 + width && y >= y0 && y < y0 + height;
                if (inside) {
                    image.setRGB(x, y, TRANSPARENT);
                } else if (y == y0 - 1 || x == x0 - 1) {
                    set(image, x, y, bark[0]);
                } else {
                    set(image, x, y, wood[wood.length - 1]);
                }
            }
        }
    }
    /**
     * Angular chunks as the ring 2 sunstone overlay (37b): each a diamond with random stretch and one corner cut off,
     * facet tones from {@code palette} [2..5] by the light from the top left, a glint [6] on the corner nearest the
     * light, outline [1]/[0]; chunks never touch.
     */
    private static void chunks(BufferedImage image, Random random, double[] sizes, int[] palette) {
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < sizes.length; attempt++) {
            double radius = sizes[placed] + 0.3 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double stretchX = 0.75 + 0.5 * random.nextDouble();
            double stretchY = 0.75 + 0.5 * random.nextDouble();
            double cutX = random.nextBoolean() ? 1 : -1;
            double cutY = random.nextBoolean() ? 1 : -1;
            BufferedImage chunk = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int glintX = -1;
            int glintY = -1;
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = (x + 0.5 - cx) * stretchX;
                    double dy = (y + 0.5 - cy) * stretchY;
                    if (Math.abs(dx) + Math.abs(dy) > radius * 1.2 || cutX * dx + cutY * dy > radius * 0.7) {
                        continue;
                    }
                    double light = -(dx + dy) / radius;
                    int tone = light > 0.5 ? 5 : light > -0.1 ? (dx < dy ? 4 : 5) : light > -0.6 ? 3 : 2;
                    set(chunk, x, y, palette[tone]);
                    if (glintX < 0 || x + y < glintX + glintY) {
                        glintX = x;
                        glintY = y;
                    }
                }
            }
            if (glintX < 0) {
                continue;
            }
            set(chunk, glintX, glintY, palette[6]);
            outlineSelective(chunk, palette[1], palette[0]);
            if (merge(image, chunk)) {
                placed++;
            }
        }
    }
    /**
     * One angular nugget centred on (cx, cy): an octagon (diamond and square blended) stretched by (sx, sy), with facet
     * tones from {@code palette} [0..5] by the light from the top left, its lower right rim in the darkest tone (the seam
     * against what lies behind it) and one glint on the lit corner.
     */
    private static void nugget(BufferedImage image, double cx, double cy, double radius, double sx, double sy, int[] palette) {
        int glintX = -1;
        int glintY = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = (x + 0.5 - cx) / sx;
                double dy = (y + 0.5 - cy) / sy;
                double edge = Math.max(Math.abs(dx) + Math.abs(dy), 1.3 * Math.max(Math.abs(dx), Math.abs(dy))) / (1.3 * radius);
                if (edge > 1) {
                    continue;
                }
                double light = -(dx + dy) / radius;
                int tone = light > 0.45 ? 4 : light > 0.0 ? 3 : light > -0.45 ? 2 : 1;
                if (edge > 0.82 && dx + dy > 0) {
                    tone = 0;
                }
                set(image, x, y, palette[tone]);
                if (glintX < 0 || x + y < glintX + glintY) {
                    glintX = x;
                    glintY = y;
                }
            }
        }
        set(image, glintX + 1, glintY + 1, palette[5]);
    }
    /**
     * A soft sprite ({@link #SOFT_ALPHA} only) from round blobs {@code {x, y, radius, peak}}: each falls off as
     * {@code (1 - (d/r)²)²}, sampled 4×4 per pixel so the rim is round; the colour runs from {@code edge} to {@code core}
     * with the alpha.
     */
    private static void softParticle(BufferedImage image, double[][] blobs, int core, int edge) {
        double brightest = 0.0;
        for (double[] blob : blobs) {
            brightest = Math.max(brightest, blob[3]);
        }
        for (int y = 0; y < PARTICLE_SIZE; y++) {
            for (int x = 0; x < PARTICLE_SIZE; x++) {
                double sum = 0.0;
                for (int sy = 0; sy < 4; sy++) {
                    for (int sx = 0; sx < 4; sx++) {
                        double px = x + (sx + 0.5) / 4.0;
                        double py = y + (sy + 0.5) / 4.0;
                        double clear = 1.0;
                        for (double[] blob : blobs) {
                            double d = Math.hypot(px - blob[0], py - blob[1]) / blob[2];
                            if (d < 1.0) {
                                double falloff = (1.0 - d * d) * (1.0 - d * d);
                                clear *= 1.0 - blob[3] * falloff;
                            }
                        }
                        sum += 1.0 - clear;
                    }
                }
                double alpha = sum / 16.0;
                int a = (int) Math.round(alpha * 255.0);
                if (a < 6) {
                    continue;
                }
                image.setRGB(x, y, a << 24 | mix(edge, core, Math.min(1.0, alpha / brightest)));
            }
        }
    }

    private static void particle(BufferedImage image, String[] rows, int[] tones) {
        for (int y = 0; y < PARTICLE_SIZE; y++) {
            for (int x = 0; x < PARTICLE_SIZE; x++) {
                switch (rows[y].charAt(x)) {
                    case '#' -> set(image, x, y, tones[0]);
                    case 'o' -> set(image, x, y, tones[1]);
                    case 'x' -> set(image, x, y, tones[2]);
                    default -> {
                    }
                }
            }
        }
    }

    /**
     * The animation strip: the frames under each other, each the rest frame leaning towards the material's dim tone or
     * its glow colour along a cosine. Only tones lighter than the dim tone move; outline and shadows stay (37b), and so do
     * the {@code still} tones (71a review: the lamp's stone frame). Only colours change; the alpha mask stays identical.
     */
    private static BufferedImage pulseStrip(BufferedImage frame, Animation animation, Set<Integer> still) {
        int size = frame.getWidth();
        BufferedImage strip = new BufferedImage(size, size * animation.frames(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < animation.frames(); i++) {
            double glow = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / animation.frames());
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = frame.getRGB(x, y);
                    if ((argb >>> 24) == 0) {
                        strip.setRGB(x, i * size + y, TRANSPARENT);
                    } else {
                        int rgb = argb & 0xFFFFFF;
                        if (luminance(rgb) > luminance(animation.dim()) && !still.contains(rgb)) {
                            rgb = mix(mix(rgb, animation.dim(), 0.12 * (1 - glow)), animation.glow(), 0.3 * glow);
                        }
                        set(strip, x, i * size + y, rgb);
                    }
                }
            }
        }
        return strip;
    }
    /** Random cell centres in a wrapping tile, for stones, plates and cracks. */
    private record Cells(double[][] points) {
        record Nearest(int index, double distance, double secondDistance, double dx, double dy) {
        }

        static Cells random(Random random, int count) {
            double[][] points = new double[count][2];
            for (double[] point : points) {
                point[0] = random.nextDouble() * SIZE;
                point[1] = random.nextDouble() * SIZE;
            }
            return new Cells(points);
        }

        int count() {
            return points.length;
        }

        /** Nearest and second-nearest centre (wrapping), and the offset to the nearest one scaled to about [-1, 1]. */
        Nearest nearest(int x, int y) {
            int best = -1;
            double first = Double.MAX_VALUE;
            double second = Double.MAX_VALUE;
            double bestDx = 0;
            double bestDy = 0;
            for (int i = 0; i < points.length; i++) {
                double dx = wrap(x + 0.5 - points[i][0]);
                double dy = wrap(y + 0.5 - points[i][1]);
                double distance = Math.hypot(dx, dy);
                if (distance < first) {
                    second = first;
                    first = distance;
                    best = i;
                    bestDx = dx;
                    bestDy = dy;
                } else if (distance < second) {
                    second = distance;
                }
            }
            return new Nearest(best, first, second, bestDx / 4.0, bestDy / 4.0);
        }

        private static double wrap(double delta) {
            return delta - SIZE * Math.round(delta / SIZE);
        }
    }

    /** The outermost ring of pixels, shaded from {@code palette} between {@code from} and {@code to}. */
    private static void rim(BufferedImage image, Random random, int[] palette, double from, double to) {
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, i, SIZE - 1, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, 0, i, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, SIZE - 1, i, shade(palette, from + random.nextDouble() * (to - from)));
        }
    }

    /**
     * Selective outline (37b-3 A): every transparent pixel next to the shape becomes {@code dark} below or right of the
     * shape and {@code light} (a dark tint of the material) above or left of it.
     */
    private static void outlineSelective(BufferedImage image, int light, int dark) {
        boolean[][] opaque = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                opaque[x][y] = isOpaque(image, x, y);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (opaque[x][y]) {
                    continue;
                }
                boolean belowOrRight = (x > 0 && opaque[x - 1][y]) || (y > 0 && opaque[x][y - 1]);
                boolean aboveOrLeft = (x < SIZE - 1 && opaque[x + 1][y]) || (y < SIZE - 1 && opaque[x][y + 1]);
                if (belowOrRight) {
                    set(image, x, y, dark);
                } else if (aboveOrLeft) {
                    set(image, x, y, light);
                }
            }
        }
    }

    /** A hand-laid sprite: 16 strings of 16 characters, {@code '.'} transparent, others a colour from {@code legend}. */
    private static void sprite(BufferedImage image, String[] rows, Map<Character, Integer> legend) {
        if (rows.length != SIZE) {
            throw new IllegalStateException("sprite needs " + SIZE + " rows, got " + rows.length);
        }
        for (int y = 0; y < SIZE; y++) {
            if (rows[y].length() != SIZE) {
                throw new IllegalStateException("sprite row " + y + " has " + rows[y].length() + " characters");
            }
            for (int x = 0; x < SIZE; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = legend.get(c);
                if (rgb == null) {
                    throw new IllegalStateException("sprite row " + y + ": no colour for '" + c + "'");
                }
                set(image, x, y, rgb);
            }
        }
    }

    /** Copies the opaque pixels of {@code part} into {@code image} if none touches an opaque pixel; returns whether it did. */
    private static boolean merge(BufferedImage image, BufferedImage part) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (isOpaque(part, x, y) && (isOpaque(image, x, y) || isOpaque(image, x - 1, y)
                        || isOpaque(image, x + 1, y) || isOpaque(image, x, y - 1) || isOpaque(image, x, y + 1))) {
                    return false;
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (isOpaque(part, x, y)) {
                    image.setRGB(x, y, part.getRGB(x, y));
                }
            }
        }
        return true;
    }

    /** Opaque pixel inside the tile; outside counts as transparent. */
    private static boolean isOpaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && (image.getRGB(x, y) >>> 24) != 0;
    }

    /**
     * How strongly a tile reads as stripes when repeated: the largest step in mean brightness between neighbouring rows,
     * columns, diagonals or anti-diagonals (all wrapping).
     */
    private static double stripeScore(BufferedImage image) {
        double[][] lines = new double[4][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double l = luminance(image.getRGB(x, y) & 0xFFFFFF) / SIZE;
                lines[0][y] += l;
                lines[1][x] += l;
                lines[2][(x + y) % SIZE] += l;
                lines[3][Math.floorMod(x - y, SIZE)] += l;
            }
        }
        double worst = 0;
        for (double[] line : lines) {
            for (int i = 0; i < SIZE; i++) {
                worst = Math.max(worst, Math.abs(line[i] - line[(i + 1) % SIZE]));
            }
        }
        return worst;
    }
    /** The block darkened as by a dark sky, its glow layer (emissive) at full brightness on top. */
    private static BufferedImage atNight(String name) throws IOException {
        BufferedImage base = render(name);
        String glowName = name + "_glow";
        BufferedImage glow = TEXTURES.containsKey(glowName) ? render(glowName) : null;
        BufferedImage night = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (glow != null && isOpaque(glow, x, y)) {
                    night.setRGB(x, y, glow.getRGB(x, y));
                } else if (isOpaque(base, x, y)) {
                    set(night, x, y, mix(base.getRGB(x, y) & 0xFFFFFF, 0x000000, 0.8));
                }
            }
        }
        return night;
    }

    /** Draws opaque pixels only; transparent pixels show {@code backdrop} (or what is already there when it is -1). */
    private static void drawOn(Graphics2D graphics, BufferedImage image, int left, int top, int scale, int backdrop) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 255) {
                    graphics.setColor(new Color(argb & 0xFFFFFF));
                } else if (alpha != 0) {
                    // A soft sprite (SOFT_ALPHA): blended over the backdrop, or drawn with its alpha without one.
                    graphics.setColor(backdrop >= 0 ? new Color(mix(backdrop, argb & 0xFFFFFF, alpha / 255.0)) : new Color(argb, true));
                } else if (backdrop >= 0) {
                    graphics.setColor(new Color(backdrop));
                } else {
                    continue;
                }
                graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
            }
        }
    }

    /** Draws a texture; transparency over a dusky blue-grey backdrop (the water ring at dusk). */
    private static void draw(Graphics2D graphics, BufferedImage image, int left, int top, int scale) {
        drawOn(graphics, image, left, top, scale, 0x3E5A66);
    }

    private static Random seeded(String name) {
        return new Random(name.hashCode());
    }
    /** Palette colour for {@code t} in [0, 1] (clamped), dark to light: quantises everything onto the palette. */
    private static int shade(int[] palette, double t) {
        int index = (int) (Math.max(0, Math.min(0.999, t)) * palette.length);
        return palette[index];
    }

    /** Per-channel mix of two RGB colours. */
    private static int mix(int from, int to, double t) {
        int r = (int) Math.round(lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, t));
        int g = (int) Math.round(lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, t));
        int b = (int) Math.round(lerp(from & 0xFF, to & 0xFF, t));
        return r << 16 | g << 8 | b;
    }

    /** Perceived brightness of an RGB colour in [0, 1] (Rec. 709 weights). */
    private static double luminance(int rgb) {
        return (0.2126 * (rgb >> 16 & 0xFF) + 0.7152 * (rgb >> 8 & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
    }
    /** Copies the pixels, and the glow marks if {@code from} has any. */
    private static void copy(BufferedImage from, BufferedImage to) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                to.setRGB(x, y, from.getRGB(x, y));
            }
        }
        boolean[][] mask = GLOW_MASKS.get(from);
        if (mask != null) {
            boolean[][] copied = new boolean[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                copied[x] = mask[x].clone();
            }
            GLOW_MASKS.put(to, copied);
        }
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private Ring4Textures() {
    }
}
