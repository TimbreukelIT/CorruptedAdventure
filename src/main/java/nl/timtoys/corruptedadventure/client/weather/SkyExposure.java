package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * How open to the sky the camera is, 0–1: sky light at the camera ÷ 15, eased over ~2 s so walking into a house or cave
 * fades the weather instead of cutting it. Shared by all weather renderers (visual only; the wind push on the server
 * uses a hard "sees the sky" check). Updated once per client tick, main thread only.
 */
public final class SkyExposure {
    /** Largest change per tick: a full step from outside to inside takes 40 ticks. */
    static final float STEP_PER_TICK = 1.0F / 40.0F;

    private static float value;
    private static @Nullable Level level;

    private SkyExposure() {
    }

    public static float get() {
        return value;
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            level = null;
            value = 0.0F;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        BlockPos camera = BlockPos.containing(minecraft.gameRenderer.getMainCamera().getPosition());
        float target = minecraft.level.getBrightness(LightLayer.SKY, camera) / 15.0F;
        // A new level (join, dimension change) starts at the real value instead of fading in from the old one.
        value = level == minecraft.level ? approach(value, target, STEP_PER_TICK) : target;
        level = minecraft.level;
    }

    /** {@code current} moved towards {@code target} by at most {@code maxStep}. */
    static float approach(float current, float target, float maxStep) {
        if (Math.abs(target - current) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, target - current);
    }
}
