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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.WeaknessItemsConfig;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.List;

/**
 * The sunstone flash (ring 2, the weakness of The Layered): a polished sunstone mirror that throws one bright flash in
 * the look direction. Everything living in the cone and in sight gets "dazzled" ({@code [weakness_items]}); The Layered
 * reacts to that on its own. The server does the flash; one flash costs one point of {@link #DURABILITY}, the cooldown
 * reaches the client by vanilla's packet.
 */
public final class SunstoneFlashItem extends Item {
    public static final int DURABILITY = 64;
    /** A variable-range sound carries 16 blocks per unit of volume: 48 blocks. */
    private static final float VOLUME = 3.0F;

    public SunstoneFlashItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            flash(serverPlayer.serverLevel(), serverPlayer);
            serverPlayer.getCooldowns().addCooldown(this,
                    ServerConfig.WEAKNESS_ITEMS.flashCooldownSeconds.get() * SharedConstants.TICKS_PER_SECOND);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Dazzles what the flash of {@code user} reaches; returns those. */
    static List<LivingEntity> flash(ServerLevel level, Player user) {
        WeaknessItemsConfig config = ServerConfig.WEAKNESS_ITEMS;
        double range = config.flashRange.get();
        List<LivingEntity> hit = WeaknessReach.inCone(level, user, range, config.flashConeDegrees.get());
        int ticks = config.dazzledSeconds.get() * SharedConstants.TICKS_PER_SECOND;
        for (LivingEntity target : hit) {
            target.addEffect(new MobEffectInstance(ModMobEffects.DAZZLED, ticks), user);
        }
        level.playSound(null, user, ModSoundEvents.SUNSTONE_FLASH.get(), SoundSource.PLAYERS, VOLUME, 1.0F);
        Vec3 eyes = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 front = eyes.add(look);
        level.sendParticles(ParticleTypes.FLASH, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
        // A widening beam of light motes along the cone.
        for (int step = 2; step <= range; step += 2) {
            Vec3 at = eyes.add(look.scale(step));
            double spread = step * 0.2;
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, spread, spread, spread, 0.01);
        }
        return hit;
    }
}
