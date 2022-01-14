package me.iron.stronghold.mod.events;

import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.Strongpoint;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 14.01.2022
 * TIME: 16:46
 */
public interface IStrongholdEvent {
    void onDefensepointsChanged(Stronghold h, int newPoints);
    void onStrongholdOwnerChanged(Stronghold h, int newOwner);
}
