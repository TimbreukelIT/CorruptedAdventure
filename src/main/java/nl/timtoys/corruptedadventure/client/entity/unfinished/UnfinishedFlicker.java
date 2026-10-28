package nl.timtoys.corruptedadventure.client.entity.unfinished;

import nl.timtoys.corruptedadventure.entity.unfinished.Graft;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.List;
import java.util.Map;

/**
 * The model swaps of The Unfinished (client only; the state comes from the server: {@link TheUnfinished#isLoaded},
 * {@link TheUnfinished#lostGrafts}). Unloaded, each weak spot's wire-frame variant and the loose unloaded bits (chest
 * wire, shard, slices, the wire leg) flicker: on most frames there, now and then gone, each on its own irregular beat,
 * and the Layered's front leg now and then swaps with its wire frame. Loaded, the flicker stops: the weak spots show their
 * solid variant (sky glass rims) and the loose bits are gone. A lost graft hides its whole limb. The beat hashes the
 * entity id and the tick, so two of them never flicker in step and nothing random is drawn.
 */
final class UnfinishedFlicker {
    private static final Map<Graft, List<String>> GRAFT_BONES = Map.of(
            Graft.HOLLE, List.of("graft_holle_arm"),
            Graft.GELAAGDE, List.of("graft_gelaagde_leg_1", "graft_gelaagde_leg_2", "graft_gelaagde_leg_3"),
            Graft.HERRIJZENDE, List.of("graft_herrijzende_wing"),
            Graft.HOEDER, List.of("graft_hoeder_tail"));
    private static final Map<Graft, String> WEAK_BONES = Map.of(Graft.HOLLE, "weak_holle", Graft.GELAAGDE, "weak_gelaagde",
            Graft.HERRIJZENDE, "weak_herrijzende", Graft.HOEDER, "weak_hoeder");
    private static final List<String> LOOSE_BITS = List.of("unloaded_wire_chest", "unloaded_missing_shard",
            "unloaded_slice_1", "unloaded_slice_2", "unloaded_slice_3");
    private static final String WIRE_LEG = "unloaded_wire_leg";
    private static final String FRONT_LEG = "graft_gelaagde_leg_1_lower";
    /** Ticks per flicker frame. */
    private static final int BEAT = 2;

    private UnfinishedFlicker() {
    }

    static void apply(BakedGeoModel model, TheUnfinished boss) {
        boolean loaded = boss.isLoaded();
        int lost = boss.lostGrafts();
        int seed = boss.getId();
        int frame = boss.tickCount / BEAT;
        int salt = 0;
        for (Graft graft : Graft.values()) {
            boolean gone = (lost & graft.bit()) != 0;
            for (String bone : GRAFT_BONES.get(graft)) {
                show(model, bone, !gone);
            }
            String weak = WEAK_BONES.get(graft);
            show(model, weak + "_loaded", loaded);
            show(model, weak + "_unloaded", !loaded && there(seed, ++salt, frame));
        }
        for (String bone : LOOSE_BITS) {
            show(model, bone, !loaded && there(seed, ++salt, frame));
        }
        boolean legs = (lost & Graft.GELAAGDE.bit()) == 0;
        boolean wire = legs && !loaded && swapped(seed, frame);
        show(model, WIRE_LEG, wire);
        if (legs) {
            // The real leg's cubes go while its wire frame shows; its children (the weak spot) stay.
            model.getBone(FRONT_LEG).ifPresent(bone -> {
                bone.setHidden(wire);
                bone.setChildrenHidden(false);
            });
        }
    }

    private static void show(BakedGeoModel model, String name, boolean visible) {
        model.getBone(name).ifPresent(bone -> bone.setHidden(!visible));
    }

    /** On about 4 frames in 5; the gaps come in short irregular runs. */
    static boolean there(int seed, int salt, int frame) {
        return (hash(seed, salt, frame) & 0xF) >= 3 && (hash(seed, salt, frame / 3) & 0x1F) != 0;
    }

    /** The front leg shows its wire frame in bursts of a few frames, a few times a minute. */
    static boolean swapped(int seed, int frame) {
        return (hash(seed, 99, frame / 4) & 0x1F) == 0 || (hash(seed, 98, frame) & 0x3F) == 0;
    }

    private static int hash(int seed, int salt, int frame) {
        int h = seed * 0x9E3779B1 + salt * 0x85EBCA6B + frame * 0xC2B2AE35;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return h;
    }
}
