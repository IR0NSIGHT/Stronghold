package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SectorNotFoundException;

import javax.vecmath.Vector3f;
import java.util.Collection;
import java.util.HashSet;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 16:42
 */
public class SelectiveVoidShield extends VoidShield{
    private final HashSet<Integer> protectFactions = new HashSet<>();
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



    @Override
    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        if  (isActive() &&
            ((StellarControllableArea)getParent()).isSectorInArea(shieldAddOn.getSegmentController().getSector(new Vector3i())) &&
            isProtectedFaction(shieldAddOn.getSegmentController().getFactionId()))
        {
            return 0;
        }

        return super.handleShieldHit(shieldAddOn, damager, defenseSet, hitPoint, projectileSectorId, damageType, hitType, damage, weaponId);
    }
}
