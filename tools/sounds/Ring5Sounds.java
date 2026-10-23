import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Synthesises the ring 5 and Nether ring 5 sounds from our own maths (package 98; pattern of {@code Ring4Sounds.java}):
 * no vanilla audio is read or copied. The three ring 5 weather loops (glitch storm, reverse rain, standstill), the void
 * drone of the outer band, the glitch one-shots, the silent "music" of the silent biomes and the
 * heartbeat at the top of the silence meter.
 * Run from the repository root: {@code java tools/sounds/Ring5Sounds.java [path-to-ffmpeg]}; the ffmpeg path can also
 * come from the environment variable {@code FFMPEG}; {@code --wav-only} writes only the WAVs.
 *
 * <p>Loops are exactly periodic (sines on whole periods, circular filters, events wrapped around the end), streamed;
 * one-shots are silent at both ends and checked for clipping. Deterministic: every file has a fixed seed (hash of its
 * name) and uses {@link StrictMath}, so running twice gives identical WAVs in {@code build/sounds/} (not in git). ffmpeg
 * turns them into mono Ogg Vorbis under {@code src/main/resources/assets/corruptedadventure/sounds/}; only those are
 * committed. This script writes only ring 5 files.
 */
public final class Ring5Sounds {
    private static final int RATE = 44100;
    private static final Path WAV_DIR = Path.of("build/sounds");
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/sounds");

    private record Sound(String path, String variant, int index) {
        boolean loop() {
            return path.contains("_loop_");
        }
    }

    private static final List<Sound> SOUNDS = List.of(
            // Ring 5 weather (package 92; the loops were vanilla placeholders until 98).
            new Sound("weather/ring_5/glitch_storm_loop_1", "glitch_storm", 1),
            new Sound("weather/ring_5/glitch_storm_loop_2", "glitch_storm", 2),
            new Sound("weather/ring_5/reverse_rain_loop_1", "reverse_rain", 1),
            new Sound("weather/ring_5/reverse_rain_loop_2", "reverse_rain", 2),
            new Sound("weather/ring_5/standstill_loop_1", "standstill", 1),
            // Ring 5 ambience (98).
            new Sound("ambient/ring_5/void_drone_loop_1", "void_drone", 1),
            new Sound("ambient/ring_5/void_drone_loop_2", "void_drone", 2),
            new Sound("ambient/ring_5/glitch_1", "glitch", 1),
            new Sound("ambient/ring_5/glitch_2", "glitch", 2),
            new Sound("ambient/ring_5/glitch_3", "glitch", 3),
            new Sound("ambient/ring_5/glitch_4", "glitch", 4),
            // Nether ring 5 (98).
            new Sound("ambient/nether_ring_5/silence_loop_1", "silence", 1),
            new Sound("ambient/nether_ring_5/heartbeat_1", "heartbeat", 1),
            new Sound("ambient/nether_ring_5/heartbeat_2", "heartbeat", 2));

    private Ring5Sounds() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean wavOnly = args.length > 0 && args[0].equals("--wav-only");
        String ffmpeg = !wavOnly && args.length > 0 ? args[0] : System.getenv().getOrDefault("FFMPEG", "ffmpeg");
        if (!wavOnly && !ffmpegAvailable(ffmpeg)) {
            System.err.println("ffmpeg not found (" + ffmpeg + "). Install ffmpeg with libvorbis, or pass its path as an "
                    + "argument or in the FFMPEG environment variable. --wav-only writes only the WAVs.");
            System.exit(1);
        }
        for (Sound sound : SOUNDS) {
            Random random = new Random(sound.path().hashCode());
            double[] samples = switch (sound.variant()) {
                case "glitch_storm" -> glitchStorm(random, sound.index());
                case "reverse_rain" -> reverseRain(random, sound.index());
                case "standstill" -> standstill(random);
                case "void_drone" -> voidDrone(random, sound.index());
                case "glitch" -> glitch(random, sound.index());
                case "silence" -> silence(random);
                case "heartbeat" -> heartbeat(random, sound.index());
                default -> throw new IllegalStateException(sound.variant());
            };
            check(sound, samples);
            Path wav = WAV_DIR.resolve(sound.path() + ".wav");
            Files.createDirectories(wav.getParent());
            writeWav(wav, samples);
            if (!wavOnly) {
                Path ogg = OUTPUT.resolve(sound.path() + ".ogg");
                Files.createDirectories(ogg.getParent());
                int exit = new ProcessBuilder(ffmpeg, "-y", "-loglevel", "error", "-i", wav.toString(), "-c:a", "libvorbis",
                        "-ac", "1", "-q:a", "4", ogg.toString()).inheritIO().start().waitFor();
                if (exit != 0) {
                    throw new IllegalStateException("ffmpeg failed for " + sound.path());
                }
            }
            System.out.printf("%s: %.1f s%n", sound.path(), samples.length / (double) RATE);
        }
    }

    // ------------------------------------------------------------------ loops (exactly periodic)

    /**
     * Glitch storm, 10 or 12 s: crushed digital noise (held samples on a few levels) that stutters on and off in short
     * cuts, a low mains-like hum under it and sharp crackles; the cuts are a fixed pattern around the loop.
     */
    private static double[] glitchStorm(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] crushed = new double[length];
        int i = 0;
        while (i < length) {
            int hold = 3 + random.nextInt(40);
            double value = Math.round((random.nextDouble() * 2 - 1) * 4) / 4.0;
            for (int k = 0; k < hold && i < length; k++, i++) {
                crushed[i] = value;
            }
        }
        double[] gate = new double[length];
        int position = 0;
        boolean on = true;
        while (position < length) {
            int cut = (int) ((on ? 0.04 + random.nextDouble() * 0.25 : 0.02 + random.nextDouble() * 0.12) * RATE);
            for (int k = 0; k < cut && position < length; k++, position++) {
                gate[position] = on ? 1.0 : 0.08;
            }
            on = !on;
        }
        // The gate's last cut runs into its first: smooth both ends of every cut a little (2 ms) circularly.
        gate = lowpass(gate, 80);
        double[] out = new double[length];
        addSine(out, loopFrequency(55, seconds), random.nextDouble() * 2 * Math.PI, 0.18);
        addSine(out, loopFrequency(110, seconds), random.nextDouble() * 2 * Math.PI, 0.08);
        double[] body = lowpass(crushed, 5000);
        for (int s = 0; s < length; s++) {
            out[s] += 0.7 * body[s] * gate[s];
        }
        for (int crackle = 0; crackle < seconds * 6; crackle++) {
            click(out, random.nextDouble() * seconds, 1800 + random.nextDouble() * 4000, 0.4 + random.nextDouble() * 0.3,
                    0.001 + random.nextDouble() * 0.002);
        }
        return normalise(out, 0.55);
    }

    /**
     * Reverse rain, 10 or 12 s: a hiss that swells up instead of falling, and many drops played backwards (a rise that
     * stops dead), wrapped around the end of the loop.
     */
    private static double[] reverseRain(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] hiss = normalise(band(white(random, length), 600, 5000), 1.0);
        double[] low = normalise(band(white(random, length), 120, 600), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            // A slow saw-like rise per period of 2 s: the hiss swells and drops off, backwards rain.
            double phase = (t * loopFrequency(0.5, seconds)) % 1.0;
            double swell = 0.35 + 0.65 * StrictMath.pow(phase, 2);
            double soften = Math.min(1, Math.min((1 - phase) * 40, phase * 200));
            out[i] = (0.45 * hiss[i] + 0.3 * low[i]) * swell * soften;
        }
        for (int drop = 0; drop < seconds * 20; drop++) {
            reversedClick(out, random.nextDouble() * seconds, 900 + random.nextDouble() * 2200, 0.12 + random.nextDouble() * 0.15,
                    0.004 + random.nextDouble() * 0.006);
        }
        return normalise(out, 0.45);
    }

    /** Standstill, 12 s: almost nothing: a faint low tone, a thin ringing in the ears and a hush far under it. */
    private static double[] standstill(Random random) {
        double seconds = 12;
        int length = (int) (seconds * RATE);
        double[] out = normalise(lowpass(lowpass(white(random, length), 200), 200), 0.02);
        addSine(out, loopFrequency(49, seconds), random.nextDouble() * 2 * Math.PI, 0.05);
        double ring = loopFrequency(6200, seconds);
        double[] ringing = new double[length];
        addSine(ringing, ring, random.nextDouble() * 2 * Math.PI, 1.0);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            out[i] += 0.012 * ringing[i] * (0.7 + 0.3 * StrictMath.sin(2 * Math.PI * t / seconds));
        }
        return normalise(out, 0.15);
    }

    /**
     * The void drone, 12 or 14 s: very low beating tones with their overtones, and a hollow breath that is drawn in
     * (swelling, then cut off softly) twice per loop: the void sucking at the edge.
     */
    private static double[] voidDrone(Random random, int index) {
        double seconds = 10 + index * 2;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        for (double root : new double[] {30.9, 36.7, 46.2}) {
            for (int detune = -1; detune <= 1; detune += 2) {
                double base = loopFrequency(root * (1 + detune * 0.006), seconds);
                for (int h = 1; h <= 7; h++) {
                    addSine(out, loopFrequency(base * h, seconds), random.nextDouble() * 2 * Math.PI, 0.25 / h);
                }
            }
        }
        double hum = 0;
        for (double sample : out) {
            hum = Math.max(hum, Math.abs(sample));
        }
        double[] breath = normalise(band(white(random, length), 40, 320), 1.0);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double phase = (t * 2 / seconds) % 1.0;
            double draw = StrictMath.pow(StrictMath.sin(Math.PI * StrictMath.pow(phase, 1.6)), 2);
            double swell = 0.8 + 0.2 * StrictMath.sin(2 * Math.PI * t / seconds);
            out[i] = out[i] / hum * swell + 0.45 * breath[i] * draw;
        }
        return normalise(out, 0.5);
    }

    /** Silence, 20 s: a noise floor about 60 dB down, so the music system has a track that is all but empty. */
    private static double[] silence(Random random) {
        int length = 20 * RATE;
        return normalise(lowpass(lowpass(white(random, length), 300), 300), 0.001);
    }

    // ------------------------------------------------------------------ one-shots

    /**
     * A glitch, 0.2–0.6 s: a short grain of crushed noise repeated in a stutter (each repeat a little shifted), and a
     * thin digital beep cut off mid-tone.
     */
    private static double[] glitch(Random random, int index) {
        double seconds = 0.2 + index * 0.1;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        int grain = (int) ((0.015 + random.nextDouble() * 0.025) * RATE);
        double[] source = new double[grain];
        int hold = 2 + random.nextInt(12);
        double value = 0;
        for (int i = 0; i < grain; i++) {
            if (i % hold == 0) {
                value = Math.round((random.nextDouble() * 2 - 1) * 3) / 3.0;
            }
            source[i] = value;
        }
        int position = (int) (0.01 * RATE);
        while (position + grain < length * 0.8) {
            double gain = 0.5 + random.nextDouble() * 0.5;
            int step = 1 + random.nextInt(2);
            for (int i = 0; i < grain / step && position + i < length; i++) {
                out[position + i] += gain * source[(i * step) % grain];
            }
            position += grain / step + (random.nextInt(3) == 0 ? (int) (0.02 * RATE) : 0);
        }
        double beep = 800 + random.nextDouble() * 1600;
        int beepStart = (int) (random.nextDouble() * length * 0.5);
        int beepLength = (int) ((0.03 + random.nextDouble() * 0.05) * RATE);
        for (int i = 0; i < beepLength && beepStart + i < length; i++) {
            out[beepStart + i] += 0.35 * Math.signum(StrictMath.sin(2 * Math.PI * beep * i / RATE));
        }
        out = lowpass(out, 7000);
        fadeEnds(out, 0.005);
        return normalise(out, 0.7);
    }

    /** A heartbeat, 0.9 s: "lub-dub", two low, muffled thumps, the second softer. */
    private static double[] heartbeat(Random random, int index) {
        int length = (int) (0.9 * RATE);
        double[] out = new double[length];
        double pitch = 48 + index * 4;
        thump(out, random, 0.03, pitch, 1.0);
        thump(out, random, 0.31, pitch * 1.15, 0.65);
        out = lowpass(out, 400);
        fadeEnds(out, 0.01);
        return normalise(out, 0.8);
    }

    // ------------------------------------------------------------------ building blocks

    private static double[] white(Random random, int length) {
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            out[i] = random.nextDouble() * 2 - 1;
        }
        return out;
    }

    /**
     * A one-pole low-pass, circular: run over the buffer once to settle, then again to keep. The second pass starts
     * from the state the first left at the end, so the result is one period of an endless loop.
     */
    private static double[] lowpass(double[] in, double cutoff) {
        double alpha = 1 - StrictMath.exp(-2 * Math.PI * cutoff / RATE);
        double state = 0;
        for (double sample : in) {
            state += alpha * (sample - state);
        }
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++) {
            state += alpha * (in[i] - state);
            out[i] = state;
        }
        return out;
    }

    /** A circular band-pass: two low-passes at {@code high} minus two at {@code low}. */
    private static double[] band(double[] in, double low, double high) {
        double[] upper = lowpass(lowpass(in, high), high);
        double[] lower = lowpass(lowpass(in, low), low);
        for (int i = 0; i < upper.length; i++) {
            upper[i] -= lower[i];
        }
        return upper;
    }

    /** {@code frequency} rounded to a whole number of periods in {@code seconds}, so a loop of that length is seamless. */
    private static double loopFrequency(double frequency, double seconds) {
        return Math.max(1, Math.round(frequency * seconds)) / seconds;
    }

    private static void addSine(double[] out, double frequency, double phase, double amp) {
        // Exact phase per sample (not accumulated): the last sample meets the first.
        for (int i = 0; i < out.length; i++) {
            out[i] += amp * StrictMath.sin(2 * Math.PI * frequency * i / RATE + phase);
        }
    }

    /** A short decaying tone burst, wrapped around the end of {@code out}. */
    private static void click(double[] out, double start, double frequency, double amp, double decay) {
        int first = (int) (start * RATE);
        int count = (int) (decay * 6 * RATE);
        for (int i = 0; i < count; i++) {
            double t = i / (double) RATE;
            double envelope = (1 - StrictMath.exp(-t * 4000)) * StrictMath.exp(-t / decay);
            out[Math.floorMod(first + i, out.length)] += amp * envelope * StrictMath.sin(2 * Math.PI * frequency * t);
        }
    }

    /** A click played backwards: it grows out of nothing and stops dead (with a 1 ms fade); wrapped around the end. */
    private static void reversedClick(double[] out, double start, double frequency, double amp, double decay) {
        int first = (int) (start * RATE);
        int count = (int) (decay * 6 * RATE);
        int fade = Math.max(1, RATE / 1000);
        for (int i = 0; i < count; i++) {
            double t = (count - i) / (double) RATE;
            double envelope = StrictMath.exp(-t / decay) * Math.min(1.0, (count - i) / (double) fade);
            out[Math.floorMod(first + i, out.length)] += amp * envelope * StrictMath.sin(2 * Math.PI * frequency * t);
        }
    }

    /** One low, damped thump of a heart: a falling tone with a little noise. */
    private static void thump(double[] out, Random random, double start, double frequency, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (0.22 * RATE);
        double phase = 0;
        for (int i = 0; i < count && first + i < out.length; i++) {
            double t = i / (double) RATE;
            phase += 2 * Math.PI * frequency * (1.3 - 0.3 * Math.min(1, t / 0.1)) / RATE;
            double envelope = (1 - StrictMath.exp(-t * 300)) * StrictMath.exp(-t / 0.05);
            out[first + i] += amp * envelope * (StrictMath.sin(phase) + 0.15 * (random.nextDouble() * 2 - 1));
        }
    }

    /** Raised-sine fade over {@code seconds} at both ends (one-shots only). */
    private static void fadeEnds(double[] out, double seconds) {
        int count = Math.min(out.length / 2, (int) (seconds * RATE));
        for (int i = 0; i < count; i++) {
            double gain = StrictMath.sin(0.5 * Math.PI * i / count);
            out[i] *= gain * gain;
            out[out.length - 1 - i] *= gain * gain;
        }
    }

    private static double[] normalise(double[] samples, double peak) {
        double max = 0;
        for (double sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        if (max == 0) {
            return samples;
        }
        for (int i = 0; i < samples.length; i++) {
            samples[i] = samples[i] / max * peak;
        }
        return samples;
    }

    /** No clip; one-shots silent at both ends; loops meet themselves (the jump end → start is a normal step). */
    private static void check(Sound sound, double[] samples) {
        for (double sample : samples) {
            if (Math.abs(sample) > 1.0) {
                throw new IllegalStateException(sound.path() + " clips");
            }
        }
        if (sound.loop()) {
            double wrap = Math.abs(samples[0] - samples[samples.length - 1]);
            double largest = 0;
            for (int i = 1; i < samples.length; i++) {
                largest = Math.max(largest, Math.abs(samples[i] - samples[i - 1]));
            }
            if (wrap > largest) {
                throw new IllegalStateException(sound.path() + " clicks at the loop point");
            }
        } else if (Math.abs(samples[0]) > 0.01 || Math.abs(samples[samples.length - 1]) > 0.01) {
            throw new IllegalStateException(sound.path() + " starts or ends with a click");
        }
    }

    private static void writeWav(Path path, double[] samples) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (double sample : samples) {
            buffer.putShort((short) Math.round(sample * 32767));
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(buffer.array()), format, samples.length)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, path.toFile());
        }
    }

    private static boolean ffmpegAvailable(String ffmpeg) {
        try {
            return new ProcessBuilder(ffmpeg, "-version").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
