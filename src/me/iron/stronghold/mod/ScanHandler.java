package me.iron.stronghold.mod;

import api.listener.Listener;
import api.listener.events.entity.EntityScanEvent;
import api.mod.StarLoader;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.voidshield.VoidShield;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

import java.io.IOException;

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
                if (!entityScanEvent.isServer())
                    return;

                Stronghold s = StrongholdController.getInstance().getStrongholdFromSector(entityScanEvent.getEntity().getSector(new Vector3i()));
                //get relevant info of stronghold. who, how much, where, name

               String mssg = String.format("Stronghold %s [%s]\n" +
                       "defensepoints: %s, voidshield active: %s\n"+
                       "strongpoints:\n%s",
                       s.getName(), Stronghold.tryGetFactionName(s.getOwner()), s.getDefensePoints(),
                       VoidShield.isStrongholdShielded(s),s.strongholdsToString()
               );
               entityScanEvent.getEntity().sendControllingPlayersServerMessage(Lng.astr(mssg), ServerMessage.MESSAGE_TYPE_SIMPLE);
            }
        },ModMain.instance);
    }


}
