import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Deoptimization storms in JFR recordings (2026-10-26: 56 million in Ring5Field.terrain made ring 5 692x slower).
 * <p>
 * C2 compiles a trap with action {@code none} once the methods it inlines have been recompiled often enough together
 * (Compile::too_many_recompiles: their summed decompile count reaches PerMethodRecompilationCutoff / 2 + 1). Such a
 * trap never recompiles the method: every pass over the branch drops into the interpreter. Normal runs have a few
 * hundred of those; a storm has millions at one site.
 * <p>
 * Run with the server's JDK as a single-file program: {@code java DeoptStorm.java <recording.jfr>...}. Prints
 * {@code total <n>}, {@code none <n>} and per site with action none {@code site <count> <method> line=<l> bci=<b>
 * <reason>}, most first (at most 10).
 */
public class DeoptStorm {
    public static void main(String[] args) throws Exception {
        long total = 0;
        long none = 0;
        Map<String, long[]> sites = new HashMap<>();
        for (String file : args) {
            try (RecordingFile recording = new RecordingFile(Path.of(file))) {
                while (recording.hasMoreEvents()) {
                    RecordedEvent event = recording.readEvent();
                    if (!event.getEventType().getName().equals("jdk.Deoptimization")) {
                        continue;
                    }
                    total++;
                    if (!"none".equals(event.getString("action"))) {
                        continue;
                    }
                    none++;
                    RecordedMethod method = event.getValue("method");
                    String name = method == null ? "?" : method.getType().getName() + "." + method.getName();
                    String site = name + " line=" + event.getValue("lineNumber") + " bci=" + event.getValue("bci")
                            + " " + event.getString("reason");
                    sites.computeIfAbsent(site, s -> new long[1])[0]++;
                }
            }
        }
        System.out.println("total " + total);
        System.out.println("none " + none);
        sites.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(10)
                .forEach(e -> System.out.println("site " + e.getValue()[0] + " " + e.getKey()));
    }
}
