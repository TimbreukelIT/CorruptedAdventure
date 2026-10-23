package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The echo footsteps of ring 5 (package 98, V98-6 A, design "horror-elementen"): your own steps sound again a little
 * later, behind you, as if someone follows you. Client only and per player (nothing synced, nobody else hears it). The
 * client's own step sounds are recognised as the local player plays them ({@code LocalPlayer#playSound} fires
 * {@link PlayLevelSoundEvent.AtEntity} for itself; a {@code *.step} sound of the {@code PLAYERS} category); the echo replays that same sound (the block's own step,
 * V98-6 a), a little lower, from behind the way the player looks at the moment it plays. When it comes is
 * {@link EchoSteps}. In both bands, in caves and in {@code false_meadow} (ring 5's weight at the camera &gt; ½).
 */
public final class EchoFootsteps {
    /** Ring 5 counts from this weight at the camera (the middle of zone 4→5, like the ring weather's gameplay switch). */
    static final float IN_RING_5 = 0.5F;
    /** Horizontal speed² per tick above which an on-ground player walks. */
    private static final double WALKING = 1.0E-4;

    private static final EchoSteps STEPS = new EchoSteps();
    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static Pending last;

    private EchoFootsteps() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EchoFootsteps::onClientTick);
        gameBus.addListener(EchoFootsteps::onPlaySound);
        gameBus.addListener(EchoFootsteps::onLoggingOut);
    }

    private static boolean active(ClientLevel level) {
        return level.dimension() == Level.OVERWORLD && Ring5Sky.ring5() > IN_RING_5;
    }

    private static void onPlaySound(PlayLevelSoundEvent.AtEntity event) {
        // Also fired by the integrated server for its players, on its own thread: only the local player counts.
        if (!(event.getEntity() instanceof LocalPlayer) || !(event.getLevel() instanceof ClientLevel level)
                || event.getSound() == null || event.getSource() != SoundSource.PLAYERS || !active(level)) {
            return;
        }
        Holder<SoundEvent> sound = event.getSound();
        if (!sound.value().getLocation().getPath().endsWith(".step")) {
            return;
        }
        long now = level.getGameTime();
        Pending step = new Pending(STEPS.step(now), sound, event.getNewVolume(), event.getNewPitch());
        last = step;
        if (step.due() != EchoSteps.NONE) {
            PENDING.addLast(step);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        if (!active(level)) {
            STEPS.reset();
            PENDING.clear();
            return;
        }
        long now = level.getGameTime();
        boolean walking = player.onGround() && !player.isPassenger() && player.getDeltaMovement().horizontalDistanceSqr() > WALKING;
        long extra = STEPS.tick(now, walking, level.getRandom());
        if (extra != EchoSteps.NONE && last != null) {
            PENDING.addLast(new Pending(extra, last.sound(), last.volume(), last.pitch()));
        }
        while (!PENDING.isEmpty() && PENDING.peekFirst().due() <= now) {
            play(minecraft, player, PENDING.removeFirst(), level.getRandom());
        }
    }

    private static void play(Minecraft minecraft, LocalPlayer player, Pending step, RandomSource random) {
        // Behind the way the player looks now, at his feet's height: the follower keeps his distance.
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());
        double behind = STEPS.behind();
        Vec3 at = player.position().subtract(look.scale(behind));
        SoundInstance echo = new SimpleSoundInstance(step.sound().value(), SoundSource.PLAYERS, STEPS.volume(step.volume()),
                step.pitch() * EchoSteps.PITCH, random, at.x, at.y, at.z);
        minecraft.getSoundManager().play(echo);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STEPS.reset();
        PENDING.clear();
        last = null;
    }

    /** A step to echo at tick {@code due}: its sound, volume and pitch as the player heard it. */
    private record Pending(long due, Holder<SoundEvent> sound, float volume, float pitch) {
    }
}
