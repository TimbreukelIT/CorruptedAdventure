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
 * {@code Ring3Sounds.java}): the ambience of {@code verdant_falls} (Q8 = A) and, since change 80, the rest of ring 4 and
 * Nether ring 4: the three weather loops (the storm with creaking islands mixed in, not positional), the lapping water
 * of the mythical band, the night choir, the reeds, bubbles from the deep, the drone of zone 4→5 (with the creak of
 * drifting chunks), the dry wind and crystal hum of Nether ring 4, and the block sounds of swamp gas, bubble spring,
 * updraft and sucking mud.
 * Run from the repository root: {@code java tools/sounds/Ring4Sounds.java [path-to-ffmpeg]}; the ffmpeg path can also
 * come from the environment variable {@code FFMPEG}.
 *
 * <p>Two kinds of sound:
 * <ul>
 *   <li><b>loops</b> (falls, water, weather, drone, wind): noise filtered <i>circularly</i> (every filter runs over the
 *       buffer twice and keeps the second pass, so its state at the start is the one at the end) and modulated with
 *       whole periods of the loop, so the end runs straight into the start without a click; streamed;</li>
 *   <li><b>one-shots</b> (birds, frogs, choir, reeds, bubbles, hum, block sounds): silent at both ends and checked for clipping.</li>
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
            new Sound("ambient/ring_4/frog_2", "frog", 2),
            // Ring 4 weather (change 80, backlog: the loops were vanilla placeholders).
            new Sound("weather/ring_4/dense_fog_loop_1", "dense_fog", 1),
            new Sound("weather/ring_4/storm_wind_loop_1", "storm_wind", 1),
            new Sound("weather/ring_4/storm_wind_loop_2", "storm_wind", 2),
            new Sound("weather/ring_4/toxic_rain_loop_1", "toxic_rain", 1),
            new Sound("weather/ring_4/toxic_rain_loop_2", "toxic_rain", 2),
            // Ring 4 ambience (change 80).
            new Sound("ambient/ring_4/water_loop_1", "water", 1),
            new Sound("ambient/ring_4/water_loop_2", "water", 2),
            new Sound("ambient/ring_4/night_choir_1", "night_choir", 1),
            new Sound("ambient/ring_4/night_choir_2", "night_choir", 2),
            new Sound("ambient/ring_4/night_choir_3", "night_choir", 3),
            new Sound("ambient/ring_4/reeds_1", "reeds", 1),
            new Sound("ambient/ring_4/reeds_2", "reeds", 2),
            new Sound("ambient/ring_4/reeds_3", "reeds", 3),
            new Sound("ambient/ring_4/bubbles_1", "bubbles", 1),
            new Sound("ambient/ring_4/bubbles_2", "bubbles", 2),
            new Sound("ambient/ring_4/bubbles_3", "bubbles", 3),
            new Sound("ambient/ring_4/sundered_drone_loop_1", "drone", 1),
            // Nether ring 4 (change 80).
            new Sound("ambient/nether_ring_4/wind_loop_1", "dry_wind", 1),
            new Sound("ambient/nether_ring_4/wind_loop_2", "dry_wind", 2),
            new Sound("ambient/nether_ring_4/crystal_hum_1", "crystal_hum", 1),
            new Sound("ambient/nether_ring_4/crystal_hum_2", "crystal_hum", 2),
            new Sound("ambient/nether_ring_4/crystal_hum_3", "crystal_hum", 3),
            // Ring 4 block sounds (stream B plays them, package 72).
            new Sound("block/ring_4/swamp_gas_ignite_1", "ignite", 1),
            new Sound("block/ring_4/swamp_gas_ignite_2", "ignite", 2),
            new Sound("block/ring_4/bubble_spring_bubble_1", "spring_bubble", 1),
            new Sound("block/ring_4/bubble_spring_bubble_2", "spring_bubble", 2),
            new Sound("block/ring_4/bubble_spring_bubble_3", "spring_bubble", 3),
            new Sound("block/ring_4/updraft_flow_1", "updraft", 1),
            new Sound("block/ring_4/updraft_flow_2", "updraft", 2),
            new Sound("block/ring_4/sucking_mud_slurp_1", "slurp", 1),
            new Sound("block/ring_4/sucking_mud_slurp_2", "slurp", 2),
            new Sound("block/ring_4/sucking_mud_slurp_3", "slurp", 3));

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
                case "dense_fog" -> denseFog(random);
                case "storm_wind" -> stormWind(random, sound.index());
                case "toxic_rain" -> toxicRain(random, sound.index());
                case "water" -> water(random, sound.index());
                case "night_choir" -> nightChoir(random, sound.index());
                case "reeds" -> reeds(random, sound.index());
                case "bubbles" -> bubbles(random, sound.index());
                case "drone" -> drone(random);
                case "dry_wind" -> dryWind(random, sound.index());
                case "crystal_hum" -> crystalHum(random, sound.index());
                case "ignite" -> ignite(random, sound.index());
                case "spring_bubble" -> springBubble(random, sound.index());
                case "updraft" -> updraft(random, sound.index());
                case "slurp" -> slurp(random, sound.index());
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


    /** Dense fog, 12 s: a soft, muffled low hush with a faint, far-off pair of tones; the world gone quiet. */
    private static double[] denseFog(Random random) {
        double seconds = 12;
        int length = (int) (seconds * RATE);
        double[] hush = normalise(lowpass(lowpass(white(random, length), 180), 180), 1.0);
        double[] air = normalise(band(white(random, length), 300, 1200), 1.0);
        double[] tones = new double[length];
        addSine(tones, loopFrequency(98, seconds), random.nextDouble() * 2 * Math.PI, 0.05);
        addSine(tones, loopFrequency(147, seconds), random.nextDouble() * 2 * Math.PI, 0.03);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double swell = 0.75 + 0.25 * StrictMath.sin(2 * Math.PI * t / seconds);
            out[i] = (hush[i] + 0.15 * air[i]) * swell + tones[i];
        }
        return normalise(out, 0.3);
    }

    /**
     * Storm wind, 12 or 14 s: a hard, gusting roar with a whistle now and then, and the islands' roots creaking under the
     * strain twice per loop (mixed in, so not positional).
     */
    private static double[] stormWind(Random random, int index) {
        double seconds = 10 + index * 2;
        int length = (int) (seconds * RATE);
        double[] roar = normalise(band(white(random, length), 100, 2500), 1.0);
        double[] highs = normalise(band(white(random, length), 1500, 6000), 1.0);
        double[] whistle = new double[length];
        double pitch = loopFrequency(680 + random.nextDouble() * 160, seconds);
        addSine(whistle, pitch, random.nextDouble(), 0.1);
        addSine(whistle, loopFrequency(pitch * 1.5, seconds), random.nextDouble(), 0.04);
        double[] creaks = new double[length];
        for (int creak = 0; creak < 2; creak++) {
            groan(creaks, random, (creak + random.nextDouble() * 0.6) * seconds / 2, 1.0 + random.nextDouble() * 0.6,
                    110 + random.nextDouble() * 90, 0.35);
        }
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double gust = 0.5 + 0.3 * StrictMath.sin(2 * Math.PI * 3 * t / seconds + index)
                    + 0.2 * StrictMath.sin(2 * Math.PI * 7 * t / seconds + 1.1);
            double whistleGain = StrictMath.pow(Math.max(0, StrictMath.sin(2 * Math.PI * t / seconds + index)), 3);
            out[i] = (roar[i] + 0.25 * highs[i] * gust) * gust + whistle[i] * whistleGain + creaks[i];
        }
        return normalise(out, 0.6);
    }

    /** Toxic rain, 10 or 12 s: a steady rain hiss with many small drops and a faint, slow sizzle on top. */
    private static double[] toxicRain(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] rain = normalise(band(white(random, length), 800, 7000), 1.0);
        double[] sizzle = normalise(band(white(random, length), 4000, 10000), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            out[i] = 0.6 * rain[i] + 0.3 * sizzle[i] * (0.6 + 0.4 * StrictMath.sin(2 * Math.PI * 4 * t / seconds));
        }
        for (int drop = 0; drop < seconds * 25; drop++) {
            click(out, random.nextDouble() * seconds, 1500 + random.nextDouble() * 3000, 0.1 + random.nextDouble() * 0.15,
                    0.002 + random.nextDouble() * 0.003);
        }
        return normalise(out, 0.45);
    }

    /** Lapping water, 11 or 13 s: a soft wash against the banks with small plops; calm, the mythical band's loop. */
    private static double[] water(Random random, int index) {
        double seconds = 9 + index * 2;
        int length = (int) (seconds * RATE);
        double[] wash = normalise(band(white(random, length), 120, 900), 1.0);
        double[] ripple = normalise(band(white(random, length), 900, 3000), 1.0);
        double[] slow = normalise(lowpass(lowpass(white(random, length), 0.8), 0.8), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double lap = 0.65 + 0.2 * StrictMath.sin(2 * Math.PI * 3 * t / seconds + index)
                    + 0.15 * StrictMath.sin(2 * Math.PI * 5 * t / seconds + 1.3);
            out[i] = (0.8 * wash[i] + 0.25 * ripple[i]) * lap * (0.85 + 0.15 * slow[i]);
        }
        for (int plop = 0; plop < seconds * 1.5; plop++) {
            bubble(out, random.nextDouble() * seconds, 500 + random.nextDouble() * 700, 0.25 + random.nextDouble() * 0.2,
                    0.03 + random.nextDouble() * 0.03);
        }
        return normalise(out, 0.3);
    }

    /**
     * The drone of zone 4→5, 12 s: a low, beating hum under a hollow wind, and twice per loop the long creak of a
     * drifting chunk of land.
     */
    private static double[] drone(Random random) {
        double seconds = 12;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        for (double root : new double[] {41.2, 61.7, 82.4}) {
            for (int detune = -1; detune <= 1; detune += 2) {
                double base = loopFrequency(root * (1 + detune * 0.004), seconds);
                for (int h = 1; h <= 6; h++) {
                    addSine(out, loopFrequency(base * h, seconds), random.nextDouble() * 2 * Math.PI, 0.2 / h);
                }
            }
        }
        double[] wind = normalise(band(white(random, length), 60, 500), 1.0);
        double[] creaks = new double[length];
        for (int creak = 0; creak < 2; creak++) {
            groan(creaks, random, (creak + random.nextDouble() * 0.6) * seconds / 2, 1.2 + random.nextDouble() * 0.6,
                    80 + random.nextDouble() * 60, 0.5);
        }
        double hum = 0;
        for (double sample : out) {
            hum = Math.max(hum, Math.abs(sample));
        }
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double swell = 0.8 + 0.2 * StrictMath.sin(2 * Math.PI * t / seconds);
            out[i] = out[i] / hum * swell + 0.35 * wind[i] + creaks[i];
        }
        return normalise(out, 0.5);
    }

    /** The dry wind of Nether ring 4, 10 or 12 s: a hissing, sandy wind without water in it, gusting softly. */
    private static double[] dryWind(Random random, int index) {
        double seconds = 8 + index * 2;
        int length = (int) (seconds * RATE);
        double[] dry = normalise(band(white(random, length), 400, 4000), 1.0);
        double[] body = normalise(band(white(random, length), 90, 500), 1.0);
        double[] sand = normalise(band(white(random, length), 3000, 8000), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double gust = 0.55 + 0.3 * StrictMath.sin(2 * Math.PI * 2 * t / seconds + index)
                    + 0.15 * StrictMath.sin(2 * Math.PI * 5 * t / seconds + 0.4);
            out[i] = (0.6 * dry[i] + 0.7 * body[i]) * gust + 0.3 * sand[i] * gust * gust;
        }
        return normalise(out, 0.5);
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


    /**
     * The night choir, 5 s: a soft choir chord swelling in and out, a harp arpeggio over it and a few tinkling high
     * notes at the end, with a long echo. Three chords (A minor, G major, B flat major).
     */
    private static double[] nightChoir(Random random, int index) {
        double seconds = 5.0;
        double[] out = new double[(int) (seconds * RATE)];
        double[][] chords = {{220, 261.6, 329.6, 440}, {196, 246.9, 293.7, 392}, {233.1, 293.7, 349.2, 466.2}};
        double[] chord = chords[index - 1];
        for (double note : chord) {
            for (int h = 1; h <= 8; h++) {
                double f = note * h;
                double amp = (1.0 / h) * (0.3 + formant(f, 420, 120) + 0.6 * formant(f, 820, 180)) * 0.12;
                double phase = random.nextDouble() * 2 * Math.PI;
                for (int i = 0; i < out.length; i++) {
                    double t = i / (double) RATE;
                    double vibrato = 1 + 0.003 * StrictMath.sin(2 * Math.PI * 5 * t + phase);
                    out[i] += amp * swell(t, seconds, 1.5, 2.5) * StrictMath.sin(2 * Math.PI * f * vibrato * t + phase);
                }
            }
        }
        for (int step = 0; step < 6; step++) {
            pluck(out, 0.3 + step * 0.32, chord[step % chord.length] * (step < 4 ? 2 : 4), 0.35);
        }
        for (int bell = 0; bell < 3; bell++) {
            tone(out, 2.7 + bell * 0.28, 0.5, chord[(bell + 1) % chord.length] * 8, 0.06);
        }
        echo(out, 0.23, 0.3);
        echo(out, 0.41, 0.15);
        fadeEnds(out, 0.02);
        return normalise(out, 0.5);
    }

    /**
     * Something in the reeds, 1.4 s: a few dry rustling bursts; (3) also slow, soft steps in the mud underneath.
     */
    private static double[] reeds(Random random, int index) {
        double seconds = 1.4;
        int length = (int) (seconds * RATE);
        double[] hiss = normalise(band(white(random, length), 1800, 7000), 1.0);
        double[] crackle = normalise(band(white(random, length), 600, 2500), 1.0);
        double[] envelope = new double[length];
        int bursts = 3 + random.nextInt(3);
        for (int burst = 0; burst < bursts; burst++) {
            double start = 0.05 + random.nextDouble() * (seconds - 0.5);
            double duration = 0.15 + random.nextDouble() * 0.2;
            double amp = 0.5 + random.nextDouble() * 0.5;
            for (int i = 0; i < duration * RATE && start * RATE + i < length; i++) {
                double x = i / (duration * RATE);
                double shape = StrictMath.sin(Math.PI * x);
                envelope[(int) (start * RATE) + i] += amp * shape * shape;
            }
        }
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            out[i] = 0.7 * hiss[i] * envelope[i] + 0.4 * crackle[i] * envelope[i] * envelope[i];
        }
        if (index == 3) {
            for (int step = 0; step < 3; step++) {
                thud(out, random, 0.2 + step * 0.42, 0.6);
            }
        }
        fadeEnds(out, 0.01);
        return normalise(out, 0.55);
    }

    /** Bubbles from the deep, 3-3.8 s: a stream of large, low bubbles over a gurgle, swelling and dying away. */
    private static double[] bubbles(Random random, int index) {
        double seconds = 2.6 + index * 0.4;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double t = 0.05;
        while (t < seconds - 0.4) {
            double progress = t / seconds;
            double strength = 0.4 + 0.6 * StrictMath.sin(Math.PI * progress);
            bubble(out, t, 110 + random.nextDouble() * 180, 0.5 * strength, 0.08 + random.nextDouble() * 0.1);
            t += 0.04 + random.nextDouble() * 0.12 * (1.2 - StrictMath.sin(Math.PI * progress));
        }
        double[] gurgle = normalise(band(white(random, length), 80, 400), 1.0);
        for (int i = 0; i < length; i++) {
            out[i] += 0.3 * gurgle[i] * StrictMath.sin(Math.PI * i / length);
        }
        fadeEnds(out, 0.02);
        return normalise(out, 0.55);
    }

    /**
     * The crystals of Nether ring 4 humming, 3.6-4.8 s: glassy, slightly inharmonic partials that beat slowly against
     * a detuned twin, swelling in and out, with a faint shimmer.
     */
    private static double[] crystalHum(Random random, int index) {
        double seconds = 3.0 + index * 0.6;
        double[] out = new double[(int) (seconds * RATE)];
        double base = 300 + random.nextDouble() * 120;
        double[] ratios = {1, 2.01, 2.76, 4.07, 5.4};
        double[] amps = {1, 0.4, 0.5, 0.25, 0.2};
        for (int p = 0; p < ratios.length; p++) {
            for (double beat : new double[] {0, 1.3 + random.nextDouble()}) {
                double f = base * ratios[p] + beat;
                double phase = random.nextDouble() * 2 * Math.PI;
                for (int i = 0; i < out.length; i++) {
                    double t = i / (double) RATE;
                    out[i] += amps[p] * 0.2 * swell(t, seconds, 1.0, 1.6) * StrictMath.sin(2 * Math.PI * f * t + phase);
                }
            }
        }
        tone(out, seconds * 0.35, seconds * 0.4, base * 6.3, 0.03);
        fadeEnds(out, 0.02);
        return normalise(out, 0.45);
    }

    /** Swamp gas catching fire, 1.1 s: a low "whoomp" (a thump falling from 110 to 40 Hz), a short roar of flame, crackle. */
    private static double[] ignite(Random random, int index) {
        double seconds = 1.0 + index * 0.1;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double frequency = 40 + 70 * StrictMath.exp(-t / 0.08);
            phase += 2 * Math.PI * frequency / RATE;
            out[i] += 0.9 * (1 - StrictMath.exp(-t * 300)) * StrictMath.exp(-t / 0.25) * StrictMath.sin(phase);
        }
        double[] flame = normalise(band(white(random, length), 150, 2500), 1.0);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            out[i] += 0.6 * flame[i] * (1 - StrictMath.exp(-t * 50)) * StrictMath.exp(-t / 0.35);
        }
        for (int crack = 0; crack < 15; crack++) {
            click(out, 0.03 + random.nextDouble() * 0.6, 1000 + random.nextDouble() * 3000, 0.15 + random.nextDouble() * 0.15, 0.003);
        }
        fadeEnds(out, 0.01);
        return normalise(out, 0.75);
    }

    /** A bubble spring, 0.65-0.85 s: two to four clear bubbles rising out of the spring. */
    private static double[] springBubble(Random random, int index) {
        double seconds = 0.55 + index * 0.1;
        double[] out = new double[(int) (seconds * RATE)];
        int count = 2 + random.nextInt(3);
        for (int b = 0; b < count; b++) {
            bubble(out, 0.03 + random.nextDouble() * (seconds - 0.2), 220 + random.nextDouble() * 260, 0.6,
                    0.06 + random.nextDouble() * 0.06);
        }
        fadeEnds(out, 0.01);
        return normalise(out, 0.5);
    }

    /** An updraft, 1.6 s: a rushing whoosh swelling up, with a run of bubbles climbing in pitch. */
    private static double[] updraft(Random random, int index) {
        double seconds = 1.6;
        int length = (int) (seconds * RATE);
        double[] rush = normalise(band(white(random, length), 250, 2200), 1.0);
        double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            out[i] = 0.6 * rush[i] * swell(i / (double) RATE, seconds, 0.25, 0.6);
        }
        for (int b = 0; b < 12; b++) {
            double progress = b / 12.0;
            bubble(out, 0.1 + progress * 1.2 + random.nextDouble() * 0.05, (300 + 600 * progress) * (0.9 + 0.2 * random.nextDouble()) + index * 30,
                    0.3, 0.05 + random.nextDouble() * 0.04);
        }
        fadeEnds(out, 0.01);
        return normalise(out, 0.5);
    }

    /**
     * Sucking mud, 0.95-1.25 s: a wet suction (a tone gliding down from 260 to 110 Hz, wobbling like a gurgle, over
     * damp noise) and a thick bloop as the mud lets go.
     */
    private static double[] slurp(Random random, int index) {
        double seconds = 0.8 + index * 0.15;
        int length = (int) (seconds * RATE);
        double[] out = new double[length];
        double[] wet = normalise(band(white(random, length), 200, 1200), 1.0);
        double suck = 0.45;
        double phase = 0;
        double pitch = 0.84 + random.nextDouble() * 0.28;
        for (int i = 0; i < suck * RATE; i++) {
            double t = i / (double) RATE;
            double x = t / suck;
            double frequency = (260 - 150 * x) * pitch;
            phase += 2 * Math.PI * frequency / RATE;
            double envelope = StrictMath.pow(StrictMath.sin(Math.PI * x), 2);
            double wobble = 0.6 + 0.4 * StrictMath.sin(2 * Math.PI * 25 * t);
            out[i] += envelope * wobble * (0.5 * StrictMath.sin(phase) + 0.25 * StrictMath.sin(2 * phase + 0.3)) + 0.35 * envelope * wet[i];
        }
        bubble(out, suck, 80 + random.nextDouble() * 30, 0.8, 0.2);
        fadeEnds(out, 0.01);
        return normalise(out, 0.6);
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

    /** A short decaying tone burst (tick, drop, creak pulse), wrapped around the end of {@code out}. */
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

    /**
     * A long creak of wood or roots under strain: a train of low pulses, slow at first, faster and a little higher
     * towards the end, swelling in and out; wrapped around the end of {@code out}.
     */
    private static void groan(double[] out, Random random, double start, double duration, double pitch, double amp) {
        double t = 0;
        double rate = 14 + random.nextDouble() * 10;
        while (t < duration) {
            double progress = t / duration;
            click(out, start + t, pitch * (1 + 0.3 * progress), amp * StrictMath.sin(Math.PI * progress), 0.012);
            t += 1 / (rate * (1 + 1.2 * progress)) * (0.8 + random.nextDouble() * 0.4);
        }
    }

    /** A harp string: a quick attack, a long exponential decay, two weak overtones. */
    private static void pluck(double[] out, double start, double frequency, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (1.6 * RATE);
        for (int i = 0; i < count && first + i < out.length; i++) {
            double t = i / (double) RATE;
            double envelope = (1 - StrictMath.exp(-t * 800)) * StrictMath.exp(-t * 3);
            double w = 2 * Math.PI * frequency * t;
            out[first + i] += amp * envelope * (StrictMath.sin(w) + 0.3 * StrictMath.sin(2 * w + 0.2) + 0.1 * StrictMath.sin(3 * w + 0.5));
        }
    }

    /** A soft tone with a sine envelope (a tinkling bell, a shimmer). */
    private static void tone(double[] out, double start, double duration, double frequency, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        for (int i = 0; i < count && first + i < out.length; i++) {
            double x = i / (double) count;
            double envelope = StrictMath.sin(Math.PI * x);
            out[first + i] += amp * envelope * envelope * StrictMath.sin(2 * Math.PI * frequency * i / RATE);
        }
    }

    /** A soft step in mud: a low, damped thump with a little noise. */
    private static void thud(double[] out, Random random, double start, double amp) {
        int first = (int) (start * RATE);
        int count = (int) (0.15 * RATE);
        for (int i = 0; i < count && first + i < out.length; i++) {
            double t = i / (double) RATE;
            double envelope = (1 - StrictMath.exp(-t * 600)) * StrictMath.exp(-t / 0.03);
            out[first + i] += amp * envelope * (StrictMath.sin(2 * Math.PI * 65 * t) + 0.4 * (random.nextDouble() * 2 - 1));
        }
    }

    private static double formant(double frequency, double centre, double width) {
        double d = (frequency - centre) / width;
        return StrictMath.exp(-d * d);
    }

    /** Squared raised-sine fade in over {@code attack} seconds and out over {@code release} seconds. */
    private static double swell(double t, double seconds, double attack, double release) {
        double gain = Math.max(0, Math.min(Math.min(1, t / attack), Math.min(1, (seconds - t) / release)));
        double shaped = StrictMath.sin(gain * Math.PI / 2);
        return shaped * shaped;
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
