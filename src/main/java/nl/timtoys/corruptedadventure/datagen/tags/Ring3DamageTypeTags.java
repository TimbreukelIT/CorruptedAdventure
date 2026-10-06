package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.function.Function;

/**
 * Damage-type tags of the ring 3 conditions (heat, cold), written by {@code ModDamageTypeTagsProvider}. Owned by stream C
 * ({@code docs/plans/ring-3/waves.md}); empty until the heat condition exists.
 */
public final class Ring3DamageTypeTags {
    private Ring3DamageTypeTags() {
    }

    public static void addTags(Function<TagKey<DamageType>, TagsProvider.TagAppender<DamageType>> tag) {
    }
}
