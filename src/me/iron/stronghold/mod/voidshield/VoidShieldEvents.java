package me.iron.stronghold.mod.voidshield;

import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.framework.Strongpoint;
import org.schema.game.client.data.GameClientState;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 14.01.2022
 * TIME: 17:17
 */
class VoidShieldEvents implements IStrongpointEvent, IStrongholdEvent {
    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {
        if (GameClientState.instance != null) {
            int pFaction = GameClientState.instance.getPlayer().getFactionId();
            boolean sameF = pFaction==h.getOwner();
            boolean inStronghold = StrongholdController.getInstance().getStrongholdFromSector(GameClientState.instance.getPlayer().getCurrentSector()).equals(h);
            boolean shieldGotActive = h.getDefensePoints()>=VoidShield.getRequiredPointsForShield() && VoidShield.getRequiredPointsForShield()>newPoints;
            boolean shieldGotInactive = h.getDefensePoints()<VoidShield.getRequiredPointsForShield() && VoidShield.getRequiredPointsForShield()<= newPoints;
            if (sameF && inStronghold && shieldGotActive) {
                SoundManager.instance.queueSound(SoundManager.Sound.voidshield_acivated);
            }
            if (sameF && inStronghold && shieldGotInactive) {
                SoundManager.instance.queueSound(SoundManager.Sound.voidshield_deactivated);
            }
        }
    }

    @Override
    public void onStrongholdOwnerChanged(Stronghold h, int newOwner) {

    }

    @Override
    public void onStrongholdBalanceChanged(Stronghold h, int newBalance) {

    }

    @Override
    public void onStrongpointOwnerChanged(Strongpoint p, int newOwner) {

    }
}
