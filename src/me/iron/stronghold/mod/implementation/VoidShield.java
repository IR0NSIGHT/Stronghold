package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.IAreaEvent;
import org.lwjgl.Sys;
import org.schema.schine.graphicsengine.core.Timer;

public class VoidShield extends AbstractAreaEffect implements IAreaEvent {
    private int defPoints;
    private int requiredPoints = 60*60; //seconds
    transient private long lastUpdate;

    public VoidShield(AbstractControllableArea parent) {
        super(parent);
        parent.addListener(this);
    }

    public boolean isShieldActive() {
        return defPoints>=requiredPoints;
    }

    @Override
    protected void update(Timer timer) {
        super.update(timer);
        if (parent.getOwnerFaction() != 0) {
            long seconds = (timer.currentTime-lastUpdate)/1000;
            lastUpdate = timer.lastUpdate;
            if (defPoints<requiredPoints&&defPoints+seconds>=requiredPoints)
                System.out.println("VoidShield for "+parent.getName()+" activated");
            defPoints += seconds;
        }
    }

    private void reset() {
        System.out.println("VoidShield for area " + parent.getName() + " was reset");
    }

 //is listening to parent for internal use
    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        if (area.equals(this.parent))
            reset();
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {

    }

    @Override
    public void onUpdate(AbstractControllableArea area) {

    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, AbstractControllableArea child, boolean removed) {

    }

    @Override
    public void onParentChanged(AbstractControllableArea child, AbstractControllableArea parent, boolean removed) {

    }
}
