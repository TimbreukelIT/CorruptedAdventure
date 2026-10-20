package nl.timtoys.corruptedadventure.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ring 4's clouds (sparring 2026-10-20, C1-3): the vanilla layer at Y 192 cuts through the high islands, so in ring 4
 * it rises to {@link #HEIGHT} (above the highest islands, Y ~250), gradually by ring 4's weight at the camera (over the
 * zones 3→4 and 4→5, like the ring weather mixes), and takes a light tint: pale aqua in the mythical band, a faint
 * yellow-green in the blighted one, pink at a mythical dusk. Hooks: {@code DimensionSpecialEffectsMixin} (height),
 * {@code ClientLevelSkyColorMixin} (colour), {@code LevelRendererCloudsMixin} (fog while the clouds draw).
 * <p>
 * Vanilla fades clouds out by the terrain fog, with the vertical distance counted too (cylinder fog): from sea level a
 * layer at Y 300 would lie beyond a 12-chunk fog and vanish. While the clouds draw, the fog therefore moves out by the
 * height the layer was raised, so they fade as the vanilla layer would at Y 192.
 */
public final class Ring4Clouds {
    /** Cloud layer in ring 4 (decision C1-3: ~Y 300). */
    static final float HEIGHT = 300.0F;
    /** Vanilla's terrain fog end at a 12-chunk view: the fog at which the clouds get the full rise. */
    static final float REFERENCE_FOG = 192.0F;
    /** Multipliers on vanilla's cloud colour: mythical pale aqua, blighted faint yellow-green, mythical dusk pink. */
    private static final Vec3 MYTHIC = new Vec3(0.92, 0.98, 1.0);
    private static final Vec3 BLIGHTED = new Vec3(0.94, 0.96, 0.78);
    private static final Vec3 MYTHIC_DUSK = new Vec3(1.0, 0.80, 0.84);
    private static final Vec3 BLIGHTED_DUSK = new Vec3(0.92, 0.96, 0.74);

    private static float raisedBy;
    private static float savedStart;
    private static float savedEnd;

    private Ring4Clouds() {
    }

    /** The cloud height in the overworld: vanilla's raised towards {@link #HEIGHT} by ring 4's weight. */
    public static float cloudHeight(float vanilla) {
        float weight = Ring4SkyState.ring4();
        if (weight <= 0.0F || Float.isNaN(vanilla) || vanilla >= HEIGHT) {
            return vanilla;
        }
        return Mth.lerp(weight, vanilla, HEIGHT);
    }

    /** Vanilla's cloud colour × ring 4's tint; {@code colour} itself outside ring 4. */
    public static Vec3 cloudColor(ClientLevel level, Vec3 colour, float partialTick) {
        float weight = Ring4SkyState.ring4();
        if (weight <= 0.0F) {
            return colour;
        }
        float blight = Ring4SkyState.blighted();
        Vec3 tint = MYTHIC.lerp(BLIGHTED, blight);
        Vec3 dusk = MYTHIC_DUSK.lerp(BLIGHTED_DUSK, blight);
        tint = tint.lerp(dusk, Ring4Dusk.duskness(level.getTimeOfDay(partialTick)));
        Vec3 multiplier = new Vec3(1.0, 1.0, 1.0).lerp(tint, weight);
        return colour.multiply(multiplier);
    }

    /**
     * Before vanilla draws the clouds ({@code LevelRenderer#renderClouds}, render thread): moves the fog out by
     * {@code raisedBy} blocks (the layer's height above vanilla's), keeping the old values for {@link #afterClouds}.
     */
    public static void beforeClouds(float raised) {
        // Also NaN: a dimension without clouds (the Nether) has a NaN height.
        if (!(raised > 0.0F)) {
            raisedBy = 0.0F;
            return;
        }
        raisedBy = raised;
        savedStart = RenderSystem.getShaderFogStart();
        savedEnd = RenderSystem.getShaderFogEnd();
        float end = savedEnd + fogExtra(raised, savedEnd);
        RenderSystem.setShaderFogStart(cloudFogStart(savedStart, savedEnd, end));
        RenderSystem.setShaderFogEnd(end);
    }

    /**
     * The fog start for the clouds: moved out as far as the end, but never before vanilla's own fade (the last tenth,
     * 4–64 blocks): ring 4's distance haze starts the terrain fog early, and the clouds overhead would fade in it.
     */
    static float cloudFogStart(float start, float end, float cloudEnd) {
        return Math.max(start + (cloudEnd - end), cloudEnd - Mth.clamp(cloudEnd / 10.0F, 4.0F, 64.0F));
    }

    /**
     * How far the fog moves out for the clouds: the full rise at vanilla's 12-chunk fog ({@link #REFERENCE_FOG}) or
     * more, less in a closer fog, so thick fog (dense fog, low mist, a short view) still hides them as it would the
     * vanilla layer.
     */
    static float fogExtra(float raised, float fogEnd) {
        return raised * Mth.clamp(fogEnd / REFERENCE_FOG, 0.0F, 1.0F);
    }

    /** After the clouds: vanilla's fog back for what follows (weather, hand). */
    public static void afterClouds() {
        if (raisedBy > 0.0F) {
            RenderSystem.setShaderFogStart(savedStart);
            RenderSystem.setShaderFogEnd(savedEnd);
            raisedBy = 0.0F;
        }
    }
}
