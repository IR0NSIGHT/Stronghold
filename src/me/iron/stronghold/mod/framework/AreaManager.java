package me.iron.stronghold.mod.framework;

import com.google.gson.GsonBuilder;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.LinkedList;

public class AreaManager extends AbstractControllableArea {
    private LinkedList<AbstractControllableArea> synchQueue = new LinkedList<>();

    protected AreaManager(boolean isServer, boolean isClient) { //is a singelton (hopefully)
        super(getNextID(), "AbstractAreaManager", null);
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    protected void update(Timer timer) {
        super.update(timer); //update all children
        //collect all children that want to be synched.
        AbstractAreaContainer ac = new AbstractAreaContainer();
        if (synchQueue.isEmpty())
            return;
        ac.addAreas(synchQueue);
        try {
            testMain.synchSim(ac);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchQueue.clear();
    }

    @Override
    public void requestSynchToClient(AbstractControllableArea area) {
        super.requestSynchToClient(area);
        synchQueue.add(area);
    }
}
