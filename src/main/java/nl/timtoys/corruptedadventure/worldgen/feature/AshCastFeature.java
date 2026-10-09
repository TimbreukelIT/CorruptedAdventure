package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:ash_cast} (change 49): a group of 1–3 Pompeii-like figures of ash (crawling, kneeling,
 * standing with an arm raised, lying, curled up), the glowing {@code smoldering_ash} in their cracks. Each figure turns
 * to the nearest volcano ({@link Ring3Terrain#facing}); it stands on level listed ground (every column of its footprint
 * at the same height), inside its own chunk at least one block from the edge, and only fills air or replaceable plants.
 * Reads the frozen {@code WORLD_SURFACE_WG} heightmap and its own chunk; writes only its own chunk.
 */
public final class AshCastFeature extends Feature<AshCastConfiguration> {
    private static final int MARGIN = 1;

    public AshCastFeature(Codec<AshCastConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AshCastConfiguration> context) {
        AshCastConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
        int count = config.count().sample(random);
        boolean placed = false;
        for (int i = 0; i < count; i++) {
            int x = origin.getX() + (i == 0 ? 0 : random.nextInt(2 * config.spread() + 1) - config.spread());
            int z = origin.getZ() + (i == 0 ? 0 : random.nextInt(2 * config.spread() + 1) - config.spread());
            ResourceLocation id = config.templates().get(random.nextInt(config.templates().size()));
            Rotation rotation = Ring3Terrain.facing(level, x, z, random);
            placed |= place(level, manager.getOrCreate(id), rotation, new BlockPos(x, 0, z), config);
        }
        return placed;
    }

    /** One figure with its first column at {@code at} (x, z); false where it does not fit. */
    static boolean place(WorldGenLevel level, StructureTemplate template, Rotation rotation, BlockPos at, AshCastConfiguration config) {
        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (Holder<Block> material : config.materials()) {
            infos.addAll(template.filterBlocks(BlockPos.ZERO, settings, material.value()));
        }
        if (infos.isEmpty()) {
            return false;
        }
        int minX = at.getX() & ~15;
        int minZ = at.getZ() & ~15;
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX(), at.getZ()) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int lowY = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : infos) {
            lowY = Math.min(lowY, info.pos().getY());
        }
        List<BlockPos> targets = new ArrayList<>(infos.size());
        for (StructureTemplate.StructureBlockInfo info : infos) {
            int x = at.getX() + info.pos().getX();
            int z = at.getZ() + info.pos().getZ();
            if (x < minX + MARGIN || x >= minX + 16 - MARGIN || z < minZ + MARGIN || z >= minZ + 16 - MARGIN) {
                return false;
            }
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            if (ground != base || !level.getBlockState(pos.set(x, ground, z)).is(config.ground())) {
                return false;
            }
            BlockPos target = new BlockPos(x, base + 1 + info.pos().getY() - lowY, z);
            BlockState current = level.getBlockState(target);
            if (!current.isAir() && !(current.canBeReplaced() && current.getFluidState().isEmpty())) {
                return false;
            }
            targets.add(target);
        }
        for (int i = 0; i < infos.size(); i++) {
            level.setBlock(targets.get(i), infos.get(i).state(), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
