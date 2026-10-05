package nl.timtoys.corruptedadventure.client.ambient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * The singing sand: a low hum from the dunes, mostly at night. Only in the {@code dune_sea} biome and only under open
 * sky (not in the caves below), a random point 8-16 blocks around the player so it comes from a changing direction.
 * Ambience like vanilla's: per player, on the client's own random, not synced, so two players hear it at different moments.
 */
public final class SingingSandAmbience {
    private static final int MIN_DISTANCE = 8;
    private static final int MAX_DISTANCE = 16;
    /** Ticks until a hum may sound again. Not reset on leaving the biome: walking in and out gives no extra hums. */
    private static int cooldown;

    private SingingSandAmbience() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.getBiome(head).is(ModBiomes.DUNE_SEA) || !level.canSeeSky(head)) {
            return;
        }
        RandomSource random = level.random;
        float chance = SingingSandTiming.chancePerTick(SingingSandTiming.nightFactor(level.getTimeOfDay(1.0F)));
        if (random.nextFloat() >= chance) {
            return;
        }
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        double x = player.getX() + Math.cos(angle) * distance;
        double y = player.getEyeY() + random.nextInt(5) - 2;
        double z = player.getZ() + Math.sin(angle) * distance;
        // Pitch and volume come from sounds.json (volume 0.8) and this small spread: the files differ already.
        minecraft.getSoundManager().play(new SimpleSoundInstance(ModSoundEvents.RING_2_SINGING_SAND.get(), SoundSource.AMBIENT,
                0.9F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.2F, random, x, y, z));
        cooldown = SingingSandTiming.COOLDOWN_TICKS;
    }
}
