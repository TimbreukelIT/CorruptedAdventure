package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.OptionalInt;
import java.util.function.ToDoubleFunction;

/**
 * {@code /ca tpring <ring> [nether]} and {@code /ca tpboundary <boundary> [nether]}: teleport the player along their
 * current heading from (0,0) to the middle of a ring or onto a noisy boundary (blend 0.5).
 * Without {@code nether} the target is the overworld.
 */
final class RingTeleportCommand {
    private static final int OVERWORLD_FALLBACK_Y = 100;
    private static final int NETHER_FALLBACK_Y = 64;
    // Start the search this far below the logical height, so we never land between the layers of a bedrock roof.
    private static final int CEILING_MARGIN = 8;

    private RingTeleportCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildTpRing() {
        return Commands.literal("tpring").then(Commands.argument("ring", IntegerArgumentType.integer(1, Ring.values().length))
                .executes(context -> tpRing(context, false))
                .then(Commands.literal("nether").executes(context -> tpRing(context, true))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildTpBoundary() {
        return Commands.literal("tpboundary").then(Commands.argument("boundary", IntegerArgumentType.integer(1, RingSettings.RING_COUNT))
                .executes(context -> tpBoundary(context, false))
                .then(Commands.literal("nether").executes(context -> tpBoundary(context, true))));
    }

    private static int tpRing(CommandContext<CommandSourceStack> context, boolean nether) throws CommandSyntaxException {
        Ring ring = Ring.byIndex(IntegerArgumentType.getInteger(context, "ring") - 1);
        double radius = RingTargets.middleRadius(RingSettingsSnapshot.current(), ring);
        return teleport(context.getSource(), nether, heading -> radius, "the middle of " + RingInfoCommand.describe(ring));
    }

    private static int tpBoundary(CommandContext<CommandSourceStack> context, boolean nether) throws CommandSyntaxException {
        int boundary = IntegerArgumentType.getInteger(context, "boundary");
        return teleport(context.getSource(), nether,
                heading -> RingTargets.boundaryRadius(RingLayout.current(), RingSettingsSnapshot.current(), boundary, heading),
                "boundary " + boundary);
    }

    /** @param overworldRadius target radius in overworld blocks along the player's heading */
    private static int teleport(CommandSourceStack source, boolean nether, ToDoubleFunction<RingTargets.Heading> overworldRadius,
                                String target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getServer().getLevel(nether ? Level.NETHER : Level.OVERWORLD);
        if (level == null) {
            source.sendFailure(Component.literal("The " + (nether ? "Nether" : "overworld") + " is not loaded"));
            return 0;
        }

        RingTargets.Heading heading = RingTargets.Heading.towards(player.getX(), player.getZ());
        double radius = overworldRadius.applyAsDouble(heading) / RingLayout.scale(level.dimension());
        int x = Mth.floor(heading.x() * radius);
        int z = Mth.floor(heading.z() * radius);
        OptionalInt safeY = findSafeY(level, x, z);
        int y = safeY.orElse(nether ? NETHER_FALLBACK_Y : OVERWORLD_FALLBACK_Y);

        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());

        String landing = RingInfoCommand.describe(RingLayout.current().sample(level.dimension(), x + 0.5, z + 0.5));
        source.sendSuccess(() -> Component.literal("Teleported to " + target + " at " + x + " " + y + " " + z
                + " in " + level.dimension().location() + ": " + landing), true);
        if (safeY.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No safe ground found, placed at Y " + y).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    /** The Y to stand on at a column (the landing rule of {@code tpring}), generating the chunk; empty if none is safe. */
    static OptionalInt findSafeY(ServerLevel level, int x, int z) {
        // Generates the chunk if needed; heightmaps of unloaded chunks read as empty.
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        if (level.dimensionType().hasCeiling()) {
            return findStandableBelowCeiling(level, x, z);
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (y <= level.getMinBuildHeight() || level.getFluidState(new BlockPos(x, y - 1, z)).is(FluidTags.LAVA)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(y);
    }

    private static OptionalInt findStandableBelowCeiling(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        for (int y = level.getLogicalHeight() - CEILING_MARGIN; y > level.getMinBuildHeight(); y--) {
            if (isStandable(level, feet.set(x, y, z))) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        return isFree(level, feet) && isFree(level, feet.above())
                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }
}
