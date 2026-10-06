package nl.timtoys.corruptedadventure.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Test access to the hand-written worldgen JSON under {@code src/main/resources/data/corruptedadventure/worldgen}. */
public final class WorldgenData {
    private static final String RELATIVE = "src/main/resources/data/corruptedadventure/worldgen";

    private WorldgenData() {
    }

    /** The worldgen folder, found upwards from the working directory (Gradle runs tests in the project folder). */
    public static Path root() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(RELATIVE))) {
                return dir.resolve(RELATIVE);
            }
        }
        throw new IllegalStateException("Could not find " + RELATIVE);
    }

    /** A JSON object, {@code relative} to {@link #root()} (e.g. {@code "noise/ring_3_heat.json"}). */
    public static JsonObject read(String relative) {
        return read(root().resolve(relative));
    }

    public static JsonObject read(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The parameters of {@code noise/<name>.json}. */
    public static NormalNoise.NoiseParameters noiseParameters(String name) {
        JsonObject json = read("noise/" + name + ".json");
        JsonArray amplitudes = json.getAsJsonArray("amplitudes");
        double[] rest = new double[amplitudes.size() - 1];
        for (int i = 1; i < amplitudes.size(); i++) {
            rest[i - 1] = amplitudes.get(i).getAsDouble();
        }
        return new NormalNoise.NoiseParameters(json.get("firstOctave").getAsInt(), amplitudes.get(0).getAsDouble(), rest);
    }
}
