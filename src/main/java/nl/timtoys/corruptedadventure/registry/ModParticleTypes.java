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
    /** Falling ash of the ring 3 weather (package 45); sprites {@code ash_flake_0..3} from stream T. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH_FLAKE =
            PARTICLE_TYPES.register("ash_flake", () -> new SimpleParticleType(false));
    /** Glowing spark of an eruption and an ash cloud (package 45); sprites {@code ember_0..3} from stream T. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER =
            PARTICLE_TYPES.register("ember", () -> new SimpleParticleType(false));

    /** Puff of steam (package 50-0): fumaroles, {@code sulfur_fields}, {@code steam_fens}; sprites {@code steam_puff_0..3} from stream T. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM_PUFF =
            PARTICLE_TYPES.register("steam_puff", () -> new SimpleParticleType(false));
    /** Drifting ice crystal of Nether ring 3 (package 50-0); sprites {@code ice_glint_0..3} from stream T. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_GLINT =
            PARTICLE_TYPES.register("ice_glint", () -> new SimpleParticleType(false));

    // Ambient variants for the ring 3 biomes (package 50): a biome's ambient particle comes without speed, so these move
    // by themselves; same sprites as their base type.
    /** Ash drifting down ({@code ash_flake} sprites). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRIFTING_ASH =
            PARTICLE_TYPES.register("drifting_ash", () -> new SimpleParticleType(false));
    /** Sparks rising ({@code ember} sprites). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RISING_EMBER =
            PARTICLE_TYPES.register("rising_ember", () -> new SimpleParticleType(false));
    /** Yellow-green steam of the sulfur fields ({@code steam_puff} sprites). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SULFUR_STEAM =
            PARTICLE_TYPES.register("sulfur_steam", () -> new SimpleParticleType(false));

    /** Fine white spray drifting through {@code verdant_falls} (its biome particle); sprites {@code waterfall_mist_0..3} from stream T. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATERFALL_MIST =
            PARTICLE_TYPES.register("waterfall_mist", () -> new SimpleParticleType(false));

    private ModParticleTypes() {
    }

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
