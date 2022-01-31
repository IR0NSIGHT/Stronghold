package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

public class AbstractAreaContainer extends SimpleSerializerWrapper implements Serializable {
    transient public LinkedList<AbstractControllableArea> objects = new LinkedList<>();
    public AbstractAreaContainer() {}

    public void addAreas(Collection<AbstractControllableArea> areas) {
        this.objects.addAll(areas);
    }

    @Override
    public void onDeserialize(PacketReadBuffer b) {
        try {
            objects = new LinkedList<>();
            int size = b.readInt();
            for (int i = 0; i < size; i++) {
                String className = b.readString();
                Class<?> c = Class.forName(className);
                Object o = b.readObject(c);
                objects.add((AbstractControllableArea) o);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSerialize(PacketWriteBuffer b) {
        try {
            b.writeInt(objects.size());
            for (AbstractControllableArea o: objects) {
                //make dummy object, fill with values
                AbstractControllableArea dummy = o.getClass().newInstance();
                dummy.updateFromObject(o); //write values we want

                b.writeString(dummy.getClass().getName());
                b.writeObject(dummy);
            }
        } catch (IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
