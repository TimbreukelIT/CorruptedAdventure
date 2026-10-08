package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.ring.Ring;

/**
 * Breath in the cold of Nether ring 3 (package 56, G2-A5 = A): players and the creatures that clearly breathe (animals,
 * villagers, piglins, illagers; not the undead, not slimes, blazes, golems or armour stands) puff a small white cloud
 * about every two seconds while they stand in Nether ring 3. Client only: each client draws it for everyone it sees,
 * so no sync is needed. Not in spectator mode, not with the head in water, lava or powder snow, and not for the own
 * player in first person (the puff would sit right in front of the camera). Vanilla's "Particles" option applies
 * ({@code addParticle}).
 */
public final class ColdBreath {
    /** How far from the camera breath is drawn. */
    private static final double RANGE = 24.0;
    /** Ticks between two breaths of one entity; each entity has its own phase. */
    private static final int INTERVAL = 40;

    private ColdBreath() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ColdBreath::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused() || level.dimension() != Level.NETHER) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        for (Entity entity : level.entitiesForRendering()) {
            // Cheapest checks first: most entities are not breathing this tick.
            if (!(entity instanceof LivingEntity living) || (entity.tickCount + entity.getId() * 7) % INTERVAL != 0
                    || entity.distanceToSqr(camera) > RANGE * RANGE
                    || (firstPerson && entity == minecraft.player)
                    || !breathes(living)
                    || !inNetherRing3(level, entity.getX(), entity.getZ())) {
                continue;
            }
            puff(level, living);
        }
    }

    private static boolean breathes(LivingEntity entity) {
        boolean breather = entity instanceof Player || entity instanceof Animal || entity instanceof AbstractVillager
                || entity instanceof AbstractPiglin || entity instanceof AbstractIllager;
        return breather && entity.isAlive() && !entity.isSpectator() && !entity.isInvisible()
                && !entity.getType().is(EntityTypeTags.UNDEAD)
                && !entity.isEyeInFluid(FluidTags.WATER) && !entity.isEyeInFluid(FluidTags.LAVA) && !entity.isInPowderSnow;
    }

    private static boolean inNetherRing3(ClientLevel level, double x, double z) {
        return ClientRingLayout.ringAt(level.dimension(), x, z).map(position -> position.ring() == Ring.RING_3).orElse(false);
    }

    /** Two or three small puffs just in front of the mouth, drifting the way the head looks. */
    private static void puff(ClientLevel level, LivingEntity entity) {
        Vec3 look = Vec3.directionFromRotation(0.0F, entity.getYHeadRot());
        double reach = entity.getBbWidth() * 0.5 + 0.25;
        double x = entity.getX() + look.x * reach;
        double y = entity.getEyeY() - 0.1;
        double z = entity.getZ() + look.z * reach;
        int count = 2 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, look.x * 0.03 + level.getRandom().nextGaussian() * 0.005,
                    0.005, look.z * 0.03 + level.getRandom().nextGaussian() * 0.005);
        }
    }
}
