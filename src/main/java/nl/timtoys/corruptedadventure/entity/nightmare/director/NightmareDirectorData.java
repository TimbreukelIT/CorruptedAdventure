package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;

import java.util.EnumMap;

/**
 * The director's memory, one per server ({@code <world>/data/corruptedadventure_nightmares.dat}, in the overworld's
 * storage): a {@link NightmareState} per {@link NightmareKind}, also for kinds whose entity does not exist yet (they just
 * stay roaming and unplaced). Loading never throws; an unreadable kind starts fresh.
 */
public final class NightmareDirectorData extends SavedData {
    public static final String FILE_NAME = CorruptedAdventure.MOD_ID + "_nightmares";

    private static final String KINDS = "kinds";

    private final EnumMap<NightmareKind, NightmareState> states = new EnumMap<>(NightmareKind.class);

    public NightmareDirectorData() {
        for (NightmareKind kind : NightmareKind.values()) {
            states.put(kind, new NightmareState());
        }
    }

    public static Factory<NightmareDirectorData> factory() {
        return new Factory<>(() -> {
            NightmareDirectorData data = new NightmareDirectorData();
            data.setDirty();
            return data;
        }, NightmareDirectorData::load);
    }

    static NightmareDirectorData load(CompoundTag tag, HolderLookup.Provider registries) {
        NightmareDirectorData data = new NightmareDirectorData();
        CompoundTag kinds = tag.getCompound(KINDS);
        for (NightmareKind kind : NightmareKind.values()) {
            String key = kind.getSerializedName();
            if (kinds.contains(key, Tag.TAG_COMPOUND)) {
                data.states.put(kind, NightmareState.load(kinds.getCompound(key)));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag kinds = new CompoundTag();
        states.forEach((kind, state) -> kinds.put(kind.getSerializedName(), state.save()));
        tag.put(KINDS, kinds);
        return tag;
    }

    public NightmareState get(NightmareKind kind) {
        return states.get(kind);
    }
}
