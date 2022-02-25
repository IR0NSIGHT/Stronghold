package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;

import java.util.Arrays;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 16:18
 */
public class SemiProtectedArea extends StellarControllableArea {
    private transient Integer[] protectedFactions;
    //TODO this is garbage, use a factory instead for construction of special PVE like areas.
    public SemiProtectedArea() {
        super();
    }

    public SemiProtectedArea(Vector3i start, Vector3i end, String name, Integer[] protectedFactions) {
        super(start, end, name);
        this.protectedFactions = protectedFactions;
    }

    @Override
    protected void onFirstUpdatePersistent() {
        super.onFirstUpdatePersistent();

        //create shield that only blocks damage to specific factions.
        SelectiveVoidShield s = new SelectiveVoidShield();
        s.addProtectedFaction(Arrays.asList(protectedFactions));
        addChildObject(new SelectiveVoidShield());
    }
}
