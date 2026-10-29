package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The burning ground The Risen leaves (the fire trail of its swoop, the embers of its ember rain): not fire blocks, but
 * tiles of its own that burn for {@code fireSeconds} and set whoever stands in them alight. So nothing ever burns down
 * or spreads, the world stays as it was. A tile only lies on free ground (air on a sturdy top face, not in a fluid), at
 * most {@code fireMaxTiles} at once (the oldest go out first). Server-side and not saved: unloaded, the fire is out.
 */
public final class RisenFire {
    /** Ticks between two damage checks; the particles run on their own beat per tile. */
    private static final int BURN_INTERVAL = 10;
    private static final int PARTICLE_INTERVAL = 6;

    /** Tile (the air block above the ground) → game time it goes out. */
    private final Map<BlockPos, Long> tiles = new LinkedHashMap<>();

    /** The free tile at the surface of column (x, z), if that surface lies within {@code reach} of {@code aroundY}. */
    @Nullable
    public static BlockPos surfaceTile(ServerLevel level, double x, double z, double aroundY, double reach) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
        if (Math.abs(top.getY() - aroundY) > reach) {
            return null;
        }
        return isFree(level, top) ? top : null;
    }

    /** Air (no fluid) on a sturdy top face: there the fire may lie. */
    public static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Lights the tile at {@code pos} if it is free and fire is on; true if it burns (again) now. */
    public boolean light(ServerLevel level, BlockPos pos, RisenConfig config) {
        if (!config.fire.get() || config.fireMaxTiles.get() <= 0 || !isFree(level, pos)) {
            return false;
        }
        BlockPos tile = pos.immutable();
        tiles.remove(tile);
        while (tiles.size() >= config.fireMaxTiles.get() && !tiles.isEmpty()) {
            Iterator<BlockPos> oldest = tiles.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        tiles.put(tile, level.getGameTime() + config.fireSeconds.get() * 20L);
        RisenEffects.fireCatches(level, tile);
        return true;
    }

    public boolean isBurning(BlockPos pos) {
        return tiles.containsKey(pos);
    }

    public int size() {
        return tiles.size();
    }

    public void clear() {
        tiles.clear();
    }

    /** Every tick: tiles go out, flicker, and burn whoever stands in them (not {@code owner}, nor what cannot burn). */
    public void tick(ServerLevel level, @Nullable Entity owner, float strength, RisenConfig config) {
        if (tiles.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Iterator<Map.Entry<BlockPos, Long>> it = tiles.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> entry = it.next();
            BlockPos tile = entry.getKey();
            // Gone out, or someone put a block on it, or water ran over it.
            if (now >= entry.getValue() || !level.isLoaded(tile) || !isFree(level, tile)) {
                it.remove();
                continue;
            }
            if (Math.floorMod(tile.hashCode() + now, PARTICLE_INTERVAL) == 0) {
                RisenEffects.fireFlicker(level, tile, entry.getValue() - now < 30);
            }
            minX = Math.min(minX, tile.getX());
            minY = Math.min(minY, tile.getY());
            minZ = Math.min(minZ, tile.getZ());
            maxX = Math.max(maxX, tile.getX());
            maxY = Math.max(maxY, tile.getY());
            maxZ = Math.max(maxZ, tile.getZ());
        }
        if (tiles.isEmpty() || now % BURN_INTERVAL != 0) {
            return;
        }
        AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(0.5);
        List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && e.isAlive() && !e.fireImmune() && !e.isSpectator());
        for (LivingEntity entity : inside) {
            if (standsIn(entity)) {
                entity.igniteForSeconds(config.fireIgniteSeconds.get());
                entity.hurt(level.damageSources().inFire(), config.fireDamage.get().floatValue() * strength);
            }
        }
    }

    /** A burning tile under any part of its feet. */
    private boolean standsIn(LivingEntity entity) {
        AABB feet = entity.getBoundingBox();
        int y = BlockPos.containing(entity.getX(), feet.minY + 0.01, entity.getZ()).getY();
        for (int x = (int) Math.floor(feet.minX); x <= (int) Math.floor(feet.maxX - 1.0E-4); x++) {
            for (int z = (int) Math.floor(feet.minZ); z <= (int) Math.floor(feet.maxZ - 1.0E-4); z++) {
                if (tiles.containsKey(new BlockPos(x, y, z))) {
                    return true;
                }
            }
        }
        return false;
    }
}
