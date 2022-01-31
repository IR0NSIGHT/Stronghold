package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;

public abstract class AbstractControllableArea implements Serializable, IAreaEvent {
    protected boolean canBeConquered;
    protected String name;
    protected int ownerFaction;
    protected ArrayList<AbstractControllableArea> children = new ArrayList<>();
    protected AbstractControllableArea parent = null;
    protected ArrayList<AbstractAreaEffect> effects = new ArrayList<>();
    protected ArrayList<IAreaEvent> listeners = new ArrayList<>();

    protected AbstractControllableArea(String name, @Nullable AbstractControllableArea parent) {
        this.parent = parent;
        this.name = name;
    }

    protected void addChildArea(AbstractControllableArea child) {
        if (!children.contains(child)) {
            children.add(child);
            onChildChanged(this, child, false);
        }
    }

    protected boolean removeChildArea(AbstractControllableArea child) {
        boolean out = children.remove(child);
        if (out)
            onChildChanged(this, child, true);
        return out;
    }

    public void addListener(IAreaEvent e) {
        listeners.add(e);
    }

    public void removeListener(IAreaEvent e) {
        listeners.remove(e);
    }

    public void addEffect(AbstractAreaEffect e) {
        effects.add(e);
    }

    public boolean removeEffect(AbstractAreaEffect e) {
        return effects.remove(e);
    }
    public boolean canBeConquered() {
        return false;
    };

    public String getName() {
        return name;
    }

    public int getOwnerFaction() {
        return ownerFaction;
    }

    protected void update(Timer timer) {
        for (AbstractControllableArea c: children) {
            c.update(timer);
        }
        for (AbstractAreaEffect e: effects) {
            e.update(timer);
        }
        onUpdate(this);
    }

    public ArrayList<AbstractControllableArea> getChildren() {
        return children;
    }

    public void setCanBeConquered(boolean canBeConquered) {
        boolean oldVal = canBeConquered();
        if (oldVal != canBeConquered) {
            this.canBeConquered = canBeConquered;
            onCanBeConqueredChanged(this,oldVal);
        }
    }

    public void setOwnerFaction(int ownerFaction) {
        int oldVal = this.ownerFaction;
        if (oldVal != ownerFaction) {
            this.ownerFaction = ownerFaction;
            onConquered(this, oldVal);
        }
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        for (IAreaEvent e: listeners) {
            e.onConquered(area,oldOwner);
        }
        if (this.parent != null)
            this.parent.onConquered(area, oldOwner);
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {
        for (IAreaEvent e: listeners)
            e.onCanBeConqueredChanged(area,oldValue);
        if (parent!=null)
            parent.onCanBeConqueredChanged(area, oldValue);
    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
        for (IAreaEvent e: listeners)
            e.onUpdate(area);
        if (parent!=null)
            parent.onUpdate(area);
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, AbstractControllableArea child, boolean removed) {
        for (IAreaEvent e: listeners)
            e.onChildChanged(parent,child,removed);
        if (this.parent!=null)
            this.parent.onChildChanged(parent, child, removed);
    }

    @Override
    public void onParentChanged(AbstractControllableArea child, AbstractControllableArea parent, boolean removed) {
        for (IAreaEvent e: listeners)
            e.onParentChanged(child, parent, removed);
        if (this.parent!=null)
            this.parent.onParentChanged(parent,child,removed);
    }

}
