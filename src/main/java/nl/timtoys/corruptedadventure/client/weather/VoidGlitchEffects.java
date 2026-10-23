package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The void meter's first threshold on the client (package 98, V98-11 A; the server does nothing there, package 91): a
 * light form of the glitch storm's noise on the screen ({@link GlitchStormOverlay#feedVoid}) and glitch pixels around the
 * player, growing with the meter from the first threshold (0) to 100 ({@link #MAX_SHARE} of a full storm). No hotbar
 * hiccup (that stays the storm's). × vanilla's "Screen Effect Scale" for the screen noise (0 = off). From the synced
 * meter and the synced thresholds; client only.
 */
public final class VoidGlitchEffects {
    /** A full meter shows this much of a full glitch storm's noise. */
    static final float MAX_SHARE = 0.6F;
    /** Glitch pixels per tick around the player at a full meter. */
    static final float MAX_PIXELS = 0.5F;

    private VoidGlitchEffects() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(VoidGlitchEffects::onClientTick);
    }

    /** The share of a full storm at {@code meter} for the first threshold {@code low}: 0 below it, {@link #MAX_SHARE} at 100. Pure. */
    static float strength(int meter, int low) {
        if (meter < low || low >= 100) {
            return 0.0F;
        }
        return MAX_SHARE * Mth.clamp((meter - low) / (float) (100 - low), 0.0F, 1.0F);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        int[] levels = Ring5Meters.thresholds(Condition.VOID, 1).orElse(null);
        if (levels == null || !Ring5Meters.exposed(minecraft)) {
            return;
        }
        float strength = strength(Ring5Meters.value(player, Condition.VOID), levels[0]);
        if (strength <= 0.0F) {
            return;
        }
        long now = minecraft.level.getGameTime();
        GlitchStormOverlay.feedVoid(now, strength * minecraft.options.screenEffectScale().get().floatValue());
        RandomSource random = minecraft.level.getRandom();
        float expected = MAX_PIXELS * strength / MAX_SHARE;
        if (random.nextFloat() < expected) {
            Vec3 at = player.position().add((random.nextDouble() - 0.5) * 6.0, random.nextDouble() * 2.5, (random.nextDouble() - 0.5) * 6.0);
            minecraft.level.addParticle(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }
}
