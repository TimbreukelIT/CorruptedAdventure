package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ash of his Risen wing: the clouds he has let loose and the layers of ash on each player. Once a second a player in
 * a cloud gets a layer, outside one loses a layer; each layer is a level of slowness, and at {@code ashMaxStacks} the
 * player stands fast (slowness that stops walking). A simple own version for step 1; R3's ash (afgietsel, knocking the ash
 * off) can replace it once that is merged (backlog). Lives on the boss, never saved: a reload clears the ash.
 */
final class UnfinishedAsh {
    private static final int PUFF_TICKS = 4;
    private static final int LAYER_TICKS = 20;
    /** Slowness level that stops walking (7 levels x 15 %). */
    private static final int STAND_FAST_AMPLIFIER = 6;

    private final List<Cloud> clouds = new ArrayList<>();
    private final Map<UUID, Integer> layers = new HashMap<>();

    void add(Vec3 centre, double radius, int ticks, int now) {
        clouds.add(new Cloud(centre, radius, now + ticks));
    }

    void tick(ServerLevel level, Nightmare boss) {
        int now = boss.tickCount;
        clouds.removeIf(cloud -> now >= cloud.endTick());
        if (now % PUFF_TICKS == 0) {
            for (Cloud cloud : clouds) {
                UnfinishedEffects.ashCloud(level, cloud.centre(), cloud.radius());
            }
        }
        if (now % LAYER_TICKS != 0 || (clouds.isEmpty() && layers.isEmpty())) {
            return;
        }
        int max = ServerConfig.UNFINISHED.ashMaxStacks.get();
        List<Player> fighters = boss.fighters();
        for (Player player : fighters) {
            int current = layers.getOrDefault(player.getUUID(), 0);
            int next = inCloud(player) ? Math.min(max, current + 1) : current - 1;
            if (next <= 0) {
                layers.remove(player.getUUID());
                continue;
            }
            layers.put(player.getUUID(), next);
            int amplifier = next >= max ? STAND_FAST_AMPLIFIER : next - 1;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LAYER_TICKS + 10, amplifier, false, true),
                    boss);
        }
        // Players who left the fight lose their ash.
        layers.keySet().removeIf(id -> fighters.stream().noneMatch(player -> player.getUUID().equals(id)));
    }

    int layers(Player player) {
        return layers.getOrDefault(player.getUUID(), 0);
    }

    private boolean inCloud(Player player) {
        for (Cloud cloud : clouds) {
            double dx = player.getX() - cloud.centre().x;
            double dz = player.getZ() - cloud.centre().z;
            double dy = player.getY() - cloud.centre().y;
            if (dx * dx + dz * dz <= cloud.radius() * cloud.radius() && dy > -4.0 && dy < 12.0) {
                return true;
            }
        }
        return false;
    }

    private record Cloud(Vec3 centre, double radius, int endTick) {
    }
}
