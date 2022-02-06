package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;

import java.util.LinkedList;

public class AbstractChunk extends AbstractControllableArea {
    Vector3i gridPos; //position in own grid
    public AbstractChunk(Vector3i gridPos) {
        super("Chunk"+gridPos);
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
    protected void addChildObject(SendableUpdateable child) {
        if (!(child instanceof StellarControllableArea)) //reject anything that doesnt have a physical dimension.
            return;
        super.addChildObject(child);
    }
}
