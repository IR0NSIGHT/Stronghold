package me.iron.stronghold.mod.framework;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;

import java.util.LinkedList;

/**
 * this class is a chunk, represents one cube of a fixed grid in the galaxy with fixed sidelengths. It has references to stellarareas that are fully or partly inside of the chunk.
 * Its a supporting datastructure to quickly collect all areas a position is inside of. All chunks are managed by the chunkmanager.
 * The chunk is not accessible from outside and only used by the CM.
 */
public class AbstractChunk extends AbstractControllableArea {
    Vector3i gridPos; //position in own grid
    public AbstractChunk(Vector3i gridPos) {
        //super("Chunk"+gridPos);
        this.gridPos = gridPos;
    }

    /**
     * get which areas this point lies in (might have overlaps)
     * @return
     */
    public LinkedList<StellarControllableArea> getAreasFromSector(Vector3i sector) {
        LinkedList<StellarControllableArea> as = new LinkedList<>();
        for (SendableUpdateable a: getChildren()) {
            if (((StellarControllableArea)a).isSectorInArea(sector))
                as.add((StellarControllableArea) a);
        }
        return as;
    }

    @Override
    public void addChildObject(SendableUpdateable child) {
        if (!(child instanceof StellarControllableArea)) //reject anything that doesnt have a physical dimension.
            return;
        children.add(child);
    }

    @Override
    protected boolean removeChildObject(SendableUpdateable child) {
        boolean out =  children.remove(child);
        System.out.println(""+getName()+" has " + children.size() + " left.");
        return out;
    }

    protected boolean isEmpty() {
        return getChildren().isEmpty();
    }

}
