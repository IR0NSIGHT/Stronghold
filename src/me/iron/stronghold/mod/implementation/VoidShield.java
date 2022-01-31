package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.IAreaEvent;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

public class VoidShield extends AbstractAreaEffect implements IAreaEvent {
    private int defPoints;
    private int requiredPoints = 60*60; //seconds
    private boolean isShieldActive;
    transient private long lastUpdate;

    public VoidShield(long UID, AbstractControllableArea parent) {
        super(UID,parent);
        parent.addListener(this);
    }

    public boolean isShieldActive() {
        return isShieldActive;
    }

    @Override
    protected void update(Timer timer) {
        super.update(timer);
        if (parent.getOwnerFaction() != 0) {
            long seconds = (timer.currentTime-lastUpdate)/1000;
            lastUpdate = timer.lastUpdate;
            defPoints += seconds;
            if (defPoints>=requiredPoints && !isShieldActive) {
                activateShield();
            } else if (defPoints<requiredPoints && isShieldActive) {
                deactivateShield();
            }
        }
    }

    private void activateShield() {
        System.out.println("VoidShield for "+parent.getName()+" activated");
        isShieldActive = true;
    }

    private void deactivateShield() {
        System.out.println("VoidShield for "+parent.getName()+" deactivated");
        isShieldActive = false;
    }

    private void reset() {
        //System.out.println("VoidShield for area " + parent.getName() + " was reset");
        defPoints = 0;
        if (isShieldActive)
            deactivateShield();
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

    @Override
    public void onAttacked(Timer t, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        System.out.println("VoidShield under fire by " + attackerFaction);
    }
}
