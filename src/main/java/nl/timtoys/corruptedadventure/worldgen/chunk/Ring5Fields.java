package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ring 5 terrain ({@link Ring5Field}) of each loaded dimension of the running server, for readers without a random
 * state: {@code /ca castles}, and later the biome hooks, conditions and structures that need the castle sites (they exist
 * on the server only). Found through the level's router ({@link RingChunkGenerator#ring5Field}) when the level loads,
 * before its spawn chunks generate; cleared when the server stops. Like {@link IslandFields} for ring 4.
 */
public final class Ring5Fields {
    // Written on the server thread, read by worldgen worker threads.
    private static final Map<ResourceKey<Level>, Ring5Field> FIELDS = new ConcurrentHashMap<>();

    private Ring5Fields() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring5Fields::onLevelLoad);
        gameBus.addListener(Ring5Fields::onServerStopped);
    }

    /** The ring 5 terrain of {@code dimension} on the running server, or null (none, or no server). */
    @Nullable
    public static Ring5Field current(ResourceKey<Level> dimension) {
        return FIELDS.get(dimension);
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Ring5Field field = level.getChunkSource().getGenerator() instanceof RingChunkGenerator generator
                ? generator.ring5Field(level.getChunkSource().randomState())
                : null;
        if (field == null) {
            FIELDS.remove(level.dimension());
        } else {
            FIELDS.put(level.dimension(), field);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        FIELDS.clear();
    }
}
