package nl.timtoys.corruptedadventure.command;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@code /ca} root for the mod's debug commands. OP-only (permission level 2).
 */
public final class CaCommand {
    private CaCommand() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(CaCommand::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ca")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(RingInfoCommand.build())
                .then(RingTeleportCommand.buildTpRing())
                .then(RingTeleportCommand.buildTpBoundary())
                .then(BorderInfoCommand.build()));
    }
}
