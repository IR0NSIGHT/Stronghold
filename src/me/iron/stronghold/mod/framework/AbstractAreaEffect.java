package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

public abstract class AbstractAreaEffect {
    public AbstractAreaEffect(AbstractControllableArea parent) {
        this.parent = parent;
    }
    protected AbstractControllableArea parent = null;
    protected void update(Timer timer) {};
}
