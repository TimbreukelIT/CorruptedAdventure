package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Particle types. Registered on both sides (the registry is synced); how they look is client code
 * ({@code client/particle/}, providers in {@code CorruptedAdventureClient}) plus {@code particles/<name>.json} from
 * datagen.
 */
public final class ModParticleTypes {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, CorruptedAdventure.MOD_ID);

    /** Sand blown by the ring 2 sandstorm (change 28); vanilla {@code generic_*} sprites until change 37. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SAND_GUST =
            PARTICLE_TYPES.register("sand_gust", () -> new SimpleParticleType(false));

    private ModParticleTypes() {
    }

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
