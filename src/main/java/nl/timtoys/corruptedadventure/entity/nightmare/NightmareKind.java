package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.Locale;
import java.util.Optional;

/**
 * The five Nightmares, one per overworld ring (fase-4-nightmares-concept.md). A kind is known by its entity's registry
 * name; a kind whose entity is not registered yet ({@link #type()} empty) is skipped by the director and the commands.
 * A ring stream signs its Nightmare up with one line in the mod constructor, e.g.
 * {@code NightmareKind.LAYERED.arrival(new LayeredArrival());}; until then it comes with {@link DefaultNightmareArrival}.
 */
public enum NightmareKind implements StringRepresentable {
    HOLLOW(Ring.RING_1, "the_hollow"),
    LAYERED(Ring.RING_2, "the_layered"),
    RISEN(Ring.RING_3, "the_risen"),
    LOST_KEEPER(Ring.RING_4, "the_lost_keeper"),
    UNFINISHED(Ring.RING_5, "the_unfinished");

    private static final NightmareKind[] VALUES = values();

    private final Ring ring;
    private final ResourceLocation entityId;
    private final String serializedName = name().toLowerCase(Locale.ROOT);
    // Set from the mod constructor, before any server thread reads it.
    private volatile NightmareArrival arrival = DefaultNightmareArrival.INSTANCE;

    NightmareKind(Ring ring, String entityName) {
        this.ring = ring;
        this.entityId = CorruptedAdventure.id(entityName);
    }

    /** The kind of a ring; empty for the void. */
    public static Optional<NightmareKind> of(Ring ring) {
        for (NightmareKind kind : VALUES) {
            if (kind.ring == ring) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /** The kind whose entity has this type; empty for any other type. */
    public static Optional<NightmareKind> of(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        for (NightmareKind kind : VALUES) {
            if (kind.entityId.equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public Ring ring() {
        return ring;
    }

    public ResourceLocation entityId() {
        return entityId;
    }

    /**
     * Its entity type, or empty while no stream has registered it. Unchecked: the entity it creates is checked to be a
     * {@link Nightmare} where it is spawned.
     */
    @SuppressWarnings("unchecked")
    public Optional<EntityType<? extends Nightmare>> type() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).map(type -> (EntityType<? extends Nightmare>) type);
    }

    public NightmareArrival arrival() {
        return arrival;
    }

    /** Its own omens and appearance; call once from the mod constructor. */
    public void arrival(NightmareArrival arrival) {
        this.arrival = arrival;
    }

    /** {@code hollow}, {@code layered}, …: the key in the director's saved data. */
    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
