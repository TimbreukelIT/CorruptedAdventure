package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Fake registry lookup: one unbound stand-alone reference per known key (same instance on every call); every tag exists, empty. */
final class StandaloneHolderGetter<T> implements HolderGetter<T> {
    private final HolderOwner<T> owner = new HolderOwner<>() {
    };
    private final Map<ResourceKey<T>, Holder.Reference<T>> references = new HashMap<>();
    private final Predicate<ResourceKey<T>> known;

    StandaloneHolderGetter(Predicate<ResourceKey<T>> known) {
        this.known = known;
    }

    HolderOwner<T> owner() {
        return owner;
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
        return known.test(key)
                ? Optional.of(references.computeIfAbsent(key, k -> Holder.Reference.createStandAlone(owner, k)))
                : Optional.empty();
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
        return Optional.of(HolderSet.emptyNamed(owner, tag));
    }
}
