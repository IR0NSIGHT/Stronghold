package me.iron.stronghold.mod.voidshield;

import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.Strongpoint;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 14.01.2022
 * TIME: 17:17
 */
class VoidShieldInternalHandler implements IStrongpointEvent, IStrongholdEvent {
    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {
        boolean shieldGotActive =h.getDefensePoints()< VoidShieldController.getRequiredPointsForShield() && VoidShieldController.getRequiredPointsForShield()<= newPoints;
        boolean shieldGotInactive = h.getDefensePoints()>= VoidShieldController.getRequiredPointsForShield() && VoidShieldController.getRequiredPointsForShield()>newPoints;
        if (shieldGotActive) {
            for (IVoidShieldEvent e: VoidShieldController.eventlisteners) {
                e.onShieldActivate(h);
            }
        }
        if (shieldGotInactive) {
            for (IVoidShieldEvent e: VoidShieldController.eventlisteners) {
                e.onShieldDeactivate(h);
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
