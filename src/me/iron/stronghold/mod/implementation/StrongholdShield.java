package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.schine.graphicsengine.core.Timer;

import java.util.HashMap;

public class StrongholdShield extends SelectiveVoidShield {
    public StrongholdShield() {
        super();
    }
    public StrongholdShield(String name) {
        super(name);
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
    }

    @Override
    protected boolean isProtectedObject(SegmentController sc) {
        return super.isProtectedObject(sc) && !isInvulnerableCZ(sc.getSector(new Vector3i()));
    }

    protected boolean isInvulnerableCZ(Vector3i pos) {
        //CZ is invulnerable if it cant be conquered.
        if (getParent() instanceof StrongholdArea) {
            ControlZoneArea cz = ((StrongholdArea) getParent()).getCZAt(pos);
            return (cz != null && !cz.canBeConquered());
        }else
            return false;
    }
}
