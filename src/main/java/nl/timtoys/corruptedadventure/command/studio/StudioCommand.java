package nl.timtoys.corruptedadventure.command.studio;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * {@code /ca studio}: the photo studio for structures (S0, dev only, see {@code docs/plans/structures/s0-studio.md}).
 * Places one template or registered structure in the studio dimensions (an empty void by day and by night, from the
 * datapack {@code tools/structures/studio-datapack}, which never ships in the jar) or in-situ in the source's level,
 * and lists the camera poses that {@code tools/structures/studio.sh} photographs. Server-side only: the camera is the
 * player, teleported.
 *
 * <pre>
 * /ca studio template &lt;id&gt; [studio|here|&lt;pos&gt;] [rotation] [mirror]
 * /ca studio structure &lt;id&gt; [studio|here|&lt;pos&gt;] [seed]
 * /ca studio shots [floors]      poses of the current placement; floors "y,y,..." overrides the detected ones
 * /ca studio cut &lt;y&gt; | restore   remove everything above y in the box (cut-away) / put it back
 * /ca studio clear | info
 * </pre>
 */
public final class StudioCommand {
    public static final ResourceKey<Level> STUDIO = dimension("studio");
    public static final ResourceKey<Level> STUDIO_NIGHT = dimension("studio_night");
    /** Things stand on the studio's invisible floor around x = z = 0. */
    static final int STUDIO_GROUND_Y = StudioSession.STUDIO_FLOOR_Y + 1;
    /** The seed of a structure placement without one: reruns place the same pieces. */
    static final long DEFAULT_SEED = 20260930L;
    private static final int MAX_VIEW_DISTANCE = 32;
    private static final float CLEAR_VIEW_STEP = 10.0F;
    private static final float CLEAR_VIEW_MAX_PITCH = 80.0F;

    private static final SuggestionProvider<CommandSourceStack> TEMPLATES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(context.getSource().getLevel().getStructureManager().listTemplates(), builder);

    private StudioCommand() {
    }

    public static void register(IEventBus gameBus) {
        StudioSession.register(gameBus);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("studio")
                .then(Commands.literal("template").then(templateLocations(Commands.argument("template", ResourceLocationArgument.id())
                        .suggests(TEMPLATES))))
                .then(Commands.literal("structure").then(structureLocations(Commands.argument("structure",
                        ResourceKeyArgument.key(Registries.STRUCTURE)))))
                .then(Commands.literal("shots")
                        .executes(context -> shots(context.getSource(), null))
                        .then(Commands.argument("floors", StringArgumentType.greedyString())
                                .executes(context -> shots(context.getSource(), StringArgumentType.getString(context, "floors")))))
                .then(Commands.literal("cut").then(Commands.argument("y", IntegerArgumentType.integer())
                        .executes(context -> cut(context.getSource(), IntegerArgumentType.getInteger(context, "y")))))
                .then(Commands.literal("restore").executes(context -> restore(context.getSource())))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("info").executes(context -> info(context.getSource())));
    }

    // --- where ---------------------------------------------------------------------------------------------------

    /** Where a placement goes: the studio (both dimensions), or one position in the source's level. */
    private record Where(boolean studio, @Nullable BlockPos pos) {
        static final Where IN_STUDIO = new Where(true, null);
    }

    private interface WhereGetter {
        Where get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    private interface Run {
        int run(CommandContext<CommandSourceStack> context, Where where) throws CommandSyntaxException;
    }

    /** {@code <then>} under each location ({@code studio}, {@code here}, {@code <pos>}) and the bare form (studio). */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T locations(T node, Run run,
            Function<WhereGetter, List<ArgumentBuilder<CommandSourceStack, ?>>> options) {
        node.executes(context -> run.run(context, Where.IN_STUDIO));
        WhereGetter studio = context -> Where.IN_STUDIO;
        WhereGetter here = context -> new Where(false, BlockPos.containing(context.getSource().getPosition()));
        WhereGetter at = context -> new Where(false, BlockPosArgument.getBlockPos(context, "pos"));
        node.then(withOptions(Commands.literal("studio").executes(context -> run.run(context, studio.get(context))), options.apply(studio)));
        node.then(withOptions(Commands.literal("here").executes(context -> run.run(context, here.get(context))), options.apply(here)));
        node.then(withOptions(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> run.run(context, at.get(context))),
                options.apply(at)));
        return node;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> withOptions(ArgumentBuilder<CommandSourceStack, ?> node,
            List<ArgumentBuilder<CommandSourceStack, ?>> options) {
        options.forEach(node::then);
        return node;
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T templateLocations(T node) {
        return locations(node, (context, where) -> template(context, where, Rotation.NONE, Mirror.NONE), where -> List.of(
                Commands.argument("rotation", TemplateRotationArgument.templateRotation())
                        .executes(context -> template(context, where.get(context), TemplateRotationArgument.getRotation(context, "rotation"), Mirror.NONE))
                        .then(Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
                                .executes(context -> template(context, where.get(context), TemplateRotationArgument.getRotation(context, "rotation"),
                                        TemplateMirrorArgument.getMirror(context, "mirror"))))));
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T structureLocations(T node) {
        return locations(node, (context, where) -> structure(context, where, DEFAULT_SEED), where -> List.of(
                Commands.argument("seed", LongArgumentType.longArg())
                        .executes(context -> structure(context, where.get(context), LongArgumentType.getLong(context, "seed")))));
    }

    private static List<ServerLevel> levels(CommandSourceStack source, Where where) throws CommandSyntaxException {
        if (!where.studio()) {
            return List.of(source.getLevel());
        }
        MinecraftServer server = source.getServer();
        ServerLevel day = server.getLevel(STUDIO);
        ServerLevel night = server.getLevel(STUDIO_NIGHT);
        if (day == null || night == null) {
            throw StudioSession.fail("No studio dimensions in this world: start the server with tools/test/server.sh start <level> --studio"
                    + " (installs tools/structures/studio-datapack), or place it here / at a position");
        }
        return List.of(day, night);
    }

    // --- placing -------------------------------------------------------------------------------------------------

    private static int template(CommandContext<CommandSourceStack> context, Where where, Rotation rotation, Mirror mirror)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResourceLocation id = ResourceLocationArgument.getId(context, "template");
        StructureTemplate template;
        try {
            template = source.getLevel().getStructureManager().get(id)
                    .orElseThrow(() -> StudioSession.fail("No structure template " + id));
        } catch (ResourceLocationException e) {
            throw StudioSession.fail("Invalid template id " + id);
        }
        // Jigsaw blocks become what they would turn into in a jigsaw structure; data structure blocks are left out.
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror)
                .addProcessor(JigsawReplacementProcessor.INSTANCE).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        BlockPos pos = where.studio() ? centred(template.getBoundingBox(settings, BlockPos.ZERO)) : where.pos();
        String what = "template " + id + " " + rotation.getSerializedName() + " " + mirror.getSerializedName();
        StudioSession session = StudioSession.place(source.getServer(), what, where.studio(), levels(source, where), level -> new StudioSession.Prepared() {
            @Override
            public BoundingBox box() {
                return template.getBoundingBox(settings, pos);
            }

            @Override
            public void place() throws CommandSyntaxException {
                if (!template.placeInWorld(level, pos, pos, settings, StructureBlockEntity.createRandom(DEFAULT_SEED), Block.UPDATE_CLIENTS)) {
                    throw StudioSession.fail("Template " + id + " placed nothing");
                }
            }
        });
        return placed(source, session);
    }

    /** The template origin that puts its box around x = z = 0 on the studio ground. */
    private static BlockPos centred(BoundingBox atZero) {
        return new BlockPos(-(atZero.minX() + atZero.maxX()) / 2, STUDIO_GROUND_Y - atZero.minY(), -(atZero.minZ() + atZero.maxZ()) / 2);
    }

    private static int structure(CommandContext<CommandSourceStack> context, Where where, long seed) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Holder.Reference<Structure> holder = ResourceKeyArgument.getStructure(context, "structure");
        ChunkPos chunk = where.studio() ? new ChunkPos(0, 0) : new ChunkPos(where.pos());
        String what = "structure " + holder.key().location() + " seed " + seed;
        StudioSession session = StudioSession.place(source.getServer(), what, where.studio(), levels(source, where), level -> {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            StructureStart start = holder.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), seed, chunk, 0, level, biome -> true);
            if (!start.isValid()) {
                throw StudioSession.fail("Structure " + holder.key().location() + " cannot start in chunk " + chunk.x + " " + chunk.z);
            }
            return new StudioSession.Prepared() {
                @Override
                public BoundingBox box() {
                    return start.getBoundingBox();
                }

                @Override
                public void place() {
                    BoundingBox box = start.getBoundingBox();
                    ChunkPos from = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
                    ChunkPos to = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
                    ChunkPos.rangeClosed(from, to).forEach(at -> start.placeInChunk(level, level.structureManager(), generator,
                            // A random per chunk from the seed (vanilla's /place uses the level's random): same blocks per rerun.
                            RandomSource.create(seed ^ at.toLong() * 0x9E3779B97F4A7C15L),
                            new BoundingBox(at.getMinBlockX(), level.getMinBuildHeight(), at.getMinBlockZ(), at.getMaxBlockX(),
                                    level.getMaxBuildHeight(), at.getMaxBlockZ()), at));
                }
            };
        });
        return placed(source, session);
    }

    private static int placed(CommandSourceStack source, StudioSession session) {
        source.sendSuccess(() -> Component.literal("Placed " + describe(session)), false);
        return 1;
    }

    private static String describe(StudioSession session) {
        StringBuilder text = new StringBuilder(session.what);
        text.append(session.inStudio ? " in the studio" : " in situ in " + session.stages.getFirst().dimension().location());
        text.append("\nbox ").append(StudioSession.describe(session.stages.getFirst().before().box()));
        text.append("\nstructure ").append(StudioSession.describe(session.tight));
        text.append("\nfloors ").append(join(session.floors));
        return text.toString();
    }

    // --- shots, cut, clear ---------------------------------------------------------------------------------------

    private static int shots(CommandSourceStack source, @Nullable String floorList) throws CommandSyntaxException {
        StudioSession session = StudioSession.require();
        if (session.columns.isEmpty()) {
            throw StudioSession.fail("The last placement failed: nothing to photograph (/ca studio clear)");
        }
        List<Integer> floors = floorList == null ? session.floors : parseFloors(floorList);
        List<StudioCamera.Pose> poses = StudioCamera.poses(session.columns, StudioFloors.cuts(floors));
        double[][] points = StudioCamera.corners(session.columns, Integer.MAX_VALUE);
        if (!session.inStudio) {
            ServerLevel level = session.primary(source.getServer());
            poses = poses.stream().map(pose -> clearView(level, session, pose)).toList();
        }
        double reach = 0.0;
        for (StudioCamera.Pose pose : poses) {
            reach = Math.max(reach, StudioCamera.reach(pose, points));
        }
        int view = Math.min(MAX_VIEW_DISTANCE, (int) Math.ceil(reach / 16.0) + 1);
        MinecraftServer server = source.getServer();
        int current = server.getPlayerList().getViewDistance();
        // Far cameras need the chunks of the far side; only ever raised, until the server restarts.
        if (view > current) {
            server.getPlayerList().setViewDistance(view);
        }
        ResourceKey<Level> day = session.stages.getFirst().dimension();
        ResourceKey<Level> night = session.inStudio ? STUDIO_NIGHT : day;
        StringBuilder text = new StringBuilder(session.what).append("\nfloors ").append(join(floors))
                .append(String.format(Locale.ROOT, "\nview %d (server %d; the client's render distance must reach it too)",
                        view, Math.max(view, current)));
        for (int i = 0; i < poses.size(); i++) {
            StudioCamera.Pose pose = poses.get(i);
            text.append(String.format(Locale.ROOT, "\npose %02d_%s %s %s %.2f %.2f %.2f %.1f %.1f %s", i + 1, pose.label(),
                    (pose.night() ? night : day).location(), pose.night() ? "night" : "day", pose.x(), pose.y(), pose.z(),
                    pose.yaw(), pose.pitch(), pose.cutY() == null ? "-" : pose.cutY().toString()));
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return poses.size();
    }

    /**
     * In situ, terrain or trees can stand between a pose and the structure: raise the camera (pitch up in steps of
     * {@link #CLEAR_VIEW_STEP}, at most to {@link #CLEAR_VIEW_MAX_PITCH}) until it sees the structure's middle and top.
     */
    private static StudioCamera.Pose clearView(ServerLevel level, StudioSession session, StudioCamera.Pose pose) {
        double[][] points = StudioCamera.corners(session.columns, pose.cutY() == null ? Integer.MAX_VALUE : pose.cutY());
        StudioCamera.Pose current = pose;
        float pitch = pose.pitch();
        while (blocked(level, session.tight, current) && pitch < CLEAR_VIEW_MAX_PITCH) {
            pitch = Math.min(CLEAR_VIEW_MAX_PITCH, pitch + CLEAR_VIEW_STEP);
            current = StudioCamera.frame(pose.label(), pose.night(), points, pose.yaw(), pitch, pose.cutY());
        }
        return current;
    }

    private static boolean blocked(ServerLevel level, BoundingBox structure, StudioCamera.Pose pose) {
        Vec3 eye = new Vec3(pose.x(), pose.eyeY(), pose.z());
        double cx = (structure.minX() + structure.maxX() + 1) / 2.0;
        double cz = (structure.minZ() + structure.maxZ() + 1) / 2.0;
        int top = pose.cutY() == null ? structure.maxY() : Math.min(structure.maxY(), pose.cutY());
        BoundingBox own = structure.inflatedBy(1);
        for (Vec3 target : List.of(new Vec3(cx, (structure.minY() + top + 1) / 2.0, cz), new Vec3(cx, top + 1.0, cz))) {
            BlockHitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK && !own.isInside(hit.getBlockPos())) {
                return true;
            }
        }
        return false;
    }

    static List<Integer> parseFloors(String list) throws CommandSyntaxException {
        List<Integer> floors = new ArrayList<>();
        for (String part : list.split("[,\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                floors.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                throw StudioSession.fail("Floors must be Y values like 64,69 (got " + part + ")");
            }
        }
        floors.sort(null);
        return floors;
    }

    private static int cut(CommandSourceStack source, int keepY) throws CommandSyntaxException {
        int removed = StudioSession.require().cut(source.getServer(), keepY);
        source.sendSuccess(() -> Component.literal("Cut above Y " + keepY + ": " + removed + " blocks removed (/ca studio restore)"), false);
        return removed;
    }

    private static int restore(CommandSourceStack source) throws CommandSyntaxException {
        boolean restored = StudioSession.require().restoreCut(source.getServer());
        source.sendSuccess(() -> Component.literal(restored ? "Cut-away restored" : "No cut-away to restore"), false);
        return restored ? 1 : 0;
    }

    private static int clear(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (StudioSession.clear(server)) {
            source.sendSuccess(() -> Component.literal("Studio cleared"), false);
            return 1;
        }
        // No session (e.g. after a restart): empty the studio area of both studio dimensions.
        int wiped = 0;
        for (ResourceKey<Level> key : List.of(STUDIO, STUDIO_NIGHT)) {
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                wiped += StudioSession.wipeStudio(level, StudioSession.STUDIO_AREA);
            }
        }
        int blocks = wiped;
        source.sendSuccess(() -> Component.literal("No session; studio area emptied (" + blocks + " blocks)"), false);
        return 1;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        StudioSession session = StudioSession.require();
        source.sendSuccess(() -> Component.literal(describe(session)), false);
        return 1;
    }

    private static String join(List<Integer> values) {
        return values.isEmpty() ? "-" : String.join(",", values.stream().map(String::valueOf).toList());
    }

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(CorruptedAdventure.MOD_ID, name));
    }
}
