package nl.timtoys.corruptedadventure.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import nl.timtoys.corruptedadventure.worldgen.chunk.CarverLava;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carver lava in a ring world (package 59): at and below a carver's lava level vanilla places lava without asking the
 * aquifer, but ring 4 is dry down there and its dams must stay rock, so the ring aquifer decides ({@link CarverLava}).
 * Outside ring 4 and its dams the answer is vanilla's lava. Worldgen only runs on the server.
 */
@Mixin(WorldCarver.class)
abstract class WorldCarverLavaMixin {
    @Inject(method = "getCarveState", at = @At("HEAD"), cancellable = true)
    private void corruptedadventure$ringLava(CarvingContext context, CarverConfiguration config, BlockPos pos, Aquifer aquifer,
                                             CallbackInfoReturnable<BlockState> cir) {
        if (CarverLava.decides(aquifer) && pos.getY() <= config.lavaLevel.resolveY(context)) {
            cir.setReturnValue(CarverLava.at(aquifer, pos));
        }
    }
}
