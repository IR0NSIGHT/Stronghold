package me.iron.stronghold.mod.framework;

import org.lwjgl.Sys;
import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class AreaManager extends AbstractControllableArea {
    private boolean client;
    private boolean server;
    private AbstractAreaContainer container = new AbstractAreaContainer();
    private HashMap<Long,AbstractControllableArea> UID_to_object = new HashMap<>();
    protected AreaManager(boolean isServer, boolean isClient) { //is a singelton (hopefully)
        super(getNextID(), "AbstractAreaManager", null);
        UID_to_object.put(UID,this);
        server = isServer;
        client = isClient;
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    protected void update(Timer timer) {
        super.update(timer); //update all children
        //collect all children that want to be synched.
        if (container.isEmpty())
            return;
        try {
            broadcast("synching server->client");
            testMain.synchSim(container);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void requestSynchToClient(AbstractControllableArea area) {
        super.requestSynchToClient(area);
        if (client)
            return;
        broadcast("[manager]area " + area.getName() +" request synch");
        container.addForSynch(area);
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, AbstractControllableArea child, boolean removed) {
        super.onChildChanged(parent, child, removed);
        if (client)
            broadcast("child changed on client: class="+child+" name='"+child.getName() +"' was "+(removed?"removed":"added"));
        if (removed){
            UID_to_object.remove(child.UID);
        } else {
            //child was added
            UID_to_object.put(child.UID, child);
            if (server && !client) {
                //request that clientmanager instantiates an empty child and creates the parent->child and child->parent connections.
                //collect parent chain from child to manager and the parents classes
                LinkedList<AbstractControllableArea> chain = new LinkedList<>();
                AbstractControllableArea iterator = child;
                while (iterator.parent != null) {
                    chain.add(iterator);
                    iterator = iterator.parent;
                }
                chain.add(iterator);
                Collections.reverse(chain); //manager->...->child
                assert iterator.equals(this); //all areas MUST be children of the manager.
                container.addChainForInstantiation(chain);
            }

        }
    }

    protected void updateArea(AbstractControllableArea area) {
        long UID = area.UID;
        AbstractControllableArea target= UID_to_object.get(UID);
        if (target != null) {
            target.updateFromObject(area);
        } else {
            System.err.println("area "+area.getName()+"("+area.UID+") has no local counterpart. cant update.");
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
                if (o instanceof AbstractControllableArea) {
                    AbstractControllableArea parentObj = UID_to_object.get(parent);
                    AbstractControllableArea child = (AbstractControllableArea)o;
                    child.setUID(UID);
                    //broadcast("instantiating child type " + className + " to parent type " + parentObj.getClass().getName());
                    //link parent and child
                    child.parent = parentObj;
                    parentObj.addChildArea(child);

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
