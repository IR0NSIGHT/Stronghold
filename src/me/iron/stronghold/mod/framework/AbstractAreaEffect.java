package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

public abstract class AbstractAreaEffect {
    protected  long UID;
    transient protected AbstractControllableArea parent = null;

    public AbstractAreaEffect(){}//for deserializing
    public AbstractAreaEffect(long UID, AbstractControllableArea parent) {
        this.parent = parent;
    }
    protected void update(Timer timer) {};
}
