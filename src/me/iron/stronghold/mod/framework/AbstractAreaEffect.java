package me.iron.stronghold.mod.framework;

import me.iron.stronghold.mod.ModMain;
import org.schema.schine.graphicsengine.core.Timer;

public abstract class AbstractAreaEffect extends SendableUpdateable{
    public AbstractAreaEffect(){
        super();
    }//for deserializing
    public AbstractAreaEffect(String name) {
        super(AreaManager.getNextID(), name);
    }

    public void update(Timer timer) {
        assert getParent() != null;
        super.update(timer);
    }


}
