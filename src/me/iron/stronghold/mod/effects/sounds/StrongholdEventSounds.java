package me.iron.stronghold.mod.effects.sounds;

import me.iron.stronghold.mod.ModMain;
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
public class StrongholdEventSounds implements IStrongpointEvent, IStrongholdEvent {
    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {

    }

    @Override
    public void onStrongholdOwnerChanged(Stronghold h, int newOwner) {
        if (GameServerState.instance != null) {
            String s = null;
            if (h.getOwner()!=0 && newOwner == 0) {
                s = String.format("[%s] has lost %s %s",Stronghold.tryGetFactionName(h.getOwner()), StrongholdController.getStrongholdTerm, h.getName());
            }
            if (h.getOwner()==0 && newOwner != 0) {
                s = String.format("[%s] conquered %s %s",Stronghold.tryGetFactionName(newOwner), StrongholdController.getStrongholdTerm, h.getName());
            }
            if (h.getOwner()!=0 && newOwner != 0) {
                s = String.format("[%s] took %s %s from %s",Stronghold.tryGetFactionName(newOwner), StrongholdController.getStrongholdTerm, h.getName(), Stronghold.tryGetFactionName(h.getOwner()));
            }
            if (s != null)
                ModMain.log(s);
        }
    }

    @Override
    public void onStrongpointOwnerChanged(Strongpoint p, int newOwner) {
        if (GameServerState.instance != null) {
            String s = null;
            if (p.getOwner()!=0 && newOwner == 0) {
                s = Stronghold.tryGetFactionName(p.getOwner())+"has lost Strongpoint "+p.getSector();
            }
            if (p.getOwner()==0 && newOwner != 0) {
                s = Stronghold.tryGetFactionName(newOwner) + " captured Strongpoint " + p.getSector();
            }
            if (p.getOwner()!=0 && newOwner != 0) {
                s = Stronghold.tryGetFactionName(newOwner) + " took Strongpoint " + p.getSector() + " from " + Stronghold.tryGetFactionName(p.getOwner())+"!";
            }
            if (s != null)
                ModMain.log(s);
        }

        if (GameClientState.instance!= null) {
            SoundManager.Sound s = null;
            int playerF = GameClientState.instance.getPlayer().getFactionId();
            Vector3i playerPos = GameClientState.instance.getPlayer().getCurrentSystem();
            Vector3i pointPos = new Vector3i(p.getSector());
            StrongholdController.mutateSectorToSystem(pointPos);
            if (p.getOwner()==playerF) {
                s = SoundManager.Sound.strongpoint_lost;
            }
            else if (newOwner == playerF) {
                s = SoundManager.Sound.strongpoint_captured;
            }
            else if (pointPos.equals(playerPos)) {
                s = SoundManager.Sound.strongpoint_contested;
            }
            if (s != null) {
                SoundManager.instance.queueSound(s);
            }
        }
    }
}
