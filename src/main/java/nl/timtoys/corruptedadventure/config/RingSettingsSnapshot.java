package nl.timtoys.corruptedadventure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Freezes the ring settings per world, so a later config change never moves boundaries through generated terrain.
 * <p>
 * This is a plain file instead of {@code SavedData}: SavedData only exists once the levels are loaded, but the
 * snapshot must be known before the first (spawn) chunk is generated. {@link ServerAboutToStartEvent} fires
 * right before {@code loadLevel()} on both the dedicated and the integrated server.
 * <p>
 * Server-side only; clients receive these settings with the ring layout on login ({@code RingLayoutPayload}).
 */
public final class RingSettingsSnapshot {
    private static final String FILE = CorruptedAdventure.MOD_ID + "/ring_settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Written on the server thread, read by worldgen worker threads.
    private static volatile RingSettings current;

    private RingSettingsSnapshot() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RingSettingsSnapshot::onServerAboutToStart);
        gameBus.addListener(RingSettingsSnapshot::onServerStopped);
    }

    /** Ring settings of the running server's world. */
    public static RingSettings current() {
        RingSettings settings = current;
        if (settings == null) {
            throw new IllegalStateException("Ring settings are only available while a server is running");
        }
        return settings;
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path file = event.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE);
        current = Files.exists(file) ? read(file) : create(file);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        current = null;
    }

    private static RingSettings read(Path file) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            RingSettings settings = RingSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(
                    error -> new IllegalStateException("Invalid ring settings snapshot " + file + ": " + error));
            CorruptedAdventure.LOGGER.info("Loaded ring settings snapshot: {}", settings);
            return settings;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read ring settings snapshot " + file, e);
        }
    }

    private static RingSettings create(Path file) {
        RingSettings settings = CommonConfig.ringSettings().validate().getOrThrow(
                error -> new IllegalStateException("Invalid ring settings in corruptedadventure-common.toml: " + error));
        JsonElement json = RingSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        // Write-then-move, so a crash on the first start can never leave a truncated snapshot behind.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(json));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write ring settings snapshot " + file, e);
        }
        CorruptedAdventure.LOGGER.info("Created ring settings snapshot from config: {}", settings);
        return settings;
    }
}
