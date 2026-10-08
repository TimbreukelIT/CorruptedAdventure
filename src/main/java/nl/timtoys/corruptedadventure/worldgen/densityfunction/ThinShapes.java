package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/**
 * The thin shapes of a ring 3 column (split out of {@link VolcanicField}, 39d): hoodoos, column prisms, lava tube tunnels,
 * sea stacks, titan bones, archipelago pillars, floating volcanoes, needles, arches and abyss bridges, as solid and
 * void intervals of the column (outside the terrain interpolation, output {@code thin}).
 */
final class ThinShapes {
    private ThinShapes() {
    }

    /** Hoodoos, column prisms, lava tube tunnels, sea stacks and titan bones (solid and void intervals of a column). */
    static void thinShapes(VolcanicField f, VolcanicField.Column c, double px, double pz, VolcanicField.Regions r, double keep, double gullyFloor, double plateau,
                            double tube, double height) {
        long salt = f.salt();
        double[] span = c.span;
        if (gullyFloor > 0.85 && CellShapes.hoodoo(salt, px, pz, span)) {
            c.addSolid(height + span[0], height + span[1] * keep);
            c.cliff = Math.max(c.cliff, 0.6);
        }
        if (r.flowsS() > 0.5 && c.cliff > 0.0) {
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
            c.voidBottom = Mth.floor(centre - half);
            c.voidTop = Mth.floor(centre + half);
        }
        if (r.seabedS() > 0.7) {
            double stack = CellShapes.seaStack(salt, px, pz);
            if (stack > 0.0) {
                c.addSolid(height - 3.0, height + stack * r.seabedS());
                c.cliff = 1.0;
            }
        }
        if (r.titanS() > 0.0) {
            TitanBones.Rib rib = f.rib(Math.floorDiv(Mth.floor(px), TitanBones.RIB_CELL), Math.floorDiv(Mth.floor(pz), TitanBones.RIB_CELL));
            if (rib != null && rib.interval(px, pz, span)) {
                c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BONE);
            }
            TitanBones.Cage cage = f.cage(Math.floorDiv(Mth.floor(px), TitanBones.CAGE_CELL), Math.floorDiv(Mth.floor(pz), TitanBones.CAGE_CELL));
            if (cage != null && Math.abs(px - cage.x()) < cage.reach() && Math.abs(pz - cage.z()) < cage.reach()) {
                for (TitanBones.Rib cageRib : cage.ribs()) {
                    if (cageRib.interval(px, pz, span)) {
                        c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BONE);
                        break;
                    }
                }
                if (TitanBones.vertebra(cage, px, pz, span)) {
                    c.addSolid(height + span[0], height + span[1], Landmarks.KIND_BONE);
                }
            }
        }
    }

    /** Floating volcanoes, archipelago pillars, needles, arches over rivers and rifts, bridges over the abyss. */
    static void landmarks(VolcanicField f, VolcanicField.Column c, int x, int z, double px, double pz, VolcanicField.Regions r, double height, Volcano[] around,
                           @Nullable Landmarks.Abyss abyss, double chasm, double warpX) {
        double[] span = c.span;
        for (Volcano v : around) {
            if (v == null || v.pillars().isEmpty() || !f.islandClear(v.islands().get(0))) {
                continue;
            }
            for (Volcano.Pillar pillar : v.pillars()) {
                double dx = px - pillar.x();
                double dz = pz - pillar.z();
                if (dx * dx + dz * dz <= pillar.radius() * pillar.radius()) {
                    c.addSolid(height - 2.0, pillar.top(), Landmarks.KIND_BASALT);
                }
            }
        }
        if (r.core() > 0.0) {
            Landmarks.FloatingVolcano fv = f.floating(Math.floorDiv(x, Landmarks.FLOATING_CELL), Math.floorDiv(z, Landmarks.FLOATING_CELL));
            if (fv != null && Math.abs(px - fv.x()) < fv.radius() + 4.0 && Math.abs(pz - fv.z()) < fv.radius() + 4.0) {
                double d = Math.hypot(px - fv.x(), pz - fv.z()) / fv.radius() + 0.05 * warpX;
                if (d < 1.0) {
                    c.addIslandInterval(fv.topAt(d), fv.bottomAt(d), d);
                    if (d * fv.radius() < fv.crater() && !c.hasLava()) {
                        c.lavaTop = Mth.floor(fv.lakeTop());
                        c.lavaBottom = Mth.floor(fv.craterFloor()) - 2;
                    }
                    for (int k = 0; k < fv.springX().length; k++) {
                        if (fv.springX()[k] == x && fv.springZ()[k] == z) {
                            c.setSpring(fv.springY()[k] + 1);
                        }
                    }
                }
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
                c.addSolid(span[0], span[1], Landmarks.KIND_BASALT);
            }
        }
        if (r.core() > 0.0) {
            for (Landmarks.Arch arch : f.tileFor(px, pz).arches()) {
                if (arch.interval(px, pz, span)) {
                    c.addSolid(span[0], span[1], Landmarks.KIND_BASALT);
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
