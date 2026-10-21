package nl.timtoys.corruptedadventure.command;

import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CastlesCommandTest {
    private static final List<Ring5Field.CastleSite> SITES = List.of(
            new Ring5Field.CastleSite(0, 12.25, 9064, 1968, 48, 131, 60),
            new Ring5Field.CastleSite(1, 99.0, -1451, 9161, 40, 120, 80),
            new Ring5Field.CastleSite(2, 190.5, -9119, -1691, 56, 140, 40),
            new Ring5Field.CastleSite(3, 281.0, 1770, -9105, 41, 127, 55));

    @Test
    void linesNumberTheSitesFromOneWithDistanceFromTheSource() {
        assertEquals("#1 12.3° 9064 1968 r=48 top=131 belt=60 d=9275", CastlesCommand.line(SITES.getFirst(), 0.0, 0.0));
        assertEquals("#3 190.5° -9119 -1691 r=56 top=140 belt=40 d=0", CastlesCommand.line(SITES.get(2), -9119, -1691));
        String text = CastlesCommand.describe(SITES, 0.0, 0.0);
        assertEquals(5, text.split("\n").length);
        assertEquals("Castle sites: 4 (# angle x z radius top belt distance)", text.split("\n")[0]);
    }

    @Test
    void tpTargetsTheSiteOfItsNumber() {
        for (int number = 1; number <= 4; number++) {
            assertSame(SITES.get(number - 1), CastlesCommand.site(SITES, number));
        }
        assertNull(CastlesCommand.site(SITES, 5));
        assertNull(CastlesCommand.site(List.of(), 1));
    }
}
