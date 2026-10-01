package nl.timtoys.corruptedadventure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the hard rule that a dedicated server never loads a client class: outside {@code client/} (and
 * {@code datagen/}, whose NeoForge model generators live in a client package but only run in {@code runData}) no source
 * file may name a client package, in an import or fully qualified. A text check, so it is a safety net, not a proof.
 */
class ClientIsolationTest {
    private static final String SOURCE_ROOT = "src/main/java/nl/timtoys/corruptedadventure";
    private static final List<String> CLIENT_PACKAGES = List.of(
            "net.minecraft.client.",
            "com.mojang.blaze3d.",
            "net.neoforged.neoforge.client.",
            "nl.timtoys.corruptedadventure.client.");
    private static final List<String> EXEMPT = List.of("client", "datagen");

    @Test
    void commonCodeNeverNamesAClientPackage() throws IOException {
        Path root = findSourceRoot();
        List<String> violations = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (EXEMPT.contains(root.relativize(file).getName(0).toString())) {
                    continue;
                }
                checked++;
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).strip();
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                        continue;
                    }
                    for (String clientPackage : CLIENT_PACKAGES) {
                        if (line.contains(clientPackage)) {
                            violations.add(root.relativize(file) + ":" + (i + 1) + " " + line);
                        }
                    }
                }
            }
        }
        assertTrue(checked > 50, "only " + checked + " common source files found under " + root);
        assertTrue(violations.isEmpty(), "Common code names a client package:\n" + String.join("\n", violations));
    }

    /** Unit tests may run from the project or a sub-directory; walk up until the sources are found. */
    private static Path findSourceRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(SOURCE_ROOT);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return fail("Could not find " + SOURCE_ROOT + " from " + Path.of("").toAbsolutePath());
    }
}
