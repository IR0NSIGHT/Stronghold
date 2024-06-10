package me.iron.stronghold.mod.implementation;

import com.bulletphysics.linearmath.Transform;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.effects.map.AreaMapDrawer;
import me.iron.stronghold.mod.effects.map.FactionRelation;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapDrawable;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapLine;
import me.iron.stronghold.mod.effects.map.RadarMapDrawer;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.effects.Indication;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.util.Arrays;
import java.util.LinkedList;

public class StellarControllableArea extends AbstractControllableArea implements AreaShipMovementEvent, MapDrawable {
    //has a position/size that belongs to it.
    private final Vector3i[] dimensions = new Vector3i[2];
    //TODO config value
    public static Vector3i maxDimension = new Vector3i(16*8,16*8,16*8); //in sectors

    public StellarControllableArea() {
        super();
    }

    public StellarControllableArea(Vector3i start, Vector3i end, String name) {
        super(name);
        setDimensions(start,end);
    }

    @Override
    protected void destroy() {
        super.destroy();
    }

    /**
     * stets the dimensions of the area in sector coordinates. start must be less or equal in all fields than end.
     * @param start
     * @param end
     */
    public void setDimensions(Vector3i start, Vector3i end) {
        assert start!=null && end!=null;
        Vector3i dim = new Vector3i(end); dim.sub(start);
        if (dim.x < 0 || dim.y < 0 || dim.z < 0) {
            System.err.println("Dimensions start not smaller than end for Area "+ getName() +" start "+start+ " end " + end);
        }

        if (dim.x>maxDimension.x||dim.y>maxDimension.y||dim.z>maxDimension.z) {
            System.err.println("Dimensions for stellar area exceed maximum dimensions allowed: " + getName());
        }

        dimensions[0] = start;
        dimensions[1] = end;
    }

    public Vector3i getDimensionsStart() {
        return dimensions[0];
    }

    public Vector3i getDimensionsEnd() {
        return dimensions[1];
    }
    
    public boolean isSectorInArea(Vector3i sector) {
        assert sector != null;
        return (
                getDimensionsStart().x<=sector.x && sector.x <= getDimensionsEnd().x &&
                getDimensionsStart().y<=sector.y && sector.y <= getDimensionsEnd().y &&
                getDimensionsStart().z<=sector.z && sector.z <= getDimensionsEnd().z
        );
    }

    public LinkedList<PlayerState> getPlayersInArea() {
        LinkedList<PlayerState> found = new LinkedList<>();
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values())
            if (isSectorInArea(p.getCurrentSector()))
                found.add(p);
        return found;
    }

    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        assert a instanceof StellarControllableArea;
        Vector3i[] arr =((StellarControllableArea)a).dimensions;
        setDimensions(arr[0],arr[1]);
    }

    @Override
    public String toString() {
        return super.toString() +
                ", dimensions="+Arrays.toString(dimensions)+" ";
    }

    /**
     * input for sector change of any ship, will call event methods internally for specific movement types
     * @param oldPos
     * @param newPos
     * @param ship
     */
    public void onShipChangeSector(Vector3i oldPos, Vector3i newPos, Ship ship) {
        boolean startInArea = isSectorInArea(oldPos), endInArea = isSectorInArea(newPos);
        if (startInArea&&endInArea)
            onAreaInnerMovement(this,oldPos,newPos, ship);

        if (startInArea&& !endInArea)
            onAreaLeft(this, oldPos, ship);

        if (!startInArea && endInArea)
            onAreaEntered(this, newPos, ship);

    }

    @Override
    public void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object) {
        if (area.equals(this))
           log("[AREA] ship "+object.getName()+" entered "+area.getName()+" at "+enteredSector);
        //cascade downwards to children
        for (SendableUpdateable c: children) {
            if (c instanceof AreaShipMovementEvent) {
                ((AreaShipMovementEvent) c).onAreaEntered(area, enteredSector, object);
            }
        }
    }

    @Override
    public void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object) {
        //cascade downwards to children
        for (SendableUpdateable c: children) {
            if (c instanceof AreaShipMovementEvent) {
                ((AreaShipMovementEvent) c).onAreaInnerMovement(area,leftSector ,enteredSector, object);
            }
        }
    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        if (area.equals(this))
            log("[AREA] ship "+object.getName()+" left "+area.getName()+" at "+leftSector);

        //cascade downwards to children
        for (SendableUpdateable c: children) {
            if (c instanceof AreaShipMovementEvent) {
                ((AreaShipMovementEvent) c).onAreaLeft(area,leftSector, object);
            }
        }
    }
    private void log(String s) {
        //DebugFile.log(s,ModMain.instance);
        //System.out.println("[STRONGHOLDS]"+s);
    }

    @Override
    public LinkedList<SimpleMapMarker> getMarkers() {
        LinkedList<SimpleMapMarker> out = new LinkedList<>();
        for (SendableUpdateable u: children) {
            if (u instanceof MapDrawable && ((MapDrawable) u).isVisibleOnMap())
                out.addAll(((MapDrawable) u).getMarkers());
        }
        return out;
    }

    @Override
    public LinkedList<MapLine> getLines() {
        LinkedList<MapLine> out = new LinkedList<>();
        for (SendableUpdateable u: children) {
            if (u instanceof MapDrawable && ((MapDrawable) u).isVisibleOnMap())
                out.addAll(((MapDrawable) u).getLines());
        }
        assert getDimensionsStart() != null && getDimensionsEnd() != null;
        FactionRelation r = FactionRelation.getRelation(
                GameClientState.instance.getPlayer().getFactionId(),
                getOwnerFaction(),
                GameClientState.instance.getFactionManager());
        out.addAll(AreaMapDrawer.outlineSquare(getDimensionsStart().toVector3f(), getDimensionsEnd().toVector3f(),RadarMapDrawer.getColorFromRelation(r)));
        return out;
    }

    @Override
    public LinkedList<Indication> getIndications() {
        LinkedList<Indication> out = new LinkedList<>();
        Transform t = new Transform(); t.setIdentity(); t.origin.set(AbstractMapDrawer.posFromSector(getDimensionsStart().toVector3f(),true));
        out.add(new ConstantIndication(t,getName()+"["+ AbstractMapDrawer.getFactionName(getOwnerFaction())+"]"));
        for (SendableUpdateable u: children) {
            if (u instanceof MapDrawable && ((MapDrawable) u).isVisibleOnMap())
                out.addAll(((MapDrawable) u).getIndications());
        }
        return out;
    }

    @Override
    public boolean isVisibleOnMap() {
        return true;
    }
}
