package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

/**
 * The dressing of Nether ring 5 (96, the user's decision of 2026-10-23), as the surface paints it on the shape of
 * {@link NetherCathedrals}: a band of still light in every grid column at a height of its own, a core of white core ore
 * in about one grid column in ten (halfway up, in the column's inside), and in a cathedral's giant column a vertical vein
 * of white core ore in its heart plus an imprint of polished white stone and bricks in the floor round its foot. Pure:
 * per grid column from a seeded hash of its low corner, per cathedral from its shape.
 */
public final class NetherRing5Dressing {
    /** The salt of a grid column's hash; the cells use salts 0 to {@code attempts} (≤ 64), so this never meets theirs. */
    static final long PILLAR_SALT = 1000;
    /** A column without dressing: no band, no ore, a plain floor. */
    public static final Column NONE = new Column(1, 0, 1, 0, null);

    private NetherRing5Dressing() {
    }

    /** What the surface paints differently in one column; an empty Y range has {@code from > to}. */
    public record Column(int lightFrom, int lightTo, int oreFrom, int oreTo, @Nullable Part floor) {
        /** Whether block Y of this column gets {@code part} (the floor parts only on the floor's top block, which the rule checks). */
        public boolean has(Part part, int y) {
            return switch (part) {
                case LIGHT -> y >= lightFrom && y <= lightTo;
                case ORE -> y >= oreFrom && y <= oreTo;
                case IMPRINT_POLISHED, IMPRINT_BRICKS -> floor == part;
            };
        }
    }

    /** The parts the surface condition {@code nether_ring_5_dressing} asks for. */
    public enum Part implements StringRepresentable {
        /** Still light: the band in a grid column. */
        LIGHT("light"),
        /** White core ore: a grid column's core or the giant column's vein. */
        ORE("ore"),
        /** The floor's top block round a giant column's foot: polished white stone. */
        IMPRINT_POLISHED("imprint_polished"),
        /** The same, its inner and outer border: white stone bricks. */
        IMPRINT_BRICKS("imprint_bricks");

        public static final Codec<Part> CODEC = StringRepresentable.fromEnum(Part::values);
        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * {@code settings.dressing} of {@code nether_cathedrals}: the band's lowest Y per grid column (one of
     * {@code light_y.min}, + {@code light_step}, … up to {@code light_y.max}) and its height; the chance of a core and its
     * size in ore (stacked in one inner column, centred halfway between the floor and the ceiling); the vein's radius
     * round the giant column's axis (0: the middle column only); the imprint's width beyond the giant column's foot and
     * the width of its brick borders (inside and outside).
     */
    public record Settings(Range lightY, int lightStep, int lightHeight, double coreChance, Range coreSize,
                           double veinRadius, int imprintWidth, int imprintBorder) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Range.within(0.0, 127.0).fieldOf("light_y").forGetter(Settings::lightY),
                Codec.intRange(1, 64).fieldOf("light_step").forGetter(Settings::lightStep),
                Codec.intRange(1, 16).fieldOf("light_height").forGetter(Settings::lightHeight),
                Codec.doubleRange(0.0, 1.0).fieldOf("core_chance").forGetter(Settings::coreChance),
                Range.within(1.0, 16.0).fieldOf("core_size").forGetter(Settings::coreSize),
                Codec.doubleRange(0.0, 64.0).fieldOf("vein_radius").forGetter(Settings::veinRadius),
                Codec.intRange(0, 64).fieldOf("imprint_width").forGetter(Settings::imprintWidth),
                Codec.intRange(0, 32).fieldOf("imprint_border").forGetter(Settings::imprintBorder)
        ).apply(instance, Settings::new));

        /** The band's levels (≥ 1). */
        int lightLevels() {
            return ((int) lightY.max() - (int) lightY.min()) / lightStep + 1;
        }

        /** The highest Y a band reaches. */
        int lightTop() {
            return (int) lightY.min() + (lightLevels() - 1) * lightStep + lightHeight - 1;
        }

        /** The middle of a grid column between the floor's top and the ceiling. */
        static int middle(NetherCathedrals.HallSettings halls) {
            return (halls.floorTop() + 1 + halls.ceiling() - 1) / 2;
        }

        /** The lowest Y of a core of {@code size} ore. */
        static int coreFrom(NetherCathedrals.HallSettings halls, int size) {
            return middle(halls) - (size - 1) / 2;
        }

        /** Checked against the halls and cathedrals by {@link NetherCathedrals.Settings}. */
        DataResult<Settings> validate(NetherCathedrals.HallSettings h, NetherCathedrals.CathedralSettings c) {
            int plainsTop = h.floorTop() + (int) h.plainsHeight().max();
            if ((int) lightY.min() <= plainsTop) {
                return DataResult.error(() -> "nether_cathedrals: the light band must lie above the highest plain (Y " + plainsTop + ")");
            }
            int coreTop = coreFrom(h, (int) coreSize.max()) + (int) coreSize.max() - 1;
            if (lightTop() >= coreFrom(h, (int) coreSize.max()) || coreTop >= h.ceiling()) {
                return DataResult.error(() -> "nether_cathedrals: the light band must lie below the core, the core below the ceiling");
            }
            if (h.pillarSize() < 3) {
                return DataResult.error(() -> "nether_cathedrals: a column needs an inside for its core (pillar_size >= 3)");
            }
            if (veinRadius >= c.pillarRadius().min()) {
                return DataResult.error(() -> "nether_cathedrals: the vein must lie inside the giant column");
            }
            if (2 * imprintBorder > imprintWidth || c.pillarRadius().max() + imprintWidth >= c.radius().min() - c.rim().max()) {
                return DataResult.error(() -> "nether_cathedrals: the imprint must hold its borders and lie under the lifted roof");
            }
            return DataResult.success(this);
        }
    }

    /** The dressing of column (x, z) of {@code field}. Pure. */
    static Column of(NetherCathedrals field, int x, int z) {
        NetherCathedrals.Settings settings = field.settings();
        Settings d = settings.dressing();
        NetherCathedrals.HallSettings h = settings.halls();
        NetherCathedrals.Cathedral cathedral = field.cathedralAt(x, z);
        if (cathedral != null) {
            double distance = cathedral.distance(x, z);
            if (distance <= d.veinRadius()) {
                return new Column(1, 0, h.floorTop() + 1, h.roof() - 1, null);
            }
            double t = distance - cathedral.pillarRadius();
            if (t > 0.0 && t <= d.imprintWidth()) {
                boolean border = t <= d.imprintBorder() || t > d.imprintWidth() - d.imprintBorder();
                return new Column(1, 0, 1, 0, border ? Part.IMPRINT_BRICKS : Part.IMPRINT_POLISHED);
            }
            return NONE;
        }
        long corner = field.gridPillarCorner(x, z);
        if (corner == NetherCathedrals.NO_PILLAR) {
            return NONE;
        }
        int px = (int) (corner >> 32);
        int pz = (int) corner;
        long hash = field.pillarHash(px, pz, PILLAR_SALT);
        int lightFrom = (int) d.lightY().min() + d.lightStep() * Math.min(d.lightLevels() - 1,
                (int) (SeededHash.unit(hash, 0) * d.lightLevels()));
        int lightTo = lightFrom + d.lightHeight() - 1;
        if (SeededHash.unit(hash, 1) >= d.coreChance()) {
            return new Column(lightFrom, lightTo, 1, 0, null);
        }
        // the core: one of the inner columns (u, v in 1 .. size - 2)
        int inner = h.pillarSize() - 2;
        int spot = Math.min(inner * inner - 1, (int) (SeededHash.unit(hash, 2) * inner * inner));
        if (x - px != 1 + spot % inner || z - pz != 1 + spot / inner) {
            return new Column(lightFrom, lightTo, 1, 0, null);
        }
        int size = NetherCathedrals.pick(d.coreSize(), SeededHash.unit(hash, 3));
        int from = Settings.coreFrom(h, size);
        return new Column(lightFrom, lightTo, from, from + size - 1, null);
    }
}
