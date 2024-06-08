package me.iron.stronghold.mod.utility;

import com.bulletphysics.linearmath.Transform;
import org.schema.game.common.controller.SegmentController;

import javax.vecmath.Vector3f;

public class SimpleTools {
    public static void moveObjectToInSectorPosition(SegmentController sc, Vector3f positionInSector) {
        Transform moved = new Transform(sc.getWorldTransform());
        moved.origin.set(positionInSector);
        sc.warpTransformable(moved, true, false, null);
    }
}
