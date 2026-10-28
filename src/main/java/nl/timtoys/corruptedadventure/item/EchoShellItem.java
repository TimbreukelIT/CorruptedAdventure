package nl.timtoys.corruptedadventure.item;

import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.timtoys.corruptedadventure.condition.ConsumableDoses;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * The echo shell (fase-2-items.md §7.3): blown like a goat horn, a loud echo for everyone nearby. The server plays the
 * sound and sends the goat horn's game event ({@link GameEvent#INSTRUMENT_PLAY}), so the silence breaks for everyone in
 * reach like after a goat horn ({@code SilenceSounds}); the one who blows it also gets
 * {@link ConsumableDoses#echoShellBlow}. One blow costs one point of {@link #DURABILITY}; the cooldown is
 * {@code [conditions.silence] echoShellCooldownSeconds}, set by the server (it reaches the client by vanilla's packet).
 */
public final class EchoShellItem extends Item {
    public static final int DURABILITY = 16;
    /** How long the horn pose lasts: about the length of the sound. */
    static final int USE_TICKS = 60;
    /** A variable-range sound carries 16 blocks per unit of volume: 64 blocks (a goat horn: 256). */
    private static final float VOLUME = 4.0F;

    public EchoShellItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        // No pose (another item already in use, or a mod cancelled the start): no blow either.
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            blow(serverPlayer);
            serverPlayer.getCooldowns().addCooldown(this,
                    ServerConfig.SILENCE.echoShellCooldownSeconds.get() * SharedConstants.TICKS_PER_SECOND);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void blow(ServerPlayer player) {
        Level level = player.level();
        level.playSound(null, player, ModSoundEvents.ECHO_SHELL_BLOW.get(), SoundSource.PLAYERS, VOLUME, 1.0F);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        if (!(player instanceof FakePlayer)) {
            ConsumableDoses.echoShellBlow().apply(player);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }
}
