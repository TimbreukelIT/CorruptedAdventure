package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * What every condition rule may look at, built once per player per tick and shared by all conditions. Deliberately
 * minimal: derived checks (sun, sky, water) are added by the first condition that needs them, so their meaning is
 * decided there; the ring weather is read by its own {@link RateModifier}, not through this context.
 *
 * @param position the ring at the player's feet ({@link RingLayout#ringAt})
 * @param biome    the biome at the player's feet
 */
public record ConditionContext(ServerPlayer player, ServerLevel level, RingPosition position, Holder<Biome> biome) {
    public static ConditionContext of(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return new ConditionContext(player, level,
                RingLayout.ringAt(level.dimension(), player.getX(), player.getZ()),
                level.getBiome(player.blockPosition()));
    }
}
