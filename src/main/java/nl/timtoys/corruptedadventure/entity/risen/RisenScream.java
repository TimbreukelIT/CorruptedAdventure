package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;

/**
 * The choir's scream (design doc ring 3, "Koorschreeuw"): every voice at once. Fighters within {@code screamRadius} get
 * weakness and nausea; animals and mobs within {@code fleeRadius} run from it (not the Nightmares, not its own ash
 * walkers). Server-side.
 */
final class RisenScream {
    private static final double FLEE_SPEED = 1.5;

    private RisenScream() {
    }

    static void scream(ServerLevel level, Nightmare risen, RisenConfig config) {
        RisenEffects.scream(level, risen);
        int weakness = config.screamWeaknessSeconds.get() * 20;
        int nausea = config.screamNauseaSeconds.get() * 20;
        for (Player player : risen.fighters(config.screamRadius.get())) {
            if (weakness > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness), risen);
            }
            if (nausea > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea), risen);
            }
        }
        double flee = config.fleeRadius.get();
        Vec3 from = risen.position();
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, risen.getBoundingBox().inflate(flee),
                mob -> !(mob instanceof Nightmare) && !(mob instanceof AshWalker) && mob.isAlive())) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, from);
            if (away != null) {
                mob.setTarget(null);
                mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }
}
