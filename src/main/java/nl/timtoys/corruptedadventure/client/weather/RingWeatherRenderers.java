package nl.timtoys.corruptedadventure.client.weather;

import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renderer per weather type. Filled from {@code CorruptedAdventureClient} during mod construction (empty in change 20).
 */
public final class RingWeatherRenderers {
    private static final Map<RingWeatherType, RingWeatherRenderer> RENDERERS = new EnumMap<>(RingWeatherType.class);

    private RingWeatherRenderers() {
    }

    public static void register(RingWeatherType type, RingWeatherRenderer renderer) {
        if (type == RingWeatherType.CLEAR) {
            throw new IllegalArgumentException("clear has nothing to render");
        }
        if (RENDERERS.putIfAbsent(type, renderer) != null) {
            throw new IllegalStateException("Second renderer for " + type.getSerializedName());
        }
    }

    static RingWeatherRenderer get(RingWeatherType type) {
        return RENDERERS.get(type);
    }

    static boolean isEmpty() {
        return RENDERERS.isEmpty();
    }
}
