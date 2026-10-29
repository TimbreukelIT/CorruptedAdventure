package nl.timtoys.corruptedadventure.entity.unfinished;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The blocks The Unfinished changes for a while, per level ({@code <world>/<dim>/data/corruptedadventure_unfinished_blocks.dat}):
 * the ground he unloads ({@link Kind#GROUND}: air, or a flickering glitch block just before) and the memory shards that lie
 * where they fell ({@link Kind#SHARD}). Each remembers the exact block that was there and goes back at its time, bottom
 * first (a player inside a block that comes back is pushed up onto it). Saved with the level, so a chunk that unloads with
 * a hole in it is mended as soon as it is loaded again, and the server mends every loaded one before it stops. A hole
 * position that was changed meanwhile (a player put a block in it) is left as it is; a shard goes even if it turned. Breaking a tracked block gives nothing (it
 * glitches back at once), explosions skip them. Also the holes the ground left, while they are open ({@link Hole}), so the
 * void can catch whoever fell in. Server-side.
 */
public final class UnfinishedTempBlocks extends SavedData {
    public static final String FILE_NAME = CorruptedAdventure.MOD_ID + "_unfinished_blocks";
    /** No neighbour or shape updates: nothing next to it (a torch, sand, water) notices the block coming and going. */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public enum Kind {
        GROUND, SHARD
    }

    /** An open hole down to the void: whoever falls deeper than its surface is caught ({@link UnfinishedUnload#catchFallen}). */
    record Hole(BlockPos centre, int radius, int surface, long until) {
        boolean contains(Player player) {
            double dx = player.getX() - (centre.getX() + 0.5);
            double dz = player.getZ() - (centre.getZ() + 0.5);
            double reach = radius + 0.5;
            return dx * dx + dz * dz <= reach * reach;
        }
    }

    private static final class Entry {
        private final BlockState original;
        private BlockState placed;
        private long restoreAt;
        private final Kind kind;

        private Entry(BlockState original, BlockState placed, long restoreAt, Kind kind) {
            this.original = original;
            this.placed = placed;
            this.restoreAt = restoreAt;
            this.kind = kind;
        }
    }

    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
    private final List<Hole> holes = new ArrayList<>();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(UnfinishedTempBlocks::onLevelTick);
        gameBus.addListener(UnfinishedTempBlocks::onServerStopping);
        gameBus.addListener(UnfinishedTempBlocks::onBreak);
        gameBus.addListener(UnfinishedTempBlocks::onExplosion);
    }

    static Factory<UnfinishedTempBlocks> factory() {
        // Not dirty until something is changed: levels he never touches get no file.
        return new Factory<>(UnfinishedTempBlocks::new, UnfinishedTempBlocks::load);
    }

    public static UnfinishedTempBlocks of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), FILE_NAME);
    }

    /**
     * Puts {@code placed} at {@code pos} until game time {@code restoreAt}; the first change remembers what was there.
     * False when the position is not loaded or the block there has a block entity.
     */
    public boolean swap(ServerLevel level, BlockPos pos, BlockState placed, long restoreAt, Kind kind) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        long key = pos.asLong();
        Entry entry = entries.get(key);
        if (entry == null) {
            BlockState original = level.getBlockState(pos);
            if (original.hasBlockEntity()) {
                return false;
            }
            entry = new Entry(original, placed, restoreAt, kind);
            entries.put(key, entry);
        } else {
            entry.placed = placed;
            entry.restoreAt = restoreAt;
        }
        level.setBlock(pos, placed, FLAGS);
        setDirty();
        return true;
    }

    public boolean isTracked(BlockPos pos) {
        return entries.containsKey(pos.asLong());
    }

    public boolean isShard(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        return entry != null && entry.kind == Kind.SHARD;
    }

    int count(Kind kind) {
        int count = 0;
        for (Entry entry : entries.values()) {
            if (entry.kind == kind) {
                count++;
            }
        }
        return count;
    }

    /** Puts the block back now (if it is still the one placed) and forgets the position. */
    boolean restore(ServerLevel level, BlockPos pos) {
        Entry entry = entries.remove(pos.asLong());
        if (entry == null) {
            return false;
        }
        setDirty();
        BlockState now = level.getBlockState(pos);
        // A shard may have turned meanwhile (grass to dirt, a log stripped): it goes all the same. A hole someone built in
        // stays as he left it.
        boolean still = entry.kind == Kind.SHARD ? !now.isAir() && !now.hasBlockEntity() : now.equals(entry.placed);
        if (!still) {
            return false;
        }
        level.setBlock(pos, entry.original, FLAGS);
        if (!entry.original.getCollisionShape(level, pos).isEmpty()) {
            liftOut(level, pos);
        }
        if (entry.kind == Kind.SHARD) {
            UnfinishedEffects.shardGone(level, pos);
        }
        return true;
    }

    void addHole(Hole hole) {
        holes.add(hole);
        setDirty();
    }

    List<Hole> holes() {
        return holes;
    }

    /** Every tracked block in a loaded chunk back now (the server stops; tests). */
    public void restoreAll(ServerLevel level) {
        restoreDue(level, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

    private void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (!holes.isEmpty()) {
            if (holes.removeIf(hole -> now >= hole.until())) {
                setDirty();
            }
            for (Hole hole : holes) {
                UnfinishedUnload.catchFallen(level, hole);
            }
        }
        if (!entries.isEmpty()) {
            restoreDue(level, now, ServerConfig.UNFINISHED.tempRestorePerTick.get());
        }
    }

    private void restoreDue(ServerLevel level, long now, int max) {
        List<BlockPos> due = new ArrayList<>();
        for (Long2ObjectMap.Entry<Entry> entry : entries.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (entry.getValue().restoreAt <= now && level.isLoaded(pos)) {
                due.add(pos);
            }
        }
        // Bottom first: a column fills from below, and sand never hangs.
        due.sort(Comparator.comparingInt(BlockPos::getY));
        for (int i = 0; i < due.size() && i < max; i++) {
            restore(level, due.get(i));
        }
    }

    /** A player inside a block that came back stands on it instead (as the column fills, it lifts him). */
    private static void liftOut(ServerLevel level, BlockPos pos) {
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos))) {
            player.teleportTo(player.getX(), pos.getY() + 1.0, player.getZ());
            player.resetFallDistance();
        }
    }

    // ------------------------------------------------------------------------------------------------ events

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            of(level).tick(level);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            UnfinishedTempBlocks data = level.getDataStorage().get(factory(), FILE_NAME);
            if (data != null) {
                data.restoreAll(level);
            }
        }
    }

    /** A tracked block gives nothing: it glitches back to what it was. */
    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            UnfinishedTempBlocks data = of(level);
            if (data.isTracked(event.getPos())) {
                event.setCanceled(true);
                data.restore(level, event.getPos());
            }
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            UnfinishedTempBlocks data = of(level);
            if (!data.entries.isEmpty()) {
                event.getAffectedBlocks().removeIf(data::isTracked);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ NBT

    static UnfinishedTempBlocks load(CompoundTag tag, HolderLookup.Provider registries) {
        UnfinishedTempBlocks data = new UnfinishedTempBlocks();
        HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Kind kind = entry.getString("kind").equals("shard") ? Kind.SHARD : Kind.GROUND;
            data.entries.put(entry.getLong("pos"), new Entry(NbtUtils.readBlockState(blocks, entry.getCompound("original")),
                    NbtUtils.readBlockState(blocks, entry.getCompound("placed")), entry.getLong("restoreAt"), kind));
        }
        ListTag holes = tag.getList("holes", Tag.TAG_COMPOUND);
        for (int i = 0; i < holes.size(); i++) {
            CompoundTag hole = holes.getCompound(i);
            data.holes.add(new Hole(BlockPos.of(hole.getLong("centre")), hole.getInt("radius"), hole.getInt("surface"),
                    hole.getLong("until")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Long2ObjectMap.Entry<Entry> entry : entries.long2ObjectEntrySet()) {
            Entry value = entry.getValue();
            CompoundTag saved = new CompoundTag();
            saved.putLong("pos", entry.getLongKey());
            saved.put("original", NbtUtils.writeBlockState(value.original));
            saved.put("placed", NbtUtils.writeBlockState(value.placed));
            saved.putLong("restoreAt", value.restoreAt);
            saved.putString("kind", value.kind == Kind.SHARD ? "shard" : "ground");
            list.add(saved);
        }
        tag.put("blocks", list);
        ListTag holeList = new ListTag();
        for (Hole hole : holes) {
            CompoundTag saved = new CompoundTag();
            saved.putLong("centre", hole.centre().asLong());
            saved.putInt("radius", hole.radius());
            saved.putInt("surface", hole.surface());
            saved.putLong("until", hole.until());
            holeList.add(saved);
        }
        tag.put("holes", holeList);
        return tag;
    }
}
