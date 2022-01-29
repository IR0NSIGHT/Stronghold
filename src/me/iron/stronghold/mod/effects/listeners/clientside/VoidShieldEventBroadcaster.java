package me.iron.stronghold.mod.effects.listeners.clientside;

import me.iron.stronghold.mod.effects.AmbienceUtils;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.voidshield.IVoidShieldEvent;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.Damager;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 15:56
 */
public class VoidShieldEventBroadcaster implements IVoidShieldEvent {
    long l;
    @Override
    public void onShieldHit(Stronghold h, SegmentController hitObject, Damager d) {
        //send shooter or stronghold owner a message that the attack is occuring/the system is voidshielded.
        if (System.currentTimeMillis() - l > 1000*15) {
           // l = System.currentTimeMillis();
            if (GameClientState.instance.getPlayer().getFactionId() == h.getOwner())
                AmbienceUtils.clientShowMessage(String.format("Voidshield in %s under fire by %s in sector %s",h.getName(),d.getName(), hitObject.getSector(new Vector3i())));
            else if (GameClientState.instance.getPlayer().getFactionId() == d.getFactionId()) {
                if (h.getBalance()<0) {
                    long secondsTillShieldfail = h.getDefensePoints()/h.getBalance();
                    AmbienceUtils.clientShowMessage(String.format("%s has an active voidshield for another %smin:%ss.",h.getName(),secondsTillShieldfail/60,secondsTillShieldfail%60));
                } else
                    AmbienceUtils.clientShowMessage(String.format("%s is voidshielded and stable.",h.getName()));
            }
        }
    }

    @Override
    public void onShieldActivate(Stronghold h) {
        int pFaction = GameClientState.instance.getPlayer().getFactionId();
        boolean sameF = pFaction==h.getOwner();
        boolean inStronghold = StrongholdController.getInstance().getStrongholdFromSector(GameClientState.instance.getPlayer().getCurrentSector()).equals(h);
        if (sameF) {
            SoundManager.instance.queueSound(SoundManager.Sound.voidshield_acivated);
            AmbienceUtils.clientShowMessage(String.format("Our stronghold %s has activated its voidshield.",h.getName()));
        }
        else if (inStronghold) {
            SoundManager.instance.queueSound(SoundManager.Sound.voidshield_acivated);
            AmbienceUtils.clientShowMessage(String.format("The stronghold %s you are in has activated its voidshield.",h.getName()));
        }
    }

    @Override
    public void onShieldDeactivate(Stronghold h) {
        int pFaction = GameClientState.instance.getPlayer().getFactionId();
        boolean sameF = pFaction==h.getOwner();
        boolean inStronghold = StrongholdController.getInstance().getStrongholdFromSector(GameClientState.instance.getPlayer().getCurrentSector()).equals(h);
        if (sameF) {
            SoundManager.instance.queueSound(SoundManager.Sound.voidshield_deactivated);
            AmbienceUtils.clientShowMessage(String.format("Our stronghold %s has lost its voidshield.",h.getName()));
        }
        else if (inStronghold) {
            SoundManager.instance.queueSound(SoundManager.Sound.voidshield_deactivated);
            AmbienceUtils.clientShowMessage(String.format("The stronghold %s you are in has lost its voidshield.",h.getName()));
        }
    }
}
