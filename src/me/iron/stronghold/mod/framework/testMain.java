package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.ModMain;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;

public class testMain {
    public static AreaManager server;
    public static AreaManager client;
    public static void main(String[] args) throws IOException {
     //  new ModMain().onEnable();
     //  new ModMain().onServerCreated(null);
     //  new ModMain().onClientCreated(null);
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
