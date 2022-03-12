package me.iron.stronghold.mod.framework;

import me.iron.stronghold.mod.ModMain;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.Serializable;
import java.util.Objects;

/**
 * this class is the highest abstract object that everything derives from. its an object that can be updated, saved, sent and synched between server and client.
 * The most important inherting classes are the AbstractControllableArea and teh AbstractAreaEffect.
 * Create your own SendableUpdateable: add your own fields (they dont have to be serializable) and to have persistent fields, overwrite/extend the updateFromObject method, where you use to origin to copy the new, custom values into your object.
 */
public abstract class SendableUpdateable implements Serializable {
    private long UID;
    private String name;
    private SendableUpdateable parent;
    private transient boolean firstUpdateRuntime = false;
    private boolean firstUpdatePersistent = false;

    public SendableUpdateable(){}
    public SendableUpdateable(long UID, String name) {
        this.UID = UID;
        this.name = name;
    }

    protected void update(Timer t){
        if (!firstUpdatePersistent) {
            firstUpdatePersistent = true;
            onFirstUpdatePersistent();
        }
        if (!firstUpdateRuntime) {
            firstUpdateRuntime = true;
            onFirstUpdateRuntime();
        }
    }

    /**
     * called right before the first ever update after every server restart.
     */
    protected void onFirstUpdateRuntime() {}

    /**
     * called once after initial creation before very fisrt update.
     */
    protected void onFirstUpdatePersistent() {

    }

    public long getUID(){
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
        //AreaManager.dlog("destroy SU "+ this);
        requestSynchToClient(this);
    }

    public void updateFromObject(SendableUpdateable origin) { //yes a wrapper, its necessary.
        synch(origin);//gets overritten by descendendats
    }

    protected boolean isServer() {
        return ModMain.areaManager.isServer();
    }

    protected boolean isClient() {
        return ModMain.areaManager.isClient();
    }

    protected void synch(SendableUpdateable origin){
        setUID(origin.getUID());
        setName(origin.getName());
        firstUpdatePersistent = origin.firstUpdatePersistent;
    };

    public void requestSynchToClient(SendableUpdateable area) {
    //    //System.out.println("request synch for area "+area.getName()+" at level " +this.getName());
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

    @Override
    public String toString() {
        return getClass().getSimpleName() +"{" +
                "UID=" + getUID() +
                ", name='" + getName() + '\''+
                ", parent='"+(getParent()!=null?getParent().getName():"NULL")+"\'"+
                ", p/r="+(firstUpdatePersistent?"T":"F")+"/"+(firstUpdateRuntime?"T":"F");
    }
}
