package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The ash rain of The Risen's wings (design doc ring 3, "Tot afgietsel maken"): who stands under it with open sky above
 * gets a layer of ash ({@link AshenMobEffect}, level = layers) every {@code ashTicksPerLayer} ticks, until six layers hold
 * them fast. The effect's duration is the timer of the next change: out of the rain a layer falls off every
 * {@code ashDecaySeconds}; held fast, you break free after {@code ashEncasedSeconds} (back to three layers). A hit of
 * another player knocks {@code ashKnockOff} layers off and does no damage. Server-side; the effect syncs itself.
 * Dying held fast leaves a cast in step 2 (backlog).
 */
public final class RisenAshRain {
    public static final int MAX_LAYERS = 6;
    private static final int BROKEN_FREE_LAYERS = 3;

    private RisenAshRain() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RisenAshRain::onPlayerTick);
        gameBus.addListener(RisenAshRain::onAttack);
    }

    /**
     * Tick {@code tick} of a rain from {@code risen}: ash particles down to the ground and, on the beat, a layer on every
     * fighter under it.
     */
    static void rain(ServerLevel level, Nightmare risen, int tick, RisenConfig config) {
        double radius = config.ashRainRadius.get();
        if (tick % 2 == 0) {
            double bottom = level.getHeight(Heightmap.Types.MOTION_BLOCKING, risen.getBlockX(), risen.getBlockZ());
            RisenEffects.ashRain(level, risen.getX(), risen.getY(), bottom, risen.getZ(), radius);
        }
        if (tick % config.ashTicksPerLayer.get() != 0) {
            return;
        }
        for (Player player : risen.fighters()) {
            double dx = player.getX() - risen.getX();
            double dz = player.getZ() - risen.getZ();
            if (dx * dx + dz * dz <= radius * radius && player.getY() < risen.getY()
                    && level.canSeeSky(player.blockPosition().above())) {
                setLayers(player, layers(player) + 1, config);
            }
        }
    }

    /** Layers of ash on an entity, 0 without the effect. */
    public static int layers(LivingEntity entity) {
        MobEffectInstance ash = entity.getEffect(ModMobEffects.ASHEN);
        return ash == null ? 0 : ash.getAmplifier() + 1;
    }

    /**
     * Sets the layers (clamped) with a fresh timer. More ash on someone held fast changes nothing, so the timer runs on
     * and they always break free in time.
     */
    static void setLayers(LivingEntity entity, int layers, RisenConfig config) {
        int target = Math.max(0, Math.min(MAX_LAYERS, layers));
        if (target == layers(entity)) {
            return;
        }
        entity.removeEffect(ModMobEffects.ASHEN);
        if (target > 0) {
            int seconds = target == MAX_LAYERS ? config.ashEncasedSeconds.get() : config.ashDecaySeconds.get();
            entity.addEffect(new MobEffectInstance(ModMobEffects.ASHEN, seconds * 20, target - 1));
        }
    }

    /** The timer runs out one tick before vanilla would remove the effect: a layer falls off, or you break free. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        MobEffectInstance ash = player.getEffect(ModMobEffects.ASHEN);
        // An infinite one (-1, e.g. from /effect) is not a timer: it stays until removed.
        if (ash != null && !ash.isInfiniteDuration() && ash.getDuration() <= 1) {
            int layers = ash.getAmplifier() + 1;
            setLayers(player, layers == MAX_LAYERS ? BROKEN_FREE_LAYERS : layers - 1, ServerConfig.RISEN);
        }
    }

    /** Another player hitting someone covered in ash knocks it off instead of hurting them. */
    private static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (attacker.level().isClientSide() || !(event.getTarget() instanceof Player target) || target == attacker
                || attacker.isSpectator()) {
            return;
        }
        if (knockOff(target)) {
            event.setCanceled(true);
        }
    }

    /** A hit of another player on {@code target}: knocks ash off; false (a normal hit) when none came off. */
    public static boolean knockOff(Player target) {
        int layers = layers(target);
        if (layers == 0) {
            return false;
        }
        setLayers(target, layers - ServerConfig.RISEN.ashKnockOff.get(), ServerConfig.RISEN);
        return layers(target) < layers;
    }

    /** One more layer of ash, as from the rain. */
    public static void addLayer(LivingEntity entity) {
        setLayers(entity, layers(entity) + 1, ServerConfig.RISEN);
    }
}
