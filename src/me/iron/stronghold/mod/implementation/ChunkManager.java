package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.IAreaEvent;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

public class ChunkManager implements IAreaEvent {
    private int systemsPerGrid = 8;
    private AbstractChunk[] chunks;
    public ChunkManager() {
        chunks = new AbstractChunk[128*128*128/(systemsPerGrid*systemsPerGrid*systemsPerGrid)];
    }


    private static void mutateToGrid(Vector3i sector) {
        //to system
        int shift = 4+3;
        sector.x = sector.x>>shift; sector.y = sector.y>>shift; sector.z = sector.z>>shift;
        //to grid
    }

    public static void main(String[] args) {
        //AbstractChunk root = new AbstractChunk();
        Vector3i v1 = new Vector3i(16*8-1,-16*8,-1), v2 = new Vector3i(32,16,1);
        mutateToGrid(v1); mutateToGrid(v2);
        System.out.println(v1 +""+v2);
        ChunkManager cm = new ChunkManager();
        //cm.initChunks();
        for (int i = 0; i < 7; i++) {
            cm.addArea(new StellarControllableArea(new Vector3i(-i,0,0),new Vector3i(i,0,0),"Area_"+i));
        }
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
            }
        }
    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {

    }

    @Override
    public void onAttacked(Timer t, AbstractControllableArea area, int attackerFaction, Vector3i position) {

    }

    private void addArea(StellarControllableArea area) {
        Vector3i start = new Vector3i(area.getDimensionsStart());
        Vector3i end = new Vector3i(area.getDimensionsEnd());
        for (int x = start.x; x <= end.x; x+= systemsPerGrid) {
            for (int y = start.y; y <= end.y; y+= systemsPerGrid) {
                for (int z = start.z; z <= end.z; z+= systemsPerGrid) {
                    Vector3i gridP = new Vector3i(x,y,z);
                    AbstractChunk c = getChunkFromSector(gridP); //mutates vec to gridpos
                    if (c == null) { //create new chunk if doesnt exist yet.
                        c = new AbstractChunk(gridP);
                        addChunk(c);
                    }
                    c.addChildObject(area);
                }
            }
        }
    }

    private int getIndexFromGridPos(Vector3i gridPos) {
        return gridPos.x+systemsPerGrid + (gridPos.y+systemsPerGrid) * systemsPerGrid + (gridPos.z+systemsPerGrid) * systemsPerGrid * systemsPerGrid;
    }

    private void addChunk(AbstractChunk c) {
        int idx = getIndexFromGridPos(c.gridPos);
        chunks[idx] = c;
    }
    /**
     * get chunk from internal chunk array
     * @param sector will be mutated to gridpos
     * @return
     */
    private AbstractChunk getChunkFromSector(Vector3i sector) {
        mutateToGrid(sector);
        return chunks[getIndexFromGridPos(sector)];
    }
}
