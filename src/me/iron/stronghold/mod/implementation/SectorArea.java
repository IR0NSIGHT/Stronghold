package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

public class SectorArea extends AbstractControllableArea {
    public SectorArea(){}
    public SectorArea(long UID, String name, AbstractControllableArea parent) {
        super(UID, name, parent);
    }

    @Override
    protected void update(Timer timer) {
        super.update(timer);
        //exists station in this sector -> set conquered
    }

    @Override
    public boolean canBeConquered() {
        return true;
    }
}
