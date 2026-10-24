package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A memo of pure plans of the ring 5 dressing (the memories, their shapes, the mirrored pairs): per {@link Ring5World} by
 * identity and weakly (a closed world's plans go with it; two worlds of one seed in one session never share any), and per
 * world the last {@code size} keys. The plan is worked out outside the lock: racing threads work out the same one.
 */
final class Ring5PlanCache<K, V> {
    private final Map<Ring5World, Map<K, Optional<V>>> worlds = new MapMaker().weakKeys().makeMap();
    private final int size;

    Ring5PlanCache(int size) {
        this.size = size;
    }

    /** The plan of {@code key} in {@code world} (null allowed: no plan there). */
    V get(Ring5World world, K key, Function<K, V> plan) {
        Map<K, Optional<V>> known = worlds.computeIfAbsent(world, w -> Collections.synchronizedMap(lru(size)));
        Optional<V> value = known.get(key);
        if (value == null) {
            value = Optional.ofNullable(plan.apply(key));
            Optional<V> raced = known.putIfAbsent(key, value);
            value = raced != null ? raced : value;
        }
        return value.orElse(null);
    }

    private static <K, V> Map<K, V> lru(int size) {
        return new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > size;
            }
        };
    }
}
