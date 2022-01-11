package me.iron.stronghold;

import api.listener.Listener;
import api.listener.events.entity.EntityScanEvent;
import api.mod.StarLoader;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

import java.io.IOException;
import java.util.Vector;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 11.01.2022
 * TIME: 12:44
 */
public class ScanHandler {
    public ScanHandler() {
        StarLoader.registerListener(EntityScanEvent.class, new Listener<EntityScanEvent>() {
            @Override
            public void onEvent(EntityScanEvent entityScanEvent) {
                Vector3i system = entityScanEvent.getEntity().getSystem(new Vector3i());

                StrongholdSystem s = SystemController.getInstance().getSystem(system);
                if (s==null) {
                    try { //new dummy system that gets scrapped afterwards.
                        int owners = GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(system).getOwnerFaction();
                        s = new StrongholdSystem(system, owners);
                        s.init();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                assert s != null;
                s.update(System.currentTimeMillis()/1000);
                String mssg = String.format("System %s [%s]\n" +
                        "shieldpoints: %s, voidshield active: %s\n"+
                        "strongpoints:\n%s",
                        system, StrongholdSystem.tryGetFactionName(s.getOwner()), s.getHp(),
                        s.isProtected(),s.strongholdsToString()
                );
                entityScanEvent.getEntity().sendControllingPlayersServerMessage(Lng.astr(mssg), ServerMessage.MESSAGE_TYPE_SIMPLE);
            }
        },ModMain.instance);
    }


}
