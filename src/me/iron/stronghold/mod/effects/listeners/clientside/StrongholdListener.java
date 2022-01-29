package me.iron.stronghold.mod.effects.listeners.clientside;

import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.AmbienceUtils;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.framework.Strongpoint;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.server.data.GameServerState;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 14.01.2022
 * TIME: 16:59
 * basic event sounds: voice announcer
 */
public class StrongholdListener implements IStrongpointEvent, IStrongholdEvent {
    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {

    }

    @Override
    public void onStrongholdOwnerChanged(Stronghold h, int newOwner) {
        if (GameServerState.instance != null) {
            String s = null;
            if (h.getOwner()!=0 && newOwner == 0) {
                s = String.format("[%s] has lost %s %s", AmbienceUtils.tryGetFactionName(h.getOwner()), StrongholdController.getStrongholdTerm, h.getName());
            }
            if (h.getOwner()==0 && newOwner != 0) {
                s = String.format("[%s] conquered %s %s", AmbienceUtils.tryGetFactionName(newOwner), StrongholdController.getStrongholdTerm, h.getName());
            }
            if (h.getOwner()!=0 && newOwner != 0) {
                s = String.format("[%s] took %s %s from %s", AmbienceUtils.tryGetFactionName(newOwner), StrongholdController.getStrongholdTerm, h.getName(), AmbienceUtils.tryGetFactionName(h.getOwner()));
            }
            if (s != null)
                ModMain.log(s);
        }
        if (GameClientState.instance != null) {
            int ownF = GameClientState.instance.getPlayer().getFactionId();
            if (h.getOwner() == ownF)
                SoundManager.instance.queueSound(SoundManager.Sound.lost_a_region);
            if (newOwner == ownF)
                SoundManager.instance.queueSound(SoundManager.Sound.conquered_a_region);
        }
    }

    @Override
    public void onStrongholdBalanceChanged(Stronghold h, int newBalance) {
        System.out.println("event: stronghold balance changed.");

        if (GameClientState.instance==null)
            return;
        if (h.getBalance()<=0 && newBalance > 0) {
            //defPoints rising
            SoundManager.instance.queueSound(SoundManager.Sound.winning_this_region);
        } else if (h.getBalance()>=0 && newBalance< 0) {
            //defPoints falling
            SoundManager.instance.queueSound(SoundManager.Sound.loosing_this_region);
        }
    }

    @Override
    public void onStrongpointOwnerChanged(Strongpoint p, int n) {
        int oldOwner = n, newOwner = p.getOwner();
        String s = null;
        if (oldOwner!=0 && newOwner == 0) {
            s = AmbienceUtils.tryGetFactionName(oldOwner)+"has lost Strongpoint "+p.getSector();
        } else if (oldOwner==0 && newOwner != 0) {
            s = AmbienceUtils.tryGetFactionName(newOwner) + " captured Strongpoint " + p.getSector();
        } else if (oldOwner!=0 && newOwner != 0) {
            s = AmbienceUtils.tryGetFactionName(newOwner) + " took Strongpoint " + p.getSector() + " from " + AmbienceUtils.tryGetFactionName(oldOwner)+"!";
        }

        if (GameServerState.instance != null && s != null)
            ModMain.log(s);

        if (GameClientState.instance!= null) {
            SoundManager.Sound sound = null;
            int playerF = GameClientState.instance.getPlayer().getFactionId();
            Vector3i playerPos = GameClientState.instance.getPlayer().getCurrentSystem();
            Vector3i pointPos = new Vector3i(p.getSector());
            StrongholdController.mutateSectorToSystem(pointPos);

            if (oldOwner==playerF) {
                sound = SoundManager.Sound.strongpoint_lost;
            } else if (newOwner == playerF) {
                sound = SoundManager.Sound.strongpoint_captured;
            } else if (pointPos.equals(playerPos)) {
                sound = SoundManager.Sound.strongpoint_contested;
            }

            if (sound != null) {
                SoundManager.instance.queueSound(sound);
            }
            if (s != null) {
                AmbienceUtils.clientShowMessage(s);
            }
        }
    }
}
