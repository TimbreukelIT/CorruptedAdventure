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
 * Synthesises the ring 2 sounds from our own maths: no vanilla audio is read or copied. The one sound script for
 * ring 2; later ring 2 changes (change 36: a {@code loop} variant for the Nether) extend this file. Run from the
 * repository root: {@code java tools/sounds/Ring2Sounds.java [path-to-ffmpeg]}. The ffmpeg path can also come from the
 * environment variable {@code FFMPEG}; the default is {@code ffmpeg} on the PATH.
 *
 * <p>Variants:
 * <ul>
 *   <li>{@code drone}: a low, slowly swelling hum with a few overtones, a slow beat and a hint of filtered noise
 *       (singing sand, change 32);</li>
 *   <li>{@code ambient}: short birds and insects (oasis, change 32).</li>
 * </ul>
 *
 * <p>Deterministic: every file has a fixed seed (hash of its name) and uses {@link StrictMath}, so running twice gives
 * byte-identical WAVs in {@code build/sounds/} (not in git). ffmpeg turns them into mono Ogg Vorbis in
 * {@code src/main/resources/assets/corruptedadventure/sounds/}; those {@code .ogg}s are committed, so the build needs
 * neither this script nor ffmpeg. The {@code .ogg} may differ per ffmpeg version: the WAV is the source.
 */
public final class Ring2Sounds {
    private static final int RATE = 44100;
    private static final Path WAV_DIR = Path.of("build/sounds");
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/sounds");

    private record Sound(String path, String variant, int index) {
    }

    private static final List<Sound> SOUNDS = List.of(
            new Sound("ambient/ring_2/singing_sand_1", "drone", 1),
            new Sound("ambient/ring_2/singing_sand_2", "drone", 2),
            new Sound("ambient/ring_2/singing_sand_3", "drone", 3),
            new Sound("ambient/ring_2/oasis_1", "ambient", 1),
            new Sound("ambient/ring_2/oasis_2", "ambient", 2),
            new Sound("ambient/ring_2/oasis_3", "ambient", 3));

    private Ring2Sounds() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String ffmpeg = args.length > 0 ? args[0] : System.getenv().getOrDefault("FFMPEG", "ffmpeg");
        if (!ffmpegAvailable(ffmpeg)) {
            System.err.println("ffmpeg not found (" + ffmpeg + "). Install ffmpeg with libvorbis, or pass its path as an "
                    + "argument or in the FFMPEG environment variable.");
            System.exit(1);
        }
        for (Sound sound : SOUNDS) {
            Random random = new Random(sound.path().hashCode());
            double[] samples = switch (sound.variant()) {
                case "drone" -> drone(random, sound.index());
                case "ambient" -> ambient(random, sound.index());
                default -> throw new IllegalStateException(sound.variant());
            };
            check(sound, samples);
            Path wav = WAV_DIR.resolve(sound.path() + ".wav");
            Path ogg = OUTPUT.resolve(sound.path() + ".ogg");
            Files.createDirectories(wav.getParent());
            Files.createDirectories(ogg.getParent());
            writeWav(wav, samples);
            int exit = new ProcessBuilder(ffmpeg, "-y", "-loglevel", "error", "-i", wav.toString(), "-c:a", "libvorbis",
                    "-ac", "1", "-q:a", "4", ogg.toString()).inheritIO().start().waitFor();
            if (exit != 0) {
                throw new IllegalStateException("ffmpeg failed for " + sound.path());
            }
            System.out.printf("%s: %.1f s%n", sound.path(), samples.length / (double) RATE);
        }
    }

    /**
     * Singing sand: 5-7 s, fundamental 70-110 Hz with overtones at 2x, 3x and 4x (laptop speakers cannot play the
     * fundamental, the overtones carry it), a slow drift, a beat of 2-5 Hz, filtered noise, swell in 1.5 s, fade out in 2 s.
     */
    private static double[] drone(Random random, int index) {
        double seconds = 4.5 + index * 0.8 + random.nextDouble();
        int length = (int) (seconds * RATE);
        double fundamental = 70 + random.nextDouble() * 40;
        double beat = 2 + random.nextDouble() * 3;
        double driftRate = 0.15 + random.nextDouble() * 0.2;
        double[] phase = new double[4];
        double[] amps = {1.0, 0.55, 0.35, 0.18};
        double noiseLow = 0;
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double drift = 1 + 0.012 * StrictMath.sin(2 * Math.PI * driftRate * t);
            double sum = 0;
            for (int h = 0; h < 4; h++) {
                phase[h] += 2 * Math.PI * fundamental * (h + 1) * drift / RATE;
                sum += amps[h] * StrictMath.sin(phase[h]);
            }
            double tremolo = 0.8 + 0.2 * StrictMath.sin(2 * Math.PI * beat * t);
            // One-pole low-pass of white noise: soft, dusty rumble around a few hundred Hz.
            noiseLow += 0.04 * ((random.nextDouble() * 2 - 1) - noiseLow);
            out[i] = (sum * tremolo + 9 * noiseLow) * swell(t, seconds, 1.5, 2.0);
        }
        return normalise(out, 0.8);
    }

    /** Oasis life, 2-3.5 s: 1 a two-note bird, 2 a cricket trill, 3 a run of small chirps. */
    private static double[] ambient(Random random, int index) {
        double seconds = 2.0 + index * 0.5;
        double[] out = new double[(int) (seconds * RATE)];
        switch (index) {
            case 1 -> {
                chirp(out, 0.2, 0.22, 2600, 3500, 0.7);
                chirp(out, 0.55, 0.3, 3300, 2500, 0.8);
                chirp(out, 1.4 + random.nextDouble() * 0.1, 0.2, 2800, 3700, 0.6);
            }
            case 2 -> {
                for (int pulse = 0; pulse < 14; pulse++) {
                    trillPulse(out, 0.2 + pulse * 0.085, 4300 + random.nextDouble() * 60, 0.5 * (1 - Math.abs(pulse - 7) / 9.0));
                }
            }
            default -> {
                double start = 0.15;
                for (int note = 0; note < 7; note++) {
                    double base = 3000 + random.nextDouble() * 800;
                    chirp(out, start, 0.07, base, base + 500 + random.nextDouble() * 400, 0.4 + 0.05 * note);
                    start += 0.16 + random.nextDouble() * 0.12;
                }
            }
        }
        return normalise(out, 0.6);
    }

    /** A bird note: a frequency sweep with a smooth envelope and a light vibrato, added into {@code out}. */
    private static void chirp(double[] out, double start, double duration, double from, double to, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        double phase = 0;
        for (int i = 0; i < count && first + i < out.length; i++) {
            double x = i / (double) count;
            double frequency = from + (to - from) * x + 60 * StrictMath.sin(2 * Math.PI * 28 * x * duration);
            phase += 2 * Math.PI * frequency / RATE;
            double envelope = StrictMath.sin(Math.PI * x);
            out[first + i] += amp * envelope * envelope * StrictMath.sin(phase);
        }
    }

    /** One cricket pulse: a short burst of a high tone, added into {@code out}. */
    private static void trillPulse(double[] out, double start, double frequency, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (0.05 * RATE);
        for (int i = 0; i < count && first + i < out.length; i++) {
            double x = i / (double) count;
            double envelope = StrictMath.sin(Math.PI * x);
            out[first + i] += amp * envelope * envelope * StrictMath.sin(2 * Math.PI * frequency * i / RATE);
        }
    }

    /** Squared raised-sine fade in over {@code attack} seconds and out over {@code release} seconds. */
    private static double swell(double t, double seconds, double attack, double release) {
        double gain = Math.min(Math.min(1, t / attack), Math.min(1, (seconds - t) / release));
        double shaped = StrictMath.sin(gain * Math.PI / 2);
        return shaped * shaped;
    }

    private static double[] normalise(double[] samples, double peak) {
        double max = 0;
        for (double sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        for (int i = 0; i < samples.length; i++) {
            samples[i] = samples[i] / max * peak;
        }
        return samples;
    }

    /** No clip, no click: silent (or nearly) at both ends. */
    private static void check(Sound sound, double[] samples) {
        if (Math.abs(samples[0]) > 0.01 || Math.abs(samples[samples.length - 1]) > 0.01) {
            throw new IllegalStateException(sound.path() + " starts or ends with a click");
        }
        for (double sample : samples) {
            if (Math.abs(sample) > 1.0) {
                throw new IllegalStateException(sound.path() + " clips");
            }
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
