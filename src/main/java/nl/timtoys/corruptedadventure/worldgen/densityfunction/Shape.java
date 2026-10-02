package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

/**
 * A rock shape of {@link RockPillars}: drawn once per cell from the cell hash ({@link #draw}), then asked per column
 * for its solid blocks ({@link #column}). All lengths in blocks, heights above the lowest floor of the footprint.
 */
sealed interface Shape permits Shape.Needle, Shape.Boulder, Shape.Fin {
    Codec<Shape> CODEC = Codec.STRING.validate(type -> switch (type) {
                case "needle", "boulder", "fin" -> DataResult.success(type);
                default -> DataResult.error(() -> "Unknown rock shape " + type);
            })
            .dispatch("type", Shape::type, type -> switch (type) {
                case "needle" -> Needle.MAP_CODEC;
                case "boulder" -> Boulder.MAP_CODEC;
                default -> Fin.MAP_CODEC;
            });

    String type();

    /** The random parameters of this shape for one cell (unscaled). */
    Draw draw(long hash, DensityFunction.NoiseHolder noise, int cellX, int cellZ, int cellSize);

    /** Adds the solid intervals of the column at x, z (block centre) to {@code out}. */
    void column(RockPillars.Placed placed, double x, double z, RockPillars.Column out);

    /** Drawn parameters; {@code halfX}/{@code halfZ} bound the footprint around the centre. */
    interface Draw {
        double halfX();

        double halfZ();

        /** Horizontally shrunk by about {@code fit} (≤ 1), to make the footprint fit in the cell. */
        Draw scaled(double fit);

        /** Height × weight, horizontal size × (0.5 + 0.5 · weight). */
        Draw weighted(double weight);
    }

    private static double grow(double weight) {
        return 0.5 + 0.5 * weight;
    }

    /** Adds [lo, hi] to {@code out}, merged with an overlapping or touching interval already there. */
    private static void union(RockPillars.Column out, int lo, int hi) {
        if (hi < lo) {
            return;
        }
        if (out.count == 1 && lo <= out.hi1 + 1 && hi >= out.lo1 - 1) {
            out.lo1 = Math.min(out.lo1, lo);
            out.hi1 = Math.max(out.hi1, hi);
            return;
        }
        out.add(lo, hi);
    }

    /** Needle (rock spire): radius narrows linearly from base to top, plus a flared foot and an optional wider cap. */
    record Needle(Range height, Range baseRadius, Range topRadius, Range flare, double flareHeight, Optional<Cap> cap) implements Shape {
        static final MapCodec<Needle> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Range.within(1, 256).fieldOf("height").forGetter(Needle::height),
                Range.within(0.5, 64).fieldOf("base_radius").forGetter(Needle::baseRadius),
                Range.within(0.5, 64).fieldOf("top_radius").forGetter(Needle::topRadius),
                Range.within(0, 64).optionalFieldOf("flare", new Range(0, 0)).forGetter(Needle::flare),
                Codec.doubleRange(0.01, 1.0).optionalFieldOf("flare_height", 0.3).forGetter(Needle::flareHeight),
                Cap.CODEC.optionalFieldOf("cap").forGetter(Needle::cap)
        ).apply(instance, Needle::new));

        @Override
        public String type() {
            return "needle";
        }

        @Override
        public Draw draw(long hash, DensityFunction.NoiseHolder noise, int cellX, int cellZ, int cellSize) {
            double base = baseRadius.at(SeededHash.unit(hash, 5));
            double top = Math.min(base, topRadius.at(SeededHash.unit(hash, 6)));
            double capRadius = 0.0;
            double capThickness = 0.0;
            if (cap.isPresent() && SeededHash.unit(hash, 9) < cap.get().chance()) {
                capRadius = top + cap.get().extraRadius().at(SeededHash.unit(hash, 10));
                capThickness = cap.get().thickness().at(SeededHash.unit(hash, 11));
            }
            return new NeedleDraw(height.at(SeededHash.unit(hash, 4)), base, top, flare.at(SeededHash.unit(hash, 7)),
                    flareHeight, capRadius, capThickness);
        }

        @Override
        public void column(RockPillars.Placed placed, double x, double z, RockPillars.Column out) {
            NeedleDraw d = (NeedleDraw) placed.draw();
            double r = Math.hypot(x - placed.x(), z - placed.z());
            if (r <= d.radiusAt(0.0)) {
                double t;
                if (d.radiusAt(1.0) >= r) {
                    t = 1.0;
                } else {
                    double lo = 0.0;
                    double hi = 1.0;
                    for (int i = 0; i < 16; i++) {
                        double mid = (lo + hi) / 2.0;
                        if (d.radiusAt(mid) >= r) {
                            lo = mid;
                        } else {
                            hi = mid;
                        }
                    }
                    t = lo;
                }
                union(out, placed.base(), Mth.floor(placed.floor() + d.height * t - 0.5));
            }
            if (d.capRadius > 0.0 && r <= d.capRadius) {
                double top = placed.floor() + d.height;
                union(out, Mth.floor(top - d.capThickness + 0.5), Mth.floor(top - 0.5));
            }
        }
    }

    record NeedleDraw(double height, double baseRadius, double topRadius, double flare, double flareHeight,
                      double capRadius, double capThickness) implements Draw {
        double radiusAt(double t) {
            double foot = Math.max(0.0, 1.0 - t / flareHeight);
            return topRadius + (baseRadius - topRadius) * (1.0 - t) + flare * foot * foot;
        }

        @Override
        public double halfX() {
            return Math.max(baseRadius + flare, capRadius) + 0.5;
        }

        @Override
        public double halfZ() {
            return halfX();
        }

        @Override
        public Draw scaled(double fit) {
            return new NeedleDraw(height, baseRadius * fit, topRadius * fit, flare * fit, flareHeight, capRadius * fit, capThickness);
        }

        @Override
        public Draw weighted(double weight) {
            double g = grow(weight);
            return new NeedleDraw(height * weight, baseRadius * g, topRadius * g, flare * g, flareHeight, capRadius * g, capThickness);
        }
    }

    record Cap(double chance, Range extraRadius, Range thickness) {
        static final Codec<Cap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(Cap::chance),
                Range.within(0, 64).fieldOf("extra_radius").forGetter(Cap::extraRadius),
                Range.within(1, 64).fieldOf("thickness").forGetter(Cap::thickness)
        ).apply(instance, Cap::new));
    }

    /** Boulder: a flattened half-buried ellipsoid; its columns run down to the base like every shape. */
    record Boulder(Range radius, Range flatness, Range buried) implements Shape {
        static final MapCodec<Boulder> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Range.within(0.5, 64).fieldOf("radius").forGetter(Boulder::radius),
                Range.within(0.1, 2).optionalFieldOf("flatness", new Range(0.6, 0.9)).forGetter(Boulder::flatness),
                Range.within(0, 0.9).optionalFieldOf("buried", new Range(0.3, 0.5)).forGetter(Boulder::buried)
        ).apply(instance, Boulder::new));

        @Override
        public String type() {
            return "boulder";
        }

        @Override
        public Draw draw(long hash, DensityFunction.NoiseHolder noise, int cellX, int cellZ, int cellSize) {
            return new BoulderDraw(radius.at(SeededHash.unit(hash, 4)), flatness.at(SeededHash.unit(hash, 5)), buried.at(SeededHash.unit(hash, 6)));
        }

        @Override
        public void column(RockPillars.Placed placed, double x, double z, RockPillars.Column out) {
            BoulderDraw d = (BoulderDraw) placed.draw();
            double r = Math.hypot(x - placed.x(), z - placed.z());
            if (r > d.radius) {
                return;
            }
            double vertical = d.radius * d.flatness;
            double centre = placed.floor() + vertical * (1.0 - 2.0 * d.buried);
            double top = centre + vertical * Math.sqrt(1.0 - (r / d.radius) * (r / d.radius));
            if (top >= placed.floor() + 0.5) {
                union(out, placed.base(), Mth.floor(top - 0.5));
            }
        }
    }

    record BoulderDraw(double radius, double flatness, double buried) implements Draw {
        @Override
        public double halfX() {
            return radius + 0.5;
        }

        @Override
        public double halfZ() {
            return halfX();
        }

        @Override
        public Draw scaled(double fit) {
            return new BoulderDraw(radius * fit, flatness, buried);
        }

        @Override
        public Draw weighted(double weight) {
            return new BoulderDraw(radius * grow(weight), flatness, buried);
        }
    }

    enum Orientation implements StringRepresentable {
        RANDOM("random"), ALIGNED("aligned");

        static final Codec<Orientation> CODEC = StringRepresentable.fromEnum(Orientation::values);
        private final String name;

        Orientation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * Fin (rib): a vertical slab with a flat top, rounded ends and bevelled upper edges. {@code aligned} fins in one
     * area share a direction (a coarse field from the hash noise) plus {@code spread}. An optional {@code hole} under
     * the middle of the fin makes an arch; the hole starts above the highest floor of the footprint, so its bottom is
     * open and both legs stand on the ground.
     */
    record Fin(Range length, Range thickness, Range height, Orientation orientation, double spread, double alignScale,
               Optional<Hole> hole) implements Shape {
        static final MapCodec<Fin> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Range.within(2, 256).fieldOf("length").forGetter(Fin::length),
                Range.within(1, 64).fieldOf("thickness").forGetter(Fin::thickness),
                Range.within(1, 256).fieldOf("height").forGetter(Fin::height),
                Orientation.CODEC.optionalFieldOf("orientation", Orientation.RANDOM).forGetter(Fin::orientation),
                Codec.doubleRange(0.0, 180.0).optionalFieldOf("spread", 25.0).forGetter(Fin::spread),
                Codec.doubleRange(16.0, 4096.0).optionalFieldOf("align_scale", 256.0).forGetter(Fin::alignScale),
                Hole.CODEC.optionalFieldOf("hole").forGetter(Fin::hole)
        ).apply(instance, Fin::new));

        @Override
        public String type() {
            return "fin";
        }

        @Override
        public Draw draw(long hash, DensityFunction.NoiseHolder noise, int cellX, int cellZ, int cellSize) {
            double angle;
            if (orientation == Orientation.ALIGNED) {
                double centreX = (cellX + 0.5) * cellSize;
                double centreZ = (cellZ + 0.5) * cellSize;
                double field = noise.getValue(centreX / alignScale, 333.5, centreZ / alignScale);
                angle = Math.PI * field + Math.toRadians(spread) * (SeededHash.unit(hash, 8) - 0.5);
            } else {
                angle = Math.PI * SeededHash.unit(hash, 8);
            }
            double holeWidth = 0.0;
            double holeHeight = 0.0;
            if (hole.isPresent() && SeededHash.unit(hash, 9) < hole.get().chance()) {
                holeWidth = hole.get().width().at(SeededHash.unit(hash, 10));
                holeHeight = hole.get().height().at(SeededHash.unit(hash, 11));
            }
            return new FinDraw(length.at(SeededHash.unit(hash, 4)), thickness.at(SeededHash.unit(hash, 5)),
                    height.at(SeededHash.unit(hash, 6)), angle, holeWidth, holeHeight);
        }

        @Override
        public void column(RockPillars.Placed placed, double x, double z, RockPillars.Column out) {
            FinDraw d = (FinDraw) placed.draw();
            double cos = Math.cos(d.angle);
            double sin = Math.sin(d.angle);
            double s = (x - placed.x()) * cos + (z - placed.z()) * sin;
            double n = -(x - placed.x()) * sin + (z - placed.z()) * cos;
            if (Math.abs(s) > d.length / 2.0 || Math.abs(n) > d.thickness / 2.0) {
                return;
            }
            double top = placed.floor() + d.height * Math.min(FinDraw.profile(s / (d.length / 2.0)), FinDraw.bevel(n / (d.thickness / 2.0)));
            if (top < placed.floor() + 0.5) {
                return;
            }
            int hi = Mth.floor(top - 0.5);
            if (d.hasHole(placed) && Math.abs(s) < d.holeHalfWidth()) {
                int bottom = placed.ceilingFloor() + 1;
                double roof = bottom + (d.holeTop(placed) - bottom) * Math.sqrt(1.0 - Math.pow(s / d.holeHalfWidth(), 2));
                out.add(placed.base(), bottom - 1);
                out.add(Mth.floor(roof) + 1, hi);
                return;
            }
            out.add(placed.base(), hi);
        }
    }

    record FinDraw(double length, double thickness, double height, double angle, double holeWidth, double holeHeight) implements Draw {
        /** 1 along most of the fin, rounding off to 0 at the ends ({@code u} = −1 … 1). */
        static double profile(double u) {
            return Math.sqrt(Math.max(0.0, 1.0 - Math.pow(Math.abs(u), 4)));
        }

        /** Full height in the middle half of the thickness, bevelled to 0 at the faces ({@code v} = −1 … 1). */
        static double bevel(double v) {
            return Math.min(1.0, 2.0 * (1.0 - Math.abs(v)));
        }

        double holeHalfWidth() {
            return holeWidth * length / 2.0;
        }

        double holeTop(RockPillars.Placed placed) {
            return placed.floor() + height * holeHeight;
        }

        /** A hole needs room: at least 3 blocks high above the highest floor and a roof of 2 blocks over its full width. */
        boolean hasHole(RockPillars.Placed placed) {
            if (holeWidth <= 0.0 || holeHalfWidth() < 1.0) {
                return false;
            }
            double roofTop = placed.floor() + height * profile(holeHalfWidth() / (length / 2.0));
            return holeTop(placed) - (placed.ceilingFloor() + 1) >= 3.0 && roofTop - holeTop(placed) >= 2.0;
        }

        @Override
        public double halfX() {
            return Math.abs(Math.cos(angle)) * length / 2.0 + Math.abs(Math.sin(angle)) * thickness / 2.0 + 0.5;
        }

        @Override
        public double halfZ() {
            return Math.abs(Math.sin(angle)) * length / 2.0 + Math.abs(Math.cos(angle)) * thickness / 2.0 + 0.5;
        }

        /** Only shorter: the length is what does not fit in a cell, the thickness stays. */
        @Override
        public Draw scaled(double fit) {
            return new FinDraw(length * Math.min(1.0, fit), thickness, height, angle, holeWidth, holeHeight);
        }

        @Override
        public Draw weighted(double weight) {
            double g = grow(weight);
            return new FinDraw(length * g, Math.max(1.0, thickness * g), height * weight, angle, holeWidth, holeHeight);
        }
    }

    record Hole(double chance, Range width, Range height) {
        static final Codec<Hole> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(Hole::chance),
                Range.within(0.05, 0.9).fieldOf("width").forGetter(Hole::width),
                Range.within(0.05, 0.9).fieldOf("height").forGetter(Hole::height)
        ).apply(instance, Hole::new));
    }
}
