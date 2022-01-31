package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.implementation.GenericNewsCollector;
import me.iron.stronghold.mod.implementation.SectorArea;
import me.iron.stronghold.mod.implementation.SystemArea;
import me.iron.stronghold.mod.implementation.VoidShield;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class testMain {
    static long lastUID;
    private static long getNextUID() {
        return lastUID++;
    }
    public static void main(String[] args) throws IOException {
        AreaManager m = new AreaManager(true, false);

        GenericNewsCollector g = new GenericNewsCollector();
        m.addListener(g);

        SystemArea myHold = new SystemArea(getNextUID(),"Installation 05",m);
        m.addChildArea(myHold);
        myHold.addEffect(new VoidShield(getNextUID(),myHold));
        for (int i = 0; i < 4; i++) {
            SectorArea x = new SectorArea(getNextUID(),"SectorArea_"+i, myHold);
            myHold.addChildArea(x);
        }
        Timer t = new Timer();
        t.lastUpdate = 0;
        t.currentTime = 1000*60*60*1; //5h
        m.update(t);
        for (AbstractControllableArea a: myHold.getChildren())
            a.setOwnerFaction(3);
        m.update(t);
        myHold.onAttacked(t,myHold,-1,new Vector3i(0,0,0));
        myHold.onAttacked(t,myHold,-1,new Vector3i(0,0,0));


        //synchSim(m);

    }

    public static <T extends Serializable> void  synchSim(AbstractAreaContainer container) throws IOException {
        AbstractAreaContainer target = new AbstractAreaContainer();
        readWrite(container,target);
        Iterator<AbstractControllableArea> ic = container.objects.iterator();
        Iterator<AbstractControllableArea> it = target.objects.iterator();
        while (ic.hasNext() || it.hasNext()) {
            Serializable o1 = ic.next(), o2 = it.next();
            if (!o1.equals(o2))
                throw new IOException("object equality after sending is hurt");
        }

    }

    public static void readWrite(SimpleSerializerWrapper object, SimpleSerializerWrapper target) throws IOException {
        ByteArrayOutputStream bArray = new ByteArrayOutputStream();
        PacketWriteBuffer buf = new PacketWriteBuffer(new DataOutputStream(bArray));
        object.onSerialize(buf);
        //object is written to buffer
        System.out.println("barray='"+Arrays.toString(bArray.toByteArray())+"'");
        ByteArrayInputStream inputStream = new ByteArrayInputStream( bArray.toByteArray());
        PacketReadBuffer rb = new PacketReadBuffer(new DataInputStream(inputStream));
        target.onDeserialize(rb);
    }


}
class testSer extends SimpleSerializerWrapper {
    LinkedList<Serializable> objects = new LinkedList<>();
    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            int size = buffer.readInt();
            for (int i=0; i<size; i++) {
                String className = buffer.readString();
                System.out.println("try reading class " + className);
                Serializable o =(Serializable) buffer.readObject(Class.forName(className));
                if (o instanceof SimpleSerializerWrapper)
                    ((SimpleSerializerWrapper) o).onDeserialize(buffer);
                objects.add(o);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer packetWriteBuffer) {
        try {
            packetWriteBuffer.writeInt(objects.size());
            for (Serializable o: objects) {
                packetWriteBuffer.writeString(o.getClass().getName());
                packetWriteBuffer.writeObject(o);
                if (o instanceof SimpleSerializerWrapper)
                    ((SimpleSerializerWrapper) o).onSerialize(packetWriteBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
