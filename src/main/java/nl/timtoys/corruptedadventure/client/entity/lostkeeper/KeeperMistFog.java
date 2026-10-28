package nl.timtoys.corruptedadventure.client.entity.lostkeeper;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.network.KeeperMistPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Lost Keeper's mist bank on the client: the banks the server sends ({@link KeeperMistPayload}), each eased in and
 * out, become fog round the camera: full within the bank's radius, thinning out over its fade distance, so the mist
 * thickens as it comes closer, also in clear weather. Far plane towards the bank's visibility, colour towards a pale
 * blue-white mist (dimmed at night). Runs after the ring weather and ring 4's own fog and only ever brings the planes
 * closer. Not under water, in lava or blind. A bank that stops coming (out of range) fades away. Client only.
 */
public final class KeeperMistFog {
    /** Pale blue-white mist (concept plate, "mist"). */
    private static final float RED = 0xBF / 255.0F;
    private static final float GREEN = 0xE8 / 255.0F;
    private static final float BLUE = 0xEC / 255.0F;
    private static final float COLOUR_SHARE = 0.85F;
    private static final float MIN_LIGHT = 0.15F;
    /** The sky fog reaches this multiple of the mist's visibility: the sky overhead stays a bright haze. */
    private static final float SKY_FACTOR = 2.5F;
    /** Per tick the shown strength moves this much towards the server's. */
    private static final float EASE = 1.0F / 40.0F;
    /** Ticks without a payload before a bank fades out (the server sends one each second). */
    private static final int STALE_TICKS = 60;

    private static final Map<Integer, Bank> BANKS = new HashMap<>();
    private static ClientLevel bankLevel;

    private KeeperMistFog() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, true, KeeperMistFog::onRenderFog);
        gameBus.addListener(EventPriority.LOW, KeeperMistFog::onComputeFogColor);
        gameBus.addListener(ClientTickEvent.Post.class, event -> tick());
        gameBus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BANKS.clear());
    }

    public static void handle(KeeperMistPayload payload, IPayloadContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) {
            return;
        }
        if (level != bankLevel) {
            BANKS.clear();
            bankLevel = level;
        }
        Bank bank = BANKS.computeIfAbsent(payload.keeperId(), id -> new Bank());
        bank.centre = new Vec3(payload.x(), payload.y(), payload.z());
        bank.radius = Math.max(0.0F, payload.radius());
        bank.fade = Math.max(1.0F, payload.fade());
        bank.visibility = Math.max(2.0F, payload.visibility());
        bank.target = Mth.clamp(payload.strength(), 0.0F, 1.0F);
        bank.lastSeen = level.getGameTime();
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != bankLevel) {
            BANKS.clear();
            bankLevel = level;
        }
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Bank> it = BANKS.values().iterator(); it.hasNext(); ) {
            Bank bank = it.next();
            if (now - bank.lastSeen > STALE_TICKS) {
                bank.target = 0.0F;
            }
            bank.shown += Mth.clamp(bank.target - bank.shown, -EASE, EASE);
            if (bank.shown <= 0.0F && bank.target <= 0.0F) {
                it.remove();
            }
        }
    }

    /** The thickest mist at {@code at}, 0–1, and the visibility of the bank that gives it (index 1). */
    static float[] mistAt(Vec3 at) {
        float best = 0.0F;
        float visibility = Float.MAX_VALUE;
        for (Bank bank : BANKS.values()) {
            float amount = bank.shown * falloff((float) at.distanceTo(bank.centre), bank.radius, bank.fade);
            if (amount > best) {
                best = amount;
                visibility = bank.visibility;
            }
        }
        return new float[]{best, visibility};
    }

    /** 1 within the radius, smoothly 0 at radius + fade. */
    static float falloff(float distance, float radius, float fade) {
        float t = Mth.clamp((distance - radius) / fade, 0.0F, 1.0F);
        return 1.0F - t * t * (3.0F - 2.0F * t);
    }

    private static boolean clearView(Camera camera) {
        if (camera.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        return !(camera.getEntity() instanceof LivingEntity living)
                || !living.hasEffect(MobEffects.BLINDNESS) && !living.hasEffect(MobEffects.DARKNESS);
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (BANKS.isEmpty() || !clearView(event.getCamera())) {
            return;
        }
        float[] mist = mistAt(event.getCamera().getPosition());
        float amount = mist[0];
        if (amount <= 0.0F) {
            return;
        }
        boolean sky = event.getMode() == FogRenderer.FogMode.FOG_SKY;
        float far = event.getFarPlaneDistance();
        float target = Math.min(far, mist[1] * (sky ? SKY_FACTOR : 1.0F));
        float newFar = Mth.lerp(amount, far, target);
        float newNear = Math.min(event.getNearPlaneDistance(), Mth.lerp(amount, event.getNearPlaneDistance(), 0.0F));
        if (newFar >= far && newNear >= event.getNearPlaneDistance()) {
            return;
        }
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Math.min(newNear, newFar));
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (BANKS.isEmpty() || !clearView(event.getCamera())
                || !(event.getCamera().getEntity().level() instanceof ClientLevel level)) {
            return;
        }
        float amount = mistAt(event.getCamera().getPosition())[0] * COLOUR_SHARE;
        if (amount <= 0.0F) {
            return;
        }
        float light = Math.max(MIN_LIGHT, level.getSkyDarken((float) event.getPartialTick()));
        event.setRed(Mth.lerp(amount, event.getRed(), RED * light));
        event.setGreen(Mth.lerp(amount, event.getGreen(), GREEN * light));
        event.setBlue(Mth.lerp(amount, event.getBlue(), BLUE * light));
    }

    private static final class Bank {
        private Vec3 centre = Vec3.ZERO;
        private float radius;
        private float fade = 1.0F;
        private float visibility = 16.0F;
        private float target;
        private float shown;
        private long lastSeen;
    }
}
