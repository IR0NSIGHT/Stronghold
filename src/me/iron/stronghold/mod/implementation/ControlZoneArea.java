package me.iron.stronghold.mod.implementation;

import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.effects.map.AreaMapDrawer;
import me.iron.stronghold.mod.effects.map.FactionRelation;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import me.iron.stronghold.mod.effects.map.RadarMapDrawer;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 19:01
 */
public class ControlZoneArea extends StellarControllableArea {
    private int idx;
    public ControlZoneArea() {
        super();
    }

    public ControlZoneArea(Vector3i sector, int index) {
        super(sector,sector,getNameFromIndex(index));
        this.idx = index;
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (isServer() && GameServerState.instance.getUniverse().isSectorLoaded(getDimensionsStart())) {
            updateLoaded(timer);
        }
    //    requestSynchToClient(this);
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
            case 0: return "CZ Alpha";
            case 1: return "CZ Bravo";
            case 2: return "CZ Charlie";
            case 3: return "CZ Delta";
            case 4: return "CZ Echo";
            case 5: return "CZ Foxtrot";
            case 6: return "CZ Gamma";
            default: return"CZ Zulu";
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
        if (a instanceof ControlZoneArea)
            idx = ((ControlZoneArea) a).getIdx();
    }

    @Override
    public LinkedList<SimpleMapMarker> getMarkers() {
        LinkedList<SimpleMapMarker> out = super.getMarkers();
        SimpleMapMarker m =  new SimpleMapMarker(
                AreaMapDrawer.areaSprite,
                0,
                RadarMapDrawer.getColorFromRelation(
                        FactionRelation.getRelation(
                                getOwnerFaction(),
                                ((AbstractControllableArea)getParent()).getOwnerFaction(),
                                GameClientState.instance.getFactionManager()
                        )),
                AbstractMapDrawer.posFromSector(getDimensionsStart(),true)
        );
        m.setScale(0.01f);
        out.add(m);

        return out;
    }

}
