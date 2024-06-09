package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 12:34
 */
public interface AreaShipMovementEvent {
    void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object);

    void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object);

    void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object);
}
