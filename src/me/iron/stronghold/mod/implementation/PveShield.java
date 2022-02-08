package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SectorNotFoundException;

import javax.vecmath.Vector3f;

public class PveShield extends VoidShield {
    public PveShield() {
        super();
    }
    @Override
    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        if (isActive() &&
            ((StellarControllableArea)getParent()).isSectorInArea(shieldAddOn.getSegmentController().getSector(new Vector3i())) &&
                (shieldAddOn.getSegmentController().getFactionId()>0&&damager.getFactionId()>0) //hit is player AND damager is player
        ) //allows damage from NPCs or to NPCs

            return 0;
        return damage;
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
