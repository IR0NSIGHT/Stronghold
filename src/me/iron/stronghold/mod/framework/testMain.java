package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.implementation.SectorArea;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import me.iron.stronghold.mod.implementation.SystemArea;
import me.iron.stronghold.mod.implementation.VoidShield;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.*;
import java.util.LinkedList;
import java.util.Random;

public class testMain {
    public static AreaManager server;
    public static AreaManager client;
    public static void main(String[] args) throws IOException {
        AreaManager server = new AreaManager(true, false);
        AreaManager client = new AreaManager(false, true);

        ChunkManager cm = new ChunkManager();
        server.addListener(cm);

        int amountAreas = 3;
        StellarControllableArea[] arr = new StellarControllableArea[amountAreas];
        Random r = new Random(420);
        int i;
        for (i = 0; i < amountAreas; i++) {
            //random position in system 0..128 with space for one max size area
            Vector3i start = new Vector3i(
                    r.nextInt(16*128-StellarControllableArea.maxDimension.x),
                    r.nextInt(16*128-StellarControllableArea.maxDimension.y),
                    r.nextInt(16*128-StellarControllableArea.maxDimension.z)
            );
            //shift from 0..16 to -8..+7
            start.sub(16*64,16*64,16*64);
            Vector3i end = new Vector3i(start);

            //add dimensions to start -> make end
            end.add(r.nextInt(StellarControllableArea.maxDimension.x),
                    r.nextInt(StellarControllableArea.maxDimension.y),
                    r.nextInt(StellarControllableArea.maxDimension.z));

            start.set(200-i,200-i,200-i);
            end.set(200+2*i,200+2*i,200+2*i);
            //instantiate and add area to cm.
            arr[i]= new StellarControllableArea(start,end,"area_"+i);
            System.out.print("add area, start:"+arr[i].getDimensionsStart()+" end "+arr[i].getDimensionsEnd());
            server.addChildObject(arr[i]);
        }
        cm.printChunks();
        Vector3i myPos = new Vector3i(200,200,200);
        System.out.println("i am in sector " + myPos + ", and these areas:");
        LinkedList<StellarControllableArea> l = cm.getAreasFromSector(myPos);
        for (StellarControllableArea a: l) {
            System.out.println(a.getName());
        }
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
