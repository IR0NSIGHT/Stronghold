package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.*;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SectorNotFoundException;
import org.schema.schine.graphicsengine.core.Timer;

import javax.vecmath.Vector3f;
import java.util.LinkedList;

//will make shields unbreakable if active
public class VoidShield extends ActivateableAreaEffect implements IAreaEvent {
    public static LinkedList<VoidShield> shields = new LinkedList<>();
    private int defPoints;
    private int requiredPoints = 60*60; //seconds
    transient private long lastUpdate;

    public VoidShield() {
        super("VoidShield");
        shields.add(this);
    }

    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        return damage;
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (getParent() instanceof StellarControllableArea && ((AbstractControllableArea)getParent()).getOwnerFaction() != 0) {
            long seconds = (timer.currentTime-lastUpdate)/1000;
            lastUpdate = timer.lastUpdate;
            defPoints += seconds;
            setActive(defPoints>=requiredPoints);
        } else {
            setActive(false);
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

    @Override
    protected void destroy() {
        super.destroy();
        shields.remove(this);
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
