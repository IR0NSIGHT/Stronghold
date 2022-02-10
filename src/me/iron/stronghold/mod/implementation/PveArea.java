package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

public class PveArea extends StellarControllableArea{
    public PveArea() {
        super();
    };
    public PveArea(Vector3i start, Vector3i end, String name) {
        super(start, end, name);
    }
    private boolean init;

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (!init) {
            init = true;
            for (SendableUpdateable c: getChildren()) {
                if (c instanceof PveShield)
                    return;
            }
            addChildObject(new PveShield());
        }
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }
}
