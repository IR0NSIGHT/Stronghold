package me.iron.stronghold.mod.effects.listeners.serverside;

import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.voidshield.VoidShieldController;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 18:25
 */
public class ServerEventManager {
    public ServerEventManager() {
        FactionBoardPoster fbp = new FactionBoardPoster();
        VoidShieldController.eventlisteners.add(fbp);
        StrongholdController.getInstance().addStrongholdEventListener(fbp);
        StrongholdController.getInstance().addStrongpointEventListener(fbp);
    }
}
