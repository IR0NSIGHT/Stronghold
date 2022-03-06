package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.util.*;


/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 18:58
 */
public class StrongholdArea extends StellarControllableArea {
    private transient OwnerMap ownerMap;
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
        LinkedList<Vector3i> sector = getCPSectors(getDimensionsStart(), getDimensionsEnd(),6);
        ownerMap = new OwnerMap(sector.size());
        for (Vector3i pos: sector) {
            ControlZoneArea a = new ControlZoneArea(pos,i);
            ownerMap.setOwner(i,0);
            addChildObject(a);

            i++;
        }
    }

    @Override
    protected void onFirstUpdateRuntime() {
        super.onFirstUpdateRuntime();
        int i = 0;
        for (SendableUpdateable child: getChildren()) {
            if (child instanceof ControlZoneArea) {
                i++;
            }
        }
        ownerMap = new OwnerMap(i);
        for (SendableUpdateable child: getChildren()) {
            if (child instanceof ControlZoneArea) {
               ownerMap.setOwner (((ControlZoneArea) child).getIdx() ,((ControlZoneArea) child).getOwnerFaction());
            }
        }
        System.out.println("OWNERMAP AFTER INIT: "+ownerMap.toString());
    }

    /**
     * get highest index of the control zones owned by stronghold owner.
     * @return
     */
    public int getLastOwned() {
        return lastOwned;
    }

    public long getTimeoutAfterConquer() {
        return timeoutAfterConquer;
    }

    @Override
    public boolean canBeConquered() {
        return true;
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

    /**
     * if owning faction and allies hold no more territory, area is conquered by faction that owned the most CZs.
     * @param area
     * @param oldOwner
     */
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

        //test if the owning faction and its allies still hold any control zones.
        boolean lostAll = true;
        Collection<Integer> owningFs = ownerMap.getOwningFactions();
        int mostOwned = 0; int mostOwner = 0;
        for (Integer f: owningFs) {
            if (f == getOwnerFaction() || isAllied(f)) {
                lostAll = false; break;
            }
            //update faction that owns the most CZs, exclude neutral.
            if (f != 0 && ownerMap.getOwnedBy(f).size()>mostOwned) {
                mostOwned = ownerMap.getOwnedBy(f).size();
                mostOwner = f;
            }
        }
        if (lostAll) {
            setOwnerFaction(mostOwner);
        }

    }

    private boolean isAllied(int faction) {
        return GameServerState.instance.getFactionManager().isFriend(faction,this.getOwnerFaction());

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
    public static void main(String[] args) {
        OwnerMap m = new OwnerMap(10);
        Random r = new Random(420);
        for (int x = 0; x < 10; x++) {
            for (int i = 0; i < m.getAmountIndices(); i++) {
                m.setOwner(i,r.nextInt(4)-1);
            }
            m.setOwner(4,0);
            System.out.println(m);
        }
        for (Integer faction: m.getOwningFactions()) {
            System.out.println(String.format("Faction %s owns %s cp's",faction,m.getOwnedBy(faction).size()));
        }
    }

    @Override
    public boolean isVisibleOnMap() {
        return true;
    }
    static class OwnerMap {
        private HashMap<Integer,LinkedList<Integer>> factions;
        private int[] indices; //idx[0] = 10001> idx 0 is owned by factionid 10001

        public OwnerMap(int amountIndices) {
            indices = new int[amountIndices];
            factions = new HashMap<>(amountIndices);
            for (int i = 0; i < amountIndices; i++) {
                setOwner(i,0);
            }
        }

        public int getAmountIndices() {
            return indices.length;
        }

        public Collection<Integer> getOwningFactions() {
            return factions.keySet();
        }

        public void setOwner(int idx, int faction) {
            assert idx < indices.length:"out of bounds, ownermap idx "+idx+ " out of array "+Arrays.toString(indices);
            //get previous owner
            int oldOwner = indices[idx];
            //remove idx from old owner
            LinkedList<Integer> idcs = factions.get(oldOwner);
            if (idcs != null) {
                idcs = factions.get(oldOwner);
                assert idcs != null: "idx " + idx+ "has owner "+oldOwner+", but owner doesnt have idx.";
                idcs.remove((Object)idx);
                if (idcs.isEmpty()) //garbage collection
                    factions.remove(oldOwner);
            }



            //set owner of index
            indices[idx] = faction;

            //add idx to new owner
            idcs = factions.get(faction);
            if (idcs == null) {
                idcs =new LinkedList<>();
                factions.put(faction,idcs);
            }
            if (!idcs.contains(idx))
                idcs.add(idx);

        }

        public int getOwnerFaction(int idx) {
            assert idx < indices.length;
            return indices[idx];
        }

        public LinkedList<Integer> getOwnedBy(int factionid) {
            if (factions.containsKey(factionid))
                return factions.get(factionid);
            else
                return new LinkedList<>();
        }

        @Override
        public String toString() {
            return "OwnerMap{" +
                    "factions=" + factionToString() +
                    ", indices=" + Arrays.toString(indices) +
                    '}';
        }
        private String factionToString() {
            StringBuilder b = new StringBuilder("{");
            Iterator<Integer> faction = factions.keySet().iterator();
            while (faction.hasNext()) {
                int i = faction.next();
                b.append("[").append(i).append("]:{");
                Iterator<Integer> it = factions.get(i).iterator();
                while(it.hasNext()) {
                    b.append(it.next());
                    if (it.hasNext())
                        b.append(", ");
                }
                b.append("}");
                if (faction.hasNext())
                    b.append(", ");
            }
            b.append("}");
            return b.toString();
        }
    }
}

