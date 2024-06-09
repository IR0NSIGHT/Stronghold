package me.iron.stronghold.mod.implementation;

import api.DebugFile;
import api.utils.game.PlayerUtils;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SectorNotFoundException;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

import javax.vecmath.Vector3f;

public class PveShield extends VoidShield {
    public PveShield() {
        super();
    }
    public PveShield(String name) {
        super(name);
    }
    @Override
    public double handleShieldHit(ShieldAddOn shieldAddOn, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        assert getParent() != null;
        if (isActive() &&
            ((StellarControllableArea)getParent()).isSectorInArea(shieldAddOn.getSegmentController().getSector(new Vector3i())) && //inside geometry of parent area
                (shieldAddOn.getSegmentController().getFactionId()!=damager.getFactionId()) && //not blue-on-blue
                (shieldAddOn.getSegmentController().getFactionId()>0&&damager.getFactionId()>0) //hit is player AND damager is player
        ) //allows damage from NPCs or to NPCs
        {
            onPeaceViolated(damager);
            return 0;
        }
        return damage;
    }

    @Override
    protected boolean canToggle(boolean to) {
        //to off? no
        //to on? yes
        return to;
    }

    protected void onPeaceViolated(Damager d) {
        d.sendServerMessage(Lng.astr("This is a PVE area. No PVP allowed."), ServerMessage.MESSAGE_TYPE_WARNING);
        //DebugFile.log("[PVE VIOLATION]"+d.getShootingEntity().getUniqueIdentifier()+"["+d.getShootingEntity().getFaction().getName()+"] violated PVE rules ", ModMain.instance);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(true); //only ON allowed.
    }
}
