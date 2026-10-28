package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * The glitch storm on the screen (ring 5, package 92): image noise (short flickering blocks and faint scanlines, under
 * the HUD) and now and then a hiccup of the hotbar: shifted or gone for 1–2 ticks, never longer than 0.1 s (and never
 * while paused). Fed every
 * tick by {@link GlitchStormRenderer} while the storm is felt; without a feed it is gone the next tick. Hidden with F1.
 * Since package 98 the void meter's glitch visuals feed the same noise ({@link #feedVoid}, {@link VoidGlitchEffects}),
 * without the hotbar hiccup; the stronger of the two shows.
 */
final class GlitchStormOverlay {
    /** Noise blocks on screen in a full storm. */
    static final int MAX_BLOCKS = 48;
    /** The noise pattern changes every this many ticks (a flicker, not a blur). */
    private static final int PATTERN_TICKS = 2;
    /** Longest hiccup of the hotbar, in ticks (0.1 s). */
    static final int MAX_HICCUP_TICKS = 2;
    /** Chance per tick of a hotbar hiccup in a full storm. */
    private static final float HICCUP_CHANCE = 0.02F;
    /** Violet, magenta, cyan-white and black, as in the glitch specks. */
    private static final int[] COLOURS = {0x7A3CFF, 0xD03CFF, 0xE8F0FF, 0x000000, 0x1A1022};

    /** A game time long before any feed. */
    private static final long NEVER = -1000L;

    private static float amount;
    private static long fedAt = NEVER;
    private static float voidAmount;
    private static long voidFedAt = NEVER;
    private static int hiccupTicks;
    /** Horizontal shift of the hotbar during a hiccup, 0 = hidden. */
    private static int hiccupShift;
    /** The translation the shift layer applied this frame, taken back by the layer above the hotbar. */
    private static float appliedX;
    private static float appliedY;

    private GlitchStormOverlay() {
    }

    static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(GlitchStormOverlay::onRegisterGuiLayers);
        gameBus.addListener(GlitchStormOverlay::onLoggingOut);
    }

    /** The storm's strength × sky exposure this tick; may start a hotbar hiccup. */
    static void feed(long gameTime, float strength, RandomSource random) {
        amount = Math.clamp(strength, 0.0F, 1.0F);
        fedAt = gameTime;
        if (hiccupTicks > 0) {
            hiccupTicks--;
        } else if (random.nextFloat() < HICCUP_CHANCE * amount * GlitchVisuals.scale()) {
            hiccupTicks = 1 + random.nextInt(MAX_HICCUP_TICKS);
            hiccupShift = random.nextInt(3) == 0 ? 0 : (random.nextBoolean() ? 1 : -1) * (4 + random.nextInt(12));
        }
    }

    /** The void meter's glitch visuals this tick (98): noise only, no hotbar hiccup. */
    static void feedVoid(long gameTime, float strength) {
        voidAmount = Math.clamp(strength, 0.0F, 1.0F);
        voidFedAt = gameTime;
    }

    /** The storm's strength now: the last feed, if it came this or the previous tick (the renderer feeds only while felt). */
    private static float current(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.level.getGameTime() - fedAt > 1L) {
            return 0.0F;
        }
        return amount;
    }

    /**
     * The noise's strength now: the storm's or the void meter's, whichever is stronger, × what the sky glass lens leaves
     * of it ({@link GlitchVisuals}).
     */
    private static float noise(Minecraft minecraft) {
        float voidNow = minecraft.level == null || minecraft.level.getGameTime() - voidFedAt > 1L ? 0.0F : voidAmount;
        return Math.max(current(minecraft), voidNow) * GlitchVisuals.scale();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, CorruptedAdventure.id("glitch_storm"), GlitchStormOverlay::render);
        // The hiccup: our own layers right below and right above the hotbar move it and move it back. Only translations,
        // no push/pop, so the pose stack stays balanced whatever other mods cancel.
        event.registerBelow(VanillaGuiLayers.HOTBAR, CorruptedAdventure.id("glitch_hotbar_shift"), GlitchStormOverlay::shiftHotbar);
        event.registerAbove(VanillaGuiLayers.HOTBAR, CorruptedAdventure.id("glitch_hotbar_unshift"), GlitchStormOverlay::unshiftHotbar);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        float strength = noise(minecraft);
        // Our layer is not inside vanilla's F1 group, so check hideGui ourselves.
        if (strength <= 0.0F || minecraft.options.hideGui || minecraft.level == null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // Faint scanlines over the whole screen.
        int scanline = alpha(0.06F * strength);
        for (int y = 0; y < height; y += 3) {
            graphics.fill(0, y, width, y + 1, scanline);
        }
        // The same blocks for PATTERN_TICKS ticks, then a new pattern.
        RandomSource random = RandomSource.create(minecraft.level.getGameTime() / PATTERN_TICKS * 0x9E3779B97F4A7C15L);
        int blocks = blocks(strength);
        for (int i = 0; i < blocks; i++) {
            int w = 2 + random.nextInt(28);
            int h = 1 + random.nextInt(6);
            int x = random.nextInt(Math.max(1, width - w));
            int y = random.nextInt(Math.max(1, height - h));
            int colour = COLOURS[random.nextInt(COLOURS.length)];
            graphics.fill(x, y, x + w, y + h, alpha((0.15F + 0.3F * random.nextFloat()) * strength) | colour);
        }
    }

    /** Noise blocks at {@code strength}. */
    static int blocks(float strength) {
        return Math.round(MAX_BLOCKS * Math.clamp(strength, 0.0F, 1.0F));
    }

    private static int alpha(float alpha) {
        return Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
    }

    /** Below the hotbar: shifts it sideways, or off the bottom of the screen (gone), during a hiccup. Not while paused. */
    private static void shiftHotbar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        appliedX = 0.0F;
        appliedY = 0.0F;
        Minecraft minecraft = Minecraft.getInstance();
        if (hiccupTicks <= 0 || minecraft.isPaused() || current(minecraft) <= 0.0F) {
            return;
        }
        appliedX = hiccupShift;
        appliedY = hiccupShift == 0 ? graphics.guiHeight() : 0.0F;
        graphics.pose().translate(appliedX, appliedY, 0.0F);
    }

    /** Above the hotbar: takes the shift back. */
    private static void unshiftHotbar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (appliedX != 0.0F || appliedY != 0.0F) {
            graphics.pose().translate(-appliedX, -appliedY, 0.0F);
            appliedX = 0.0F;
            appliedY = 0.0F;
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        amount = 0.0F;
        fedAt = NEVER;
        voidAmount = 0.0F;
        voidFedAt = NEVER;
        hiccupTicks = 0;
    }
}
