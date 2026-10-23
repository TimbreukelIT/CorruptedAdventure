package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.condition.Ring5DamageTypes;

import java.util.function.Function;

/**
 * Damage-type tags of the ring 5 conditions (void, the silence's heartbeat), written by {@code ModDamageTypeTagsProvider}. Owned by stream C
 * ({@code docs/plans/ring-5/waves.md}). The void damage is tagged like the poison's wither of ring 4: through armour, no
 * knockback, environment, and past the hurt cooldown (another hit can land just before it, and inside the cooldown a
 * hit of equal size would be swallowed). The heartbeat of the silence (package 97) is tagged the same way.
 */
public final class Ring5DamageTypeTags {
    private Ring5DamageTypeTags() {
    }

    public static void addTags(Function<TagKey<DamageType>, TagsProvider.TagAppender<DamageType>> tag) {
        tag.apply(DamageTypeTags.BYPASSES_ARMOR).add(Ring5DamageTypes.VOID, Ring5DamageTypes.SILENCE);
        tag.apply(DamageTypeTags.NO_KNOCKBACK).add(Ring5DamageTypes.VOID, Ring5DamageTypes.SILENCE);
        tag.apply(DamageTypeTags.BYPASSES_COOLDOWN).add(Ring5DamageTypes.VOID, Ring5DamageTypes.SILENCE);
        tag.apply(Tags.DamageTypes.IS_ENVIRONMENT).add(Ring5DamageTypes.VOID, Ring5DamageTypes.SILENCE);
    }
}
