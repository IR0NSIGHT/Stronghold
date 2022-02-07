package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;

public class PveArea extends StellarControllableArea{
    public PveArea(Vector3i start, Vector3i end, String name) {
        super(start, end, name);
        addChildObject(new PveShield());
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }
}
