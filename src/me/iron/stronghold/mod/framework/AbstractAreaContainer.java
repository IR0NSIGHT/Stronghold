package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.lwjgl.Sys;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class AbstractAreaContainer extends SimpleSerializerWrapper {
    transient private LinkedList<SendableUpdateable> updateObjects;
    transient private LinkedList<Long> deleteUIDs = new LinkedList<>();
    transient private DummyArea newObjectTree;
    public AbstractAreaContainer() {
        updateObjects = new LinkedList<>();
    }
    public boolean isEmpty() {
        return updateObjects.isEmpty() && newObjectTree == null;
    }
    public void addForSynch(SendableUpdateable a) {
        //System.out.println("added sendable "+a.getName()+" to container for synch.");
        updateObjects.add(a);
    }

    /** will add sendable to the container listed in -instantiate and -update
     *
     * @param su
     */
    public void addForInstantiation(SendableUpdateable su) {
        assert !(su instanceof AreaManager);
        LinkedList<SendableUpdateable> chain = new LinkedList<>();
        SendableUpdateable iterator = su;
        while (iterator.getParent() != null) {
            chain.add(iterator);
            //  System.out.print(iterator.getName()+">>");
            iterator = iterator.getParent();
        }
        ////System.out.println(""+iterator.getName());
        chain.add(iterator); //add manager
        Collections.reverse(chain); //manager->...->child
        assert (iterator instanceof AreaManager):"root for SU "+su+" is not area manager but "+iterator; //all areas MUST be children of the manager.


        //every chain start with the manager
        //replicate chain structure into the dummy tree
        Iterator<SendableUpdateable> it = chain.iterator();

        SendableUpdateable a = it.next();
        if (newObjectTree == null) {
            assert a instanceof AreaManager:"tree root is not manager";
            newObjectTree = new DummyArea(a.getClass().getName(),a.getUID());
        }
        DummyArea parent = newObjectTree;
        while (it.hasNext()) {
            a = it.next();
            DummyArea child = new DummyArea(a.getClass().getName(), a.getUID());
            boolean exists = false;
            for (DummyArea c1: parent.children) {
                if (c1.equals(child)) {
                    child = c1;
                    exists =true;
                    break;
                }
            }
            if (!exists) {
                parent.children.add(child);
                ////System.out.println("added " +child.className+ " to parent "+ parent.className);
            }
            parent = child;
        }
        addForSynch(su);
    }

    public void addForDeletion(SendableUpdateable obj) {
        AreaManager.dlog("adding UID "+obj.getUID()+"for deletion to container");
        deleteUIDs.add(obj.getUID());
    }
    public DummyArea getTree() {
        return newObjectTree;
    }

    public void clearData() {
        deleteUIDs.clear();
        updateObjects.clear();
        newObjectTree = null;
    }

    /**
     * iterator for accessing the received objects
     * @return
     */
    public Iterator<SendableUpdateable> getSynchObjectIterator() {
        return updateObjects.iterator();
    }

    public Iterator<Long> getDeleteUIDIterator() {
        return deleteUIDs.iterator();
    }
    @Override
    public void onDeserialize(PacketReadBuffer b) {
        try {
            updateObjects = new LinkedList<>();
            deleteUIDs = new LinkedList<>();
            if (b.readBoolean())
                newObjectTree = b.readObject(DummyArea.class);
            String control = b.readString();
            assert control.equals("stop");

            int size = b.readInt();
            for (int i = 0; i < size; i++) {
                String className = b.readString();
                //System.out.println("deserializing " +className);
                Class<?> c = Class.forName(className);
                Object o = b.readObject(c);
                updateObjects.add((SendableUpdateable) o);
            }

            deleteUIDs.addAll(b.readLongList());
            if (!deleteUIDs.isEmpty()) {
                AreaManager.dlog("delete UIDs after deseraialize container:");
            }
            for (long UID: deleteUIDs) {
                AreaManager.dlog(""+UID);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSerialize(PacketWriteBuffer b) {

        //System.out.println("DELETE UIDS: {");
        Iterator<Long> it = deleteUIDs.iterator();
        while (it.hasNext()) {
            //System.out.println("\t"+it.next());
        }
        //System.out.println("}");
        try {
            b.writeBoolean(newObjectTree != null);
            if (newObjectTree != null)
                b.writeObject(newObjectTree);
            b.writeString("stop");

            b.writeInt(updateObjects.size());
            for (SendableUpdateable o: updateObjects) {
                //make dummy object, fill with values
                SendableUpdateable dummy = o.getClass().newInstance();
                dummy.updateFromObject(o); //write values we want

                b.writeString(dummy.getClass().getName());
                //System.out.println("writing object"+ o);
                b.writeObject(dummy);
            }

            b.writeLongList(deleteUIDs);
            AreaManager.dlog("wrote delete UIDs from container:");
            for (long UID: deleteUIDs) {
                AreaManager.dlog(""+UID);
            }
            AreaManager.dlog("------ container serialization done");
        } catch ( IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    class DummyArea implements Serializable {
        DummyArea(String className, long UID) {
            this.className = className;
            this.UID = UID;
        }
        void addChild(DummyArea a) {
            children.add(a);
        }
        LinkedList<DummyArea> children = new LinkedList<>();
        String className;
        long UID;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DummyArea dummyArea = (DummyArea) o;
            return UID == dummyArea.UID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(UID);
        }

        @Override
        public String toString() {
            return "DummyArea{" +
                    "children(amount)=" + children.size() +
                    ", className='" + className + '\'' +
                    ", UID=" + UID +
                    '}';
        }
    }
}
