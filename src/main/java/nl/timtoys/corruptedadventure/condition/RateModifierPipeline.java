package nl.timtoys.corruptedadventure.condition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The registered {@link RateModifier}s in pipeline order: by {@link RateModifier.Stage}, within a stage in
 * registration order. Registration happens during mod setup, reading on every condition tick, so reads get an
 * immutable snapshot.
 */
final class RateModifierPipeline {
    private final List<Entry> entries = new ArrayList<>();
    private volatile List<RateModifier> ordered = List.of();

    synchronized void add(RateModifier.Stage stage, RateModifier modifier) {
        entries.add(new Entry(stage, entries.size(), modifier));
        ordered = entries.stream()
                .sorted(Comparator.comparing(Entry::stage).thenComparingInt(Entry::sequence))
                .map(Entry::modifier)
                .toList();
    }

    List<RateModifier> ordered() {
        return ordered;
    }

    private record Entry(RateModifier.Stage stage, int sequence, RateModifier modifier) {
    }
}
