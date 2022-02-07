package me.iron.stronghold.mod.framework;

import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.util.*;

public class AreaManager extends AbstractControllableArea {
    private boolean client;
    private boolean server;
    private ChunkManager chunkManager = new ChunkManager();
    private AbstractAreaContainer container = new AbstractAreaContainer();
    private HashMap<Long,SendableUpdateable> UID_to_object = new HashMap<>();
    protected AreaManager(boolean isServer, boolean isClient) { //is a singelton (hopefully)
        super("AbstractAreaManager");
        UID_to_object.put(getUID(),this);
        server = isServer;
        client = isClient;
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    public void update(Timer timer) {
        updateLoaded(timer);
        //collect all children that want to be synched.
        if (container.isEmpty())
            return;
        broadcast("synching server->client");
        UpdatePacket p = new UpdatePacket();
        p.addContainer(container);
        testMain.simulateNetwork(p);
    }

    public void updateAll(Timer timer) {
        for (SendableUpdateable c: children)
            c.update(timer);
    }

    /**
     * will update any direct children (highest level areas) that a player is inside of.
     * @param timer
     */
    public void updateLoaded(Timer timer) {
        if (GameServerState.instance != null) {
            //collect all loaded areas => areas that players are currently in.
            HashSet<SendableUpdateable> loadedAreas = new HashSet<>(GameServerState.instance.getPlayerStatesByName().size()*4);
            for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
                loadedAreas.addAll(chunkManager.getAreasFromSector(p.getCurrentSector()));
            }
            for (SendableUpdateable child: getChildren()) {
                if (loadedAreas.contains(child)) {
                    child.update(timer);
                }
            }
        }

    }

    @Override
    public void requestSynchToClient(SendableUpdateable area) {
        super.requestSynchToClient(area);
        if (client)
            return;
        broadcast("[manager]area " + area.getName() +" request synch");
        container.addForSynch(area);
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {
        super.onChildChanged(parent, child, removed);
        if (client)
            broadcast("child changed on client: class="+child+" name='"+child.getName() +"' was "+(removed?"removed":"added"));

        if (removed){
            UID_to_object.remove(child.getUID());
            if (server && !client)
                container.addForDeletion(child);
        } else {
            //child was added
            UID_to_object.put(child.getUID(), child);
            if (server && !client) {
            //    System.out.println("child added, make a chain: " + child.getName());
                //request that clientmanager instantiates an empty child and creates the parent->child and child->parent connections.
                //collect parent chain from child to manager and the parents classes
                LinkedList<SendableUpdateable> chain = new LinkedList<>();
                SendableUpdateable iterator = child;
                while (iterator.getParent() != null) {
                    chain.add(iterator);
                   //  System.out.print(iterator.getName()+">>");
                    iterator = iterator.getParent();
                }
                //System.out.println(""+iterator.getName());
                chain.add(iterator);
                Collections.reverse(chain); //manager->...->child
                assert iterator.equals(this); //all areas MUST be children of the manager.
                container.addChainForInstantiation(chain);
            }

        }
    }

    protected void removeObject(long UID) {
        SendableUpdateable obj = UID_to_object.get(UID);
        if (obj != null) {
            SendableUpdateable a = obj.getParent();
            if (a instanceof AbstractControllableArea) {
                ((AbstractControllableArea)a).removeChildObject(obj);
            }
            obj.destroy();
        }
    }

    protected void updateObject(SendableUpdateable area) {
        long UID = area.getUID();
        SendableUpdateable target= UID_to_object.get(UID);
        if (target != null) {
            target.updateFromObject(area);
            broadcast("updating object "+target.getName());

        } else {
            System.err.println("area "+area.getName()+"("+area.getUID()+") has no local counterpart. cant update.");
        }
    }

    //will refuse to add any areas that it doesnt know the parent of.
    protected void instantiateArea(AbstractAreaContainer.DummyArea dummy,@Nullable Long parent) {
        if (!client)
            return;
        long UID = dummy.UID;
        String className = dummy.className;
        broadcast("instantiate area: UID=" + UID +" ,class="+ className );
        if (parent == null && className.equals(getClass().getName())) {
            UID_to_object.put(UID, this);
            //broadcast("no parent, recurse");
            for (AbstractAreaContainer.DummyArea child: dummy.children)
                instantiateArea(child, UID);
            return;
        }
        //manager->parent->...->parent->child/leaf
        //UID is unknown, parent UID is known.
        if (!UID_to_object.containsKey(UID) && UID_to_object.containsKey(parent)) {
            try {
                Class<?> clazz = Class.forName(className);
                Object o = clazz.newInstance();
                if (o instanceof SendableUpdateable) {
                    AbstractControllableArea parentObj = (AbstractControllableArea)UID_to_object.get(parent);
                    SendableUpdateable child = (SendableUpdateable) o;
                    child.setUID(UID);
                    //broadcast("instantiating child type " + className + " to parent type " + parentObj.getClass().getName());
                    //link parent and child
                    parentObj.addChildObject(child);

                    UID_to_object.put(UID,child);
                    //recurse
                    for (AbstractAreaContainer.DummyArea childX: dummy.children) {
                        instantiateArea(childX, UID);
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String mssg) {
    //    System.out.println("[Manager]"+(client?"[client]":"")+(server?"[server]":"")+mssg);
    }
}
