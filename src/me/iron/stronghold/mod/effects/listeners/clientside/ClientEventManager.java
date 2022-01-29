package me.iron.stronghold.mod.effects.listeners.clientside;

import me.iron.stronghold.mod.voidshield.VoidShieldController;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 15:55
 */
public class ClientEventManager {
    public ClientEventManager() {

    }

    public void initAll() {
        VoidShieldController.getInstance().addListener(new VoidShieldEventBroadcaster());
    }


}
