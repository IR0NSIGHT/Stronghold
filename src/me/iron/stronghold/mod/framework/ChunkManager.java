package me.iron.stronghold.mod.framework;

import api.listener.Listener;
import api.listener.events.player.PlayerChangeSectorEvent;
import api.mod.StarLoader;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.Galaxy;
import org.schema.game.server.data.GameServerState;

import java.util.HashSet;
import java.util.LinkedList;

public class ChunkManager extends SendableUpdateable implements IAreaEvent {
    private int systemsPerGrid = 8;
    private AreaChunk[] chunks;
    private AreaManager manager;
    private Listener<PlayerChangeSectorEvent> listener;
    public ChunkManager(AreaManager am) {
        manager = am;
        chunks = new AreaChunk[128*128*128/(systemsPerGrid*systemsPerGrid*systemsPerGrid)];

        //if (am.isServer()) {
            //add sector change Listener
        listener = new Listener<PlayerChangeSectorEvent>() {
            @Override
            public void onEvent(PlayerChangeSectorEvent event) {
                if (GameServerState.instance==null || GameServerState.instance.getUniverse() == null)
                    return;
                //generate info, get chunks
                Vector3i start = new Vector3i(), end = new Vector3i();
                Sector s1=GameServerState.instance.getUniverse().getSector(event.getOldSectorId()),s2=GameServerState.instance.getUniverse().getSector(event.getNewSectorId());
                if (s1==null || s2==null)
                    return;
                start.set(s1.pos);
                end.set(s2.pos);
                AreaChunk startC = getChunkFromSector(start), endC = getChunkFromSector(end);

                //get intruding ship
                SimpleTransformableSendableObject s= event.getPlayerState().getFirstControlledTransformableWOExc();
                if (!(s instanceof Ship))
                    return;
                Ship ship = (Ship)s;

                //notify areas in relevant chunks //TODO jumping to warp causes wrong chunk maybe bc %arraylength??
                HashSet<StellarControllableArea> areas = new HashSet<>(10);
                if (startC!=null) {
                    for (SendableUpdateable c: startC.children)
                        areas.add((StellarControllableArea) c);
                }
                if (endC!=null && (startC==null || !startC.equals(endC))) {
                    for (SendableUpdateable c: endC.children)
                        areas.add((StellarControllableArea) c);
                }
                for (StellarControllableArea c: areas)
                    c.onShipChangeSector(start,end,ship);

            }
        };

        StarLoader.registerListener(PlayerChangeSectorEvent.class, listener, ModMain.instance);
    }

    @Override
    protected void destroy() {
        super.destroy();
        StarLoader.unregisterListener(PlayerChangeSectorEvent.class,listener);
        for (int i = 0; i < chunks.length; i++) {
            if (chunks[i]!=null)
                chunks[i].destroy();
        }
    }

    @Override
    public void beforeOverwrite(AbstractControllableArea area) {
        if (area instanceof StellarControllableArea) {
            removeArea((StellarControllableArea)area);
        }
    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {
        //area had major value changes, chunks might be changed =>
        if (area instanceof StellarControllableArea) {
            addArea((StellarControllableArea) area);
        }
    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {
        //called by stellar areas BEFORE they have their values killed
        if (area instanceof StellarControllableArea)
            removeArea((StellarControllableArea) area);
    }

    @Override
    public void onDestroy(AbstractControllableArea area) {

    }

    private static void mutateToGrid(Vector3i sector) {
        //to system
        int shift = 4+3;
        sector.x = sector.x>>shift; sector.y = sector.y>>shift; sector.z = sector.z>>shift;
        //to grid
    }

    public static void main(String[] args) {
        //AbstractChunk root = new AbstractChunk();
     /*   Vector3i v1 = new Vector3i(-16*8*128,-16*8,-1), v2 = new Vector3i(32,16,1);
        mutateToGrid(v1); mutateToGrid(v2);
        System.out.println(v1 +""+v2);
        //cm.initChunks();
        for (int i = 0; i < 7; i++) {
            int from = -1*16*128-1;
            int to = -1*16*128+5;
            cm.addArea(new StellarControllableArea(new Vector3i(from,-i,-i),new Vector3i(to,i,i),"Area_"+i));
        }*/


    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {

    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {

    }

    @Override
    public void onUpdate(AbstractControllableArea area) {

    }

    @Override //listening to areamanager
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {
        if (child instanceof StellarControllableArea) {
            if (!removed) {
                System.out.println("add stellar area to chunk");
                addArea((StellarControllableArea) child);
            } else {
                System.out.println("remove stellar area from chunk");
                removeArea((StellarControllableArea) child);
            }
        }
    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {

    }

    @Override
    public void onAttacked(long time, AbstractControllableArea area, int attackerFaction, Vector3i position) {

    }

    protected void removeArea(StellarControllableArea area) {
        //get all chunks the area is in
        assert area != null;
        LinkedList<Vector3i> grids = getChunkGridsForArea(area);
        for (Vector3i gridP: grids) {
            AreaChunk c = getChunkFromGrid(gridP);
            if (c != null) { //remove area from chunk
                c.removeChildObject(area);
                if (c.isEmpty())
                    removeChunk(c);
            }
        }
    }

    /**
     * add area, get list of chunk grids it is inside of.
     * @param area
     * @return
     */
    protected LinkedList<Vector3i> addArea(StellarControllableArea area) {
        assert area != null;
        LinkedList<Vector3i> grids = getChunkGridsForArea(area);
        for (Vector3i gridP: grids) {
            AreaChunk c = getChunkFromGrid(gridP); //mutates vec to gridpos
            if (c == null) { //create new chunk if doesnt exist yet.
                c = new AreaChunk(gridP);
                addChunk(c);
            }
            c.addChildObject(area);
        }
        return grids;
    }

    private LinkedList<Vector3i> getChunkGridsForArea(StellarControllableArea area) {
        LinkedList<Vector3i> chunks = new LinkedList<>();
        if (area.getDimensionsEnd()!=null&&area.getDimensionsStart()!=null) {
            Vector3i start = new Vector3i(area.getDimensionsStart());
            Vector3i end = new Vector3i(area.getDimensionsEnd());
            mutateToGrid(start);
            mutateToGrid(end);
            for (int x = start.x; x <= end.x; x++) {
                for (int y = start.y; y <= end.y; y++) {
                    for (int z = start.z; z <= end.z; z++) {
                        chunks.add(new Vector3i(x,y,z));
                    }
                }
            }
            assert chunks.size()>0:"area "+area+" couldnt find any chunks.";
        }
        return chunks;
    }

    public LinkedList<StellarControllableArea> getAreasFromSector(Vector3i sector) {
        //get all chunks
        AreaChunk chunk = getChunkFromSector(sector);
        if (chunk == null)
            return new LinkedList<>();
        return chunk.getAreasFromSector(sector);
    }

    private int getIndexFromGridPos(Vector3i gridPos) {
        gridPos = new Vector3i(gridPos);

        //totalChunks = galaxy systems/systemsPerChunk
        int totalChunks = Galaxy.size/systemsPerGrid;
        int totalChunksHalf = totalChunks/2;
        gridPos.add(totalChunksHalf,totalChunksHalf,totalChunksHalf);

        assert gridPos.x>=0 && gridPos.y >= 0 && gridPos.z >= 0:"gridpos smaller than 000:"+gridPos;
        assert gridPos.x<= totalChunks && gridPos.y <= totalChunks && gridPos.z <= totalChunks:"gridpos exceeds max value: "+gridPos;

        int idx=gridPos.x + gridPos.y * totalChunks + gridPos.z * totalChunks * totalChunks;
        assert 0<=idx && idx<chunks.length;
        return idx;
    }

    private void addChunk(AreaChunk c) {
        int idx = getIndexFromGridPos(c.gridPos);
        System.out.println("add chunk at grid "+c.gridPos);
        chunks[idx] = c;
    }

    private void removeChunk(AreaChunk c) {
        int idx = getIndexFromGridPos(c.gridPos);
        System.out.println("removing chunk "+c.gridPos);
        chunks[idx] = null;
    }

    /**
     * get chunk from internal chunk array
     * @param sector will be mutated to gridpos
     * @return
     */
    private AreaChunk getChunkFromSector(Vector3i sector) {
        sector = new Vector3i(sector);
        mutateToGrid(sector);
        return getChunkFromGrid(sector);
    }

    private AreaChunk getChunkFromGrid(Vector3i grid) {
        return chunks[Math.abs(getIndexFromGridPos(grid)% chunks.length)];
    }

    protected void printChunks() {
        for (AreaChunk c: chunks) {
            if (c == null)
                continue;
            System.out.print(c.getName()+":\n");
            for (SendableUpdateable a: c.children) {
                System.out.print("\t"+a.getName());
            }
            System.out.println();
        }
    }
}
