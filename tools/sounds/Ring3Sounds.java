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
 * Synthesises the ring 3 and Nether ring 3 sounds from our own maths: no vanilla audio is read or copied (package 50,
 * pattern of {@code Ring2Sounds.java}). Run from the repository root: {@code java tools/sounds/Ring3Sounds.java
 * [path-to-ffmpeg]}; the ffmpeg path can also come from the environment variable {@code FFMPEG}.
 *
 * <p>Two kinds of sound:
 * <ul>
 *   <li><b>loops</b> (weather beds, ember crackle, titan hum, caldera quiet, Nether wind): exactly periodic, built from
 *       sines on whole multiples of the loop frequency, with every event wrapped around the end, so the end runs straight
 *       into the start without a click; streamed;</li>
 *   <li><b>one-shots</b> (rumbles, lava, ice creaks, block sounds): silent at both ends and checked for clipping.</li>
 * </ul>
 *
 * <p>Deterministic: every file has a fixed seed (hash of its name) and uses {@link StrictMath}, so running twice gives
 * byte-identical WAVs in {@code build/sounds/} (not in git). ffmpeg turns them into mono Ogg Vorbis under
 * {@code src/main/resources/assets/corruptedadventure/sounds/}; only those are committed. The {@code .ogg} bytes may
 * differ per ffmpeg version: the WAV is the source. This script writes only ring 3 files, never ring 2's.
 */
public final class Ring3Sounds {
    private static final int RATE = 44100;
    private static final Path WAV_DIR = Path.of("build/sounds");
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/sounds");

    private record Sound(String path, String variant, int index) {
        boolean loop() {
            return path.contains("_loop_");
        }
    }

    private static final List<Sound> SOUNDS = List.of(
            // Weather (package 45, own audio since package 50).
            new Sound("weather/ring_3/ash_rain_loop_1", "ash_rain", 1),
            new Sound("weather/ring_3/ash_rain_loop_2", "ash_rain", 2),
            new Sound("weather/ring_3/eruption_1", "rumble", 1),
            new Sound("weather/ring_3/eruption_2", "rumble", 2),
            new Sound("weather/ring_3/eruption_3", "rumble", 3),
            new Sound("weather/ring_3/ash_cloud_loop_1", "roar", 1),
            // Ambience ring 3.
            new Sound("ambient/ring_3/rumble_1", "distant_rumble", 1),
            new Sound("ambient/ring_3/rumble_2", "distant_rumble", 2),
            new Sound("ambient/ring_3/rumble_3", "distant_rumble", 3),
            new Sound("ambient/ring_3/embers_loop_1", "embers", 1),
            new Sound("ambient/ring_3/embers_loop_2", "embers", 2),
            new Sound("ambient/ring_3/lava_1", "lava", 1),
            new Sound("ambient/ring_3/lava_2", "lava", 2),
            new Sound("ambient/ring_3/lava_3", "lava", 3),
            new Sound("ambient/ring_3/titan_hum_loop_1", "titan_hum", 1),
            new Sound("ambient/ring_3/caldera_loop_1", "caldera", 1),
            // Ambience Nether ring 3.
            new Sound("ambient/nether_ring_3/wind_loop_1", "wind", 1),
            new Sound("ambient/nether_ring_3/wind_loop_2", "wind", 2),
            new Sound("ambient/nether_ring_3/ice_creak_1", "ice_creak", 1),
            new Sound("ambient/nether_ring_3/ice_creak_2", "ice_creak", 2),
            new Sound("ambient/nether_ring_3/ice_creak_3", "ice_creak", 3),
            // Blocks (package 46 plays them).
            new Sound("block/ring_3/fumarole_puff_1", "hiss", 1),
            new Sound("block/ring_3/fumarole_puff_2", "hiss", 2),
            new Sound("block/ring_3/geyser_burst_1", "geyser", 1),
            new Sound("block/ring_3/geyser_burst_2", "geyser", 2),
            // The geyser's warning (package 50c): a second of gurgling and a low groan before the burst.
            new Sound("block/ring_3/geyser_warning_1", "gurgle", 1),
            new Sound("block/ring_3/geyser_warning_2", "gurgle", 2),
            new Sound("block/ring_3/boiling_mud_bubble_1", "bloop", 1),
            new Sound("block/ring_3/boiling_mud_bubble_2", "bloop", 2),
            new Sound("block/ring_3/boiling_mud_bubble_3", "bloop", 3));

    private Ring3Sounds() {
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
                case "ash_rain" -> ashRain(random, sound.index());
                case "rumble" -> rumble(random, sound.index(), 1.0);
                case "distant_rumble" -> rumble(random, sound.index(), 0.55);
                case "roar" -> roar(random, sound.index());
                case "embers" -> embers(random, sound.index());
                case "lava" -> lava(random, sound.index());
                case "titan_hum" -> titanHum(random, sound.index());
                case "caldera" -> caldera(random, sound.index());
                case "wind" -> wind(random, sound.index());
                case "ice_creak" -> iceCreak(random, sound.index());
                case "hiss" -> hiss(random, sound.index());
                case "geyser" -> geyser(random, sound.index());
                case "gurgle" -> gurgle(random, sound.index());
                case "bloop" -> bloop(random, sound.index());
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

    /** Ash rain, 8 or 10 s: a soft, dry hiss with hundreds of tiny ticks (flakes and grit), no tone. */
    private static double[] ashRain(Random random, int index) {
        double seconds = 6 + index * 2;
        double[] out = periodicNoise(random, seconds, 150, 300, 5000, 900, 0.5);
        int ticks = (int) (seconds * 40);
        for (int k = 0; k < ticks; k++) {
            click(out, random.nextDouble() * seconds, 2500 + random.nextDouble() * 3500, 0.05 + random.nextDouble() * 0.12, 0.004);
        }
        return normalise(out, 0.6);
    }

    /** Ash cloud, 10 s: a deep roar of wind, slowly breathing, with grit ticks on top. */
    private static double[] roar(Random random, int index) {
        double seconds = 10;
        double[] out = periodicNoise(random, seconds, 220, 30, 900, 120, 1.0);
        double[] high = periodicNoise(random, seconds, 80, 900, 3500, 1500, 0.25);
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) RATE;
            // Breathing: whole periods in the loop (2 and 3 per loop).
            double gust = 0.7 + 0.2 * StrictMath.sin(2 * Math.PI * 2 * t / seconds) + 0.1 * StrictMath.sin(2 * Math.PI * 3 * t / seconds + 1.1);
            out[i] = out[i] * gust + high[i] * gust * gust;
        }
        int ticks = (int) (seconds * 25);
        for (int k = 0; k < ticks; k++) {
            click(out, random.nextDouble() * seconds, 2000 + random.nextDouble() * 3000, 0.04 + random.nextDouble() * 0.08, 0.005);
        }
        return normalise(out, 0.7);
    }

    /** Smouldering wood, 8 or 10 s: a low glow-hiss with irregular crackles and the odd pop. */
    private static double[] embers(Random random, int index) {
        double seconds = 6 + index * 2;
        double[] out = periodicNoise(random, seconds, 80, 100, 1500, 400, 0.15);
        int crackles = (int) (seconds * 9);
        for (int k = 0; k < crackles; k++) {
            double start = random.nextDouble() * seconds;
            // Crackles come in little bursts of 1-4 clicks.
            int burst = 1 + random.nextInt(4);
            for (int b = 0; b < burst; b++) {
                click(out, start + b * (0.008 + random.nextDouble() * 0.03), 1500 + random.nextDouble() * 4000,
                        0.25 + random.nextDouble() * 0.6, 0.003 + random.nextDouble() * 0.004);
            }
        }
        int pops = 2 + index;
        for (int k = 0; k < pops; k++) {
            click(out, random.nextDouble() * seconds, 600 + random.nextDouble() * 400, 0.9, 0.02);
        }
        return normalise(out, 0.65);
    }

    /**
     * Titan fields, 12 s: a low choir-like hum. Three voices on a minor-ish chord (around 55, 82 and 98 Hz) with their
     * overtones shaped by two vowel formants, a slow beat between detuned pairs; every frequency fits the loop.
     */
    private static double[] titanHum(Random random, int index) {
        double seconds = 12;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double[] roots = {55, 82.4, 98};
        for (double root : roots) {
            for (int detune = -1; detune <= 1; detune += 2) {
                double base = loopFrequency(root * (1 + detune * 0.003), seconds);
                for (int h = 1; h <= 12; h++) {
                    double f = loopFrequency(base * h, seconds);
                    // "Ooh/aah" formants near 400 and 800 Hz carry the choir; plain falloff otherwise.
                    double amp = (1.0 / h) * (0.3 + formant(f, 420, 120) + 0.6 * formant(f, 820, 180));
                    double phase = random.nextDouble() * 2 * Math.PI;
                    addSine(out, f, phase, amp * 0.25);
                }
            }
        }
        double[] air = periodicNoise(random, seconds, 60, 200, 900, 300, 0.06);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double swell = 0.8 + 0.2 * StrictMath.sin(2 * Math.PI * t / seconds);
            out[i] = out[i] * swell + air[i];
        }
        return normalise(out, 0.6);
    }

    /** Dormant crater, 12 s: a very soft breeze and a single far insect (a short trill), nothing else. */
    private static double[] caldera(Random random, int index) {
        double seconds = 12;
        double[] out = periodicNoise(random, seconds, 120, 150, 1200, 350, 0.35);
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) RATE;
            out[i] *= 0.7 + 0.3 * StrictMath.sin(2 * Math.PI * t / seconds + 0.4);
        }
        double start = 4 + random.nextDouble() * 4;
        for (int pulse = 0; pulse < 9; pulse++) {
            tone(out, start + pulse * 0.07, 0.04, 4200 + random.nextDouble() * 80, 0.05 * (1 - Math.abs(pulse - 4) / 6.0));
        }
        return normalise(out, 0.35);
    }

    /** Nether ring 3 wind, 10 or 12 s: cold, hollow wind through ice halls with a faint whistle that comes and goes. */
    private static double[] wind(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] out = periodicNoise(random, seconds, 200, 80, 1400, 260, 1.0);
        double whistle = loopFrequency(620 + random.nextDouble() * 180, seconds);
        double[] tone = new double[length];
        addSine(tone, whistle, random.nextDouble(), 0.12);
        addSine(tone, loopFrequency(whistle * 1.5, seconds), random.nextDouble(), 0.05);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double gust = 0.6 + 0.25 * StrictMath.sin(2 * Math.PI * 2 * t / seconds) + 0.15 * StrictMath.sin(2 * Math.PI * 5 * t / seconds + 0.7);
            double whistleGain = StrictMath.pow(Math.max(0, StrictMath.sin(2 * Math.PI * t / seconds + index)), 3);
            out[i] = out[i] * gust + tone[i] * whistleGain;
        }
        return normalise(out, 0.6);
    }

    // ------------------------------------------------------------------ one-shots

    /**
     * A volcanic rumble, 4-6 s: a sub-bass swell (28-45 Hz with overtones, which small speakers need) under rolling
     * low-passed noise; {@code weight} 1 = an eruption close by, lower = distant (darker, softer attack).
     */
    private static double[] rumble(Random random, int index, double weight) {
        double seconds = 4 + index * 0.7 + random.nextDouble() * 0.6;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double base = 28 + random.nextDouble() * 17;
        double[] phase = new double[5];
        double low = 0;
        double lower = 0;
        double alpha = 0.004 + 0.01 * weight;
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double drift = 1 + 0.05 * StrictMath.sin(2 * Math.PI * 0.4 * t);
            double tone = 0;
            for (int h = 0; h < phase.length; h++) {
                phase[h] += 2 * Math.PI * base * (h + 1) * drift / RATE;
                tone += StrictMath.sin(phase[h]) / (h + 1);
            }
            low += alpha * ((random.nextDouble() * 2 - 1) - low);
            lower += 0.3 * (low - lower);
            // Rolling: two slow amplitude waves on top of the envelope.
            double roll = 0.65 + 0.35 * StrictMath.sin(2 * Math.PI * (1.1 + index * 0.3) * t) * StrictMath.sin(2 * Math.PI * 0.37 * t);
            out[i] = (0.5 * tone + 30 * lower) * roll * swell(t, seconds, 0.25 + 0.8 * (1 - weight), 2.5);
        }
        return normalise(out, 0.5 + 0.35 * weight);
    }

    /** Lava, 1.2-2 s: a few thick, low bubbles bursting (falling pitch, short burst of noise at the pop). */
    private static double[] lava(Random random, int index) {
        double seconds = 1.2 + index * 0.3;
        double[] out = new double[(int) (seconds * RATE)];
        int bubbles = 2 + index;
        for (int b = 0; b < bubbles; b++) {
            double start = 0.05 + random.nextDouble() * (seconds - 0.5);
            bubble(out, start, 90 + random.nextDouble() * 80, 0.6 + random.nextDouble() * 0.4, 0.18);
            click(out, start + 0.12, 700 + random.nextDouble() * 500, 0.3, 0.012);
        }
        fadeEnds(out, 0.01);
        return normalise(out, 0.6);
    }

    /** Ice creak, 0.8-1.6 s: a stick-slip train of low pulses that speeds up and rises, like stressed ice. */
    private static double[] iceCreak(Random random, int index) {
        double seconds = 0.8 + index * 0.4;
        double[] out = new double[(int) (seconds * RATE)];
        double t = 0.05;
        double rate = 14 + random.nextDouble() * 10;
        double pitch = 160 + random.nextDouble() * 100;
        while (t < seconds - 0.12) {
            double progress = t / seconds;
            click(out, t, pitch * (1 + 0.8 * progress), 0.5 + 0.4 * StrictMath.sin(Math.PI * progress), 0.012);
            t += 1 / (rate * (1 + 1.5 * progress)) * (0.8 + random.nextDouble() * 0.4);
        }
        // A brittle tick at the end.
        click(out, seconds - 0.1, 2800 + random.nextDouble() * 800, 0.35, 0.004);
        fadeEnds(out, 0.01);
        return normalise(out, 0.6);
    }

    /** Fumarole puff, 0.8-1.2 s: a burst of hissing steam (band-passed noise, sharp attack, breathy tail). */
    private static double[] hiss(Random random, int index) {
        double seconds = 0.8 + index * 0.4;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double low = 0;
        double slow = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double white = random.nextDouble() * 2 - 1;
            low += 0.35 * (white - low);
            slow += 0.05 * (low - slow);
            double band = low - slow;
            double envelope = (1 - StrictMath.exp(-t * 60)) * StrictMath.exp(-t * (3.5 / seconds));
            out[i] = band * envelope;
        }
        fadeEnds(out, 0.02);
        return normalise(out, 0.55);
    }

    /** Geyser burst, 2-2.6 s: a gurgle, then a rushing column of steam and water that falls away. */
    private static double[] geyser(Random random, int index) {
        double seconds = 2.0 + index * 0.6;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        for (int b = 0; b < 5; b++) {
            bubble(out, 0.02 + b * 0.06 + random.nextDouble() * 0.03, 120 + random.nextDouble() * 120, 0.4, 0.12);
        }
        double low = 0;
        double slow = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double white = random.nextDouble() * 2 - 1;
            low += 0.25 * (white - low);
            slow += 0.02 * (low - slow);
            double band = low - slow;
            double rush = t < 0.3 ? 0 : (1 - StrictMath.exp(-(t - 0.3) * 25)) * StrictMath.exp(-(t - 0.3) * 1.6);
            out[i] += 1.4 * band * rush;
        }
        fadeEnds(out, 0.02);
        return normalise(out, 0.7);
    }

    /**
     * Geyser warning, 0.7-0.95 s (shorter than the 1 s warning, so it has died out when the burst comes): a low groan
     * from deep in the vent (a slowly rising 50-80 Hz tone with overtones under dark noise) and bubbles that come faster
     * and higher towards the end, the pressure building up to the burst.
     */
    private static double[] gurgle(Random random, int index) {
        double seconds = 0.6 + index * 0.12 + random.nextDouble() * 0.1;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double base = 50 + random.nextDouble() * 12;
        double phase = 0;
        double low = 0;
        double lower = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double progress = t / seconds;
            phase += 2 * Math.PI * base * (1 + 0.5 * progress * progress) / RATE;
            double tone = StrictMath.sin(phase) + 0.5 * StrictMath.sin(2 * phase) + 0.25 * StrictMath.sin(3 * phase + 0.6);
            low += 0.02 * ((random.nextDouble() * 2 - 1) - low);
            lower += 0.2 * (low - lower);
            // Groaning: the pressure swells, wavers and is cut off where the burst takes over.
            double waver = 0.75 + 0.25 * StrictMath.sin(2 * Math.PI * (5 + index) * t);
            out[i] = (0.35 * tone + 12 * lower) * waver * swell(t, seconds, 0.35, 0.12);
        }
        double t = 0.05;
        while (t < seconds - 0.2) {
            double progress = t / seconds;
            bubble(out, t, 110 + 140 * progress + random.nextDouble() * 60, 0.35 + 0.4 * progress, 0.07 + random.nextDouble() * 0.05);
            t += (0.16 - 0.11 * progress) * (0.7 + random.nextDouble() * 0.6);
        }
        fadeEnds(out, 0.02);
        return normalise(out, 0.6);
    }

    /** Boiling mud, 0.4-0.7 s: one thick, low bloop. */
    private static double[] bloop(Random random, int index) {
        double seconds = 0.4 + index * 0.1;
        double[] out = new double[(int) (seconds * RATE)];
        bubble(out, 0.02, 70 + random.nextDouble() * 50 + index * 15, 1.0, 0.22);
        click(out, 0.22, 500 + random.nextDouble() * 300, 0.15, 0.01);
        fadeEnds(out, 0.01);
        return normalise(out, 0.55);
    }

    // ------------------------------------------------------------------ building blocks

    /**
     * Exactly periodic noise: {@code partials} sines on whole multiples of {@code 1/seconds} between {@code from} and
     * {@code to} Hz, falling off above {@code corner} Hz, with random phases; scaled to peak {@code gain}.
     */
    private static double[] periodicNoise(Random random, double seconds, int partials, double from, double to, double corner, double gain) {
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        for (int p = 0; p < partials; p++) {
            double frequency = loopFrequency(from + random.nextDouble() * (to - from), seconds);
            double amp = corner / (corner + frequency);
            addSine(out, frequency, random.nextDouble() * 2 * Math.PI, amp);
        }
        return normalise(out, gain);
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

    /** A short decaying tone burst (tick, crackle, creak pulse), wrapped around the end of {@code out}. */
    private static void click(double[] out, double start, double frequency, double amp, double decay) {
        int first = (int) (start * RATE);
        int count = (int) (decay * 6 * RATE);
        for (int i = 0; i < count; i++) {
            double t = i / (double) RATE;
            double envelope = (1 - StrictMath.exp(-t * 4000)) * StrictMath.exp(-t / decay);
            out[Math.floorMod(first + i, out.length)] += amp * envelope * StrictMath.sin(2 * Math.PI * frequency * t);
        }
    }

    /** A bubble: a tone whose pitch rises as it grows, then bursts; wrapped around the end of {@code out}. */
    private static void bubble(double[] out, double start, double frequency, double amp, double duration) {
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double x = i / (double) count;
            phase += 2 * Math.PI * frequency * (1 + 1.2 * x * x) / RATE;
            double envelope = StrictMath.sin(Math.PI * Math.min(1, x * 1.15)) * (1 - x * 0.3);
            out[Math.floorMod(first + i, out.length)] += amp * Math.max(0, envelope) * StrictMath.sin(phase);
        }
    }

    /** A soft tone with a sine envelope (insect pulse). */
    private static void tone(double[] out, double start, double duration, double frequency, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        for (int i = 0; i < count; i++) {
            double x = i / (double) count;
            double envelope = StrictMath.sin(Math.PI * x);
            out[Math.floorMod(first + i, out.length)] += amp * envelope * envelope * StrictMath.sin(2 * Math.PI * frequency * i / RATE);
        }
    }

    private static double formant(double frequency, double centre, double width) {
        double d = (frequency - centre) / width;
        return StrictMath.exp(-d * d);
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

    /** Squared raised-sine fade in over {@code attack} seconds and out over {@code release} seconds. */
    private static double swell(double t, double seconds, double attack, double release) {
        double gain = Math.max(0, Math.min(Math.min(1, t / attack), Math.min(1, (seconds - t) / release)));
        double shaped = StrictMath.sin(gain * Math.PI / 2);
        return shaped * shaped;
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
