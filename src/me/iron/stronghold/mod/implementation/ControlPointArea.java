package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 19:01
 */
public class ControlPointArea extends StellarControllableArea {
    private int idx;
    public ControlPointArea() {
        super();
    }

    public ControlPointArea(Vector3i sector, int index) {
        super(sector,sector,getNameFromIndex(index));
        this.idx = index;
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (isServer() && GameServerState.instance.getUniverse().isSectorLoaded(getDimensionsStart())) {
            updateLoaded(timer);
        }
    }

    private void updateLoaded(Timer timer) {
        try {
            //get the owner of the heaviest station
            int mass = 0; int stationOwner = 0;
            for (SimpleTransformableSendableObject obj: GameServerState.instance.getUniverse().getSector(getDimensionsStart()).getEntities()) {
                if (!obj.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                    continue;

                float objMass = ((SpaceStation)obj).getMassWithoutDockIncludingStation();
                if (objMass>mass) { //heaviest station will dominate the sector and become the new owner.
                    mass =(int) objMass;
                    stationOwner = obj.getFactionId();
                }
            }
            if (getOwnerFaction() != stationOwner && canBeConquered()) {
                setOwnerFaction(stationOwner);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canBeConquered() {
        return super.canBeConquered() && ( (StrongholdArea)getParent()).getVulnerable(this);
    }

    public int getIdx() {
        return idx;
    }

    private static String getNameFromIndex(int index) {
        switch (index) {
            case 0: return "Alpha";
            case 1: return "Bravo";
            case 2: return "Charlie";
            case 3: return "Delta";
            case 4: return "Echo";
            case 5: return "Foxtrot";
            case 6: return "Gamma";
            default: return "Zulu";
        }
    }

    @Override
    public String toString() {
        return super.toString() +
                "idx=" + idx;
    }

    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        if (a instanceof ControlPointArea)
            idx = ((ControlPointArea) a).getIdx();
    }
}
