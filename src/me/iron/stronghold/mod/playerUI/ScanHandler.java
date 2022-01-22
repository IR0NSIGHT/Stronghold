package me.iron.stronghold.mod.playerUI;

import api.listener.Listener;
import api.listener.events.entity.EntityScanEvent;
import api.mod.StarLoader;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.effects.sounds.StrongholdEventSounds;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.voidshield.VoidShield;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;


/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 11.01.2022
 * TIME: 12:44
 */
public class ScanHandler {
    public ScanHandler() {
        if (GameServerState.instance != null) {
            StarLoader.registerListener(EntityScanEvent.class, new Listener<EntityScanEvent>() {
                @Override
                public void onEvent(EntityScanEvent entityScanEvent) {
                    //get relevant info of stronghold. who, how much, where, name
                    if (entityScanEvent.isServer()) {
                        Stronghold s = StrongholdController.getInstance().getStrongholdFromSector(entityScanEvent.getEntity().getSector(new Vector3i()));
                        assert s != null;
                        String mssg = getStrongholdInfo(s);
                        entityScanEvent.getEntity().sendControllingPlayersServerMessage(Lng.astr(mssg), ServerMessage.MESSAGE_TYPE_SIMPLE);
                    }
                }
            }, ModMain.instance);
        }

        if (GameClientState.instance!= null) {
            StrongholdEventSounds s = new StrongholdEventSounds();
            StrongholdController.getInstance().addStrongpointEventListener(s);
            StrongholdController.getInstance().addStrongholdEventListener(s);
        }
    }

    public static String getStrongholdInfo(Stronghold s) {
        String mssg = String.format("Stronghold %s [%s]\n" +
                        "defensepoints: %s, balance: %s, voidshield active: %s\n"+
                        "strongpoints:\n%s",
                s.getName(), Stronghold.tryGetFactionName(s.getOwner()), s.getDefensePoints(), s.getBalance(),
                VoidShield.isStrongholdShielded(s),s.strongholdsToString()
        );
        return mssg;
    }


}
