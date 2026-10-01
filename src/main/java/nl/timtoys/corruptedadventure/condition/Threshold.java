package nl.timtoys.corruptedadventure.condition;

/** One threshold of a condition: reached when the meter is at least {@code level}. */
public record Threshold(int level, ThresholdEffect effect) {
}
