package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lining of the giant geodes (78, N3 = A) on a box of pure kinds: a geode cut into solid rock, as the terrain digs it.
 * The floor stays unlined (bone sand, walkable), the walls and dome get three layers, a cluster never stands on the floor.
 */
class GiantGeodeLiningFeatureTest {
    private static final GiantGeodeShape.Settings SETTINGS = GiantGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE,
            WorldgenData.read("density_function/ring_4/nether/geodes.json").getAsJsonObject("settings")).getOrThrow();
    private static final GiantGeodeShape.CellHash HASH = (x, z, salt) -> x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 31L;

    @Test
    void floorStaysBareWallsAndDomeAreLined() {
        for (int attempt = 0; attempt < 12; attempt++) {
            GiantGeodeShape.Geode bare = GiantGeodeShape.candidate(SETTINGS, HASH, attempt, 2, attempt);
            GiantGeodeShape.Geode geode = bare.withDoors(List.of(new GiantGeodeShape.Door(1.0, 0.0, bare.wall() + 10.0)));
            int reach = (int) Math.ceil(geode.wall()) + 6;
            int x0 = geode.x() - reach;
            int z0 = geode.z() - reach;
            int y0 = geode.floor() - 1;
            int nx = 2 * reach + 1;
            int nz = nx;
            int ny = (int) Math.ceil(geode.radius() * geode.stretchY()) + (geode.y() - geode.floor()) + 8;
            byte[] kind = new byte[nx * ny * nz];
            for (int x = 0; x < nx; x++) {
                for (int y = 0; y < ny; y++) {
                    for (int z = 0; z < nz; z++) {
                        int wx = x0 + x, wy = y0 + y, wz = z0 + z;
                        boolean hollow = wy >= geode.floor() && geode.hollow(wx, wy, wz);
                        kind[(x * ny + y) * nz + z] = GiantGeodeLiningFeature.classify(geode, wx, wy, wz, hollow, !hollow);
                    }
                }
            }
            byte[] layer = FireGeodeFeature.layers(kind, nx, ny, nz);
            int lined = 0;
            for (int x = 0; x < nx; x++) {
                for (int z = 0; z < nz; z++) {
                    // y index 0 is the floor block (Y floor − 1): never lined
                    assertEquals(0, layer[(x * ny) * nz + z], "floor block lined");
                    for (int y = 1; y < ny; y++) {
                        int i = (x * ny + y) * nz + z;
                        if (layer[i] == 1) {
                            lined++;
                        }
                        if (kind[i] == FireGeodeFeature.OPEN) {
                            Direction wall = GiantGeodeLiningFeature.wall(layer, x, y, z, nx, ny, nz);
                            if (wall == Direction.DOWN) {
                                assertTrue(y > 1, "a cluster never stands on the floor");
                            }
                        }
                    }
                }
            }
            assertTrue(lined > 200, "the dome is lined: " + lined);
            // the top of the dome: three layers above the hollow's highest open block at the centre
            int cx = geode.x() - x0;
            int cz = geode.z() - z0;
            int top = 0;
            for (int y = 0; y < ny; y++) {
                if (kind[(cx * ny + y) * nz + cz] == FireGeodeFeature.OPEN) {
                    top = y;
                }
            }
            for (int k = 1; k <= FireGeodeFeature.LAYERS && top + k < ny; k++) {
                assertEquals(k, layer[(cx * ny + top + k) * nz + cz], "layer " + k + " over the dome");
            }
            // the doorway past its mouth is not lined: a block beside it, well outside the dome
            int far = (int) Math.ceil(geode.wall()) + 6;
            int side = geode.z() + geode.doorWidth() / 2 + 1 - z0;
            if (far < nx - geode.x() + x0 && side < nz) {
                assertNotEquals(1, layer[((geode.x() + far - x0) * ny + 2) * nz + side], "doorway walls past the mouth stay rock");
            }
        }
    }
}
