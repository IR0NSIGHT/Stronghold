package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.*;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;
//will make shields unbreakable if active
public class VoidShield extends ActivateableAreaEffect implements IAreaEvent {
    private int defPoints;
    private int requiredPoints = 60*60; //seconds
    transient private long lastUpdate;

    public VoidShield() {
        super("VoidShield");
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (getParent() instanceof AbstractControllableArea && ((AbstractControllableArea)getParent()).getOwnerFaction() != 0) {
            long seconds = (timer.currentTime-lastUpdate)/1000;
            lastUpdate = timer.lastUpdate;
            defPoints += seconds;
            setActive(defPoints>=requiredPoints);
        }
    }

    @Override
    public void updateFromObject(SendableUpdateable origin) {
        super.updateFromObject(origin);
        if (origin instanceof VoidShield) {
            VoidShield v = (VoidShield) origin;
            lastUpdate = v.lastUpdate;
            this.defPoints = v.defPoints;
        }
    }

    private void reset() {
        //System.out.println("VoidShield for area " + parent.getName() + " was reset");
        defPoints = 0;
        setActive(false);
    }

 //is listening to parent for internal use
    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        if (area.equals(this.getParent()))
            reset();
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {

    }

    @Override
    public void onUpdate(AbstractControllableArea area) {

    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {

    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {

    }

    @Override
    public void setParent(AbstractControllableArea parent) {
        super.setParent(parent);
        if (parent != null)
            parent.addListener(this);
    }

    @Override
    public void onAttacked(Timer t, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        System.out.println("VoidShield under fire by " + attackerFaction);
    }
}
