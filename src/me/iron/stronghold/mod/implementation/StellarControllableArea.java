package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;

public class StellarControllableArea extends AbstractControllableArea {
    //has a position/size that belongs to it.
    private Vector3i[] dimensions = new Vector3i[2];
    private Vector3i maxDimension = new Vector3i(16*8,16*8,16*8); //in sectors

    public StellarControllableArea(Vector3i start, Vector3i end, String name) {
        super(name);
        setDimensions(start,end);
    }

    /**
     * stets the dimensions of the area in sector coordinates. start must be <= in all fields than end.
     * @param start
     * @param end
     */
    public void setDimensions(Vector3i start, Vector3i end) {
        Vector3i dim = new Vector3i(end); dim.sub(start);
        if (dim.x < 0 || dim.y < 0 || dim.z < 0) {
            System.err.println("Dimensions start not smaller than end for Area "+ getName() +" start "+start+ " end " + end);
            return;
        }

        if (dim.x>maxDimension.x||dim.y>maxDimension.y||dim.z>maxDimension.z) {
            System.err.println("Dimensions for stellar area exceed maximum dimensions allowed: " + getName());
            return;
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
        return (
                getDimensionsStart().x<=sector.x && sector.x <= getDimensionsEnd().x &&
                getDimensionsStart().y<=sector.y && sector.y <= getDimensionsEnd().y &&
                getDimensionsStart().z<=sector.z && sector.z <= getDimensionsEnd().z
        );
    }
}
