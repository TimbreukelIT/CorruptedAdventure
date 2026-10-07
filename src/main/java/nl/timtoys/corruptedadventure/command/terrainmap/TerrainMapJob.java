package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs one terrain map at a time off the server thread: a coordinator thread with a few low-priority workers that
 * sample units in any order, progress in the chat per tenth, then the PNG's in
 * {@code <world>/corruptedadventure/maps/}. Worldgen code only (no chunks, no level access), so the server keeps
 * ticking; the server thread is only used to send messages. A server that stops cancels the run.
 */
public final class TerrainMapJob {
    /** Relative to the world folder. */
    public static final String DIRECTORY = "corruptedadventure/maps";
    /** Progress at most every tenth, and not before this many seconds: a short run only reports when it is done. */
    private static final long PROGRESS_SECONDS = 5L;
    private static final int PROGRESS_STEPS = 10;
    private static final long STOP_WAIT_MILLIS = 10_000L;
    private static final AtomicReference<TerrainMapJob> RUNNING = new AtomicReference<>();

    private final MinecraftServer server;
    private final CommandSourceStack source;
    private final TerrainSource terrain;
    private final MapWork work;
    private final int threads;
    private final AtomicInteger nextUnit = new AtomicInteger();
    private final AtomicInteger unitsDone = new AtomicInteger();
    private final AtomicLong busyNanos = new AtomicLong();
    private volatile boolean cancelled;
    /** A worker failed: the others stop at their next unit instead of finishing the map. */
    private volatile boolean failed;
    private Thread coordinator;

    private TerrainMapJob(CommandSourceStack source, TerrainSource terrain, MapWork work) {
        this.server = source.getServer();
        this.source = source;
        this.terrain = terrain;
        this.work = work;
        this.threads = workerThreads(Runtime.getRuntime().availableProcessors());
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ServerStoppingEvent.class, event -> cancelRunning(true));
    }

    /** A quarter of the cores, at most 4: the worldgen workers and the server thread keep the rest. */
    static int workerThreads(int cores) {
        return Math.clamp(cores / 4, 1, 4);
    }

    /** False if another map is still running. */
    static boolean start(CommandSourceStack source, TerrainSource terrain, MapWork work, String description) {
        TerrainMapJob job = new TerrainMapJob(source, terrain, work);
        if (!RUNNING.compareAndSet(null, job)) {
            return false;
        }
        job.message(description + " on " + job.threads + (job.threads == 1 ? " thread" : " threads"), true);
        job.coordinator = new Thread(job::run, "ca-terrainmap");
        job.coordinator.setDaemon(true);
        job.coordinator.setPriority(Thread.MIN_PRIORITY);
        job.coordinator.start();
        return true;
    }

    /** True if a map was running. {@code wait}: block until its threads have stopped (server shutdown). */
    static boolean cancelRunning(boolean wait) {
        TerrainMapJob job = RUNNING.get();
        if (job == null) {
            return false;
        }
        job.cancelled = true;
        if (wait && job.coordinator != null) {
            try {
                job.coordinator.join(STOP_WAIT_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            // A job that did not stop in time must not block the next server (single player) or keep this one alive.
            RUNNING.compareAndSet(job, null);
        }
        return true;
    }

    private void run() {
        long start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "ca-terrainmap-worker");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        try {
            List<Future<?>> workers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                workers.add(pool.submit(this::sampleUnits));
            }
            pool.shutdown();
            int reported = 0;
            while (!pool.awaitTermination(1L, TimeUnit.SECONDS)) {
                int step = unitsDone.get() * PROGRESS_STEPS / work.units();
                if (!cancelled && !failed && step > reported && seconds(start) >= PROGRESS_SECONDS) {
                    reported = step;
                    message(progress(start), false);
                }
            }
            for (Future<?> worker : workers) {
                worker.get();
            }
            List<String> files = cancelled ? List.of() : write(work.render());
            if (cancelled) {
                message("Terrain map cancelled" + (files.isEmpty() ? "" : "; already written: " + String.join(", ", files)), true);
                return;
            }
            message(String.format(Locale.ROOT, "Terrain map done in %.1f s (%s): %s",
                    seconds(start), perColumn(), String.join(", ", files)), true);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Throwable failure) {
            // Also errors (out of memory on a large map, a JRE without AWT): the op must hear that it failed.
            Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
            CorruptedAdventure.LOGGER.error("Terrain map failed", cause);
            if (!cancelled) {
                failed("Terrain map failed: " + cause);
            }
        } finally {
            pool.shutdownNow();
            RUNNING.compareAndSet(this, null);
        }
    }

    private void sampleUnits() {
        try {
            TerrainSampler sampler = terrain.newSampler();
            for (int unit = nextUnit.getAndIncrement(); unit < work.units() && !cancelled && !failed;
                 unit = nextUnit.getAndIncrement()) {
                long begin = System.nanoTime();
                work.sample(sampler, unit);
                busyNanos.addAndGet(System.nanoTime() - begin);
                unitsDone.incrementAndGet();
            }
        } catch (RuntimeException | Error failure) {
            failed = true;
            throw failure;
        }
    }

    private List<String> write(Map<String, BufferedImage> images) throws IOException {
        Path directory = server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY);
        Files.createDirectories(directory);
        List<String> written = new ArrayList<>();
        for (Map.Entry<String, BufferedImage> image : images.entrySet()) {
            if (cancelled) {
                break;
            }
            Path file = directory.resolve(image.getKey());
            Path temporary = directory.resolve(image.getKey() + ".tmp");
            if (!ImageIO.write(image.getValue(), "png", temporary.toFile())) {
                throw new IOException("No PNG writer available");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException notAtomic) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            CorruptedAdventure.LOGGER.info("Terrain map written: {}", file.toAbsolutePath());
            written.add(DIRECTORY + "/" + image.getKey());
        }
        return written;
    }

    private String progress(long start) {
        double done = (double) unitsDone.get() / work.units();
        return String.format(Locale.ROOT, "Terrain map %.0f%% (%.0f s, %s)", 100.0 * done, seconds(start), perColumn());
    }

    /** Busy time of the workers per column, so it does not depend on the number of threads. */
    private String perColumn() {
        long columns = (long) unitsDone.get() * work.columnsPerUnit();
        if (columns == 0) {
            return "no columns yet";
        }
        return String.format(Locale.ROOT, "%.0f us per column per thread", busyNanos.get() / 1000.0 / columns);
    }

    private static double seconds(long start) {
        return (System.nanoTime() - start) / 1.0e9;
    }

    /** Every message also goes to the log: RCON only sees the reply of the command itself. */
    private void message(String text, boolean broadcast) {
        CorruptedAdventure.LOGGER.info(text);
        run(() -> source.sendSuccess(() -> Component.literal(text), broadcast));
    }

    private void failed(String text) {
        run(() -> source.sendFailure(Component.literal(text)));
    }

    /** Directly from the command (so RCON gets the start line in its reply), else on the server thread. */
    private void run(Runnable send) {
        if (server.isSameThread()) {
            send.run();
        } else {
            server.execute(send);
        }
    }
}
