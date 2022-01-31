package me.iron.stronghold.mod.framework;

import me.iron.stronghold.mod.implementation.GenericNewsCollector;
import me.iron.stronghold.mod.implementation.SectorArea;
import me.iron.stronghold.mod.implementation.SystemArea;
import me.iron.stronghold.mod.implementation.VoidShield;
import org.schema.schine.graphicsengine.core.Timer;

public class testMain {
    public static void main(String[] args) {
        GenericNewsCollector g = new GenericNewsCollector();
        SystemArea myHold = new SystemArea("Installation 05",null);
        myHold.addListener(g);
        myHold.addEffect(new VoidShield(myHold));
        for (int i = 0; i < 4; i++) {
            SectorArea x = new SectorArea("SectorArea_"+i, myHold);
            myHold.addChildArea(x);
        }
        Timer t = new Timer();
        t.lastUpdate = 0;
        t.currentTime = 1000*60*60*1; //5h
        myHold.update(t);
        for (AbstractControllableArea a: myHold.getChildren())
            a.setOwnerFaction(3);
        myHold.update(t);
    }
}
