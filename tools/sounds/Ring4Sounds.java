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
 * Synthesises the ring 4 sounds from our own maths: no vanilla audio is read or copied (pattern of
 * {@code Ring3Sounds.java}). For now the ambience of {@code verdant_falls} (Q8 = A); change 80 adds the rest of ring 4.
 * Run from the repository root: {@code java tools/sounds/Ring4Sounds.java [path-to-ffmpeg]}; the ffmpeg path can also
 * come from the environment variable {@code FFMPEG}.
 *
 * <p>Two kinds of sound:
 * <ul>
 *   <li><b>loops</b> (the roar of distant waterfalls): noise filtered <i>circularly</i> (every filter runs over the
 *       buffer twice and keeps the second pass, so its state at the start is the one at the end) and modulated with
 *       whole periods of the loop, so the end runs straight into the start without a click; streamed;</li>
 *   <li><b>one-shots</b> (tropical birds and frogs): silent at both ends and checked for clipping.</li>
 * </ul>
 *
 * <p>Deterministic: every file has a fixed seed (hash of its name) and uses {@link StrictMath}, so running twice gives
 * byte-identical WAVs in {@code build/sounds/} (not in git). ffmpeg turns them into mono Ogg Vorbis under
 * {@code src/main/resources/assets/corruptedadventure/sounds/}; only those are committed. The {@code .ogg} bytes may
 * differ per ffmpeg version: the WAV is the source. This script writes only ring 4 files.
 */
public final class Ring4Sounds {
    private static final int RATE = 44100;
    private static final Path WAV_DIR = Path.of("build/sounds");
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/sounds");

    private record Sound(String path, String variant, int index) {
        boolean loop() {
            return path.contains("_loop_");
        }
    }

    private static final List<Sound> SOUNDS = List.of(
            // verdant_falls: the biome loop and its additions.
            new Sound("ambient/ring_4/falls_loop_1", "falls", 1),
            new Sound("ambient/ring_4/falls_loop_2", "falls", 2),
            new Sound("ambient/ring_4/tropical_bird_1", "bird", 1),
            new Sound("ambient/ring_4/tropical_bird_2", "bird", 2),
            new Sound("ambient/ring_4/tropical_bird_3", "bird", 3),
            new Sound("ambient/ring_4/frog_1", "frog", 1),
            new Sound("ambient/ring_4/frog_2", "frog", 2));

    private Ring4Sounds() {
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
                case "falls" -> falls(random, sound.index());
                case "bird" -> bird(random, sound.index());
                case "frog" -> frog(random, sound.index());
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
     * Distant waterfalls, 10 or 12 s: a deep, steady roar (band 50-700 Hz) over a low rumble, a faint far-off hiss of
     * spray, and slow irregular surges as the water's weight shifts. Distance takes the highs away: mostly roar.
     */
    private static double[] falls(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] roar = normalise(band(white(random, length), 50, 700), 1.0);
        double[] rumble = normalise(lowpass(lowpass(white(random, length), 90), 90), 1.0);
        double[] spray = normalise(band(white(random, length), 1500, 5000), 1.0);
        // Surges: very slow noise (below 1.5 Hz), itself circular.
        double[] surge = normalise(lowpass(lowpass(white(random, length), 1.5), 1.5), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double swell = 0.8 + 0.1 * StrictMath.sin(2 * Math.PI * 2 * t / seconds + index)
                    + 0.1 * StrictMath.sin(2 * Math.PI * 3 * t / seconds + 0.6);
            double gain = swell * (0.85 + 0.15 * surge[i]);
            out[i] = (roar[i] + 0.6 * rumble[i] + 0.1 * spray[i]) * gain;
        }
        // A steady roar is denser than a crackle: this peak gives about the mean level of the ring 3 ember loop.
        return normalise(out, 0.35);
    }

    // ------------------------------------------------------------------ one-shots

    /**
     * A tropical bird, 1.1-1.6 s, three kinds: (1) three rising-and-falling whoops, (2) a fast high trill ending in a
     * long falling note, (3) a low-high "tu-wee" twice. Clear whistles with a weak second harmonic and a faint echo
     * off the cliffs.
     */
    private static double[] bird(Random random, int index) {
        double seconds = switch (index) {
            case 1 -> 1.3;
            case 2 -> 1.1;
            default -> 1.6;
        };
        double[] out = new double[(int) (seconds * RATE)];
        double pitch = 0.92 + random.nextDouble() * 0.16;
        switch (index) {
            case 1 -> {
                for (int note = 0; note < 3; note++) {
                    double start = 0.04 + note * 0.26;
                    chirp(out, start, 0.18, 1400 * pitch, 2600 * pitch, 1800 * pitch, 0.5 - note * 0.08);
                }
            }
            case 2 -> {
                for (int pulse = 0; pulse < 10; pulse++) {
                    double f = (3900 - pulse * 40) * pitch;
                    chirp(out, 0.04 + pulse * 0.055, 0.035, f, f * 1.05, f * 0.9, 0.35);
                }
                chirp(out, 0.62, 0.28, 3400 * pitch, 2900 * pitch, 2100 * pitch, 0.45);
            }
            default -> {
                for (int call = 0; call < 2; call++) {
                    double start = 0.05 + call * 0.62;
                    chirp(out, start, 0.12, 1250 * pitch, 1200 * pitch, 1150 * pitch, 0.4);
                    chirp(out, start + 0.17, 0.18, 2000 * pitch, 2900 * pitch, 3200 * pitch, 0.5);
                }
            }
        }
        echo(out, 0.11, 0.22);
        fadeEnds(out, 0.01);
        return normalise(out, 0.55);
    }

    /**
     * A frog, 0.9-1.3 s, two kinds: (1) a low, buzzy "ribbit" (two pulsed bursts) and a softer second one; (2) a tree
     * frog's "ko-kee" twice (a short flat note and a rising one), the tropical night call.
     */
    private static double[] frog(Random random, int index) {
        double seconds = index == 1 ? 0.9 : 1.3;
        double[] out = new double[(int) (seconds * RATE)];
        double pitch = 0.93 + random.nextDouble() * 0.14;
        if (index == 1) {
            for (int call = 0; call < 2; call++) {
                double start = 0.04 + call * 0.42;
                double amp = call == 0 ? 0.6 : 0.4;
                croak(out, start, 0.09, 360 * pitch, 55, amp);
                croak(out, start + 0.15, 0.13, 330 * pitch, 50, amp);
            }
        } else {
            for (int call = 0; call < 2; call++) {
                double start = 0.05 + call * 0.6;
                chirp(out, start, 0.1, 1150 * pitch, 1150 * pitch, 1120 * pitch, 0.45);
                chirp(out, start + 0.15, 0.2, 1900 * pitch, 2250 * pitch, 2450 * pitch, 0.5);
            }
        }
        echo(out, 0.09, 0.15);
        fadeEnds(out, 0.01);
        return normalise(out, 0.55);
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

    /** A circular band-pass: two low-passes at {@code high} minus two at {@code low} (12 dB per octave each side). */
    private static double[] band(double[] in, double low, double high) {
        double[] upper = lowpass(lowpass(in, high), high);
        double[] lower = lowpass(lowpass(in, low), low);
        for (int i = 0; i < upper.length; i++) {
            upper[i] -= lower[i];
        }
        return upper;
    }

    /**
     * A whistled note gliding from {@code start} Hz over {@code middle} to {@code end} (a parabola through the three),
     * with a quick attack, a smooth fall and a weak second harmonic.
     */
    private static void chirp(double[] out, double startTime, double duration, double start, double middle, double end, double amp) {
        int first = (int) (startTime * RATE);
        int count = (int) (duration * RATE);
        double phase = 0;
        for (int i = 0; i < count && first + i < out.length; i++) {
            double x = i / (double) count;
            // Lagrange through (0, start), (0.5, middle), (1, end).
            double frequency = start * 2 * (x - 0.5) * (x - 1) - middle * 4 * x * (x - 1) + end * 2 * x * (x - 0.5);
            phase += 2 * Math.PI * frequency / RATE;
            double envelope = StrictMath.sin(Math.PI * StrictMath.pow(x, 0.6));
            envelope *= envelope;
            out[first + i] += amp * envelope * (StrictMath.sin(phase) + 0.2 * StrictMath.sin(2 * phase + 0.4));
        }
    }

    /** A buzzy croak: a tone with two overtones, chopped by a fast pulse train ({@code pulses} per second). */
    private static void croak(double[] out, double startTime, double duration, double frequency, double pulses, double amp) {
        int first = (int) (startTime * RATE);
        int count = (int) (duration * RATE);
        for (int i = 0; i < count && first + i < out.length; i++) {
            double t = i / (double) RATE;
            double x = i / (double) count;
            double envelope = StrictMath.sin(Math.PI * x);
            double pulse = StrictMath.pow(Math.abs(StrictMath.sin(Math.PI * pulses * t)), 4);
            double tone = StrictMath.sin(2 * Math.PI * frequency * t) + 0.5 * StrictMath.sin(4 * Math.PI * frequency * t + 0.3)
                    + 0.3 * StrictMath.sin(6 * Math.PI * frequency * t + 0.9);
            out[first + i] += amp * envelope * pulse * tone;
        }
    }

    /** One soft reflection {@code delay} seconds later (cliffs and water around the falls); nothing past the end. */
    private static void echo(double[] out, double delay, double gain) {
        int offset = (int) (delay * RATE);
        for (int i = out.length - 1; i >= offset; i--) {
            out[i] += gain * out[i - offset];
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
