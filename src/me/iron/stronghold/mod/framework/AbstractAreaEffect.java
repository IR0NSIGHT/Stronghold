package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

public abstract class AbstractAreaEffect extends SendableUpdateable{
    public AbstractAreaEffect(){
        super();
    }//for deserializing
    public AbstractAreaEffect(String name) {
        super(AreaManager.getNextUID(), name);
        AbstractControllableArea.dlog(" NEXT UID " + getUID()+ " FOR "+ this.toString());

    }

    public void update(Timer timer) {
        assert getParent() != null;
        super.update(timer);
    }


}
