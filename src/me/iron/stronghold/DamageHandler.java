package me.iron.stronghold;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.mod.StarLoader;
import org.hsqldb.Server;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

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
                    //abort if: controller says this object in that system with those system owners is protected.
                    if (SystemController.getInstance()!=null&&SystemController.getInstance().isObjectProtected(shieldHitEvent.getHitController(), system, owners)) {
                    //    ModMain.log("cancel hit");
                        shieldHitEvent.setDamage(0);
                        shieldHitEvent.getShieldHit().hasHit=false;
                        shieldHitEvent.getShieldHit().setDamage(0);
                        shieldHitEvent.getShieldHit().damager.sendServerMessage(Lng.astr("system is voidshielded."), ServerMessage.MESSAGE_TYPE_ERROR);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },ModMain.instance);
    }
}
