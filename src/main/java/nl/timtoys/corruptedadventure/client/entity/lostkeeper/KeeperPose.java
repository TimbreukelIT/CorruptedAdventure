package nl.timtoys.corruptedadventure.client.entity.lostkeeper;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperBody;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Lays The Lost Keeper's model along its body chain, per frame, right before a bone is drawn (after the animations):
 * each segment bone {@code seg_NN} gets its pivot on node NN-1 and its length along the line to node NN; the head's
 * {@code neck_mount} tips with the look; a crown branch {@code coral_0K} that has broken off is hidden.
 * <p>
 * Model space (GeckoLib, in model units): the entity renderer turns the model by 180° − body yaw round +y, a bone's
 * position x is applied negated, its rotation is Rz·Ry·Rx round its pivot. So a world offset goes into model space by
 * the inverse yaw turn, and a segment pointing along unit {@code d} (its rest direction is +z) needs
 * Ry(atan2(d.x, d.z))·Rx(asin(−d.y)). The preview tool {@code tools/textures/lost_keeper_preview.py} uses the same rule.
 * <p>
 * While it staggers a shudder runs down the body: each segment rolls and sways a little, a beat later than the one
 * before it (only the model; the hitboxes stay on the nodes).
 */
final class KeeperPose {
    private static final String SEGMENT = "seg_";
    private static final String CORAL = "coral_";
    private static final String NECK = "neck_mount";
    /** Radians of the stagger's roll and sway at their strongest. */
    private static final float SHUDDER_ROLL = 0.35F;
    private static final float SHUDDER_SWAY = 0.12F;

    private KeeperPose() {
    }

    static void apply(TheLostKeeper keeper, GeoBone bone, float partialTick) {
        String name = bone.getName();
        if (name.startsWith(SEGMENT)) {
            int index = parse(name, SEGMENT.length());
            if (index >= 1 && index <= KeeperBody.SEGMENTS && keeper.body().isPlaced()) {
                segment(keeper, bone, index, partialTick);
            }
        } else if (name.equals(NECK)) {
            // Pitch positive looks down; the head's forward is -z, so the bone tips by minus the pitch.
            bone.setRotX(-keeper.getViewXRot(partialTick) * Mth.DEG_TO_RAD);
        } else if (name.startsWith(CORAL)) {
            int branch = parse(name, CORAL.length()) - 1;
            if (branch >= 0 && branch < TheLostKeeper.CROWN_BRANCHES) {
                bone.setHidden((keeper.crown() & (1 << branch)) == 0);
            }
        }
    }

    private static void segment(TheLostKeeper keeper, GeoBone bone, int index, float partialTick) {
        Vec3 origin = keeper.getPosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, keeper.yBodyRotO, keeper.yBodyRot);
        Vec3 front = toModel(keeper.body().node(index - 1, partialTick).subtract(origin), yaw);
        Vec3 back = toModel(keeper.body().node(index, partialTick).subtract(origin), yaw);
        Vec3 direction = back.subtract(front);
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        direction = direction.normalize();
        bone.setPosX(-(float) (front.x - bone.getPivotX()));
        bone.setPosY((float) (front.y - bone.getPivotY()));
        bone.setPosZ((float) (front.z - bone.getPivotZ()));
        float roll = 0.0F;
        float sway = 0.0F;
        if (keeper.isStaggered() && !keeper.isDeadOrDying()) {
            float wave = (keeper.tickCount + partialTick) * 0.9F - index * 0.7F;
            roll = SHUDDER_ROLL * Mth.sin(wave);
            sway = SHUDDER_SWAY * Mth.sin(wave * 1.3F + 1.1F);
        }
        bone.setRotX((float) Math.asin(Mth.clamp(-direction.y, -1.0, 1.0)));
        bone.setRotY((float) Mth.atan2(direction.x, direction.z) + sway);
        bone.setRotZ(roll);
    }

    /** A world offset (blocks) into model space (model units) for a body yaw in degrees. */
    static Vec3 toModel(Vec3 offset, float bodyYaw) {
        double angle = (bodyYaw - 180.0F) * Mth.DEG_TO_RAD;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(offset.x * cos + offset.z * sin, offset.y, -offset.x * sin + offset.z * cos).scale(16.0);
    }

    private static int parse(String name, int from) {
        try {
            return Integer.parseInt(name.substring(from));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
