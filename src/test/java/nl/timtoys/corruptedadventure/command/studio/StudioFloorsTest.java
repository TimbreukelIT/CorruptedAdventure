package nl.timtoys.corruptedadventure.command.studio;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudioFloorsTest {
    @Test
    void twoStoreysWithAFlatRoof() {
        // 7 x 7 (49 columns, all from 64 to the roof at 72): floor slab at 64, walls (24), second floor at 68, flat roof.
        int[] solid = {49, 24, 24, 24, 49, 24, 24, 24, 0};
        int[] spanning = {49, 49, 49, 49, 49, 49, 49, 49, 0};
        assertEquals(List.of(64, 68), StudioFloors.detect(64, solid, spanning, 49));
        assertEquals(List.of(66, 70), StudioFloors.cuts(List.of(64, 68)));
    }

    @Test
    void lowSidePiecesDoNotHideATowersFloors() {
        // A 4 x 4 tower (16 columns, 64..80) with floors at 64, 70 and 75, next to 40 columns of tents and cages
        // (64..66, solid only at 64).
        int[] solid = new int[17];
        int[] spanning = new int[17];
        for (int y = 64; y < 80; y++) {
            spanning[y - 64] = 16 + (y < 66 ? 40 : 0);
            solid[y - 64] = y == 70 || y == 75 ? 16 : 12;
        }
        solid[0] = 56;
        assertEquals(List.of(64, 70, 75), StudioFloors.detect(64, solid, spanning, 56));
    }

    @Test
    void aThickSlabIsOneFloorWalkedOnAtItsTop() {
        int[] solid = {49, 49, 24, 24, 24, 10, 0};
        int[] spanning = {49, 49, 49, 49, 49, 20, 0};
        assertEquals(List.of(65), StudioFloors.detect(64, solid, spanning, 49));
    }

    @Test
    void aLonePillarIsNoFloorAndWithoutFloorsTheLowestLayerCounts() {
        // 30 columns of a low wall (60..61) and one pillar to 70: the pillar is solid all the way but too small.
        int[] solid = new int[11];
        int[] spanning = new int[11];
        solid[0] = 10;
        spanning[0] = 30;
        for (int y = 1; y < 10; y++) {
            solid[y] = 1;
            spanning[y] = 1;
        }
        assertEquals(List.of(60), StudioFloors.detect(60, solid, spanning, 31));
        assertEquals(List.of(), StudioFloors.detect(60, new int[0], new int[0], 0));
    }

    @Test
    void floorListsParseAndSort() throws CommandSyntaxException {
        assertEquals(List.of(64, 69, 73), StudioCommand.parseFloors("69, 64 73"));
        assertThrows(CommandSyntaxException.class, () -> StudioCommand.parseFloors("64,up"));
    }
}
