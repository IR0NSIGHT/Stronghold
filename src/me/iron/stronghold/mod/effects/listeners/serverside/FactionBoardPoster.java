package me.iron.stronghold.mod.effects.listeners.serverside;

import it.unimi.dsi.fastutil.Hash;
import me.iron.stronghold.mod.effects.AmbienceUtils;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.Strongpoint;
import me.iron.stronghold.mod.voidshield.IVoidShieldEvent;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.GameServerState;

import java.util.HashMap;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 17:53
 */
public class FactionBoardPoster implements IStrongholdEvent, IStrongpointEvent, IVoidShieldEvent {
    HashMap<Integer, Long> faction_lastHit = new HashMap<>();
    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {

    }

    @Override
    public void onStrongholdOwnerChanged(Stronghold h, int newOwner) {
        int oldOwner = h.getOwner();
        AmbienceUtils.addToFactionBoard(
                oldOwner,
                "Stronghold lost",
                String.format("We have lost stronghold %s to faction %s",h.getName(), AmbienceUtils.tryGetFactionName(newOwner)));

        AmbienceUtils.addToFactionBoard(newOwner,
                "Stronghold captured",
                String.format("We have taken stronghold %s from faction %s",h.getName(),AmbienceUtils.tryGetFactionName(oldOwner)));
    }

    @Override
    public void onStrongholdBalanceChanged(Stronghold h, int newBalance) {

    }

    @Override
    public void onStrongpointOwnerChanged(Strongpoint p, int n) {
        int oldOwner = n, newOwner = p.getOwner();
        AmbienceUtils.addToFactionBoard(
                oldOwner,
                "Controlpoint lost",
                String.format("We have lost controlpoint %s in stronghold %s to faction %s",p.getSector(), p.getStronghold().getName(), AmbienceUtils.tryGetFactionName(newOwner)));

        AmbienceUtils.addToFactionBoard(newOwner,
                "Controlpoint captured",
                String.format("We have taken controlpoint %s in stronghold %s from faction %s",p.getSector(), p.getStronghold().getName(),AmbienceUtils.tryGetFactionName(oldOwner)));
    }

    @Override
    public void onShieldHit(Stronghold h, SegmentController hitObject, Damager d) {
        Long l = faction_lastHit.get(h.getOwner());
        if (l == null || System.currentTimeMillis() - l > 1000* 60* 10) {
            faction_lastHit.put(h.getOwner(), System.currentTimeMillis());
            Faction f = GameServerState.instance.getFactionManager().getFaction(h.getOwner());
            assert f != null : "faction has voidshielded stronghold but is null: " + h.toString();
            AmbienceUtils.addToFactionBoard(h.getOwner(),
                    "Voidshield under fire",
                    String.format("The voidshield in stronghold %s, sector %s is under fire by %s",h.getName(), hitObject.getSector(new Vector3i()), d.getName()));
        }


    }

    @Override
    public void onShieldActivate(Stronghold h) {
        AmbienceUtils.addToFactionBoard(
                h.getOwner(),
                "Voidshield activated",
                String.format("Stronghold %s has activated its voidshield.",h.getName()));
    }

    @Override
    public void onShieldDeactivate(Stronghold h) {
        AmbienceUtils.addToFactionBoard(
                h.getOwner(),
                "Voidshield offline",
                String.format("Stronghold %s has lost its voidshield.",h.getName()));
    }
}
