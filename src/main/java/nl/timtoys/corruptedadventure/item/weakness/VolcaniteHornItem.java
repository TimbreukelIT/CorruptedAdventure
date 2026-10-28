package nl.timtoys.corruptedadventure.item.weakness;

import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.WeaknessItemsConfig;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.List;

/**
 * The volcanite horn (ring 3, the weakness of The Risen): a goat horn bound in volcanite, blown like one. Every mob
 * within {@code [weakness_items] hornRadius} gets "stunned", players never; The Risen reacts to that on its own, a
 * vanilla goat horn does nothing to it. Like the echo shell, the server plays the sound and sends the goat horn's game
 * event; one blow costs one point of {@link #DURABILITY}, the cooldown reaches the client by vanilla's packet.
 */
public final class VolcaniteHornItem extends Item {
    public static final int DURABILITY = 32;
    /** How long the horn pose lasts: about the length of the sound. */
    static final int USE_TICKS = 60;
    /** A variable-range sound carries 16 blocks per unit of volume: 96 blocks (a goat horn: 256). */
    private static final float VOLUME = 6.0F;
    /** Puffs in the shock ring round the one who blows. */
    private static final int RING_PUFFS = 16;

    public VolcaniteHornItem(Properties properties) {
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
            blow(serverPlayer.serverLevel(), serverPlayer);
            serverPlayer.getCooldowns().addCooldown(this,
                    ServerConfig.WEAKNESS_ITEMS.hornCooldownSeconds.get() * SharedConstants.TICKS_PER_SECOND);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Stuns what the blow of {@code user} reaches; returns those. */
    static List<LivingEntity> blow(ServerLevel level, Player user) {
        WeaknessItemsConfig config = ServerConfig.WEAKNESS_ITEMS;
        List<LivingEntity> hit = WeaknessReach.inRadius(level, user, config.hornRadius.get());
        int ticks = config.stunnedSeconds.get() * SharedConstants.TICKS_PER_SECOND;
        for (LivingEntity target : hit) {
            if (target.addEffect(new MobEffectInstance(ModMobEffects.STUNNED, ticks), user)) {
                level.sendParticles(ParticleTypes.ASH, target.getX(), target.getY(0.6), target.getZ(), 12,
                        target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.0);
            }
        }
        level.playSound(null, user, ModSoundEvents.VOLCANITE_HORN_BLOW.get(), SoundSource.PLAYERS, VOLUME, 1.0F);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, user.position(), GameEvent.Context.of(user));
        for (int i = 0; i < RING_PUFFS; i++) {
            double angle = Math.PI * 2.0 * i / RING_PUFFS;
            level.sendParticles(ParticleTypes.SMALL_GUST, user.getX() + Math.cos(angle) * 1.5, user.getY() + 0.3,
                    user.getZ() + Math.sin(angle) * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return hit;
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
