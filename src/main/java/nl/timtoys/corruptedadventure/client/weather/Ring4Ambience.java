package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.client.particle.DeepSilhouetteParticle;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;

/**
 * Ring 4 ambience that biome data cannot give (change 80), client only and per player (not synced, by design, like the
 * rumble of ring 3):
 * <ul>
 *   <li><b>day and night</b> in the mythical band (Q19 = A): birds and frogs by day; by night the soft harp and choir
 *       tones and the night glow, fireflies hanging over the swamp and the lagoon, fewer elsewhere;</li>
 *   <li><b>the reeds</b> of {@code reed_marsh}: rustling and steps somewhere just behind the player, never in view;</li>
 *   <li><b>the silhouette</b> (Q18 = A): rarely, while the camera is under water, a dark, enormous shape far away in
 *       water at least 20 deep, a few seconds long; at most one per {@link #SILHOUETTE_COOLDOWN} per player;</li>
 *   <li><b>glitch specks</b> across zone 4→5, from nothing at its inner edge to the most at its outer edge (Q20 = A: no
 *       flickering sky in phase 1).</li>
 * </ul>
 */
public final class Ring4Ambience {
    /** Pauses in ticks: birds and frogs by day, the choir by night, the reeds. */
    static final int WILDLIFE_MIN = 160;
    static final int WILDLIFE_MAX = 400;
    static final int CHOIR_MIN = 600;
    static final int CHOIR_MAX = 1800;
    static final int RUSTLE_MIN = 120;
    static final int RUSTLE_MAX = 500;
    /** Night once vanilla's sky darkening (0.2 at night to 1 at noon) is below this. */
    static final float NIGHT_LIGHT = 0.4F;
    /** Fireflies per tick around the camera at night: swamp and lagoon most (design: "vooral moeras en lagune"). */
    static final float GLOW_SWAMP = 0.5F;
    static final float GLOW_WATER = 0.25F;
    static final float GLOW_ELSEWHERE = 0.06F;
    /** Glitch specks per tick at the outer edge of zone 4→5. */
    static final float GLITCH_MAX = 0.8F;
    /** At most one silhouette per 20 minutes; while eligible, about one per two minutes under water. */
    static final int SILHOUETTE_COOLDOWN = 24000;
    static final float SILHOUETTE_CHANCE = 1.0F / 2400.0F;
    /**
     * How deep the water must be where the silhouette shows, and how far away it is: far, but within vanilla's water
     * fog (96 × water vision, at least ~24 blocks right after diving), so it is never spent unseen.
     */
    static final int SILHOUETTE_MIN_DEPTH = 20;
    private static final double SILHOUETTE_NEAR = 14.0;
    private static final double SILHOUETTE_FAR = 20.0;

    private static final float WILDLIFE_VOLUME = 0.6F;
    private static final float CHOIR_VOLUME = 0.45F;
    private static final float RUSTLE_VOLUME = 0.7F;

    private static int untilWildlife = -1;
    private static int untilChoir = -1;
    private static int untilRustle = -1;
    private static int silhouetteCooldown;

    private Ring4Ambience() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring4Ambience::onClientTick);
        gameBus.addListener(Ring4Ambience::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        untilWildlife = -1;
        untilChoir = -1;
        untilRustle = -1;
        silhouetteCooldown = 0;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        if (silhouetteCooldown > 0) {
            silhouetteCooldown--;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        RingPosition ring = ClientRingLayout.ringAt(level.dimension(), position.x, position.z).orElse(null);
        if (ring == null) {
            return;
        }
        if (ring.inner() == Ring.RING_4 && ring.outer() == Ring.RING_5) {
            WeatherParticles.spawn(level, position, ModParticleTypes.GLITCH_SPECK.get(), GLITCH_MAX * (float) ring.blend(),
                    14.0, 4.0, 10.0, Vec3.ZERO, 0.0);
        }
        if (ring.ring() != Ring.RING_4) {
            return;
        }
        RandomSource random = level.getRandom();
        Holder<Biome> biome = level.getBiome(BlockPos.containing(position));
        if (mythical(biome)) {
            if (RingWeatherRenderer.daylight(level, 1.0F) < NIGHT_LIGHT) {
                nightGlow(level, position, biome);
                untilChoir = countdown(untilChoir, random, CHOIR_MIN, CHOIR_MAX);
                if (untilChoir == 0) {
                    float volume = CHOIR_VOLUME * (0.4F + 0.6F * SkyExposure.get());
                    minecraft.getSoundManager().play(new SimpleSoundInstance(ModSoundEvents.RING_4_NIGHT_CHOIR.get().getLocation(),
                            SoundSource.AMBIENT, volume, 1.0F, SoundInstance.createUnseededRandom(), false, 0,
                            SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
                }
            } else {
                untilWildlife = countdown(untilWildlife, random, WILDLIFE_MIN, WILDLIFE_MAX);
                if (untilWildlife == 0) {
                    playAround(minecraft, ModSoundEvents.VERDANT_FALLS_WILDLIFE.get(), position, random, 8.0, 16.0, null,
                            WILDLIFE_VOLUME * SkyExposure.get());
                }
            }
        }
        if (biome.is(ModBiomes.REED_MARSH)) {
            untilRustle = countdown(untilRustle, random, RUSTLE_MIN, RUSTLE_MAX);
            if (untilRustle == 0) {
                // Behind the player: heard, never seen.
                Vec3 look = Vec3.directionFromRotation(0.0F, camera.getYRot());
                playAround(minecraft, ModSoundEvents.RING_4_REEDS.get(), position, random, 3.0, 6.0, look.reverse(), RUSTLE_VOLUME);
            }
        }
        if (silhouetteCooldown == 0 && camera.getFluidInCamera() == FogType.WATER && random.nextFloat() < SILHOUETTE_CHANCE
                && showSilhouette(minecraft, level, camera, random)) {
            silhouetteCooldown = SILHOUETTE_COOLDOWN;
        }
    }

    private static boolean mythical(Holder<Biome> biome) {
        return biome.is(ModBiomes.SKY_GARDENS) || Ring4Biomes.MYTHIC.stream().anyMatch(biome::is);
    }

    private static void nightGlow(ClientLevel level, Vec3 position, Holder<Biome> biome) {
        float rate = biome.is(ModBiomes.GLOWROOT_SWAMP) ? GLOW_SWAMP
                : biome.is(ModBiomes.CRYSTAL_LAGOON) || biome.is(ModBiomes.LOTUS_SHALLOWS) ? GLOW_WATER : GLOW_ELSEWHERE;
        ParticleOptions firefly = ModParticleTypes.FIREFLY.get();
        WeatherParticles.spawn(level, position, firefly, rate, 16.0, 3.0, 6.0, Vec3.ZERO, 0.0);
    }

    /** Counts {@code ticks} down; draws a new pause when it is not drawn yet or has just run out. 0 = now. */
    private static int countdown(int ticks, RandomSource random, int min, int max) {
        if (ticks <= 0) {
            return min + random.nextInt(max - min + 1);
        }
        return ticks - 1;
    }

    /**
     * A positional one-shot {@code near}–{@code far} blocks away at about ear height, in a random direction or, with
     * {@code towards}, within ±60° of it.
     */
    private static void playAround(Minecraft minecraft, SoundEvent sound, Vec3 position, RandomSource random, double near,
                                   double far, Vec3 towards, float volume) {
        if (volume <= 0.0F) {
            return;
        }
        double angle = towards == null ? random.nextDouble() * Math.PI * 2
                : Math.atan2(towards.z, towards.x) + (random.nextDouble() - 0.5) * Math.toRadians(120.0);
        double distance = near + random.nextDouble() * (far - near);
        double x = position.x + Math.cos(angle) * distance;
        double z = position.z + Math.sin(angle) * distance;
        double y = position.y - 1.0 + random.nextDouble() * 2.0;
        minecraft.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume,
                0.9F + random.nextFloat() * 0.2F, SoundInstance.createUnseededRandom(), x, y, z));
    }

    /**
     * Tries one spot ahead of the camera ({@code SILHOUETTE_NEAR}–{@code SILHOUETTE_FAR} away, within ±60° of the view)
     * where the water is at least {@link #SILHOUETTE_MIN_DEPTH} deep, and shows the shape in its lower half.
     */
    private static boolean showSilhouette(Minecraft minecraft, ClientLevel level, Camera camera, RandomSource random) {
        Vec3 position = camera.getPosition();
        double angle = Math.toRadians(camera.getYRot() + 90.0) + (random.nextDouble() - 0.5) * Math.toRadians(120.0);
        double distance = SILHOUETTE_NEAR + random.nextDouble() * (SILHOUETTE_FAR - SILHOUETTE_NEAR);
        int x = Mth.floor(position.x + Math.cos(angle) * distance);
        int z = Mth.floor(position.z + Math.sin(angle) * distance);
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            return false;
        }
        // The water column there: from the camera's height down to water (a bank in between), then up to its surface,
        // so the depth is the column's, not what lies below the diver.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Mth.floor(position.y), z);
        while (cursor.getY() > level.getMinBuildHeight() && !level.getFluidState(cursor).is(FluidTags.WATER)) {
            cursor.move(0, -1, 0);
            if (position.y - cursor.getY() > 16) {
                return false;
            }
        }
        while (cursor.getY() < level.getMaxBuildHeight() - 1 && level.getFluidState(cursor.above()).is(FluidTags.WATER)) {
            cursor.move(0, 1, 0);
        }
        int top = cursor.getY();
        int depth = 0;
        while (cursor.getY() > level.getMinBuildHeight() && level.getFluidState(cursor).is(FluidTags.WATER)) {
            cursor.move(0, -1, 0);
            depth++;
        }
        if (depth < SILHOUETTE_MIN_DEPTH) {
            return false;
        }
        double y = top - depth * (0.55 + random.nextDouble() * 0.3);
        double drift = angle + Math.PI / 2 * (random.nextBoolean() ? 1 : -1);
        minecraft.particleEngine.add(new DeepSilhouetteParticle(level, x + 0.5, y, z + 0.5, Math.cos(drift), Math.sin(drift)));
        return true;
    }
}
