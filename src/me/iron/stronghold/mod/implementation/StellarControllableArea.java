package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;

import java.util.Arrays;

public class StellarControllableArea extends AbstractControllableArea {
    //has a position/size that belongs to it.
    private Vector3i[] dimensions = new Vector3i[2];
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
    protected void init() {
        super.init();

    }

    @Override
    protected void destroy() {
        super.destroy();
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
                ", dimensions="+Arrays.toString(dimensions);
    }
}
