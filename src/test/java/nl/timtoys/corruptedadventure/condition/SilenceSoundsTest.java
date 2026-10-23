package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sounds that break the silence (package 97): note block, instrument (goat horn), playing jukebox, and a block
 * change only at a bell; heard within 16 blocks (3D); the relief lasts {@code soundReliefSeconds}.
 */
class SilenceSoundsTest {
    private static final int RELIEF_TICKS = 10 * 20;

    @Test
    void theFourSoundsBreakTheSilence() {
        for (Holder<GameEvent> event : List.of(GameEvent.NOTE_BLOCK_PLAY, GameEvent.INSTRUMENT_PLAY,
                GameEvent.JUKEBOX_PLAY)) {
            assertTrue(SilenceSounds.breaksSilence(event, () -> false), event.getRegisteredName());
        }
        assertTrue(SilenceSounds.breaksSilence(GameEvent.BLOCK_CHANGE, () -> true), "a bell rings");
    }

    @Test
    void aBlockChangeCountsOnlyAtABellAndTheBellIsOnlyLookedUpThen() {
        assertFalse(SilenceSounds.breaksSilence(GameEvent.BLOCK_CHANGE, () -> false), "a door or lever");
        AtomicBoolean asked = new AtomicBoolean();
        for (Holder<GameEvent> event : List.of(GameEvent.STEP, GameEvent.BLOCK_PLACE, GameEvent.EXPLODE,
                GameEvent.JUKEBOX_STOP_PLAY, GameEvent.ENTITY_ACTION, GameEvent.SHRIEK)) {
            assertFalse(SilenceSounds.breaksSilence(event, () -> {
                asked.set(true);
                return true;
            }), event.getRegisteredName());
        }
        assertFalse(asked.get(), "the block is only looked up for a block change");
    }

    @Test
    void heardWithinSixteenBlocks() {
        Vec3 sound = new Vec3(2100.5, 40.5, 0.5);
        assertTrue(SilenceSounds.inRange(sound, sound.add(16, 0, 0), 16));
        assertTrue(SilenceSounds.inRange(sound, sound.add(0, -16, 0), 16), "also through floor and ceiling");
        assertFalse(SilenceSounds.inRange(sound, sound.add(16.01, 0, 0), 16));
        assertFalse(SilenceSounds.inRange(sound, sound.add(12, 0, 12), 16), "3D, not per axis");
    }

    @Test
    void theReliefLastsTenSecondsAfterTheLastSound() {
        assertFalse(SilenceSounds.relieved((Integer) null, 1000, RELIEF_TICKS), "nothing heard");
        assertTrue(SilenceSounds.relieved(1000, 1000, RELIEF_TICKS));
        assertTrue(SilenceSounds.relieved(1000, 1000 + RELIEF_TICKS, RELIEF_TICKS));
        assertFalse(SilenceSounds.relieved(1000, 1000 + RELIEF_TICKS + 1, RELIEF_TICKS));
    }
}
