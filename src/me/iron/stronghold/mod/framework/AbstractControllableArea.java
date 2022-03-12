package me.iron.stronghold.mod.framework;

import api.DebugFile;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

import java.util.ArrayList;

public abstract class AbstractControllableArea extends SendableUpdateable implements IAreaEvent {
    private static long nextID = 1;
    protected static long getNextUID() {
        dlog("NEXT UID REQUESTED:"+(nextID));
        return nextID++;
    }
    public static long getCurrentUID() {
        return nextID;
    }
    public static void setCurrentUID(long UID) {
        nextID = UID;
    }

    protected boolean canBeConquered;
    protected int ownerFaction;
    protected long lastAttackMssg;
    protected long lastConquered;

    transient protected ArrayList<SendableUpdateable> children = new ArrayList<>();
    transient protected ArrayList<IAreaEvent> listeners = new ArrayList<>();


    public AbstractControllableArea() {
        super();
        init();
    }; //serialization stuff

    protected AbstractControllableArea(String name) {
        super(AbstractControllableArea.getNextUID(),name);
        AbstractControllableArea.dlog(" NEXT UID " + getUID()+ " FOR "+ this.toString());
        init();
    }

    /**
     * called after deserialzing to reconstruct circular references to children and effects
     */
    protected void init() {
        for (SendableUpdateable a: children)
            a.setParent(this);
    }

    /**
     * adds child to this area. sets childs parent to this. child gets pushed to back of children arraylist
     * @param child
     */
    public void addChildObject(SendableUpdateable child) {
        if (!children.contains(child)) {
            children.add(child);
            child.setParent(this);
            onChildChanged(this, child, false);
        }
    }

    protected boolean removeChildObject(SendableUpdateable child) {
        AreaManager.dlog("remove child object" + child +" from " +this);
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

    @Override
    protected void update(Timer t) {
        super.update(t);
        for (SendableUpdateable c: children) {
            c.update(t);
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
        beforeDestroy(this);
        super.destroy();
        if (getParent() instanceof AbstractControllableArea) {
            ((AbstractControllableArea) getParent()).removeChildObject(this);
            setParent(null);
        }
        //for (SendableUpdateable child: children)
        //    child.destroy();
        onDestroy(this);
    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {
        if (getParent() instanceof AbstractControllableArea) {
            ((AbstractControllableArea) getParent()).beforeDestroy(area);
        }
    }

    @Override
    public void onDestroy(AbstractControllableArea area) {
        if (getParent() instanceof AbstractControllableArea) {
            ((AbstractControllableArea) getParent()).onDestroy(area);
        }
    }

    @Override
    public void beforeOverwrite(AbstractControllableArea area) {
        if (getParent() instanceof AbstractControllableArea) {
            ((AbstractControllableArea) getParent()).beforeOverwrite(area);
        }
    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {
        if (getParent() instanceof AbstractControllableArea) {
            ((AbstractControllableArea) getParent()).onOverwrite(area);
        }
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        if (area.equals(this)) {
            lastConquered = System.currentTimeMillis();
        }

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
        ////System.out.println("child changed: p="+parent.getName()+",c="+child.getName()+" level:"+this.getName());
        for (IAreaEvent e: listeners)
            e.onChildChanged(parent,child,removed);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onChildChanged(parent, child, removed);
        //else
            //System.out.println("AREA_CHILD "+ child+ " passing area "+this+" that is non-bubble parent "+getParent()+" for onChildChanged.");
    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {
        for (IAreaEvent e: listeners)
            e.onParentChanged(child, parent, removed);
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onParentChanged(child,parent,removed);
    }

    Timer t = new Timer();
    @Override
    public void onAttacked(long time, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        if (this.equals(area)) { //only broadcast every 30 seconds for this area
            if (t.currentTime-lastAttackMssg<1000*30) {
                setLastAttacked(t.currentTime);
                return;
            }
            lastAttackMssg = t.currentTime;
        }
        for (IAreaEvent e: listeners) {
            e.onAttacked(time, area, attackerFaction, position);
        }
        if (this.getParent() != null && getParent() instanceof IAreaEvent)
            ((IAreaEvent)getParent()).onAttacked(time,area,attackerFaction,position);
    }

    public long getLastConquered() {
        return lastConquered;
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

    @Override
    public void updateFromObject(SendableUpdateable origin) {
        beforeOverwrite(this);
        super.updateFromObject(origin);
        onOverwrite(this);
    }

    /**
     * value clones input object into this object
     * @param a
     */
    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        if (a instanceof AbstractControllableArea) {
            AbstractControllableArea area = (AbstractControllableArea)a;
            setName(area.getName());
            setUID(area.getUID());
            setOwnerFaction(area.ownerFaction);
            setCanBeConquered(area.canBeConquered);
        }
    }

    @Override
    public String toString() {
        return super.toString()+", ownerF="+ getOwnerFaction();
    }

    public static void dlog(String mssg) {
        DebugFile.log("[STDB]########## "+mssg);
        System.out.println("[STDB]########## "+mssg);
    }
}
