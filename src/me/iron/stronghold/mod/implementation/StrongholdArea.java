package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;


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
        //generate the contorlpoints.
        int i = 0;
        for (Vector3i pos: getCPSectors(from, to,6)) {
            ControlPointArea a = new ControlPointArea(pos,i);
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
        Vector3i center = new Vector3i(from);
        center.sub(from); center.scaleFloat(0.5f); center.add(from);
        for (int i = 0; i < amount; i++) {
            Vector3f dir = new Vector3f(r.nextFloat(),r.nextFloat(),r.nextFloat());
            dir.normalize(); dir.scale(6);
            Vector3i p =new Vector3i(dir);
            p.add(center);
            out.add(p);
        }
        return out;
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        super.onConquered(area, oldOwner);
        if (area instanceof ControlPointArea) {
            boolean ally =GameServerState.instance.getFactionManager().isFriend(area.getOwnerFaction(),this.getOwnerFaction());
            if (area.getOwnerFaction()==this.getOwnerFaction() || ally)
                lastOwned =((ControlPointArea) area).getIdx();
            else
                lastOwned = ((ControlPointArea) area).getIdx()-1;
        }
    }

    /**
     * get area that has to be conquered before this one
     * @param next
     * @return
     */
    public boolean getVulnerable(ControlPointArea next) {
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
}
