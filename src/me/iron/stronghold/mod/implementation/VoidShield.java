package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.*;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SectorNotFoundException;

import javax.vecmath.Vector3f;
import java.util.LinkedList;

//will make shields unbreakable if active
public class VoidShield extends ActivateableAreaEffect implements IAreaEvent {
    public static LinkedList<VoidShield> shields = new LinkedList<>();
    transient private long lastUpdate;
    private boolean sendMssg;

    public VoidShield() {
        super();
    }
    public VoidShield(String name) {
        super(name);
    }

    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        if (sendMssg && getParent() instanceof AbstractControllableArea) {
            sendMssg = true;
            onAttacked(lastUpdate, (AbstractControllableArea) getParent(), damager.getFactionId(), damager.getShootingEntity().getSector(new Vector3i()));
        }
        return damage;
    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {

    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {

    }

    @Override
    protected void onFirstUpdateRuntime() {
        super.onFirstUpdateRuntime();
        shields.add(this);
    }

    @Override
    public void synch(SendableUpdateable origin) {
        super.synch(origin);
        if (origin instanceof VoidShield) {
            lastUpdate =((VoidShield)  origin).lastUpdate;
        }
    }

    @Override
    protected void destroy() { //TODO give "cascade" param.
        super.destroy();
        shields.remove(this);
    }

    @Override
    public void onDestroy(AbstractControllableArea area) {

    }

    @Override
    public void beforeOverwrite(AbstractControllableArea area) {

    }

    private void reset() {
        //setActive(false);
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
    public void onAttacked(long t, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        if (getParent() instanceof IAreaEvent)
            ((IAreaEvent) getParent()).onAttacked(t,area,attackerFaction, position);
    }
}
