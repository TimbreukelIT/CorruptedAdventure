package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Striders without jockeys in the overworld (decision 2026-10-09, worldgen/mobs; G2-WB11): ring 3 spawns striders on its
 * lava ({@link Ring3Biomes#spawns}), but vanilla's strider sometimes spawns with a zombified piglin or a baby strider as
 * its rider. In the overworld a natural or worldgen strider skips its own spawn setup (where that rider comes from), so
 * only bare striders spawn there; spawn eggs, spawners and the Nether stay vanilla. Server-side (the event fires on the
 * logical server only). To be revisited with the ring 3 mobs in phase 4.
 */
public final class Ring3StriderSpawns {
    private Ring3StriderSpawns() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring3StriderSpawns::onFinalizeSpawn);
    }

    private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.STRIDER && natural(event.getSpawnType())
                && event.getLevel().getLevel().dimension() == Level.OVERWORLD) {
            // Cancelling skips Strider.finalizeSpawn and its jockey; the strider itself still spawns.
            event.setCanceled(true);
        }
    }

    private static boolean natural(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION;
    }
}
