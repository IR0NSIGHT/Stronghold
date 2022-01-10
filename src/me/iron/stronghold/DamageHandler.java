package me.iron.stronghold;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.mod.StarLoader;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 17:50
 * makes shields in voidshielded systems invulnerable.
 */
public class DamageHandler {
    /**
     * instantiate eventhandler for shield hits that will cancel any hits on
     */
    public DamageHandler() {
        StarLoader.registerListener(ShieldHitEvent.class, new Listener<ShieldHitEvent>() {
            @Override
            public void onEvent(ShieldHitEvent shieldHitEvent) {
                try {
                    Vector3i system = shieldHitEvent.getHitController().getSystem(new Vector3i());
                    int owners = GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system).getOwnerFaction();
                    boolean isStation = shieldHitEvent.getHitController().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION),
                            factionMatchesSystem = shieldHitEvent.getHitController().getFactionId()==owners,
                            systemIsOwned = owners != 0,
                            isVoidShielded = SystemController.getInstance()!=null && SystemController.getInstance().isSystemProtected(system);
                    //abort if: station thats owned by systemowners in a protected system
                    if (    isStation && factionMatchesSystem  && systemIsOwned && isVoidShielded) {
                        ModMain.log("cancel hit");
                        shieldHitEvent.setDamage(0);
                        shieldHitEvent.getShieldHit().hasHit=false;
                        shieldHitEvent.getShieldHit().setDamage(0);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },ModMain.instance);
    }
}
