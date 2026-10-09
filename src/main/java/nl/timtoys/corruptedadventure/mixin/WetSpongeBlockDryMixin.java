package nl.timtoys.corruptedadventure.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WetSpongeBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.condition.SpongeDrying;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A wet sponge dries in ring 3 where water evaporates (package 55b, {@link SpongeDrying}). Vanilla dries it here for
 * the Nether; that check is left to vanilla. {@code onPlace} only runs on the server.
 */
@Mixin(WetSpongeBlock.class)
abstract class WetSpongeBlockDryMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void corruptedadventure$dryInTheHeat(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                                 boolean isMoving, CallbackInfo ci) {
        if (!level.dimensionType().ultraWarm() && level instanceof ServerLevel serverLevel
                && SpongeDrying.dryIfExposed(serverLevel, pos)) {
            ci.cancel();
        }
    }
}
