package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.function.Supplier;

/**
 * A timed effect from a consumable that multiplies one condition's <b>rising</b> rate (fase-2-items.md §5.3). Effects of
 * the same kind do not stack: a new dose only sets the time again. The factor comes from the SERVER config, read live.
 */
public enum RateEffect implements StringRepresentable {
    /** Canteen and cactus juice: dehydration rises at {@code cooledFactor} (0.5). */
    COOLED("cooled", Condition.DEHYDRATION, () -> ServerConfig.DEHYDRATION.cooledFactor.get().floatValue());

    private final String name;
    private final Condition condition;
    private final Supplier<Float> factor;

    RateEffect(String name, Condition condition, Supplier<Float> factor) {
        this.name = name;
        this.condition = condition;
        this.factor = factor;
    }

    public Condition condition() {
        return condition;
    }

    public float factor() {
        return factor.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
