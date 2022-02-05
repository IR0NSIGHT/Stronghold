package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

import java.io.Serializable;
import java.util.Objects;

public abstract class SendableUpdateable implements Serializable {
    private long UID;
    private String name;
    transient private SendableUpdateable parent;
    public SendableUpdateable(){}
    public SendableUpdateable(long UID, String name) {
        this.UID = UID;
        this.name = name;
    }

    protected void update(Timer t){};
    protected long getUID(){
        return UID;
    };
    protected void setUID(long UID){
        this.UID = UID;
    };
    public String getName(){
        return name;
    };

    void setName(String name) {this.name = name;}

    protected void setParent(AbstractControllableArea parent){
        this.parent = parent;
    };

    protected SendableUpdateable getParent(){
        return parent;
    };

    protected void destroy() {
        requestSynchToClient(this);
    }

    protected void updateFromObject(SendableUpdateable origin){
        setUID(origin.getUID());
        setName(origin.getName());
    };

    public void requestSynchToClient(SendableUpdateable area) {
    //    System.out.println("request synch for area "+area.getName()+" at level " +this.getName());
        if (this.getParent() != null )
            getParent().requestSynchToClient(area);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendableUpdateable that = (SendableUpdateable) o;
        return UID == that.UID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(UID);
    }
}
