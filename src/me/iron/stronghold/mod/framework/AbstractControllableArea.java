package me.iron.stronghold.mod.framework;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public abstract class AbstractControllableArea implements Serializable, IAreaEvent {
    private static long nextID;
    protected static long getNextID() {
        return nextID++;
    }

    protected boolean canBeConquered;
    protected String name = "";
    protected int ownerFaction;
    protected long UID;
    protected long lastAttackMssg;

    transient protected AbstractControllableArea parent = null;
    transient protected ArrayList<AbstractControllableArea> children = new ArrayList<>();
    transient protected ArrayList<AbstractAreaEffect> effects = new ArrayList<>();
    transient protected ArrayList<IAreaEvent> listeners = new ArrayList<>();

    public AbstractControllableArea() {}; //serialization stuff

    protected AbstractControllableArea(long UID, String name, @Nullable AbstractControllableArea parent) {
        this.parent = parent;
        this.name = name;
        this.UID = UID;
    }

    protected void init() { //call this after deserialzing to reconstruct circular references to children and effects
        for (AbstractControllableArea a: children)
            a.parent = this;
        for (AbstractAreaEffect e: effects) {
            e.parent = this;
        }
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
            requestSynchToClient(this);
        }
    }

    public void setOwnerFaction(int ownerFaction) {
        int oldVal = this.ownerFaction;
        if (oldVal != ownerFaction) {
            this.ownerFaction = ownerFaction;
            onConquered(this, oldVal);
            requestSynchToClient(this);
        }
    }

    public void setName(String name) {
        if (!Objects.equals(this.name, name)) {
            this.name = name;
            requestSynchToClient(this);
        }
    }

    public void setUID(long UID) {
        this.UID = UID;

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

    @Override
    public void onAttacked(Timer t, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        if (this.equals(area)) { //only broadcast every 30 seconds for this area
            if (t.currentTime-lastAttackMssg<1000*30) {
                setLastAttacked(t.currentTime);
                return;
            }
            lastAttackMssg = t.currentTime;
        }
        for (IAreaEvent e: listeners) {
            e.onAttacked(t, area, attackerFaction, position);
        }
        if (this.parent != null) {
            parent.onAttacked(t,area,attackerFaction,position);
        }
    }

    public long getLastAttacked() {
        return lastAttackMssg;
    }

    protected void setLastAttacked(long l) {
        if (l != lastAttackMssg) {
            lastAttackMssg = l;
            requestSynchToClient(this);
        }
    }

    public void requestSynchToClient(AbstractControllableArea area) {
        if (parent != null)
            parent.requestSynchToClient(area);
    }

    /**
     * value clones input object into this object
     * @param area
     */
    public void updateFromObject(AbstractControllableArea area) {
        setName(area.getName());
        setUID(area.UID);
        setOwnerFaction(area.ownerFaction);
        setCanBeConquered(area.canBeConquered);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractControllableArea that = (AbstractControllableArea) o;
        return canBeConquered == that.canBeConquered && ownerFaction == that.ownerFaction && UID == that.UID && lastAttackMssg == that.lastAttackMssg && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(UID);
    }
}
