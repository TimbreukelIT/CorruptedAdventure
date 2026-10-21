package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * The glitch storm on the screen (ring 5, package 92): image noise (short flickering blocks and faint scanlines, under
 * the HUD) and now and then a hiccup of the hotbar: shifted or gone for 1–2 ticks, never longer than 0.1 s. Fed every
 * tick by {@link GlitchStormRenderer} while the storm is felt; without a feed it is gone the next tick. Hidden with F1.
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
    private static int hiccupTicks;
    /** Horizontal shift of the hotbar during a hiccup, 0 = hidden. */
    private static int hiccupShift;
    private static boolean shifted;

    private GlitchStormOverlay() {
    }

    static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(GlitchStormOverlay::onRegisterGuiLayers);
        gameBus.addListener(EventPriority.LOWEST, GlitchStormOverlay::onHotbarPre);
        gameBus.addListener(GlitchStormOverlay::onHotbarPost);
        gameBus.addListener(GlitchStormOverlay::onLoggingOut);
    }

    /** The storm's strength × sky exposure this tick; may start a hotbar hiccup. */
    static void feed(long gameTime, float strength, RandomSource random) {
        amount = Math.clamp(strength, 0.0F, 1.0F);
        fedAt = gameTime;
        if (hiccupTicks > 0) {
            hiccupTicks--;
        } else if (random.nextFloat() < HICCUP_CHANCE * amount) {
            hiccupTicks = 1 + random.nextInt(MAX_HICCUP_TICKS);
            hiccupShift = random.nextInt(3) == 0 ? 0 : (random.nextBoolean() ? 1 : -1) * (4 + random.nextInt(12));
        }
    }

    /** The strength now: the last feed, if it came this or the previous tick (the renderer feeds only while felt). */
    private static float current(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.level.getGameTime() - fedAt > 1L) {
            return 0.0F;
        }
        return amount;
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, CorruptedAdventure.id("glitch_storm"), GlitchStormOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        float strength = current(minecraft);
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

    private static void onHotbarPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        shifted = false;
        if (hiccupTicks <= 0 || current(Minecraft.getInstance()) <= 0.0F) {
            return;
        }
        if (hiccupShift == 0) {
            event.setCanceled(true);
            return;
        }
        // Lowest priority: nothing cancels after us, so Post comes and pops.
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(hiccupShift, 0.0F, 0.0F);
        shifted = true;
    }

    private static void onHotbarPost(RenderGuiLayerEvent.Post event) {
        if (shifted && event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.getGuiGraphics().pose().popPose();
            shifted = false;
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        amount = 0.0F;
        fedAt = NEVER;
        hiccupTicks = 0;
        shifted = false;
    }
}
