package me.iron.stronghold.mod.framework;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

import java.util.ArrayList;

public abstract class AbstractControllableArea extends SendableUpdateable implements IAreaEvent {
    private static long nextID;
    protected static long getNextID() {
        return nextID++;
    }

    protected boolean canBeConquered;
    protected int ownerFaction;
    protected long lastAttackMssg;

    transient protected ArrayList<SendableUpdateable> children = new ArrayList<>();
    transient protected ArrayList<IAreaEvent> listeners = new ArrayList<>();

    public AbstractControllableArea() {
        super();
    }; //serialization stuff

    protected AbstractControllableArea(String name) {
        super(AreaManager.getNextID(),name);
    }

    protected void init() { //call this after deserialzing to reconstruct circular references to children and effects
        for (SendableUpdateable a: children)
            a.setParent(this);
    }

    protected void addChildObject(SendableUpdateable child) {
        if (!children.contains(child)) {
            children.add(child);
            child.setParent(this);
            onChildChanged(this, child, false);
        //    System.out.println("adding child"+child.getName()+" to parent "+getName());
        }
    }

    protected boolean removeChildObject(SendableUpdateable child) {
        boolean out = children.remove(child);
        if (out) {
            onChildChanged(this, child, true);
            child.setParent(null);
        }
        return out;
    }

    public void addListener(IAreaEvent e) {
        listeners.add(e);
    }

    public void removeListener(IAreaEvent e) {
        listeners.remove(e);
    }

    public boolean canBeConquered() {
        return false;
    };

    public int getOwnerFaction() {
        return ownerFaction;
    }

    public void update(Timer timer) {
        for (SendableUpdateable c: children) {
            c.update(timer);
        }
        onUpdate(this);
    }

    public ArrayList<SendableUpdateable> getChildren() {
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

    @Override
    public void requestSynchToClient(SendableUpdateable area) {
        super.requestSynchToClient(area);
        assert this.equals(area)||children.contains(area);
    }

    public void setOwnerFaction(int ownerFaction) {
        int oldVal = this.ownerFaction;
        if (oldVal != ownerFaction) {
            this.ownerFaction = ownerFaction;
            onConquered(this, oldVal);
            requestSynchToClient(this);
        }
    }

    @Override
    protected void destroy() {
        super.destroy();
        for (SendableUpdateable child: children)
            child.destroy();
        children.clear();
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        for (IAreaEvent e: listeners) {
            e.onConquered(area,oldOwner);
        }
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
           ( (IAreaEvent)getParent()).onConquered(area, oldOwner);
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {
        for (IAreaEvent e: listeners)
            e.onCanBeConqueredChanged(area,oldValue);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onCanBeConqueredChanged(area, oldValue);
    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
        for (IAreaEvent e: listeners)
            e.onUpdate(area);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onUpdate(area);
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {
        //System.out.println("child changed: p="+parent.getName()+",c="+child.getName()+" level:"+this.getName());
        for (IAreaEvent e: listeners)
            e.onChildChanged(parent,child,removed);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onChildChanged(parent, child, removed);
    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {
        for (IAreaEvent e: listeners)
            e.onParentChanged(child, parent, removed);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onParentChanged(child,parent,removed);
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
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onAttacked(t,area,attackerFaction,position);
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

    /**
     * value clones input object into this object
     * @param a
     */
    @Override
    public void updateFromObject(SendableUpdateable a) {
        if (a instanceof AbstractControllableArea) {
            AbstractControllableArea area = (AbstractControllableArea)a;
            setName(area.getName());
            setUID(area.getUID());
            setOwnerFaction(area.ownerFaction);
            setCanBeConquered(area.canBeConquered);
        }

    }
}
