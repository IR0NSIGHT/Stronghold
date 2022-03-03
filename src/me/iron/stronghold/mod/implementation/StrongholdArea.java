package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.util.LinkedList;
import java.util.Random;


/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 18:58
 */
public class StrongholdArea extends StellarControllableArea {
    private int lastOwned;

    private long timeoutAfterConquer = 1000*60*60*24;
    public StrongholdArea() {
        super();
    }

    public StrongholdArea(Vector3i from, Vector3i to) {
        super(from, to, "Stronghold");
    }

    @Override
    protected void onFirstUpdatePersistent() {
        super.onFirstUpdatePersistent();
        //generate child objects
        int i = 0;
        for (Vector3i pos: getCPSectors(getDimensionsStart(), getDimensionsEnd(),6)) {
            ControlZoneArea a = new ControlZoneArea(pos,i);
            i++;
            addChildObject(a);
        }
    }

    public int getLastOwned() {
        return lastOwned;
    }

    public long getTimeoutAfterConquer() {
        return timeoutAfterConquer;
    }

    /**
     * generate control points random in area around middle between from and to pos
     * @param amount amount of CPs
     * @param from pos (sector)
     * @param to pos (sector)
     * @return
     */
    protected static LinkedList<Vector3i> getCPSectors(Vector3i from, Vector3i to, int amount) {
        long code =from.code()*to.code();
        LinkedList<Vector3i> out = new LinkedList<>();
        Random r = new Random(code);

        Vector3f basis = to.toVector3f();
        basis.sub(from.toVector3f());
        for (int i = 0; i < amount; i++) {
            Vector3f dir = new Vector3f(
                    basis.x*    (0.1f+0.8f*Math.abs(r.nextFloat())),
                    basis.y*    (0.1f+0.8f*Math.abs(r.nextFloat())),
                    basis.z*    (0.1f+0.8f*Math.abs(r.nextFloat())));
            dir.add(from.toVector3f());
            out.add(new Vector3i(dir));
        }
        return out;
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        super.onConquered(area, oldOwner);
        if (area instanceof ControlZoneArea) {
            boolean ally =GameServerState.instance.getFactionManager().isFriend(area.getOwnerFaction(),this.getOwnerFaction());
            if (area.getOwnerFaction()==this.getOwnerFaction() || ally)
                lastOwned =((ControlZoneArea) area).getIdx();
            else
                lastOwned = ((ControlZoneArea) area).getIdx()-1;
        }
    }

    /**
     * get area that has to be conquered before this one
     * @param next
     * @return
     */
    public boolean getVulnerable(ControlZoneArea next) {
        int idx = next.getIdx();
        return  (idx==lastOwned || idx==lastOwned+1); //TODO forced timeout for conquerin after a conquering -> no blitzkrieg allowed
    }

    @Override
    public String toString() {
        return super.toString() +
                ", lastOwned=" + lastOwned;
    }

    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        if (a instanceof StrongholdArea) {
            timeoutAfterConquer = ((StrongholdArea) a).getTimeoutAfterConquer();
            lastOwned = ((StrongholdArea) a).getLastOwned();
        }
    }

    @Override
    public boolean isVisibleOnMap() {
        return true;
    }

}
