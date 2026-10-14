package nl.timtoys.corruptedadventure.client.weather;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * DEBUG line whenever the weather felt at the camera changes: the types, their strength in steps of 0.25 and the
 * wind of the sandstorm and of ring 4's storm. Two clients at the same spot log the same lines, which is how test S compares them without eyes.
 */
final class RingWeatherDebugLog {
    private static String last = "";

    private RingWeatherDebugLog() {
    }

    static void update(String player, List<RingWeatherMix.Share> shares) {
        String now = describe(shares);
        if (!now.equals(last)) {
            last = now;
            CorruptedAdventure.LOGGER.debug("[ring weather] {}: {}", player, now);
        }
    }

    /** E.g. {@code ring_2 sandstorm strength 0.75 wind 225° (from SW)}, or {@code none}. */
    static String describe(List<RingWeatherMix.Share> shares) {
        String text = shares.stream()
                .filter(share -> share.state().type() != RingWeatherType.CLEAR && share.strength() > 0.0F)
                .map(share -> {
                    String line = String.format(Locale.ROOT, "%s %s strength %.2f", share.ring().getSerializedName(),
                            share.state().type().getSerializedName(), Math.floor(share.strength() * 4.0F) / 4.0F);
                    return SandstormWind.hasWind(share.state().type())
                            ? line + " wind " + SandstormWind.describe(share.state()) : line;
                })
                .collect(Collectors.joining(" | "));
        return text.isEmpty() ? "none" : text;
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        last = "";
    }
}
