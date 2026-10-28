package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An open rift pulls players and items toward its middle, harder the closer they are (linear from the radius to the
 * rift). Players in creative or spectator are left alone. A player's push is sent to their client at once
 * ({@code hurtMarked}), since the client moves its own player.
 */
final class RiftSuction {
    private RiftSuction() {
    }

    static void pull(ServerLevel level, HollowRift rift, double radius, double strength) {
        if (strength <= 0.0) {
            return;
        }
        Vec3 middle = rift.position().add(0.0, rift.getBbHeight() * 0.5, 0.0);
        AABB area = new AABB(middle, middle).inflate(radius);
        for (Entity entity : level.getEntities(rift, area, RiftSuction::pulled)) {
            Vec3 toward = middle.subtract(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0));
            double distance = toward.length();
            if (distance < 0.5 || distance > radius) {
                continue;
            }
            Vec3 push = toward.scale(strength * (1.0 - distance / radius) / distance);
            entity.push(push.x, push.y * 0.5, push.z);
            if (entity instanceof ServerPlayer player) {
                player.hurtMarked = true;
            }
        }
    }

    static boolean pulled(Entity entity) {
        if (entity instanceof Player player) {
            return player.isAlive() && !player.isCreative() && !player.isSpectator();
        }
        return entity instanceof ItemEntity && entity.isAlive();
    }
}
