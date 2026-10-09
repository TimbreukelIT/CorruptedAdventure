package nl.timtoys.corruptedadventure.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.condition.IceMelting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ice melts in the heat of ring 3 (package 55b, {@link IceMelting}); NeoForge 1.21.1 has no random tick event. Before
 * vanilla's check by block light, which still applies everywhere. Only {@code minecraft:ice}: frosted ice has its own
 * {@code randomTick}, other mods' subclasses keep their rules. Random ticks only run on the server.
 */
@Mixin(IceBlock.class)
abstract class IceBlockMeltMixin {
    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void corruptedadventure$meltInTheHeat(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                                  CallbackInfo ci) {
        if (state.is(Blocks.ICE) && IceMelting.meltsByHeat(level, pos)) {
            melt(state, level, pos);
            ci.cancel();
        }
    }
}
