package me.iron.stronghold.mod.events;
import me.iron.stronghold.mod.framework.Strongpoint;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 12.01.2022
 * TIME: 18:11
 * event for a change in the strongpoint: captured, lost, contested.
 */
public class StrongpointOwnerChangedEvent extends GenericEvent {
    private Strongpoint strongpoint;
    private int newOwner;
    public StrongpointOwnerChangedEvent(Strongpoint p, int newOwner) {
        this.strongpoint = p;
        this.newOwner = newOwner;
    }

    public Strongpoint getStrongpoint() {
        return strongpoint;
    }

    public int getNewOwner() {
        return newOwner;
    }
}
