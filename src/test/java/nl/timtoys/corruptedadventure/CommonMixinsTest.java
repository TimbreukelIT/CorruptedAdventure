package nl.timtoys.corruptedadventure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.WetSpongeBlock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The common mixins of package 55b: their own config (never the client package, no client section, so a dedicated
 * server loads them without a client class), and FML applies them (the unit test runs with the mod loaded). Not in the
 * mixin package itself: Mixin refuses to load any other class from there.
 */
class CommonMixinsTest {
    @Test
    void theCommonConfigHoldsOnlyCommonMixins() throws IOException {
        JsonObject config = read("/corruptedadventure.common.mixins.json");
        assertEquals("nl.timtoys.corruptedadventure.mixin", config.get("package").getAsString());
        assertFalse(config.has("client"));
        assertFalse(config.has("server"));
        assertEquals(2, config.getAsJsonArray("mixins").size());
        // The client config stays client-only.
        JsonObject client = read("/corruptedadventure.mixins.json");
        assertEquals(0, client.getAsJsonArray("mixins").size());
    }

    @Test
    void theMixinsAreApplied() {
        assertTrue(hasHandler(IceBlock.class, "meltInTheHeat"), "IceBlockMeltMixin not applied");
        assertTrue(hasHandler(WetSpongeBlock.class, "dryInTheHeat"), "WetSpongeBlockDryMixin not applied");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName)
                .anyMatch(method -> method.endsWith("corruptedadventure$" + name));
    }

    private static JsonObject read(String resource) throws IOException {
        try (InputStream in = CommonMixinsTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
