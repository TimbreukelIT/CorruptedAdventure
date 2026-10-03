package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * {@code corruptedadventure:surface_fossil}: a vanilla fossil template half buried in the sand, weathered
 * ({@link FossilLayout}). Vanilla's {@code FossilFeature} buries its template 15–25 blocks deep and writes up to 16
 * blocks into neighbouring chunks; this one lies in its own chunk, at least one block from the edge, and reads only
 * the {@code WORLD_SURFACE_WG} heightmap and blocks of that chunk. Only bone blocks are written, replacing air,
 * replaceable plants or ground.
 * <p>
 * Templates come from the server's structure template manager, as vanilla's fossils do (its cache is concurrent).
 */
public final class SurfaceFossilFeature extends Feature<SurfaceFossilConfiguration> {
    private static final Rotation[] ROTATIONS = Rotation.values();

    public SurfaceFossilFeature(Codec<SurfaceFossilConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfaceFossilConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SurfaceFossilConfiguration config = context.config();
        BlockPos origin = context.origin();
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
        List<ResourceLocation> templates = config.templates();
        int first = random.nextInt(templates.size());
        int rotation = random.nextInt(ROTATIONS.length);
        float buried = config.buried().sample(random);
        // A template too wide in this rotation is tried a quarter turn further, then the next template.
        List<FossilLayout.Bone> bones = List.of();
        for (int i = 0; i < templates.size() && !FossilLayout.fits(bones); i++) {
            StructureTemplate template = manager.getOrCreate(templates.get((first + i) % templates.size()));
            bones = bones(template, ROTATIONS[rotation]);
            if (!FossilLayout.fits(bones)) {
                bones = bones(template, ROTATIONS[(rotation + 1) % ROTATIONS.length]);
            }
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        List<FossilLayout.Placed> placed = FossilLayout.place(bones, origin.getX(), origin.getZ(), buried, config.integrity(), random,
                new FossilLayout.Terrain() {
                    @Override
                    public int groundTop(int x, int z) {
                        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    }

                    @Override
                    public boolean ground(int x, int y, int z) {
                        return level.getBlockState(probe.set(x, y, z)).is(config.ground());
                    }

                    @Override
                    public boolean free(int x, int y, int z) {
                        BlockState state = level.getBlockState(probe.set(x, y, z));
                        return state.isAir() || state.is(config.ground()) || state.canBeReplaced() && state.getFluidState().isEmpty();
                    }
                });
        for (FossilLayout.Placed bone : placed) {
            level.setBlock(bone.pos(), bone.state(), Block.UPDATE_CLIENTS);
        }
        return !placed.isEmpty();
    }

    /** The bone blocks of a template in this rotation, shifted so its bounding box starts at 0, 0, 0. */
    static List<FossilLayout.Bone> bones(StructureTemplate template, Rotation rotation) {
        // Worldgen threads share the template; NeoForge made the palette's per-block cache concurrent (MC-271899).
        List<StructureTemplate.StructureBlockInfo> infos = template.filterBlocks(BlockPos.ZERO,
                new StructurePlaceSettings().setRotation(rotation), Blocks.BONE_BLOCK);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : infos) {
            minX = Math.min(minX, info.pos().getX());
            minY = Math.min(minY, info.pos().getY());
            minZ = Math.min(minZ, info.pos().getZ());
        }
        List<FossilLayout.Bone> bones = new ArrayList<>(infos.size());
        for (StructureTemplate.StructureBlockInfo info : infos) {
            bones.add(new FossilLayout.Bone(info.pos().getX() - minX, info.pos().getY() - minY, info.pos().getZ() - minZ, info.state()));
        }
        return bones;
    }
}
