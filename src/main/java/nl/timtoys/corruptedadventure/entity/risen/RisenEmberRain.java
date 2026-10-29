package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.RisenConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The ember rain (leven-ronde): it hangs over an area, the rim of it smoulders first ({@code emberRainWindupTicks}), then
 * burning embers fall in volleys. Each ember marks its spot with smoke and a flame {@code emberFallTicks} before it
 * lands, so you can step aside; where it lands it hurts who stands there and the ground burns a while
 * ({@link RisenFire}). The area stays where it was when the rain began: run out of it. Embers already falling still
 * land when the rain is broken off (stun). Server-side, not saved.
 */
final class RisenEmberRain {
    private static final double HIT_RADIUS = 1.5;
    /** Embers land on the surface within this many blocks up or down of the ground the area was centred on. */
    private static final double REACH = 12.0;

    private Vec3 centre = Vec3.ZERO;
    private final List<Ember> falling = new ArrayList<>();

    private record Ember(BlockPos spot, long lands) {
    }

    void start(Vec3 centre) {
        this.centre = centre;
    }

    Vec3 centre() {
        return centre;
    }

    /** Tick {@code tick} of the rain: the rim during the windup, then a volley every {@code emberRainVolleyTicks}. */
    void rain(ServerLevel level, int tick, RisenConfig config, RandomSource random) {
        double radius = config.emberRainRadius.get();
        int windup = config.emberRainWindupTicks.get();
        if (tick <= windup) {
            if (tick % 5 == 1) {
                RisenEffects.emberRainRim(level, centre, radius);
            }
            return;
        }
        if (!isVolley(tick, windup, config.emberRainVolleyTicks.get())) {
            return;
        }
        for (int i = 0; i < config.emberRainPerVolley.get(); i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            BlockPos spot = RisenFire.surfaceTile(level, centre.x + Math.cos(angle) * distance,
                    centre.z + Math.sin(angle) * distance, centre.y, REACH);
            if (spot != null) {
                falling.add(new Ember(spot, level.getGameTime() + config.emberFallTicks.get()));
            }
        }
    }

    /** Whether tick {@code tick} of the rain drops a volley: the first tick after the windup, then every {@code volley}. */
    static boolean isVolley(int tick, int windup, int volley) {
        return tick > windup && (tick - windup - 1) % volley == 0;
    }

    /** Every tick, also after the rain: warnings under the embers still falling, and the ones that land. */
    void tick(ServerLevel level, TheRisen risen, RisenFire fire, RisenConfig config) {
        long now = level.getGameTime();
        for (Iterator<Ember> it = falling.iterator(); it.hasNext(); ) {
            Ember ember = it.next();
            long left = ember.lands() - now;
            if (left > 0) {
                if (left % 5 == 0) {
                    RisenEffects.emberFalling(level, ember.spot(), left);
                }
                continue;
            }
            it.remove();
            land(level, risen, fire, ember.spot(), config);
        }
    }

    boolean isFalling() {
        return !falling.isEmpty();
    }

    void clear() {
        falling.clear();
    }

    private static void land(ServerLevel level, TheRisen risen, RisenFire fire, BlockPos spot, RisenConfig config) {
        RisenEffects.emberLands(level, spot);
        fire.light(level, spot, config);
        // Now and then it splashes on a neighbour.
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
        if (level.random.nextBoolean()) {
            fire.light(level, spot.relative(side), config);
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        AABB box = new AABB(at, at).inflate(HIT_RADIUS, 1.0, HIT_RADIUS).expandTowards(0.0, 1.0, 0.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != risen && e.isAlive() && !e.isSpectator() && !(e instanceof AshWalker))) {
            if (entity.hurt(risen.damageSources().mobAttack(risen), config.emberDamage.get().floatValue() * risen.strength())) {
                entity.igniteForSeconds(config.fireIgniteSeconds.get());
            }
        }
    }
}
