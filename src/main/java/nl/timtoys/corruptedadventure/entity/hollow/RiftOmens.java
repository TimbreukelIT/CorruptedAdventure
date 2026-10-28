package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

import java.util.List;

/**
 * The omens of an arrival (server side; ~30 s before the rift opens): animals flee from the spot, sounds play as if
 * backwards (vanilla sounds at a low, dragging pitch) and the hairline crack sheds glitch pixels. The torches flicker on
 * the client ({@code client/entity/hollow/RiftOmenFlicker}); no block is touched.
 */
final class RiftOmens {
    private static final List<SoundEvent> BACKWARDS = List.of(
            SoundEvents.PORTAL_TRIGGER, SoundEvents.AMBIENT_CAVE.value(), SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundEvents.WARDEN_HEARTBEAT, SoundEvents.ENDERMAN_STARE);

    private RiftOmens() {
    }

    static void tick(ServerLevel level, HollowRift rift, int age) {
        RandomSource random = rift.getRandom();
        Vec3 crack = rift.position().add(0.0, 3.5, 0.0);
        if (age % 20 == 0) {
            fleeAnimals(level, rift, crack);
        }
        if (age % 60 == 0) {
            SoundEvent sound = BACKWARDS.get(random.nextInt(BACKWARDS.size()));
            level.playSound(null, crack.x, crack.y, crack.z, sound, SoundSource.HOSTILE, 1.2F, 0.5F + random.nextFloat() * 0.15F);
        }
        if (age % 5 == 0) {
            level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), crack.x, crack.y, crack.z, 2, 0.05, 1.2, 0.05, 0.0);
        }
    }

    private static void fleeAnimals(ServerLevel level, HollowRift rift, Vec3 from) {
        double radius = ServerConfig.HOLLOW.fleeRadius.get();
        for (Animal animal : level.getEntitiesOfClass(Animal.class, rift.getBoundingBox().inflate(radius))) {
            if (animal.isLeashed() || animal.isVehicle() || animal.isPassenger()) {
                continue;
            }
            Vec3 away = DefaultRandomPos.getPosAway(animal, 16, 7, from);
            if (away != null) {
                animal.getNavigation().moveTo(away.x, away.y, away.z, 1.6);
            }
        }
    }
}
