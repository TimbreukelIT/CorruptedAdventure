package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;

/**
 * Surface rule condition {@code corruptedadventure:ring_5_speckle} (94/95): true in a pure random share {@code chance} of
 * the cubes of {@code size} blocks a side, per world and {@code salt}: specks and patches of 1 to {@code size}² blocks on a
 * surface (the glitch blocks of {@code glitch_fields}, V3-A; the ruined plateau of the castles, V9-B). One hash per block,
 * no lookup; the rule in front of it says where (top block, underside, biome).
 */
public record Ring5SpeckleCondition(int salt, float chance, int size) implements SurfaceRules.ConditionSource {
    public static final MapCodec<Ring5SpeckleCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("salt").forGetter(Ring5SpeckleCondition::salt),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Ring5SpeckleCondition::chance),
            Codec.intRange(1, 4).fieldOf("size").forGetter(Ring5SpeckleCondition::size)
    ).apply(instance, Ring5SpeckleCondition::new));
    public static final KeyDispatchDataCodec<Ring5SpeckleCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        long seed = Ring5World.salt(context.randomState);
        return () -> Ring5GroundCondition.unit(seed, Math.floorDiv(context.blockX, size), Math.floorDiv(context.blockY, size),
                Math.floorDiv(context.blockZ, size), salt) < chance;
    }
}
