package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class AbstractAreaContainer extends SimpleSerializerWrapper {
    transient private LinkedList<SendableUpdateable> updateObjects = new LinkedList<>();
    transient private DummyArea newObjectTree;
    public AbstractAreaContainer() {
        updateObjects = new LinkedList<>();
    }
    public boolean isEmpty() {
        return updateObjects.isEmpty() && newObjectTree == null;
    }
    public void addForSynch(AbstractControllableArea a) {
        updateObjects.add(a);
    }

    public void addChainForInstantiation(LinkedList<SendableUpdateable> chain) {
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
                //System.out.println("added " +child.className+ " to parent "+ parent.className);
            }
            parent = child;
        }

    }

    public DummyArea getTree() {
        return newObjectTree;
    }

    /**
     * iterator for accessing the received objects
     * @return
     */
    public Iterator<SendableUpdateable> getSynchObjectIterator() {
        return updateObjects.iterator();
    }

    @Override
    public void onDeserialize(PacketReadBuffer b) {
        try {
            updateObjects = new LinkedList<>();
            if (b.readBoolean())
                newObjectTree = b.readObject(DummyArea.class);
            String control = b.readString();
            assert control.equals("stop");

            int size = b.readInt();
            for (int i = 0; i < size; i++) {
                String className = b.readString();
                Class<?> c = Class.forName(className);
                Object o = b.readObject(c);
                updateObjects.add((SendableUpdateable) o);
            }


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSerialize(PacketWriteBuffer b) {
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
                b.writeObject(dummy);
            }

            updateObjects.clear();
            newObjectTree = null;

        } catch (IOException | InstantiationException | IllegalAccessException e) {
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
    }
}
