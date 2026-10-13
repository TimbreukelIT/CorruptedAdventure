package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ring 4 islands ({@link IslandField}) of each loaded dimension of the running server, for readers without a
 * random state: a biome source only gets a climate sampler, yet its height hook ({@code RingBiomeSource}) must see the
 * same islands as the terrain. Found through the level's router ({@link RingChunkGenerator#islandField}) when the level
 * loads, which is before its spawn chunks generate; like {@code RingLayout.current()} for the ring layout.
 */
public final class IslandFields {
    // Written on the server thread, read by worldgen worker threads.
    private static final Map<ResourceKey<Level>, IslandField> FIELDS = new ConcurrentHashMap<>();

    private IslandFields() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(IslandFields::onLevelLoad);
        gameBus.addListener(IslandFields::onServerStopped);
    }

    /** The islands of {@code dimension} on the running server, or null (none, or no server). */
    @Nullable
    public static IslandField current(ResourceKey<Level> dimension) {
        return FIELDS.get(dimension);
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        IslandField field = level.getChunkSource().getGenerator() instanceof RingChunkGenerator generator
                ? generator.islandField(level.getChunkSource().randomState())
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
