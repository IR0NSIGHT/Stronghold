package me.iron.stronghold.mod.events;

import me.iron.stronghold.mod.framework.Strongpoint;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 14.01.2022
 * TIME: 16:50
 */
public interface IStrongpointEvent {
    void onStrongpointOwnerChanged(Strongpoint p, int newOwner);
}
