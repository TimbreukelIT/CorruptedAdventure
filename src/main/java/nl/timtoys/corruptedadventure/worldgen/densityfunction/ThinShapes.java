package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/**
 * The thin shapes of a ring 3 column (split out of {@link VolcanicField}, 39d): hoodoos, column prisms, lava tube tunnels,
 * sea stacks, titan bones, floating volcanoes, needles, arches and abyss bridges, and (39f) the rubble, overhangs and
 * shafts of the collapses and the raised blocks of the micro relief, as solid and void intervals of the column (outside
 * the terrain interpolation, output {@code thin}). Each solid has a kind for the surface (output {@code thin_kind}).
 */
final class ThinShapes {
    /** A collapse's shaft goes this far down from its floor, but not below {@link #SHAFT_BOTTOM}. */
    private static final int SHAFT_DEPTH = 40;
    private static final int SHAFT_BOTTOM = 12;
    /** Width (blocks) of the overhanging lip beyond a collapse's rim. */
    private static final double OVERHANG = 3.5;

    private ThinShapes() {
    }

    /** Hoodoos, column prisms, lava tube tunnels, sea stacks and titan bones (solid and void intervals of a column). */
    static void thinShapes(VolcanicField f, VolcanicField.Column c, double px, double pz, VolcanicField.Regions r, double keep, double gullyFloor, double plateau,
                            double tube, double height) {
        long salt = f.salt();
        double[] span = c.span;
        // Inside a collapse (39f) its own rubble, overhangs and shaft take the place of hoodoos and column prisms.
        boolean free = c.collapse <= 0.0;
        if (free && gullyFloor > 0.85 && CellShapes.hoodoo(salt, px, pz, span)) {
            c.addSolid(height + span[0], height + span[1] * keep, Landmarks.KIND_BIOME);
            c.cliff = Math.max(c.cliff, 0.6);
        }
        if (free && r.flowsS() > 0.5 && c.cliff > 0.0) {
            CellShapes.hexagon(salt, px, pz, span);
            double level = f.relief().plateauLevel(span[0], span[1]);
            double frac = level - Math.floor(level);
            if (frac > 0.02 && frac < 0.98) {
                // A hexagonal prism of the riser reaching up to the upper tread: the cliff becomes a wall of columns.
                double top = height - keep * plateau + keep * r.flowsS() * Ring3Relief.PLATEAU_STEP * Math.ceil(level) + span[2];
                if (top > height + 0.5) {
                    c.addSolid(top - 24.0, Math.floor(top), Landmarks.KIND_BASALT);
                }
            }
        }
        if (tube > 0.0) {
            double half = 3.5 * Math.sqrt(tube);
            double centre = height - 10.0;
            int bottom = Mth.floor(centre - half);
            int top = Mth.floor(centre + half);
            // 42b: never through the band around lava (a river over the tunnel would pour into it).
            if (c.guardTop < bottom || c.guardBottom > top) {
                c.addVoid(bottom, top);
            }
        }
        if (r.seabedS() > 0.7) {
            double stack = CellShapes.seaStack(salt, px, pz);
            if (stack > 0.0) {
                c.addSolid(height - 3.0, height + stack * r.seabedS(), Landmarks.KIND_BASALT);
                c.cliff = 1.0;
            }
        }
        if (r.titanS() > 0.0) {
            // 39f: bones take the biome's own surface, crown and flanks, so they blend into the land.
            TitanBones.Rib rib = f.rib(Math.floorDiv(Mth.floor(px), TitanBones.RIB_CELL), Math.floorDiv(Mth.floor(pz), TitanBones.RIB_CELL));
            if (rib != null && rib.interval(px, pz, span)) {
                c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BIOME);
            }
            TitanBones.Cage cage = f.cage(Math.floorDiv(Mth.floor(px), TitanBones.CAGE_CELL), Math.floorDiv(Mth.floor(pz), TitanBones.CAGE_CELL));
            if (cage != null && Math.abs(px - cage.x()) < cage.reach() && Math.abs(pz - cage.z()) < cage.reach()) {
                for (TitanBones.Rib cageRib : cage.ribs()) {
                    if (cageRib.interval(px, pz, span)) {
                        c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BIOME);
                        break;
                    }
                }
                if (TitanBones.vertebra(cage, px, pz, span)) {
                    c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BIOME);
                }
            }
        }
    }

    /**
     * A collapse in this column (39f, normalised distance {@code q} from its centre): rubble on the floor, a shaft down
     * towards the caves, or an overhanging lip just beyond the rim.
     */
    static void collapse(VolcanicField f, VolcanicField.Column c, CellShapes.Collapse collapse, double q, double px, double pz,
                         double height) {
        int floor = Mth.floor(height);
        if (q < 1.0) {
            double outside = collapse.shaftOutside(px, pz);
            int bottom = Math.max(SHAFT_BOTTOM, floor - SHAFT_DEPTH);
            // 42b: the shaft's walls of ring 3 rock (a shell, painted by the surface's cave walls), not vanilla stone.
            int[] shell = {bottom - (int) LavaTubes.SHELL, floor + 1};
            if (outside < 0.0) {
                c.addCave(new int[]{bottom, floor + 1}, null, shell, Integer.MIN_VALUE);
                return;
            }
            if (outside < LavaTubes.SHELL) {
                c.addCave(null, null, shell, Integer.MIN_VALUE);
            }
            if (q < 0.55) {
                // Rubble: blocks of 2 x 2 columns, most flat, some one to three blocks high.
                long h = CellShapes.cell(f.salt(), Math.floorDiv(Mth.floor(px), 2), Math.floorDiv(Mth.floor(pz), 2), CellShapes.RUBBLE);
                double u = CellShapes.unit(h, 0);
                int k = u < 0.5 ? 0 : u < 0.8 ? 1 : u < 0.95 ? 2 : 3;
                if (k > 0) {
                    double top = Math.ceil(height) - 1.0;
                    c.addSolid(top - 1.0, top + k, Landmarks.KIND_RUBBLE);
                }
            }
        } else if (q < 1.0 + OVERHANG / collapse.radius() && collapse.firstStep() >= 6.0 && collapse.overhangs(px, pz)) {
            // The lip (3 blocks) overhangs the first step: open below it down to that step's floor.
            c.addVoid(Mth.floor(height - collapse.firstStep()) + 1, floor - 3);
        }
    }

    /**
     * The micro relief's raised blocks (39f, G2): a clumpy pattern, drawn anew for every block of height, raises columns
     * of a tread by one block, mostly just below the next step, in places the whole tread, elsewhere none; so each step
     * line frays and wanders on its own and neighbouring lines merge and part instead of running parallel. Only on gentle
     * ground without other shapes (the terrain is interpolated; three blocks fill any gap to it). {@code weight} (0..1)
     * thins the raised blocks out towards volcano feet and zone 2→3.
     */
    static void microSteps(VolcanicField f, VolcanicField.Column c, double px, double pz, double height, double weight) {
        if (c.cliff >= 0.05 || c.roughness > 0.0 || c.rise >= 1.0 || c.lavaBank > 0.0 || c.channel > 0.0 || c.collapse > 0.0
                || c.hasLava() || c.solidCount > 0 || c.voidCount > 0) {
            return;
        }
        double frac = height - Math.floor(height);
        if (frac > f.relief().microSteps(px, pz, Mth.floor(height)) + 2.0 * (1.0 - weight)) {
            c.addSolid(Math.floor(height) - 1.0, Math.floor(height) + 1.0, Landmarks.KIND_BIOME);
        }
    }

    /** Floating volcanoes, needles, arches over rivers and rifts, bridges over the abyss. */
    static void landmarks(VolcanicField f, VolcanicField.Column c, int x, int z, double px, double pz, VolcanicField.Regions r, double height,
                           @Nullable Landmarks.Abyss abyss, double chasm, double warpX) {
        double[] span = c.span;
        if (r.core() > 0.0) {
            Landmarks.FloatingVolcano fv = f.floating(Math.floorDiv(x, Landmarks.FLOATING_CELL), Math.floorDiv(z, Landmarks.FLOATING_CELL));
            if (fv != null && Math.abs(px - fv.x()) < fv.reach() && Math.abs(pz - fv.z()) < fv.reach()) {
                double d = fv.distance(px, pz, warpX);
                if (d < 1.0) {
                    c.addIslandInterval(fv.topAt(d), fv.bottomAt(d, Math.atan2(pz - fv.z(), px - fv.x())), d);
                    if (d * fv.radius() < fv.crater() && !c.hasLava()) {
                        c.lavaTop = Mth.floor(fv.lakeTop());
                        c.lavaBottom = Mth.floor(fv.craterFloor()) - 2;
                    }
                }
                c.addFalls(fv.falls());
            }
        }
        if (r.fade() > 0.0) {
            Landmarks.Needle needle = f.needle(Math.floorDiv(x, Landmarks.NEEDLE_CELL), Math.floorDiv(z, Landmarks.NEEDLE_CELL));
            if (needle != null) {
                // A needle on a high plain is shortened to end below the top slide, keeping its point.
                double top = needle.at(px, pz, Math.min(needle.height(), VolcanicField.THIN_TOP - height));
                if (top >= 0.0) {
                    c.addSolid(height - 4.0, height + top, Landmarks.KIND_OBSIDIAN);
                }
            }
            Landmarks.Arch arch = f.riftArch(Math.floorDiv(x, Landmarks.RIFT_ARCH_CELL), Math.floorDiv(z, Landmarks.RIFT_ARCH_CELL));
            if (arch != null && arch.interval(px, pz, span)) {
                c.addSolid(span[0], span[1], Landmarks.KIND_BIOME);
            }
        }
        if (r.core() > 0.0) {
            for (Landmarks.Arch arch : f.tileFor(px, pz).arches()) {
                if (arch.interval(px, pz, span)) {
                    c.addSolid(span[0], span[1], Landmarks.KIND_BIOME);
                }
            }
        }
        if (abyss != null && chasm < 1.2) {
            double along = abyss.along(px, pz);
            for (int k = 0; k < abyss.bridges().length; k++) {
                if (Math.abs(along - abyss.bridges()[k]) <= abyss.bridgeWidth()[k] * 0.5) {
                    double q = Math.min(1.0, chasm);
                    double top = abyss.bridgeY()[k] - 5.0 + 4.0 * (1.0 - q * q);
                    c.addSolid(top - 4.0, top, Landmarks.KIND_BASALT);
                }
            }
        }
    }
}
