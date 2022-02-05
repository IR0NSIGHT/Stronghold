package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.implementation.SectorArea;
import me.iron.stronghold.mod.implementation.SystemArea;
import me.iron.stronghold.mod.implementation.VoidShield;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.*;
import java.util.LinkedList;

public class testMain {
    public static AreaManager server;
    public static AreaManager client;
    public static void main(String[] args) throws IOException {
        server = new AreaManager(true, false);
        client = new AreaManager(false, true);

        GenericNewsCollector g = new GenericNewsCollector("[ServerNews]");
        server.addListener(g);
        GenericNewsCollector gC = new GenericNewsCollector("[ClientNews]");
        client.addListener(gC);

        SystemArea myHold = new SystemArea("Installation 05");
        server.addChildObject(myHold);
        for (int i = 0; i < 4; i++) {
            SectorArea x = new SectorArea("SectorArea_"+i);
            myHold.addChildObject(x);
        }
        VoidShield v = new VoidShield();
        myHold.addChildObject(v);
        myHold.setOwnerFaction(-1);

        Timer t = new Timer();
        t.lastUpdate = 0;
        t.currentTime = 1000*60*60*1; //5h
        server.update(t);
        t.currentTime++;
        server.update(t);
        v.setActive(false);
        server.update(t);
        server.removeObject(v.getUID());
        t.currentTime++;
        server.update(t);
        SectorArea child = (SectorArea) myHold.children.get(0);
        myHold.removeChildArea(child);
        child.setOwnerFaction(10001);
        assert !myHold.children.contains(child);
        t.currentTime++;
        server.update(t);
        child.setOwnerFaction(-1);
        t.currentTime++;
        server.update(t);
    }

    public static void simulateNetwork(Packet object) {
        try {
            ByteArrayOutputStream bArray = new ByteArrayOutputStream();
            PacketWriteBuffer buf = new PacketWriteBuffer(new DataOutputStream(bArray));
            object.writePacketData(buf);
            //object is written to buffer
            //System.out.println("barray='"+Arrays.toString(bArray.toByteArray())+"'");
            ByteArrayInputStream inputStream = new ByteArrayInputStream( bArray.toByteArray());
            PacketReadBuffer rb = new PacketReadBuffer(new DataInputStream(inputStream));
            Packet receiver = object.getClass().newInstance();
            receiver.readPacketData(rb);
            receiver.processPacketOnClient();
        } catch (InstantiationException|IOException|IllegalAccessException e) {
            e.printStackTrace();
        }
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
