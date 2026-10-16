package nl.timtoys.corruptedadventure.command.terrainmap;

import java.awt.Color;
import java.util.Map;

/**
 * Fixed colours of the terrain maps (RGB). The mod's own biomes have a fixed colour per ring theme; other biomes
 * (vanilla in ring 1 and Nether-ring 1) get a stable colour from their id, so a map looks the same on every run.
 */
final class MapPalette {
    static final int CONTOUR_STEP = 16;
    static final int CLOUD_Y = 192;

    static final int CLOUD_LINE = 0xFF0000;
    static final int RING_LINE_ON_HEIGHT = 0xFF00FF;
    static final int RING_LINE_ON_BIOMES = 0x000000;
    static final int WATER = 0x2A5BD7;
    static final int LAVA = 0xFF6A00;
    static final int STONE = 0x7F7F7F;
    static final int SKY = 0xBFE3FF;
    static final int CAVE = 0x3A3A46;
    static final int GUIDE_LINE = 0xFFFFFF;
    static final int SEA_LEVEL_LINE = 0x00C0FF;
    static final int BACKGROUND = 0x1E1E1E;
    static final int TEXT = 0xFFFFFF;

    /** Height ramp (fraction of the dimension's height, colour); in the overworld 0.33 is the sea level, 0.67 Y 192. */
    private static final double[] RAMP_AT = {0.0, 0.25, 0.33, 0.40, 0.47, 0.55, 0.63, 0.70, 0.85, 1.0};
    private static final int[] RAMP_RGB = {0x0D0887, 0x1F3A93, 0x2C7F3F, 0x6FB04B, 0xD6D35A, 0xD9A441, 0xB06A2E, 0x7A4A2A,
            0xB8B0A8, 0xFFFFFF};

    private static final Map<String, Integer> BIOMES = Map.ofEntries(
            // ring 1 and its border
            Map.entry("corruptedadventure:withered_expanse", 0x8A7F5A),
            Map.entry("corruptedadventure:dry_riverbeds", 0xC9A66B),
            // ring 2
            Map.entry("corruptedadventure:canyonlands", 0xB5562E),
            Map.entry("corruptedadventure:oasis_flats", 0x3FAE6A),
            Map.entry("corruptedadventure:hoodoo_fields", 0xD9822B),
            Map.entry("corruptedadventure:salt_flats", 0xF0EDE4),
            Map.entry("corruptedadventure:dune_sea", 0xEDC96B),
            // Nether-ring 2
            Map.entry("corruptedadventure:dripshroom_forest", 0x5A8F7B),
            Map.entry("corruptedadventure:spore_caves", 0x8C6BB1),
            // ring 3
            Map.entry("corruptedadventure:ashen_wastes", 0x9A9A9A),
            Map.entry("corruptedadventure:volcanic_peaks", 0x6E1E14),
            Map.entry("corruptedadventure:ember_woods", 0xD2451E),
            Map.entry("corruptedadventure:basalt_flows", 0x3C3C50),
            Map.entry("corruptedadventure:sulfur_fields", 0xD8D23A),
            Map.entry("corruptedadventure:dried_seabed", 0xB9A98A),
            Map.entry("corruptedadventure:titan_fields", 0x4FB6C9),
            Map.entry("corruptedadventure:dormant_caldera", 0x4F9A3A),
            Map.entry("corruptedadventure:steam_fens", 0x7AA6A0),
            // Nether-ring 3
            Map.entry("corruptedadventure:frozen_lava_sea", 0x9FD3FF),
            Map.entry("corruptedadventure:rime_forest", 0x5F8FB8),
            Map.entry("corruptedadventure:glacier_rifts", 0x2F4D8A),
            // ring 4: mythic band light and watery, blighted band dull, the zones on their own
            Map.entry("corruptedadventure:verdant_falls", 0x9BE35A),
            Map.entry("corruptedadventure:cascade_vale", 0xE6B8D8),
            Map.entry("corruptedadventure:crystal_lagoon", 0x2BE3B4),
            Map.entry("corruptedadventure:lotus_shallows", 0xF2C6A0),
            Map.entry("corruptedadventure:glowroot_swamp", 0x2E7D6B),
            Map.entry("corruptedadventure:mist_fjords", 0x8FA8C8),
            Map.entry("corruptedadventure:sky_gardens", 0xFFF2A8),
            Map.entry("corruptedadventure:peat_moors", 0x5B4A32),
            Map.entry("corruptedadventure:reed_marsh", 0xA8A060),
            Map.entry("corruptedadventure:drowned_forest", 0x3E4F33),
            Map.entry("corruptedadventure:withered_gardens", 0x8C7A8A),
            Map.entry("corruptedadventure:sundered_reach", 0x5E6656),
            // Nether-ring 4
            Map.entry("corruptedadventure:bone_dunes", 0xE3D7B0),
            Map.entry("corruptedadventure:crystal_spires", 0xC9C2FF),
            Map.entry("corruptedadventure:geode_hollows", 0x8A6BC9),
            // placeholders and the edge
            Map.entry("corruptedadventure:placeholder_ring_3", 0x7A4A3A),
            Map.entry("corruptedadventure:placeholder_ring_5", 0x5A3A6A),
            Map.entry("corruptedadventure:placeholder_nether_ring_5", 0x3A2A4A),
            Map.entry("corruptedadventure:void_edge", 0x101010),
            Map.entry("corruptedadventure:nether_void_edge", 0x280A0A));

    private MapPalette() {
    }

    static int biome(String id) {
        Integer fixed = BIOMES.get(id);
        return fixed != null ? fixed : fromId(id);
    }

    static boolean hasFixedColour(String id) {
        return BIOMES.containsKey(id);
    }

    /** {@link String#hashCode} is specified, so the colour is the same on every JVM. */
    private static int fromId(String id) {
        int hash = id.hashCode();
        float hue = (hash & 0xFFFF) / 65536.0F;
        float saturation = 0.45F + ((hash >>> 16) & 0xFF) / 255.0F * 0.3F;
        float brightness = 0.6F + ((hash >>> 24) & 0xFF) / 255.0F * 0.3F;
        return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    /** Height {@code y} in a dimension from {@code minY} to {@code maxY} on the ramp. */
    static int height(int y, int minY, int maxY) {
        double t = Math.clamp((double) (y - minY) / Math.max(1, maxY - minY), 0.0, 1.0);
        for (int i = 1; i < RAMP_AT.length; i++) {
            if (t <= RAMP_AT[i]) {
                double local = (t - RAMP_AT[i - 1]) / (RAMP_AT[i] - RAMP_AT[i - 1]);
                return lerp(RAMP_RGB[i - 1], RAMP_RGB[i], local);
            }
        }
        return RAMP_RGB[RAMP_RGB.length - 1];
    }

    /** Fluid surface colour, darker the deeper the fluid (depth in blocks). */
    static int fluid(boolean lava, int depth) {
        double shade = 1.0 - Math.min(depth, 32) / 32.0 * 0.55;
        return scale(lava ? LAVA : WATER, shade);
    }

    static int scale(int rgb, double factor) {
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * factor);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * factor);
        int b = (int) Math.round((rgb & 0xFF) * factor);
        return (Math.min(r, 255) << 16) | (Math.min(g, 255) << 8) | Math.min(b, 255);
    }

    static int lerp(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
