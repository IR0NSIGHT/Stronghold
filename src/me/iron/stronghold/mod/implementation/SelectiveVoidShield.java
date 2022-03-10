package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.world.SectorNotFoundException;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import javax.vecmath.Vector3f;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 16:42
 */
public class SelectiveVoidShield extends VoidShield{
    private HashSet<Integer> protectFactions = new HashSet<>();
    private boolean[] protectedTypes = new boolean[SimpleTransformableSendableObject.EntityType.values().length];
     public SelectiveVoidShield() {
        super();
    }
    public SelectiveVoidShield(String name) {
        super(name);
    }

    public void addProtectedFaction(int factionID) {
        protectFactions.add(factionID);
    }
    public void addProtectedFaction(Collection<Integer> factionID) {
        protectFactions.addAll(factionID);
    }
    public void removeProtectedFaction(int factionID) {
        protectFactions.remove(factionID);
    }
    public boolean isProtectedFaction(int factionID) {
        return protectFactions.contains(factionID);
    }

    /**
     * set if this type is protected by the voidshield
     * @param type entitytype
     * @param protect protect
     */
    public void setProtected(SimpleTransformableSendableObject.EntityType type,boolean protect) {
        protectedTypes[type.ordinal()] = protect;
    }

    /**
     * get if type is protected
     * @param type
     * @return
     */
    public boolean isProtectedType(SimpleTransformableSendableObject.EntityType type) {
        return protectedTypes[type.ordinal()];
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (getParent() instanceof StellarControllableArea ) { //TODO if is loaded/someone in area
            updateFactions();
        }
    }

    protected void updateFactions() {
        protectFactions.clear();
        int owner = ((AbstractControllableArea)getParent()).getOwnerFaction();
        protectFactions.add(owner);
        Faction f = GameServerState.instance.getFactionManager().getFaction(owner);
        if (f != null) {
            for (Faction bff: f.getFriends()) {
                protectFactions.add(bff.getIdFaction());
            }
        }
    }

    protected boolean isProtectedObject(SegmentController sc) { //TODO merge all that into an abstract super class.
        return isProtectedType(sc.getType()) && isProtectedFaction(sc.getFactionId());
    }

    @Override
    public void synch(SendableUpdateable origin) {
        super.synch(origin);
        if (origin instanceof SelectiveVoidShield) {
            protectedTypes = ((SelectiveVoidShield) origin).protectedTypes;
            protectFactions = ((SelectiveVoidShield) origin).protectFactions;
        }
    }

    @Override
    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        if  (isActive() &&
            ((StellarControllableArea)getParent()).isSectorInArea(shieldAddOn.getSegmentController().getSector(new Vector3i())) &&
            isProtectedObject(shieldAddOn.getSegmentController()))
        {
            return 0;
        }
        return super.handleShieldHit(shieldAddOn, damager, defenseSet, hitPoint, projectileSectorId, damageType, hitType, damage, weaponId);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[");
        int i = 0;
        for (int idx = 0; idx < protectedTypes.length; idx++) {
            if (protectedTypes[idx]) {
                if (i > 0)
                    b.append(", ");
                i++;
                b.append(SimpleTransformableSendableObject.EntityType.values()[idx].getName());

            }

        }
        b.append("]");
        return super.toString() +
                ", protectFactions=" + protectFactions +
                ", protectedTypes=" + b.toString();
    }
}
