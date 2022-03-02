package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.Vector;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 19:01
 */
public class ControlPointArea extends StellarControllableArea {
    public ControlPointArea() {
        super();
    }

    public ControlPointArea(Vector3i sector, int index) {
        super(sector,sector,getNameFromIndex(index));
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (isServer() && GameServerState.instance.getUniverse().isSectorLoaded(getDimensionsStart())) {
            updateLoaded(timer);
        }
    }

    private void updateLoaded(Timer timer) {
        try {
            GameServerState.instance.getUniverse().getSector(getDimensionsStart()).getEntities();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getNameFromIndex(int index) {
        switch (index) {
            case 0: return "Alpha";
            case 1: return "Bravo";
            case 2: return "Charlie";
            case 3: return "Delta";
            case 4: return "Echo";
            case 5: return "Foxtrott";
            case 6: return "Gamma";
            default: return "Zulu";
        }
    }
}
